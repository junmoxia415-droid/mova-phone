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

    /** Envía un mensaje de texto. Devuelve el resultado real del envío. */
    suspend fun send(destination: String, body: String): MessageSendResult
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
    const val EXTRA_RESULT_CODE = "extra_result_code"
}
