package com.studiolexair.movaphone.feature.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.data.automation.repository.AutomationRepositoryImpl
import com.studiolexair.movaphone.domain.automation.model.Action
import com.studiolexair.movaphone.domain.automation.model.ActionType
import com.studiolexair.movaphone.domain.automation.model.AutomationOutcome
import com.studiolexair.movaphone.domain.automation.model.AutomationRule
import com.studiolexair.movaphone.domain.automation.model.Condition
import com.studiolexair.movaphone.domain.automation.model.ConditionType
import com.studiolexair.movaphone.domain.automation.model.Trigger
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import com.studiolexair.movaphone.domain.automation.usecase.AutomationTemplatesUseCase
import com.studiolexair.movaphone.domain.automation.usecase.SaveAutomationRuleUseCase
import com.studiolexair.movaphone.domain.automation.usecase.ToggleAutomationRuleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Automatizaciones: reglas SI → ENTONCES con motor real (requisitos 18 y 19). */
class AutomationViewModel(
    private val repository: AutomationRepositoryImpl,
    private val engine: AutomationEngine,
    private val saveRule: SaveAutomationRuleUseCase,
    private val toggleRule: ToggleAutomationRuleUseCase,
    private val templates: AutomationTemplatesUseCase
) : ViewModel() {

    private val messageState = MutableStateFlow<String?>(null)

    val rules: StateFlow<List<AutomationRule>> = repository.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<AutomationOutcome>> = repository.observeHistory(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val statusMessage: StateFlow<String?> = messageState.asStateFlow()

    fun availableTemplates(): List<AutomationRule> = templates()

    fun availableTriggers(): List<TriggerType> = TriggerType.values().toList()

    fun availableConditions(): List<ConditionType> = ConditionType.values().toList()

    fun availableActions(): List<ActionType> = ActionType.values().toList()

    fun save(
        ruleId: Long,
        name: String,
        trigger: TriggerType,
        triggerValue: String?,
        condition: ConditionType,
        conditionValue: String?,
        action: ActionType,
        actionValue: String?
    ) {
        viewModelScope.launch {
            val rule = AutomationRule(
                id = ruleId,
                name = name,
                enabled = true,
                trigger = Trigger(trigger, triggerValue?.ifBlank { null }),
                condition = Condition(condition, conditionValue?.ifBlank { null }),
                action = Action(action, actionValue?.ifBlank { null })
            )
            messageState.value = when (val result = saveRule(rule)) {
                is MovaResult.Success -> "Automatización guardada"
                is MovaResult.Failure -> result.error.userMessage
            }
        }
    }

    fun toggle(rule: AutomationRule, enabled: Boolean) {
        viewModelScope.launch { toggleRule(rule.id, enabled) }
    }

    fun delete(rule: AutomationRule) {
        viewModelScope.launch {
            repository.delete(rule)
            messageState.value = "Automatización eliminada"
        }
    }

    /** Ejecuta la regla ahora mismo para que el usuario compruebe qué ocurre. */
    fun runNow(rule: AutomationRule) {
        viewModelScope.launch {
            val outcome = engine.runRule(rule.id)
            messageState.value = when {
                outcome.success -> "Regla ejecutada: ${outcome.detail.orEmpty()}"
                else -> "No se pudo ejecutar: ${outcome.detail.orEmpty()}"
            }
        }
    }

    fun clearMessage() {
        messageState.value = null
    }

    companion object {
        fun factory(
            repository: AutomationRepositoryImpl,
            engine: AutomationEngine,
            saveRule: SaveAutomationRuleUseCase,
            toggleRule: ToggleAutomationRuleUseCase,
            templates: AutomationTemplatesUseCase
        ) = viewModelFactory {
            initializer { AutomationViewModel(repository, engine, saveRule, toggleRule, templates) }
        }
    }
}
