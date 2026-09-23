package com.studiolexair.movaphone.data.messages.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Construye los PendingIntent de confirmación de SMS.
 *
 * Cada mensaje guardado en Room lleva su propio identificador dentro del Intent, de forma
 * que cuando el sistema responde ("enviado" / "entregado") el receptor sabe **qué** mensaje
 * actualizar. Así la palomita de la conversación refleja la realidad del envío.
 */
class SmsIntentFactory(private val context: Context) {

    fun sentIntent(messageId: Long, address: String): PendingIntent = pending(
        action = MessageActions.ACTION_SMS_SENT,
        messageId = messageId,
        address = address,
        requestCode = (messageId % REQUEST_CODE_MOD).toInt() * 2
    )

    fun deliveryIntent(messageId: Long, address: String): PendingIntent = pending(
        action = MessageActions.ACTION_SMS_DELIVERED,
        messageId = messageId,
        address = address,
        requestCode = (messageId % REQUEST_CODE_MOD).toInt() * 2 + 1
    )

    private fun pending(action: String, messageId: Long, address: String, requestCode: Int): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(MessageActions.EXTRA_MESSAGE_ID, messageId)
            .putExtra(MessageActions.EXTRA_ADDRESS, address)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_CODE_MOD = 100_000
    }
}
