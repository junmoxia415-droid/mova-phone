package com.studiolexair.movaphone.core.permissions

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Solicitud de permisos en contexto desde Compose.
 * Devuelve el resultado real (concedido/denegado) por permiso, de modo que la
 * pantalla reaccione sin asumir nada y sin pedir todo al iniciar la aplicación.
 */
class PermissionRequester(private val launcher: (Array<String>) -> Unit) {

    fun request(vararg permissions: MovaPermission) {
        val applicable = permissions
            .filter { Build.VERSION.SDK_INT >= it.minSdk }
            // Los que Android sólo concede desde Ajustes no se piden con diálogo.
            .filterNot { it == MovaPermission.BACKGROUND_LOCATION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R }
        if (applicable.isEmpty()) return
        launcher(applicable.map { it.androidPermission }.toTypedArray())
    }

    fun request(permissions: List<MovaPermission>) = request(*permissions.toTypedArray())
}

@Composable
fun rememberPermissionRequester(
    onResult: (Map<String, Boolean>) -> Unit = {}
): PermissionRequester {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> onResult(result) }
    return remember(launcher) { PermissionRequester { permissions -> launcher.launch(permissions) } }
}
