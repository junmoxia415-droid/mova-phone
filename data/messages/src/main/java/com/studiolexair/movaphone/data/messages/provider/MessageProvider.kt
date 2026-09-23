package com.studiolexair.movaphone.data.messages.provider

/**
 * Abstracción de transporte de mensajes (requisito 22 del proyecto).
 * V1 implementa SMS real; el chat por Internet se añadirá con otra implementación
 * sin tocar la UI ni la base de datos (campo MessageEntity.provider).
 */
interface MessageProvider {

    val id: String

    /** ¿Puede usarse este transporte en este dispositivo y con los permisos actuales? */
    suspend fun isAvailable(): Boolean

    /**
     * Envía un mensaje de texto y devuelve el resultado real del envío.
     *
     * [sentIntent] y [deliveryIntent] los rellena el transporte que pueda confirmarlos
     * (SMS): así la app sabe si el mensaje **salió** del teléfono y si **llegó** al
     * destinatario, y puede mostrar la palomita correcta en la conversación.
     */
    suspend fun send(
        destination: String,
        body: String,
        sentIntent: android.app.PendingIntent? = null,
        deliveryIntent: android.app.PendingIntent? = null
    ): MessageSendResult
}

data class MessageSendResult(
    val success: Boolean,
    val providerId: String,
    val errorMessage: String? = null
)

object MessageActions {
    const val ACTION_SMS_SENT = "com.studiolexair.movaphone.action.SMS_SENT"
    const val ACTION_SMS_DELIVERED = "com.studiolexair.movaphone.action.SMS_DELIVERED"
    const val EXTRA_MESSAGE_ID = "extra_message_id"
    const val EXTRA_ADDRESS = "extra_address"
    const val EXTRA_RESULT_CODE = "extra_result_code"
}
