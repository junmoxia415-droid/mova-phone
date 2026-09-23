package com.studiolexair.movaphone.data.calls.source

import android.content.Context
import android.provider.CallLog
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.CallRecordEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.calls.model.CallType

/**
 * Lectura del historial del sistema (CallLog.Calls, API oficial).
 * MOVA enriquece ese historial con datos propios: spam, bloqueos y duración.
 */
class DeviceCallLogDataSource(private val context: Context) {

    /** ¿Tenemos permiso para leer el historial del sistema? */
    fun hasPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALL_LOG
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun readRecent(limit: Int = 200): List<CallRecordEntity> {
        if (!hasPermission()) {
            com.studiolexair.movaphone.core.logging.MovaLog.w(
                "DeviceCallLog",
                "Sin permiso READ_CALL_LOG: no se lee el historial del sistema"
            )
            return emptyList()
        }
        val results = mutableListOf<CallRecordEntity>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $limit"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIdx).orEmpty()
                    if (number.isBlank()) continue
                    results += CallRecordEntity(
                        systemCallId = cursor.getLong(idIdx),
                        number = number,
                        normalizedNumber = PhoneNumbers.normalize(number),
                        contactName = cursor.getString(nameIdx),
                        type = CallType.fromSystemType(cursor.getInt(typeIdx)).name,
                        startedAt = cursor.getLong(dateIdx),
                        durationSeconds = cursor.getLong(durationIdx)
                    )
                }
            }
        } catch (security: SecurityException) {
            MovaLog.w(TAG, "Sin permiso READ_CALL_LOG")
            throw security
        } catch (t: Throwable) {
            MovaLog.e(TAG, "No se pudo leer el historial del sistema", t)
        }
        return results
    }

    private companion object {
        const val TAG = "DeviceCallLog"
    }
}
