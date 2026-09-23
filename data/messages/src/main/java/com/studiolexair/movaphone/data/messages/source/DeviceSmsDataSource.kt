package com.studiolexair.movaphone.data.messages.source

import android.content.Context
import android.provider.Telephony
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.logging.MovaLog

/** Lectura de la bandeja de entrada del sistema (Telephony.Sms, API oficial). */
class DeviceSmsDataSource(private val context: Context) {

    /** ¿Tenemos permiso para leer los SMS del sistema? */
    fun hasPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun readInbox(limit: Int = 300): List<MessageEntity> {
        if (!hasPermission()) {
            com.studiolexair.movaphone.core.logging.MovaLog.w(
                "DeviceSms",
                "Sin permiso READ_SMS: no se leen los SMS del sistema"
            )
            return emptyList()
        }
        val results = mutableListOf<MessageEntity>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIdx).orEmpty()
                    val body = cursor.getString(bodyIdx).orEmpty()
                    if (address.isBlank()) continue
                    val isIncoming = cursor.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_INBOX
                    val isRead = cursor.getInt(readIdx) == 1
                    results += MessageEntity(
                        systemMessageId = cursor.getLong(idIdx),
                        address = address,
                        normalizedAddress = PhoneNumbers.normalize(address),
                        body = body,
                        isIncoming = isIncoming,
                        sentAt = cursor.getLong(dateIdx),
                        state = if (isIncoming && isRead) "READ" else if (isIncoming) "RECEIVED" else "SENT"
                    )
                }
            }
        } catch (security: SecurityException) {
            MovaLog.w(TAG, "Sin permiso READ_SMS")
            throw security
        } catch (t: Throwable) {
            MovaLog.e(TAG, "No se pudo leer la bandeja de entrada", t)
        }
        return results
    }

    private companion object {
        const val TAG = "DeviceSms"
    }
}
