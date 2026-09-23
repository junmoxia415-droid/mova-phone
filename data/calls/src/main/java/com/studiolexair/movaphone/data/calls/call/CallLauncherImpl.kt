package com.studiolexair.movaphone.data.calls.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.studiolexair.movaphone.core.common.util.DeviceCapabilities
import com.studiolexair.movaphone.core.common.util.DeviceCapabilitiesReader
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.calls.repository.CallLauncher

/**
 * Marcación con APIs oficiales:
 *  - Con permiso CALL_PHONE se usa TelecomManager (llamada directa).
 *  - Sin permiso se abre el marcador del sistema con el número preparado (ACTION_DIAL),
 *    de modo que la función sigue siendo útil sin invadir la privacidad del usuario.
 */
class CallLauncherImpl(private val context: Context) : CallLauncher {

    private val capabilities: DeviceCapabilities = DeviceCapabilitiesReader.read(context)

    override fun canPlaceCalls(): Boolean = capabilities.hasTelephony

    override suspend fun placeCall(number: String): Boolean {
        val uri = Uri.parse("tel:" + Uri.encode(number))
        if (!hasCallPermission()) {
            MovaLog.i(TAG, "Sin CALL_PHONE: se abre el marcador del sistema")
            return openDialer(number)
        }
        return try {
            val telecom = context.getSystemService(TelecomManager::class.java)
            if (telecom != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telecom.placeCall(uri, android.os.Bundle())
                } else {
                    @Suppress("DEPRECATION")
                    telecom.placeCall(uri, null)
                }
                true
            } else {
                openDialer(number)
            }
        } catch (t: Throwable) {
            MovaLog.e(TAG, "No fue posible iniciar la llamada con TelecomManager", t)
            openDialer(number)
        }
    }

    override fun openDialer(number: String): Boolean = try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (t: Throwable) {
        MovaLog.e(TAG, "No hay marcador disponible", t)
        false
    }

    private fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "CallLauncher"
    }
}
