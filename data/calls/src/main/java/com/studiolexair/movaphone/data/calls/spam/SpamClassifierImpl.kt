package com.studiolexair.movaphone.data.calls.spam

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.core.database.entity.SpamRuleEntity
import com.studiolexair.movaphone.domain.calls.model.SpamVerdict
import com.studiolexair.movaphone.domain.calls.repository.SpamClassifier
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Clasificador local de spam (V1).
 * Reglas reales y explicables: números bloqueados, patrones del usuario y reportes propios.
 * La arquitectura permite añadir después reputación remota o IA implementando esta misma interfaz.
 */
class SpamClassifierImpl(
    private val securityDao: SecurityDao,
    private val now: () -> Long = { System.currentTimeMillis() }
) : SpamClassifier {

    override suspend fun classify(number: String): SpamVerdict {
        val normalized = PhoneNumbers.normalize(number)
        if (normalized.isEmpty()) return SpamVerdict.NOT_SPAM

        securityDao.isBlocked(normalized)?.let {
            return SpamVerdict(true, 100, "número bloqueado por el usuario")
        }

        val rules = securityDao.allSpamRules()
        var score = 0
        val reasons = mutableListOf<String>()
        rules.forEach { rule ->
            val match = when (rule.type) {
                "PREFIX" -> normalized.startsWith(PhoneNumbers.digitsOnly(rule.pattern))
                "CONTAINS" -> normalized.contains(PhoneNumbers.digitsOnly(rule.pattern))
                "EXACT" -> normalized == PhoneNumbers.normalize(rule.pattern)
                "USER_REPORT" -> normalized == PhoneNumbers.normalize(rule.pattern)
                else -> false
            }
            if (match) {
                score += rule.weight
                reasons += rule.type.lowercase()
            }
        }

        // Patrones locales de referencia (documentados, sin llamadas externas).
        if (isSuspiciousPattern(normalized)) {
            score += 2
            reasons += "patrón sospechoso"
        }

        return if (score >= THRESHOLD) {
            SpamVerdict(true, score, reasons.joinToString(", "))
        } else {
            SpamVerdict(false, score, reasons.joinToString(", ").ifEmpty { "sin coincidencias" })
        }
    }

    override suspend fun reportSpam(number: String, category: String) {
        val normalized = PhoneNumbers.normalize(number)
        if (normalized.isEmpty()) return
        securityDao.addSpamRule(
            SpamRuleEntity(
                pattern = normalized,
                type = if (category == "user_report") "USER_REPORT" else category.uppercase(),
                weight = 3,
                source = "user",
                createdAt = now()
            )
        )
        MovaLog.i(TAG, "Número reportado como spam (local)")
    }

    /** Heurística local: secuencias repetidas o incrementales típicas de centralita. */
    private fun isSuspiciousPattern(normalized: String): Boolean {
        if (normalized.length < 7) return false
        val digits = normalized.takeLast(9)
        val repeated = digits.windowed(3).any { it[0] == it[1] && it[1] == it[2] }
        val sequential = digits.windowed(3).any {
            it[1] == it[0] + 1 && it[2] == it[1] + 1
        }
        return repeated || sequential
    }

    private companion object {
        const val THRESHOLD = 2
        const val TAG = "SpamClassifier"
    }
}
