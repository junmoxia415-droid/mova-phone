package com.studiolexair.movaphone.domain.automation.model

/** Catálogo de plantillas de automatización listas para usar. */
object AutomationTemplateCatalog {

    val templates: List<AutomationRule> = listOf(
        AutomationRule(
            name = "Si activo SOS",
            trigger = Trigger(TriggerType.SOS_ACTIVATED),
            condition = Condition(ConditionType.ALWAYS),
            action = Action(ActionType.SHARE_LOCATION)
        ),
        AutomationRule(
            name = "Si estoy conduciendo",
            trigger = Trigger(TriggerType.DRIVING_DETECTED),
            condition = Condition(ConditionType.ALWAYS),
            action = Action(ActionType.ENABLE_DRIVING_MODE)
        ),
        AutomationRule(
            name = "Recordatorio nocturno",
            trigger = Trigger(TriggerType.TIME_OF_DAY, "21:00"),
            condition = Condition(ConditionType.ALWAYS),
            action = Action(ActionType.NOTIFY, "Recordatorio de MOVA Phone")
        ),
        AutomationRule(
            name = "Batería baja: avisar",
            trigger = Trigger(TriggerType.BATTERY_LOW),
            condition = Condition(ConditionType.BATTERY_BELOW, "15"),
            action = Action(ActionType.NOTIFY, "Batería baja")
        ),
        AutomationRule(
            name = "Si no responden en SOS",
            trigger = Trigger(TriggerType.SOS_ACTIVATED),
            condition = Condition(ConditionType.ALWAYS),
            action = Action(ActionType.CALL_CONTACT, "segundo contacto")
        ),
        AutomationRule(
            name = "Al conectar el cargador",
            trigger = Trigger(TriggerType.CHARGING),
            condition = Condition(ConditionType.IS_DRIVING),
            action = Action(ActionType.ENABLE_DRIVING_MODE)
        )
    )
}
