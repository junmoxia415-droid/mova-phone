package com.studiolexair.movaphone.data.messages.provider

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Proveedor SMS real. Usa SmsManager (API oficial) y confirma el envío con
 * PendingIntent; si el envío falla se informa al usuario, nunca se simula.
 */
class SmsMessageProvider(private val context: Context) : MessageProvider {

    override val id: String = PROVIDER_ID

    override suspend fun isAvailable(): Boolean =
        hasPermission(Manifest.permission.SEND_SMS) && smsManager() != null

    override suspend fun send(
        destination: String,
        body: String,
        sentIntent: PendingIntent?,
        deliveryIntent: PendingIntent?
    ): MessageSendResult {
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            return MessageSendResult(false, id, "Falta el permiso para enviar SMS.")
        }
        val manager = smsManager()
            ?: return MessageSendResult(false, id, "Este dispositivo no puede enviar SMS.")
        if (destination.isBlank() || body.isBlank()) {
            return MessageSendResult(false, id, "Escribe un número y un mensaje.")
        }

        return try {
            val parts = manager.divideMessage(body)
            if (parts.size > 1) {
                // Un intent de confirmación por cada parte: el estado final lo decide el receptor.
                val sentIntents = ArrayList<PendingIntent?>(parts.size)
                val deliveryIntents = ArrayList<PendingIntent?>(parts.size)
                repeat(parts.size) {
                    sentIntents.add(sentIntent)
                    deliveryIntents.add(deliveryIntent)
                }
                manager.sendMultipartTextMessage(destination, null, parts, sentIntents, deliveryIntents)
            } else {
                manager.sendTextMessage(destination, null, body, sentIntent, deliveryIntent)
            }
            MovaLog.i(TAG, "SMS encolado para envío (multipart=${parts.size > 1})")
            // Encolado correctamente: el estado definitivo llega por MovaSmsSentReceiver.
            MessageSendResult(true, id)
        } catch (t: Throwable) {
            MovaLog.e(TAG, "Fallo al enviar SMS", t)
            MessageSendResult(false, id, "No fue posible enviar el mensaje. Inténtalo nuevamente.")
        }
    }

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
    } catch (t: Throwable) {
        MovaLog.e(TAG, "SmsManager no disponible", t)
        null
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val PROVIDER_ID = "sms"
        private const val TAG = "SmsMessageProvider"
    }
}
