package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoMark
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.rememberPermissionChecker
import com.studiolexair.movaphone.core.permissions.rememberPermissionRequester

private data class PermissionGroup(
    val permission: MovaPermission,
    val icon: ImageVector,
    val title: String,
    val why: String
)

/**
 * Permisos de MOVA Phone, explicados uno por uno.
 *
 * Arreglo importante sobre la versión anterior: el estado se **recalcula de verdad** cada vez
 * que el usuario toca un botón o vuelve de los ajustes del sistema (antes la pantalla se
 * quedaba con el estado viejo y parecía que «no funcionaba»). Además:
 *  - cada permiso concedido se marca con su visto bueno (ya no es un interruptor que engaña),
 *  - los que Android sólo concede desde Ajustes (ubicación en segundo plano) abren Ajustes,
 *  - hay un botón para ir directamente a los que faltan.
 */
@Composable
fun PermissionsScreen(
    navigator: MovaNavigator,
    firstRun: Boolean,
    onFinish: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = listOf(
        PermissionGroup(MovaPermission.CALL_PHONE, Icons.Filled.Call, "Llamar desde MOVA", "Marcar y llamar sin abrir el marcador del sistema."),
        PermissionGroup(MovaPermission.READ_PHONE_STATE, Icons.Filled.Call, "Estado de las llamadas", "Saber cuándo entra una llamada para avisarte y aplicar tus reglas."),
        PermissionGroup(MovaPermission.ANSWER_PHONE_CALLS, Icons.Filled.Emergency, "Atender llamadas", "Contestar desde MOVA y desde el manos libres."),
        PermissionGroup(MovaPermission.CALL_LOG, Icons.Filled.History, "Historial de llamadas", "Mostrar tus llamadas reales: perdidas, entrantes, salientes y spam."),
        PermissionGroup(MovaPermission.READ_CONTACTS, Icons.Filled.Contacts, "Contactos del teléfono", "Traer tu agenda automáticamente para que aparezca en la app."),
        PermissionGroup(MovaPermission.SEND_SMS, Icons.Filled.Message, "Enviar SMS", "Mensajes y alerta de emergencia a tus contactos."),
        PermissionGroup(MovaPermission.READ_SMS, Icons.Filled.Message, "Leer SMS", "Ver las conversaciones que ya tienes en el teléfono."),
        PermissionGroup(MovaPermission.RECEIVE_SMS, Icons.Filled.Message, "Recibir SMS", "Avisarte de mensajes nuevos y de las respuestas a tus alertas."),
        PermissionGroup(MovaPermission.COARSE_LOCATION, Icons.Filled.LocationOn, "Ubicación aproximada", "Ubicación de respaldo si el GPS no está disponible."),
        PermissionGroup(MovaPermission.FINE_LOCATION, Icons.Filled.LocationOn, "Ubicación precisa", "Compartir tu posición exacta en la emergencia."),
        PermissionGroup(MovaPermission.BACKGROUND_LOCATION, Icons.Filled.LocationOn, "Ubicación en segundo plano", "Seguir avisando de tu posición en una emergencia con la pantalla apagada. Android sólo lo concede desde los ajustes."),
        PermissionGroup(MovaPermission.POST_NOTIFICATIONS, Icons.Filled.Notifications, "Notificaciones", "Avisos de emergencia, mensajes y automatizaciones."),
        PermissionGroup(MovaPermission.RECORD_AUDIO, Icons.Filled.Emergency, "Micrófono", "Sólo para hablar con el asistente y el modo conducción. Nada se graba en secreto.")
    )

    val checker = rememberPermissionChecker()
    // El estado se recalcula al conceder algo y al volver a la app desde los ajustes.
    var refresh by remember { mutableIntStateOf(0) }
    val requester = rememberPermissionRequester { refresh++ }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val missing = remember(refresh) { groups.filterNot { checker.isGranted(it.permission) } }
    val grantedCount = groups.size - missing.size
    var showAll by remember { mutableStateOf(false) }

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MovaLogoMark(size = MovaDimens.iconXl * 2)
                    Text(
                        text = if (firstRun) "Antes de empezar" else "Permisos de MOVA Phone",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = MovaDimens.spaceMd)
                    )
                    Text(
                        text = "MOVA Phone sólo usa lo que necesita y todo se queda en tu teléfono. " +
                            "Cada permiso se concede por separado y puedes rechazar el que quieras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                }
            }

            item {
                MovaCard {
                    Text(
                        text = "$grantedCount de ${groups.size} permisos concedidos",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (missing.isEmpty()) MovaTheme.extra.success else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (missing.isEmpty()) {
                            "Todo listo: MOVA Phone puede funcionar completo."
                        } else {
                            "Faltan ${missing.size}. Toca «Conceder» en cada uno: Android mostrará su diálogo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceXs, bottom = MovaDimens.spaceSm)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                        if (missing.isNotEmpty()) {
                            MovaPrimaryButton(
                                text = "Conceder los que faltan",
                                icon = Icons.Filled.CheckCircle,
                                modifier = Modifier.weight(1f),
                                onClick = { requester.request(missing.map { it.permission }) }
                            )
                        }
                        MovaSecondaryButton(
                            text = if (showAll) "Ver sólo los que faltan" else "Ver todos",
                            modifier = Modifier.weight(1f),
                            onClick = { showAll = !showAll }
                        )
                    }
                }
            }

            items(if (showAll) groups else missing, key = { it.permission.name }) { group ->
                PermissionRow(
                    group = group,
                    granted = checker.isGranted(group.permission),
                    fromSettings = checker.requiresSystemSettings(group.permission),
                    onRequest = { requester.request(group.permission) },
                    onOpenSettings = onOpenSystemSettings
                )
            }

            if (!showAll && missing.isEmpty()) {
                item {
                    MovaInfoBanner(
                        message = "No falta ningún permiso. Si quieres revisarlos todos, pulsa «Ver todos».",
                        tone = PillTone.Success,
                        icon = Icons.Filled.CheckCircle
                    )
                }
            }

            item {
                MovaInfoBanner(
                    message = "Si rechazas un permiso, esa función se queda desactivada y el resto de " +
                        "MOVA sigue funcionando. Puedes concederlo cuando quieras desde aquí o desde " +
                        "Ajustes del sistema.",
                    tone = PillTone.Neutral
                )
            }

            item {
                MovaPrimaryButton(
                    text = if (firstRun) "Continuar" else "Volver",
                    onClick = { if (firstRun) onFinish() else navigator.back() }
                )
            }
            item {
                MovaSecondaryButton(
                    text = "Abrir ajustes del sistema",
                    icon = Icons.Filled.OpenInNew,
                    onClick = onOpenSystemSettings
                )
            }
        }
    }
}

/** Una fila de permiso: estado real + botón para pedirlo (o abrir Ajustes si Android lo exige). */
@Composable
private fun PermissionRow(
    group: PermissionGroup,
    granted: Boolean,
    fromSettings: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    MovaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else group.icon,
                contentDescription = null,
                tint = if (granted) MovaTheme.extra.success else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = MovaDimens.spaceSm)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (granted) "Concedido" else group.why,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) MovaTheme.extra.success else MovaTheme.extra.textSecondary
                )
            }
        }

        if (!granted) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm),
                modifier = Modifier.padding(top = MovaDimens.spaceSm)
            ) {
                if (fromSettings) {
                    MovaPrimaryButton(
                        text = "Conceder en Ajustes",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSettings
                    )
                } else {
                    MovaPrimaryButton(
                        text = "Conceder",
                        modifier = Modifier.weight(1f),
                        onClick = onRequest
                    )
                }
                MovaSecondaryButton(
                    text = "Ajustes",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSettings
                )
            }
        }
    }
}
