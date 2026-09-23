package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoMark
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSectionHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSwitchRow
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle
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
 * Se muestra en el primer arranque (para que la app **pida lo que necesita**) y en
 * cualquier momento desde Más → Permisos o Ajustes → Permisos. Cada permiso se concede
 * por separado: si el usuario rechaza uno, la función afectada se marca como no disponible
 * y el resto de la aplicación sigue funcionando.
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
        PermissionGroup(MovaPermission.CALL_LOG, Icons.Filled.History, "Historial de llamadas", "Mostrar tus llamadas reales: perdidas, entrantes, salientes y spam."),
        PermissionGroup(MovaPermission.READ_CONTACTS, Icons.Filled.Contacts, "Contactos del teléfono", "Traer tu agenda automáticamente para que aparezca en la app."),
        PermissionGroup(MovaPermission.SEND_SMS, Icons.Filled.Message, "Enviar SMS", "Mensajes y alerta de emergencia a tus contactos."),
        PermissionGroup(MovaPermission.READ_SMS, Icons.Filled.Message, "Leer SMS", "Ver las conversaciones que ya tienes en el teléfono."),
        PermissionGroup(MovaPermission.RECEIVE_SMS, Icons.Filled.Message, "Recibir SMS", "Avisarte de mensajes nuevos y respuestas a tus alertas."),
        PermissionGroup(MovaPermission.COARSE_LOCATION, Icons.Filled.LocationOn, "Ubicación aproximada", "Ubicación de respaldo si el GPS no está disponible."),
        PermissionGroup(MovaPermission.FINE_LOCATION, Icons.Filled.LocationOn, "Ubicación precisa", "Compartir tu posición exacta en el protocolo SOS."),
        PermissionGroup(MovaPermission.POST_NOTIFICATIONS, Icons.Filled.Notifications, "Notificaciones", "Avisos de emergencia, mensajes y automatizaciones."),
        PermissionGroup(MovaPermission.RECORD_AUDIO, Icons.Filled.Emergency, "Micrófono", "Sólo para los comandos de voz del asistente y del modo conducción."),
        PermissionGroup(MovaPermission.ANSWER_PHONE_CALLS, Icons.Filled.Emergency, "Atender llamadas", "Contestar o rechazar desde MOVA cuando el manos libres lo permite.")
    )

    val checker = com.studiolexair.movaphone.core.permissions.rememberPermissionChecker()
    val requester = rememberPermissionRequester()
    val handle = rememberPermissionHandle(groups.map { it.permission })

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    MovaLogoMark(size = MovaDimens.iconXl * 2)
                    Text(
                        text = if (firstRun) "Antes de empezar" else "Permisos de MOVA Phone",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = MovaDimens.spaceMd)
                    )
                    Text(
                        text = "MOVA Phone sólo usa lo que necesita y todo se queda en tu teléfono. " +
                            "Puedes conceder o rechazar cada permiso por separado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                }
            }

            item {
                MovaPrimaryButton(
                    text = "Conceder todos los permisos",
                    icon = Icons.Filled.Call,
                    onClick = { requester.request(groups.map { it.permission }) }
                )
            }

            item { MovaSectionHeader(text = "Permisos uno por uno") }

            items(groups.size) { index ->
                val group = groups[index]
                val granted = checker.isGranted(group.permission)
                MovaSwitchRow(
                    title = group.title,
                    subtitle = if (granted) "Concedido · ${group.why}" else group.why,
                    checked = granted,
                    onCheckedChange = { requester.request(group.permission) }
                )
            }

            item {
                MovaInfoBanner(
                    message = if (handle.granted) {
                        "Todo listo: MOVA Phone tiene los permisos que necesita."
                    } else {
                        "Puedes concederlos más tarde desde Ajustes → Permisos o desde esta misma pantalla."
                    },
                    tone = if (handle.granted) PillTone.Success else PillTone.Neutral
                )
            }

            item {
                MovaPrimaryButton(
                    text = if (firstRun) "Empezar a usar MOVA Phone" else "Volver",
                    onClick = {
                        if (firstRun) onFinish() else navigator.back()
                    }
                )
            }
            item {
                MovaSecondaryButton(
                    text = "Abrir ajustes del sistema",
                    onClick = onOpenSystemSettings
                )
            }
        }
    }
}
