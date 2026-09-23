package com.studiolexair.movaphone.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recepción de SMS: guarda el mensaje en MOVA Phone, notifica al usuario
 * y dispara las automatizaciones configuradas ("llega un SMS").
 */
class MovaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val deps = SmsServiceDependencies
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val grouped = messages.groupBy { it.originatingAddress.orEmpty() }
                grouped.forEach { (address, parts) ->
                    if (address.isBlank()) return@forEach
                    val body = parts.joinToString("") { it.messageBody ?: "" }
                    val timestamp = parts.lastOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    val normalized = PhoneNumbers.normalize(address)
                    val contactName = deps.contactNameResolver?.invoke(address)

                    deps.messageRepository?.storeIncoming(
                        MessageEntity(
                            address = address,
                            normalizedAddress = normalized,
                            contactName = contactName,
                            body = body,
                            isIncoming = true,
                            sentAt = timestamp,
                            state = "RECEIVED"
                        )
                    )
                    deps.notifier?.general(
                        contactName ?: address,
                        body.take(120)
                    )
                    deps.automationEngine?.onTrigger(
                        TriggerType.SMS_RECEIVED,
                        TriggerPayload(number = address, contactName = contactName, message = body)
                    )
                    MovaLog.i(TAG, "SMS recibido y registrado")
                }
            } catch (t: Throwable) {
                MovaLog.e(TAG, "Fallo procesando un SMS entrante", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "MovaSmsReceiver"
    }
}
