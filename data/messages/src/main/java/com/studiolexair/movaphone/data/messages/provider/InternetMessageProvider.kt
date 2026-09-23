package com.studiolexair.movaphone.data.messages.provider

/**
 * Transporte de mensajería por Internet.
 * NO implementado en V1 a propósito (requisito 22: no montar un backend enorme todavía).
 * Cuando exista servidor, esta clase se completa y la UI lo usará sin cambios
 * porque ambas implementaciones respetan el mismo contrato MessageProvider.
 */
class InternetMessageProvider : MessageProvider {

    override val id: String = PROVIDER_ID

    override suspend fun isAvailable(): Boolean = false

    override suspend fun send(
        destination: String,
        body: String,
        sentIntent: android.app.PendingIntent?,
        deliveryIntent: android.app.PendingIntent?
    ): MessageSendResult =
        MessageSendResult(
            success = false,
            providerId = id,
            errorMessage = "La mensajería por Internet llegará en una próxima versión de MOVA Phone."
        )

    companion object {
        const val PROVIDER_ID = "internet"
    }
}
