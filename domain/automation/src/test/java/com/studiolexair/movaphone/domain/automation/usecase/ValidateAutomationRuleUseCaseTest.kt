package com.studiolexair.movaphone.domain.automation.usecase

import com.google.common.truth.Truth.assertThat
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.domain.automation.model.Action
import com.studiolexair.movaphone.domain.automation.model.ActionType
import com.studiolexair.movaphone.domain.automation.model.AutomationRule
import com.studiolexair.movaphone.domain.automation.model.Trigger
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import org.junit.Test

/** Una automatización mal formada nunca debe guardarse. */
class ValidateAutomationRuleUseCaseTest {

    private val validate = ValidateAutomationRuleUseCase()

    @Test
    fun `rechaza reglas sin nombre`() {
        val resultado = validate(
            AutomationRule(name = "  ", trigger = Trigger(TriggerType.SOS_ACTIVATED), action = Action(ActionType.ENABLE_SOS))
        )
        assertThat(resultado).isInstanceOf(MovaResult.Failure::class.java)
        assertThat((resultado as MovaResult.Failure).error.userMessage).contains("nombre")
    }

    @Test
    fun `rechaza acciones que necesitan destinatario sin valor`() {
        val resultado = validate(
            AutomationRule(
                name = "Avisar a mamá",
                trigger = Trigger(TriggerType.SOS_ACTIVATED),
                action = Action(ActionType.SEND_SMS)
            )
        )
        assertThat(resultado).isInstanceOf(MovaResult.Failure::class.java)
        assertThat((resultado as MovaResult.Failure).error.userMessage).contains("destinatario")
    }

    @Test
    fun `acepta una regla completa`() {
        val resultado = validate(
            AutomationRule(
                name = "Si activo SOS, enviar ubicación",
                trigger = Trigger(TriggerType.SOS_ACTIVATED),
                action = Action(ActionType.SHARE_LOCATION, value = "600123456")
            )
        )
        assertThat(resultado).isInstanceOf(MovaResult.Success::class.java)
    }
}
