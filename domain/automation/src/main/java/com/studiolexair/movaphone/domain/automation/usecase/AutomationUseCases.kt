package com.studiolexair.movaphone.domain.automation.usecase

import com.studiolexair.movaphone.core.common.result.ErrorCode
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.core.common.result.failure
import com.studiolexair.movaphone.domain.automation.model.Action
import com.studiolexair.movaphone.domain.automation.model.ActionType
import com.studiolexair.movaphone.domain.automation.model.AutomationRule
import com.studiolexair.movaphone.domain.automation.model.AutomationTemplateCatalog
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import com.studiolexair.movaphone.domain.automation.repository.AutomationRepository
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload

class GetAutomationRulesUseCase(private val repository: AutomationRepository) {
    operator fun invoke() = repository.observeRules()
}

class GetAutomationHistoryUseCase(private val repository: AutomationRepository) {
    operator fun invoke(limit: Int = 50) = repository.observeHistory(limit)
}

/** Validación de reglas: evita guardar automatizaciones incompletas. */
class ValidateAutomationRuleUseCase {
    operator fun invoke(rule: AutomationRule): MovaResult<Unit> {
        if (rule.name.isBlank()) return failure(ErrorCode.VALIDATION, "Ponle un nombre a la regla.")
        val requiresValue = rule.action.type in setOf(
            ActionType.SEND_SMS,
            ActionType.CALL_CONTACT,
            ActionType.NOTIFY
        )
        if (requiresValue && rule.action.value.isNullOrBlank()) {
            return failure(ErrorCode.VALIDATION, "Esta acción necesita un destinatario o un texto.")
        }
        return MovaResult.Success(Unit)
    }
}

class SaveAutomationRuleUseCase(
    private val repository: AutomationRepository,
    private val validate: ValidateAutomationRuleUseCase
) {
    suspend operator fun invoke(rule: AutomationRule): MovaResult<Long> {
        val validation = validate(rule)
        if (validation is MovaResult.Failure) {
            return MovaResult.Failure(validation.error)
        }
        return try {
            MovaResult.Success(repository.save(rule))
        } catch (t: Throwable) {
            failure(ErrorCode.UNKNOWN, "No fue posible guardar la automatización.", t.message)
        }
    }
}

class DeleteAutomationRuleUseCase(private val repository: AutomationRepository) {
    suspend operator fun invoke(rule: AutomationRule): MovaResult<Unit> = try {
        repository.delete(rule)
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible eliminar la automatización.", t.message)
    }
}

class ToggleAutomationRuleUseCase(private val repository: AutomationRepository) {
    suspend operator fun invoke(id: Long, enabled: Boolean): MovaResult<Unit> = try {
        repository.setEnabled(id, enabled)
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible cambiar el estado de la regla.", t.message)
    }
}

/** Punto único de entrada para disparadores del sistema (SOS, SMS, batería, conducción...). */
class DispatchAutomationTriggerUseCase(private val engine: AutomationEngine) {
    suspend operator fun invoke(
        type: com.studiolexair.movaphone.domain.automation.model.TriggerType,
        payload: TriggerPayload = TriggerPayload.EMPTY
    ) = engine.onTrigger(type, payload)
}

/** Plantillas listas para usar (el usuario puede editarlas antes de guardarlas). */
class AutomationTemplatesUseCase {
    operator fun invoke(): List<AutomationRule> = AutomationTemplateCatalog.templates
}

/** Acciones disponibles para el editor de reglas. */
class AutomationActionsUseCase {
    operator fun invoke(): List<Action> = ActionType.values().map { Action(it) }
}
