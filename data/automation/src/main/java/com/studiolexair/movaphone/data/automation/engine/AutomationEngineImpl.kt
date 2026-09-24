package com.studiolexair.movaphone.data.automation.engine

import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.automation.model.AutomationOutcome
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.AutomationClock
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import com.studiolexair.movaphone.domain.automation.repository.AutomationRepository
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload

/**
 * Motor de automatización (requisito 18 y 19).
 *
 * Flujo: Trigger → Condition → Action → Historial.
 * El motor no conoce detalles de Android: recibe disparadores y delega en el ejecutor,
 * por lo que añadir nuevos disparadores/acciones no rompe lo existente.
 */
class AutomationEngineImpl(
    private val repository: AutomationRepository,
    private val conditionEvaluator: AutomationConditionEvaluator,
    private val actionExecutor: AutomationActionExecutor,
    private val clock: AutomationClock,
    private val enabled: () -> Boolean = { true }
) : AutomationEngine {

    override suspend fun onTrigger(type: TriggerType, payload: TriggerPayload): List<AutomationOutcome> {
        if (!enabled()) {
            MovaLog.i(TAG, "Automatizaciones desactivadas: se ignora $type")
            return emptyList()
        }
        val rules = repository.rulesForTrigger(type)
            .filter { it.enabled && matchesTriggerValue(it.trigger.value, payload) }
        if (rules.isEmpty()) return emptyList()

        val outcomes = rules.map { rule ->
            if (!conditionEvaluator.evaluate(rule.condition, payload)) {
                AutomationOutcome(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    executed = false,
                    detail = "Condición no cumplida"
                )
            } else {
                val result = actionExecutor.execute(rule.action, payload)
                repository.markExecuted(rule.id, clock.now())
                AutomationOutcome(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    executed = true,
                    success = result.success,
                    detail = result.detail
                )
            }
        }

        outcomes.forEach { repository.logOutcome(it, clock.now()) }
        MovaLog.i(TAG, "Disparador $type evaluado: ${outcomes.count { it.executed }} reglas ejecutadas")
        return outcomes
    }

    override suspend fun runRule(ruleId: Long): AutomationOutcome {
        val rule = repository.rule(ruleId)
            ?: return AutomationOutcome(ruleId, "desconocida", executed = false, detail = "La regla no existe")
        val result = actionExecutor.execute(rule.action, TriggerPayload.EMPTY)
        repository.markExecuted(rule.id, clock.now())
        val outcome = AutomationOutcome(rule.id, rule.name, executed = true, success = result.success, detail = result.detail)
        repository.logOutcome(outcome, clock.now())
        return outcome
    }

    /**
     * Los disparadores de lugar llevan el nombre del lugar («casa», «trabajo»…).
     * Si la regla escribe un lugar concreto, sólo se ejecuta al entrar o salir de ESE lugar;
     * si no escribe nada, se ejecuta con cualquier lugar guardado.
     */
    private fun matchesTriggerValue(value: String?, payload: TriggerPayload): Boolean {
        if (value.isNullOrBlank() || payload.contactName.isNullOrBlank()) return true
        return payload.contactName.equals(value.trim(), ignoreCase = true)
    }

    private companion object {
        const val TAG = "AutomationEngine"
    }
}
