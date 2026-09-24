package com.studiolexair.movaphone.core.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Consulta del estado de permisos. Nunca lanza excepciones a la UI. */
class PermissionChecker(private val context: Context) {

    fun isGranted(permission: MovaPermission): Boolean {
        if (permission.minSdk > Build.VERSION.SDK_INT) return false
        return ContextCompat.checkSelfPermission(context, permission.androidPermission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun granted(permissions: List<MovaPermission>): List<MovaPermission> =
        permissions.filter { isGranted(it) }

    fun missing(permissions: List<MovaPermission>): List<MovaPermission> =
        permissions.filterNot { isGranted(it) }

    fun allGranted(permissions: List<MovaPermission>): Boolean = missing(permissions).isEmpty()

    /**
     * Permisos aplicables al SDK actual (evita pedir permisos inexistentes:
     * post notificaciones antes de Android 13, ubicación en segundo plano antes de Android 10...).
     */
    fun applicable(permissions: List<MovaPermission>): List<MovaPermission> =
        permissions.filter { Build.VERSION.SDK_INT >= it.minSdk }

    /**
     * Android 10 y anteriores permiten pedir la ubicación en segundo plano con un diálogo.
     * Desde Android 11 (API 30) **sólo** se puede conceder desde los ajustes de la app:
     * la pantalla de permisos debe abrir esos ajustes en lugar de pedir un diálogo que no existe.
     */
    fun requiresSystemSettings(permission: MovaPermission): Boolean =
        permission == MovaPermission.BACKGROUND_LOCATION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun isCallRecordingAvailable(): Boolean =
        // Requisito 35: la grabación de llamadas sólo se declara disponible cuando el sistema lo permite.
        // En Android 10+ la API pública está limitada y en Android 11+ muchos fabricantes la bloquean.
        Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.Q

    fun callRecordingUnavailableMessage(): String =
        "Esta función no está disponible en este dispositivo o versión de Android."
}
