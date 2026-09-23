package com.studiolexair.movaphone.feature.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.HoldToConfirmButton
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaQuickAction
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaPalette
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.PermissionPrompt
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle

/**
 * Pantalla SOS: mantenimiento de 3 segundos con anillo de progreso y cuenta regresiva.
 * Si el usuario suelta antes, no se activa nada (protección contra activaciones accidentales).
 */
@Composable
fun SosRoute(
    navigator: MovaNavigator,
    viewModel: EmergencyViewModel,
    holdMillis: Long = 3_000L,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()

    // El protocolo SOS necesita ubicación, SMS y llamadas: se piden aquí, en contexto.
    val sosPermissions = rememberPermissionHandle(
        listOf(
            MovaPermission.SEND_SMS,
            MovaPermission.FINE_LOCATION,
            MovaPermission.COARSE_LOCATION,
            MovaPermission.CALL_PHONE
        )
    )

    val activeSession = session
    if (activeSession != null && activeSession.active) {
        EmergencyActiveScreen(session = activeSession, viewModel = viewModel, navigator = navigator, modifier = modifier)
        return
    }

    AuroraBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MovaDimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceLg)
        ) {
            Text(
                text = "Emergencia",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Mantén presionado durante ${holdMillis / 1000} segundos para activar el protocolo de emergencia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MovaTheme.extra.textSecondary,
                textAlign = TextAlign.Center
            )

            if (contacts.isEmpty()) {
                MovaInfoBanner(
                    message = "Aún no tienes contactos de emergencia. Configúralos para que el protocolo pueda avisar a alguien.",
                    tone = PillTone.Warning,
                    icon = Icons.Filled.Emergency
                )
            } else {
                MovaInfoBanner(
                    message = "Se avisará a: " + contacts.joinToString(", ") { "${it.name} (${it.priority}°)" },
                    tone = PillTone.Brand,
                    icon = Icons.Filled.Emergency
                )
            }

            if (!sosPermissions.granted) {
                PermissionPrompt(
                    permissions = sosPermissions.missing,
                    title = "Permisos del protocolo de emergencia",
                    onRequest = { sosPermissions.request() }
                )
            }

            message?.let { MovaInfoBanner(message = it, tone = PillTone.Success) }

            HoldToConfirmButton(
                label = "SOS",
                onConfirmed = {
                    com.studiolexair.movaphone.data.automation.receiver.AutomationEventBridge.fire(
                        com.studiolexair.movaphone.domain.automation.model.TriggerType.SOS_ACTIVATED
                    )
                    viewModel.triggerSos()
                },
                holdMillis = holdMillis,
                dangerColors = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MovaQuickAction(
                    title = "Llamar contactos",
                    icon = Icons.Filled.Phone,
                    accent = MovaPalette.Success,
                    onClick = { navigator.toEmergencyContacts() }
                )
                MovaQuickAction(
                    title = "Enviar SMS",
                    icon = Icons.Filled.Message,
                    accent = MovaPalette.Cyan,
                    onClick = { navigator.toMessages() }
                )
                MovaQuickAction(
                    title = "Compartir ubicación",
                    icon = Icons.Filled.LocationOn,
                    accent = MovaPalette.SkyBlue,
                    onClick = { navigator.toLocation() }
                )
            }

            MovaSecondaryButton(
                text = "Configurar contactos de emergencia",
                onClick = { navigator.toEmergencyContacts() }
            )
        }
    }
}
