package com.studiolexair.movaphone.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recepción de SMS como **aplicación de mensajes del sistema**.
 *
 * Android entrega los SMS a la app predeterminada con `SMS_DELIVER` (no con `SMS_RECEIVED`,
 * que es un aviso de sólo lectura). Al recibirlos aquí, MOVA:
 *  1. los guarda en la base de datos del sistema (proveedor Telephony), como exige Android;
 *  2. los guarda en la base local de MOVA para su propia bandeja;
 *  3. avisa al usuario y dispara las automatizaciones.
 *
 * Los SMS de más de 160 caracteres llegan troceados: se unen antes de guardar y notificar.
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messages = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val deps = SmsServiceDependencies
        val writer = deps.providerWriter
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (address, parts) ->
                    if (address.isBlank()) return@forEach
                    val body = parts.joinToString("") { it.messageBody ?: "" }
                    val timestamp = parts.lastOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    val normalized = PhoneNumbers.normalize(address)
                    val contactName = deps.contactNameResolver?.invoke(address)

                    writer?.saveIncoming(address, body, timestamp)
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
                    deps.notifier?.general(contactName ?: address, body.take(120))
                    deps.onSmsReceived?.invoke(address, contactName, body, timestamp)
                    deps.automationEngine?.onTrigger(
                        TriggerType.SMS_RECEIVED,
                        TriggerPayload(number = address, contactName = contactName, message = body)
                    )
                    MovaLog.i(TAG, "SMS recibido como app predeterminada y guardado")
                }
            } catch (t: Throwable) {
                MovaLog.e(TAG, "Fallo procesando el SMS entrante", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SmsDeliverReceiver"
    }
}

/**
 * MMS: Android exige que la app de mensajes predeterminada atienda también el aviso WAP push.
 * MOVA 1.1 avisa al usuario y conserva el SMS; la descarga de MMS (necesita APN y red móvil)
 * no está implementada todavía, y se dice con claridad en lugar de fingirlo.
 */
class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        MovaLog.i(TAG, "Aviso de MMS recibido (la descarga de MMS llegará en una próxima versión)")
        SmsServiceDependencies.notifier?.general(
            "MMS recibido",
            "MOVA Phone todavía no descarga MMS. El remitente puede enviarte un SMS con el contenido."
        )
    }

    private companion object {
        const val TAG = "MmsReceiver"
    }
}
