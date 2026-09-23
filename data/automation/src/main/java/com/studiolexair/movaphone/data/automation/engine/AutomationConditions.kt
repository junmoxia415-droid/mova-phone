package com.studiolexair.movaphone.data.automation.engine

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.domain.automation.model.Condition
import com.studiolexair.movaphone.domain.automation.model.ConditionType
import com.studiolexair.movaphone.domain.automation.repository.AutomationClock
import com.studiolexair.movaphone.domain.automation.repository.DrivingModeController
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload

/**
 * Evaluador de condiciones. Cada condición es independiente y testeable:
 * añadir una nueva sólo requiere un caso más en el `when`.
 */
class AutomationConditionEvaluator(
    private val clock: AutomationClock,
    private val drivingMode: DrivingModeController
) {

    fun evaluate(condition: Condition?, payload: TriggerPayload): Boolean {
        if (condition == null) return true
        return when (condition.type) {
            ConditionType.ALWAYS -> true
            ConditionType.CONTACT_IS ->
                payload.contactName?.equals(condition.value, ignoreCase = true) == true
            ConditionType.NUMBER_IS ->
                PhoneNumbers.sameNumber(payload.number, condition.value)
            ConditionType.BATTERY_BELOW -> {
                val limit = condition.value?.toIntOrNull() ?: 15
                (payload.batteryPercent ?: 100) < limit
            }
            ConditionType.BETWEEN_HOURS -> {
                val range = condition.value?.split("-") ?: return false
                if (range.size != 2) return false
                val from = range[0].trim().toIntOrNull() ?: return false
                val to = range[1].trim().toIntOrNull() ?: return false
                val hour = payload.hourOfDay ?: clock.hourOfDay()
                if (from <= to) hour in from..to else hour >= from || hour <= to
            }
            ConditionType.IS_DRIVING -> drivingMode.isDrivingModeActive()
            ConditionType.IS_SILENT -> false // se resuelve en services:notifications (AudioManager)
        }
    }
}
