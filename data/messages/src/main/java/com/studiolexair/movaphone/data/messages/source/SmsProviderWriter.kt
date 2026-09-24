package com.studiolexair.movaphone.data.messages.source

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Escribe los mensajes en la base de datos **del sistema** (Telephony).
 *
 * Cuando MOVA Phone es la aplicación de mensajes predeterminada, es su responsabilidad
 * guardar los SMS en el proveedor del teléfono: así otros programas y los servicios del
 * sistema siguen viendo la mensajería completa, igual que con la app original.
 */
class SmsProviderWriter(private val context: Context) {

    /** Guarda un SMS recibido en la bandeja de entrada del sistema. */
    fun saveIncoming(address: String, body: String, timestamp: Long, read: Boolean = false) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, timestamp)
            put(Telephony.Sms.DATE_SENT, timestamp)
            put(Telephony.Sms.READ, if (read) 1 else 0)
            put(Telephony.Sms.SEEN, if (read) 1 else 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            put(Telephony.Sms.THREAD_ID, threadId(address))
        }
        insert(Telephony.Sms.CONTENT_URI, values, "recibido")
    }

    /** Guarda un SMS enviado por MOVA en la carpeta de enviados del sistema. */
    fun saveOutgoing(address: String, body: String, timestamp: Long) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, timestamp)
            put(Telephony.Sms.DATE_SENT, timestamp)
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            put(Telephony.Sms.THREAD_ID, threadId(address))
        }
        insert(Telephony.Sms.CONTENT_URI, values, "enviado")
    }

    /** Marca como leída toda la conversación en el proveedor del sistema. */
    fun markThreadRead(address: String) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%" + PhoneNumbers.digitsOnly(address).takeLast(SEARCH_DIGITS) + "%")
            )
        } catch (t: Throwable) {
            MovaLog.w(TAG, "No fue posible marcar como leído en el sistema: ${t.message}")
        }
    }

    private fun threadId(address: String): Long = try {
        Telephony.Threads.getOrCreateThreadId(context, address)
    } catch (t: Throwable) {
        @Suppress("DEPRECATION")
        Telephony.Threads.getOrCreateThreadId(context, setOf(address))
    }

    private fun insert(uri: Uri, values: ContentValues, kind: String) {
        try {
            context.contentResolver.insert(uri, values)
        } catch (t: Throwable) {
            // Si MOVA no es la app de SMS predeterminada, el sistema no permite escribir:
            // no es un error grave, sólo se registra.
            MovaLog.w(TAG, "No se pudo guardar el SMS $kind en el sistema: ${t.message}")
        }
    }

    private companion object {
        const val TAG = "SmsProviderWriter"
        const val SEARCH_DIGITS = 9
    }
}
