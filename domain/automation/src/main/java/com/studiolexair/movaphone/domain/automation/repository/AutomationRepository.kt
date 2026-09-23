package com.studiolexair.movaphone.domain.automation.repository

import com.studiolexair.movaphone.domain.automation.model.AutomationOutcome
import com.studiolexair.movaphone.domain.automation.model.AutomationRule
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import kotlinx.coroutines.flow.Flow

interface AutomationRepository {
    fun observeRules(): Flow<List<AutomationRule>>
    suspend fun rules(): List<AutomationRule>
    suspend fun rulesForTrigger(type: TriggerType): List<AutomationRule>
    suspend fun rule(id: Long): AutomationRule?
    suspend fun save(rule: AutomationRule): Long
    suspend fun delete(rule: AutomationRule)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun markExecuted(id: Long, timestamp: Long)

    fun observeHistory(limit: Int = 50): Flow<List<AutomationOutcome>>
    suspend fun logOutcome(outcome: AutomationOutcome, timestamp: Long)
    suspend fun purgeHistory(before: Long)
}

/** Motor de automatización: recibe un disparador y ejecuta las reglas que correspondan. */
interface AutomationEngine {
    /** Evalúa todas las reglas asociadas al disparador. */
    suspend fun onTrigger(type: TriggerType, payload: TriggerPayload = TriggerPayload.EMPTY): List<AutomationOutcome>
    /** Ejecuta una regla concreta (por ejemplo desde la UI: "probar regla"). */
    suspend fun runRule(ruleId: Long): AutomationOutcome
}

/** Datos que acompañan a un disparador (número entrante, nivel de batería, hora...). */
data class TriggerPayload(
    val number: String? = null,
    val contactName: String? = null,
    val message: String? = null,
    val batteryPercent: Int? = null,
    val hourOfDay: Int? = null
) {
    companion object {
        val EMPTY = TriggerPayload()
    }
}
