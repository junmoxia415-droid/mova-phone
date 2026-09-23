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
 * Si el sistema devuelve un error se actualiza el estado del mensaje en Room:
 * nunca se muestra "enviado" cuando no lo está.
 */
class MovaSmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MessageActions.ACTION_SMS_SENT) return
        val resultCode = resultCode ?: Activity.RESULT_OK
        val success = resultCode == Activity.RESULT_OK
        MovaLog.i(TAG, "Resultado de envío SMS: $resultCode")
        val repository = SmsServiceDependencies.messageRepository ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SmsServiceDependencies.lastSentMessageId?.let { messageId ->
                    repository.updateState(messageId, if (success) "SENT" else "FAILED")
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SmsSentReceiver"
    }
}

/** Puente de dependencias de los componentes de SMS. */
object SmsServiceDependencies {
    @Volatile var messageRepository: com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl? = null
    @Volatile var notifier: com.studiolexair.movaphone.services.notifications.MovaNotifications? = null
    @Volatile var automationEngine: com.studiolexair.movaphone.domain.automation.repository.AutomationEngine? = null
    @Volatile var contactNameResolver: ((String?) -> String?)? = null
    @Volatile var lastSentMessageId: Long? = null
}
