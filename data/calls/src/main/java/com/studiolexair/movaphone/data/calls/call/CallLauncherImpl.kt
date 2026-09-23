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
 * Marcación con APIs oficiales.
 *
 *  - Con permiso CALL_PHONE se marca directamente con TelecomManager (la llamada se
 *    inicia desde MOVA Phone, sin abrir el marcador del sistema).
 *  - Sin permiso, [placeCall] devuelve `false` para que la interfaz **pida el permiso**
 *    en contexto; si el usuario lo deniega, se le ofrece abrir el marcador del sistema
 *    de forma explícita ([openDialer]) en lugar de hacerlo por su cuenta.
 */
class CallLauncherImpl(private val context: Context) : CallLauncher {

    private val capabilities: DeviceCapabilities = DeviceCapabilitiesReader.read(context)

    override fun canPlaceCalls(): Boolean = capabilities.hasTelephony

    override fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    override suspend fun placeCall(number: String): Boolean {
        val uri = Uri.parse("tel:" + Uri.encode(number))
        if (!hasCallPermission()) {
            // No se abre el marcador del sistema sin que el usuario lo pida:
            // la capa de UI solicita CALL_PHONE y reintenta la llamada.
            MovaLog.i(TAG, "Llamada no iniciada: falta el permiso CALL_PHONE")
            return false
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

    private companion object {
        const val TAG = "CallLauncher"
    }
}
