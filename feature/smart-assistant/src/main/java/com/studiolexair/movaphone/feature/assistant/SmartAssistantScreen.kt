package com.studiolexair.movaphone.feature.assistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaDangerButton
import com.studiolexair.movaphone.core.designsystem.component.MovaFeatureTile
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/**
 * Asistente inteligente de MOVA Phone.
 * La voz la transcribe el reconocedor del sistema; la interpretación es local.
 * Toda acción sensible se confirma antes de ejecutarse.
 */
@Composable
fun SmartAssistantRoute(
    navigator: MovaNavigator,
    viewModel: SmartAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) viewModel.interpret(spoken, navigator::openFromAssistant)
        }
    }

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                MovaScreenHeader(
                    title = "Asistente MOVA",
                    subtitle = "Órdenes por voz o escritas, siempre bajo tu control"
                )
            }

            item {
                MovaPrimaryButton(
                    text = "Hablar",
                    icon = Icons.Filled.Mic,
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di una orden para MOVA Phone")
                        }
                        runCatching { voiceLauncher.launch(intent) }
                    }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("O escribe la orden") },
                        modifier = Modifier.weight(1f)
                    )
                    MovaSecondaryButton(
                        text = "Enviar",
                        icon = Icons.Filled.Send,
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.interpret(textInput, navigator::openFromAssistant)
                                textInput = ""
                            }
                        }
                    )
                }
            }

            state.transcript.takeIf { it.isNotBlank() }?.let { heard ->
                item { MovaInfoBanner(message = "Escuché: «$heard»", tone = PillTone.Brand, icon = Icons.Filled.AutoAwesome) }
            }

            state.command?.let { command ->
                item {
                    MovaCard {
                        Text(
                            text = describe(command),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (command.isSensitive && state.awaitingConfirmation) {
                            Text(
                                text = "Esta acción es sensible. Confírmala para continuar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MovaTheme.extra.warning,
                                modifier = Modifier.padding(vertical = MovaDimens.spaceSm)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                                MovaPrimaryButton(text = "Confirmar", onClick = viewModel::confirm)
                                MovaDangerButton(text = "Cancelar", onClick = viewModel::cancel)
                            }
                        }
                    }
                }
            }

            state.result?.let { result ->
                item { MovaInfoBanner(message = result, tone = PillTone.Success) }
            }

            item {
                Text(
                    text = "Ejemplos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MovaTheme.extra.textMuted
                )
            }
            item { AssistantExample("«Llamar a mamá»", "Pide confirmación antes de llamar") }
            item { AssistantExample("«Enviar mensaje a Luis diciendo llego tarde»", "Envía el SMS real") }
            item { AssistantExample("«Compartir mi ubicación»", "Obtiene posición y enlace de mapas") }
            item { AssistantExample("«Abrir seguridad»", "Navega por la aplicación") }
            item { AssistantExample("«Emergencia»", "Inicia el SOS tras confirmarlo") }

            if (state.history.isNotEmpty()) {
                item {
                    Text(
                        text = "Últimas órdenes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MovaTheme.extra.textMuted
                    )
                }
                items(state.history) { entry ->
                    Text(
                        text = "· $entry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary
                    )
                }
            }
        }
    }
}

/** Fila de ejemplo: misma tarjeta que usa el inicio, con un icono temático. */
@Composable
private fun AssistantExample(title: String, subtitle: String) {
    MovaFeatureTile(
        title = title,
        subtitle = subtitle,
        icon = Icons.Filled.AutoAwesome,
        gradient = MovaTheme.extra.tileGradientViolet,
        onClick = {}
    )
}

private fun describe(command: SmartCommand): String = when (command) {
    is SmartCommand.Open -> "Abrir ${command.destination.label}"
    is SmartCommand.Call -> "Llamar a ${command.target}"
    is SmartCommand.SendMessage -> "Enviar mensaje a ${command.target}: «${command.body}»"
    SmartCommand.ShareLocation -> "Compartir mi ubicación actual"
    SmartCommand.StartEmergency -> "Iniciar el protocolo de emergencia"
    is SmartCommand.Unknown -> "Orden no reconocida: ${command.heard}"
}

/** Traduce el destino entendido por el asistente a navegación real. */
fun MovaNavigator.openFromAssistant(destination: SmartCommand.Destination) {
    when (destination) {
        SmartCommand.Destination.HOME -> toHome()
        SmartCommand.Destination.DIALER -> toDialer()
        SmartCommand.Destination.CALLS -> toCalls()
        SmartCommand.Destination.CONTACTS -> toContacts()
        SmartCommand.Destination.MESSAGES -> toMessages()
        SmartCommand.Destination.SECURITY -> toSecurity()
        SmartCommand.Destination.LOCATION -> toLocation()
        SmartCommand.Destination.AUTOMATION -> toAutomation()
        SmartCommand.Destination.SETTINGS -> toSettings()
        SmartCommand.Destination.SOS -> toSos()
        SmartCommand.Destination.DRIVING -> toDriving()
    }
}
