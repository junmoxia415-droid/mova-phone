package com.studiolexair.movaphone.core.security.event

import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.core.database.entity.SecurityEventEntity
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Registro de eventos de seguridad (requisitos 16 y 17).
 * Todo evento sensible queda auditable localmente; el usuario puede consultarlo y borrarlo.
 */
class SecurityEventLogger(private val securityDao: SecurityDao) {

    suspend fun log(
        type: SecurityEventType,
        description: String,
        severity: Severity = Severity.INFO,
        metadata: String? = null
    ) {
        try {
            securityDao.logEvent(
                SecurityEventEntity(
                    type = type.name,
                    description = description,
                    severity = severity.value,
                    metadata = metadata,
                    occurredAt = System.currentTimeMillis()
                )
            )
        } catch (t: Throwable) {
            MovaLog.e(TAG, "No fue posible registrar el evento de seguridad", t)
        }
    }

    fun observeEvents(limit: Int = 200) = securityDao.observeEvents(limit)

    suspend fun clear() = securityDao.clearEvents()

    private companion object {
        const val TAG = "SecurityEventLogger"
    }
}

enum class SecurityEventType {
    BLOCKED_CALL, SPAM_DETECTED, SOS_TRIGGERED, SOS_CANCELLED, PIN_CHANGED, PIN_FAILED,
    BIOMETRIC_ENABLED, BIOMETRIC_FAILED, APP_UNLOCKED, NUMBER_BLOCKED, CONTACT_MARKED_PRIVATE,
    AUTOMATION_EXECUTED, LOCATION_SHARED, PRIVATE_MODE_TOGGLED
}

enum class Severity(val value: String) {
    INFO("info"), WARNING("warning"), CRITICAL("critical")
}
