package com.studiolexair.movaphone.services.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.logging.MovaLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Respuesta rápida desde la pantalla de llamada del sistema.
 *
 * Android exige cuatro piezas a la aplicación de mensajes predeterminada y ésta era la que
 * faltaba: la actividad para `sms:`/`smsto:`, el receptor de `SMS_DELIVER`, el receptor de
 * `WAP_PUSH_DELIVER` y **este servicio** para `RESPOND_VIA_MESSAGE`.
 *
 * Con él, cuando el teléfono suena y el usuario elige "responder con un mensaje" en la
 * pantalla de llamada, el texto se envía de verdad y queda guardado en la conversación con
 * el mismo estado real de entrega (palomitas) que si se hubiera escrito dentro de MOVA.
 */
class MovaRespondViaMessageService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val recipient = intent?.data?.schemeSpecificPart?.trim()?.takeIf { it.isNotBlank() }
        val text = (intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT))
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val repository = SmsServiceDependencies.messageRepository

        if (recipient == null || text == null || repository == null) {
            MovaLog.w(TAG, "Respuesta rápida incompleta: no se envía nada")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contactName = SmsServiceDependencies.contactNameResolver?.invoke(recipient)
                val sent = repository.sendMessage(
                    address = recipient,
                    normalizedAddress = PhoneNumbers.normalize(recipient),
                    body = text,
                    contactName = contactName
                )
                if (sent) {
                    MovaLog.i(TAG, "Respuesta rápida enviada y guardada en la conversación")
                } else {
                    MovaLog.w(TAG, "La respuesta rápida no pudo encolarse")
                    SmsServiceDependencies.notifier?.general(
                        contactName ?: recipient,
                        "No se pudo enviar la respuesta rápida. El texto queda guardado en la conversación."
                    )
                }
            } catch (t: Throwable) {
                MovaLog.e(TAG, "Fallo enviando la respuesta rápida", t)
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private companion object {
        const val TAG = "MovaRespondViaMessage"
    }
}
