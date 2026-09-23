package com.studiolexair.movaphone.feature.driving

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import androidx.compose.runtime.collectAsState

/**
 * Modo conducción (requisito 23): interfaz simplificada, botones grandes,
 * favoritos a un toque y comandos de voz mediante el reconocedor del sistema.
 * No se hace ninguna acción que Android prohíba en conducción.
 */
@Composable
fun DrivingRoute(
    navigator: MovaNavigator,
    contactsRepository: ContactsRepository,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by contactsRepository.observeFavorites().collectAsState(initial = emptyList())

    // El modo conducción es un disparador real de automatizaciones: entrar aquí lo lanza.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.studiolexair.movaphone.data.automation.receiver.AutomationEventBridge.fire(
            com.studiolexair.movaphone.domain.automation.model.TriggerType.DRIVING_DETECTED
        )
    }
    val context = LocalContext.current

    AuroraBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceLg)
        ) {
            MovaScreenHeader(
                title = "Modo conducción",
                subtitle = "Enfócate en la carretera"
            )

            MovaPrimaryButton(
                text = "Comando de voz",
                icon = Icons.Filled.Mic,
                onClick = { launchVoiceCommand(context) }
            )

            MovaSecondaryButton(
                text = "Llamada de emergencia",
                icon = Icons.Filled.Emergency,
                accent = MovaTheme.extra.danger,
                onClick = { navigator.toSos() }
            )

            Text(
                text = "Favoritos",
                style = MaterialTheme.typography.labelMedium,
                color = MovaTheme.extra.textMuted
            )

            if (favorites.isEmpty()) {
                MovaEmptyState(
                    title = "Sin favoritos",
                    description = "Marca contactos como favoritos para tenerlos a mano mientras conduces.",
                    icon = Icons.Filled.Call
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)) {
                    items(favorites, key = { it.id }) { contact ->
                        DrivingContactRow(contact = contact, onCall = { navigator.toDialer(contact.phoneNumber) })
                    }
                }
            }

            MovaSecondaryButton(
                text = "Salir del modo conducción",
                icon = Icons.Filled.Close,
                onClick = onExit
            )
        }
    }
}

@Composable
private fun DrivingContactRow(contact: Contact, onCall: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MovaTheme.extra.border)
    ) {
        Row(
            modifier = Modifier.padding(MovaDimens.spaceLg),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceLg)
        ) {
            MovaAvatar(initials = contact.initials, accent = MovaTheme.extra.success)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MovaTheme.extra.textSecondary
                )
            }
            com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton(
                text = "Llamar",
                icon = Icons.Filled.Call,
                onClick = onCall
            )
        }
    }
}

/** Lanza el reconocedor de voz del sistema (API oficial, sin permisos extra). */
private fun launchVoiceCommand(context: Context) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Di un comando: llamar a mamá, abrir mensajes...")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private val Int.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(this.toFloat())
