package com.studiolexair.movaphone.services.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.messages.provider.MessageActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Confirmación real del envío de SMS.
 *
 * El sistema responde a nuestro PendingIntent con el resultado y con el identificador del
 * mensaje que había en el Intent: así se actualiza **exactamente** ese mensaje en Room
 * (enviado o fallido) y la palomita de la conversación nunca miente.
 */
class MovaSmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MessageActions.ACTION_SMS_SENT) return
        val resultCode = resultCode
        val success = resultCode == Activity.RESULT_OK
        val state = if (success) STATE_SENT else STATE_FAILED
        MovaLog.i(TAG, "Resultado de envío SMS: $resultCode (${describe(resultCode)}) -> $state")

        val repository = SmsServiceDependencies.messageRepository ?: return
        val messageId = intent.getLongExtra(MessageActions.EXTRA_MESSAGE_ID, INVALID_ID)
            .takeIf { it > 0 } ?: SmsServiceDependencies.lastSentMessageId ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.updateState(messageId, state)
                if (!success) {
                    SmsServiceDependencies.notifier?.general(
                        intent.getStringExtra(MessageActions.EXTRA_ADDRESS) ?: "Mensaje",
                        "No se pudo enviar el SMS: ${describe(resultCode)}"
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun describe(code: Int): String = when (code) {
        Activity.RESULT_OK -> "enviado"
        android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "fallo genérico del módem"
        android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> "sin cobertura"
        android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> "PDU no válido"
        android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> "radio apagada (modo avión)"
        else -> "código $code"
    }

    private companion object {
        const val TAG = "SmsSentReceiver"
        const val STATE_SENT = "SENT"
        const val STATE_FAILED = "FAILED"
        const val INVALID_ID = -1L
    }
}

/**
 * Confirmación de entrega: el SMS llegó al teléfono del destinatario.
 * Es la segunda palomita; se guarda como estado DELIVERED.
 */
class MovaSmsDeliveredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MessageActions.ACTION_SMS_DELIVERED) return
        val repository = SmsServiceDependencies.messageRepository ?: return
        val messageId = intent.getLongExtra(MessageActions.EXTRA_MESSAGE_ID, -1L)
        if (messageId <= 0) return
        MovaLog.i(TAG, "SMS entregado (id=$messageId)")

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.updateState(messageId, "DELIVERED")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SmsDeliveredReceiver"
    }
}

/** Puente de dependencias de los componentes de SMS. */
object SmsServiceDependencies {
    @Volatile var messageRepository: com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl? = null
    @Volatile var notifier: com.studiolexair.movaphone.services.notifications.MovaNotifications? = null
    @Volatile var automationEngine: com.studiolexair.movaphone.domain.automation.repository.AutomationEngine? = null
    @Volatile var contactNameResolver: ((String?) -> String?)? = null

    /** Último mensaje enviado (respaldo si el sistema no conserva los extras del Intent). */
    @Volatile var lastSentMessageId: Long? = null
}
