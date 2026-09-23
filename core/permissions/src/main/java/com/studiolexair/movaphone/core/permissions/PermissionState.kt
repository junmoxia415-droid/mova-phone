package com.studiolexair.movaphone.core.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Estado de permisos listo para usar en cualquier pantalla.
 *
 * La app **pide los permisos en contexto**: cuando el usuario va a llamar, a importar
 * contactos, a enviar un SMS o a usar la ubicación. Si el permiso se concede desde los
 * ajustes del sistema, al volver a la pantalla el estado se recalcula solo.
 */
class PermissionHandle internal constructor(
    val granted: Boolean,
    val missing: List<MovaPermission>,
    val request: () -> Unit
)

@Composable
fun rememberPermissionHandle(permissions: List<MovaPermission>): PermissionHandle {
    val context = LocalContext.current
    val checker = remember(context) { PermissionChecker(context) }
    val applicable = remember(permissions) { checker.applicable(permissions) }
    var granted by remember(applicable) { mutableStateOf(checker.allGranted(applicable)) }

    val requester = rememberPermissionRequester { _ -> granted = checker.allGranted(applicable) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, applicable) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = checker.allGranted(applicable)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return PermissionHandle(
        granted = granted,
        missing = checker.missing(applicable),
        request = { requester.request(applicable) }
    )
}

/** Consulta puntual del estado de permisos (no observa cambios). */
@Composable
fun rememberPermissionChecker(): PermissionChecker {
    val context = LocalContext.current
    return remember(context) { PermissionChecker(context) }
}

/**
 * Bloque que se muestra sólo mientras faltan permisos: explica para qué se usan y
 * permite concederlos. Al concederlos invoca [onGranted] una única vez.
 */
@Composable
fun PermissionGate(
    permissions: List<MovaPermission>,
    title: String,
    onGranted: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    content: @Composable () -> Unit
) {
    val handle = rememberPermissionHandle(permissions)
    LaunchedEffect(handle.granted) {
        if (handle.granted) onGranted()
    }
    if (handle.granted) {
        content()
    } else {
        PermissionPrompt(
            permissions = handle.missing,
            title = title,
            onRequest = handle.request,
            modifier = modifier
        )
    }
}
