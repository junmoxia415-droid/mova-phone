package com.studiolexair.movaphone.domain.automation.model

/**
 * Modelo Trigger → Condition → Action.
 * Añadir nuevos disparadores o acciones no requiere tocar el motor:
 * basta registrar una nueva implementación (ver AutomationEngine).
 */
data class AutomationRule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val trigger: Trigger,
    val condition: Condition? = null,
    val action: Action,
    val lastRunAt: Long? = null,
    val runCount: Int = 0
)

data class Trigger(val type: TriggerType, val value: String? = null)
data class Condition(val type: ConditionType, val value: String? = null)
data class Action(val type: ActionType, val value: String? = null)

enum class TriggerType(val label: String) {
    SOS_ACTIVATED("Se activa SOS"),
    DRIVING_DETECTED("Estoy conduciendo"),
    CALL_INCOMING("Entra una llamada"),
    SMS_RECEIVED("Llega un SMS"),
    TIME_OF_DAY("A una hora concreta"),
    BATTERY_LOW("Batería baja"),
    CHARGING("Conectado al cargador"),
    WIFI_CONNECTED("Wi-Fi conectado"),
    BLUETOOTH_CONNECTED("Bluetooth conectado"),
    LOCATION_ENTER("Llego a un lugar"),
    LOCATION_EXIT("Salgo de un lugar"),
    APP_UNLOCKED("Se desbloquea la app"),
    MANUAL("Ejecución manual")
}

enum class ConditionType(val label: String) {
    ALWAYS("Siempre"),
    CONTACT_IS("El contacto es"),
    NUMBER_IS("El número es"),
    BATTERY_BELOW("Batería por debajo de"),
    BETWEEN_HOURS("Entre horas"),
    IS_DRIVING("Modo conducción activo"),
    IS_SILENT("Modo silencio activo")
}

enum class ActionType(val label: String) {
    SEND_SMS("Enviar SMS"),
    CALL_CONTACT("Llamar a un contacto"),
    SHARE_LOCATION("Compartir ubicación"),
    ENABLE_DRIVING_MODE("Activar modo conducción"),
    DISABLE_DRIVING_MODE("Desactivar modo conducción"),
    SILENCE_NOTIFICATIONS("Silenciar notificaciones"),
    ENABLE_SOS("Activar protocolo SOS"),
    BLOCK_NUMBER("Bloquear número"),
    NOTIFY("Mostrar notificación"),
    ADD_SECURITY_EVENT("Registrar evento de seguridad")
}

/** Resultado de evaluar y ejecutar una regla. */
data class AutomationOutcome(
    val ruleId: Long,
    val ruleName: String,
    val executed: Boolean,
    val success: Boolean = false,
    val detail: String? = null
)
