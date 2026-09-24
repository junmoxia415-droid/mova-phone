package com.studiolexair.movaphone.core.common.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager

/**
 * MOVA Phone como **app del sistema**: teléfono y mensajes.
 *
 * Android 10 (API 29) introdujo los *roles*: la app que los tiene pasa a ser la aplicación
 * de llamadas o de SMS del teléfono, con su propia pantalla de llamada y su propia bandeja.
 * En Android 8 y 9 se usa el mecanismo anterior (intents de sistema).
 *
 * Aquí sólo se prepara la petición y se consulta el estado: la interfaz muestra el botón y
 * explica al usuario qué cambia. Sin Play Services y sin atajos: son APIs oficiales.
 */
object SystemRoles {

    /**
     * Identificadores reales del sistema. `RoleManager.ROLE_DIALER` y `ROLE_SMS` son constantes
     * de compilación ("android.app.role.DIALER" / "android.app.role.SMS"): leerlas no rompe en
     * Android 8 ni 9, donde simplemente no se usan.
     */
    const val DIALER: String = RoleManager.ROLE_DIALER
    const val SMS: String = RoleManager.ROLE_SMS

    fun isRoleAvailable(context: Context, role: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return roleManager(context)?.isRoleAvailable(role) == true
    }

    fun holdsRole(context: Context, role: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return roleManager(context)?.isRoleHeld(role) == true
        }
        return when (role) {
            DIALER -> isDefaultDialer(context)
            SMS -> isDefaultSmsApp(context)
            else -> false
        }
    }

    /** Intent para pedir el rol (Android 10+) o para abrir los ajustes del sistema (Android 8-9). */
    fun requestIntent(context: Context, role: String): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = roleManager(context) ?: return null
            if (!manager.isRoleAvailable(role)) return null
            return manager.createRequestRoleIntent(role)
        }
        return if (role == DIALER) {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        } else {
            // En Android 8 y 9 la app de SMS se elige en los ajustes del sistema.
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    fun isDefaultDialer(context: Context): Boolean = try {
        val telecom = context.getSystemService(TelecomManager::class.java)
        telecom?.defaultDialerPackage == context.packageName
    } catch (t: Throwable) {
        false
    }

    fun isDefaultSmsApp(context: Context): Boolean = try {
        Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    } catch (t: Throwable) {
        false
    }

    /** Texto de estado para mostrar en Ajustes. */
    fun summary(context: Context, role: String): String {
        val held = holdsRole(context, role)
        val available = isRoleAvailable(context, role)
        val what = if (role == DIALER) "de teléfono" else "de mensajes"
        return when {
            held -> "MOVA Phone es ahora tu aplicación $what"
            !available -> "Este dispositivo no permite cambiar esta aplicación predeterminada"
            else -> "Ahora mismo la usa el sistema. Pulsa para que MOVA Phone la sustituya"
        }
    }

    private fun roleManager(context: Context): RoleManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else {
            null
        }
}
