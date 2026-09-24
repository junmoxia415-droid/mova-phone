package com.studiolexair.movaphone.feature.assistant

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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle
import com.studiolexair.movaphone.feature.assistant.ai.LocalModel
import com.studiolexair.movaphone.feature.assistant.voice.OfflineVoiceInput

/**
 * Asistente inteligente de MOVA Phone.
 *
 * La voz se transcribe **en el propio teléfono** (sin Internet) y la interpretación la hace
 * el intérprete local de MOVA o el modelo de lenguaje que el usuario descargue en el móvil.
 * Toda acción sensible se confirma antes de ejecutarse.
 */
@Composable
fun SmartAssistantRoute(
    navigator: MovaNavigator,
    viewModel: SmartAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }

    val micPermission = rememberPermissionHandle(listOf(MovaPermission.RECORD_AUDIO))
    val voice = remember(context) { OfflineVoiceInput(context) }
    val offlineAvailable = remember(context) { voice.isOnDeviceAvailable() }

    DisposableEffect(voice) {
        onDispose { voice.stop() }
    }

    fun listen() {
        viewModel.onVoiceStarted()
        voice.start(
            languageTag = "es-ES",
            onPartial = { viewModel.onVoicePartial(it) },
            onResult = { spoken ->
                viewModel.onVoiceResult(spoken)
                viewModel.interpret(spoken, navigator::openFromAssistant)
            },
            onError = { viewModel.onVoiceError(it) }
        )
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
                    subtitle = "Voz sin conexión y razonamiento en tu teléfono"
                )
            }

            if (!offlineAvailable) {
                item {
                    MovaInfoBanner(
                        message = "Para entenderte sin Internet, Android necesita el paquete de voz en el " +
                            "teléfono: Ajustes → Sistema → Idiomas → Voz → Descargar. Si no, MOVA usará lo que " +
                            "tenga el sistema y podrás escribir la orden.",
                        tone = PillTone.Warning,
                        icon = Icons.Filled.CloudOff
                    )
                }
            }

            item {
                MovaPrimaryButton(
                    text = when {
                        state.listening -> "Detener y escuchar"
                        else -> "Hablar"
                    },
                    icon = if (state.listening) Icons.Filled.MicOff else Icons.Filled.Mic,
                    onClick = {
                        if (!micPermission.granted) {
                            micPermission.request()
                        } else if (state.listening) {
                            voice.stop()
                            viewModel.onVoiceStopped()
                        } else {
                            listen()
                        }
                    }
                )
            }

            state.partial.takeIf { it.isNotBlank() }?.let { partial ->
                item { MovaInfoBanner(message = "Te estoy oyendo: «$partial»", tone = PillTone.Brand, icon = Icons.Filled.Mic) }
            }

            state.voiceNotice?.let { notice ->
                item { MovaInfoBanner(message = notice, tone = PillTone.Warning, icon = Icons.Filled.MicOff) }
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
                item {
                    MovaInfoBanner(
                        message = "Escuché: «$heard»" + (state.interpretedBy?.let { " · interpretado por $it" } ?: ""),
                        tone = PillTone.Brand,
                        icon = Icons.Filled.AutoAwesome
                    )
                }
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

            // ---------- IA local: modelo descargado en el teléfono ----------
            item {
                Text(
                    text = "Inteligencia artificial en el teléfono",
                    style = MaterialTheme.typography.labelMedium,
                    color = MovaTheme.extra.textMuted
                )
            }

            item {
                MovaInfoBanner(
                    message = "MOVA puede razonar lo que dices con un modelo pequeño guardado en el propio " +
                        "teléfono. No hace falta cuenta, ni nube, ni conexión para usarlo una vez descargado.",
                    tone = PillTone.Brand,
                    icon = Icons.Filled.AutoAwesome
                )
            }

            items(modelState.models, key = { it.id }) { model ->
                ModelCard(
                    model = model,
                    downloaded = modelState.downloadedId == model.id,
                    downloading = modelState.downloadingId == model.id,
                    progress = modelState.progress,
                    onDownload = { viewModel.downloadModel(model) },
                    onDelete = { viewModel.deleteModel(model) }
                )
            }

            item {
                MovaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Usar el modelo para entenderte", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (modelState.downloadedId == null) {
                                    "Descarga primero un modelo. Sin modelo, MOVA usa su intérprete de reglas (también sin Internet)."
                                } else {
                                    "Todo el razonamiento ocurre dentro del teléfono."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MovaTheme.extra.textSecondary
                            )
                        }
                        Switch(checked = modelState.useModel, onCheckedChange = { viewModel.setUseModel(it) })
                    }
                }
            }

            modelState.message?.let { message ->
                item { MovaInfoBanner(message = message, tone = PillTone.Neutral) }
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

/** Tarjeta de un modelo: tamaño, estado real y botón de descargar o borrar. */
@Composable
private fun ModelCard(
    model: LocalModel,
    downloaded: Boolean,
    downloading: Boolean,
    progress: Float,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    MovaCard {
        Column(verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
            Text(model.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MovaTheme.extra.textSecondary
            )
            Text(
                text = when {
                    downloaded -> "Descargado y listo para funcionar sin conexión"
                    downloading -> "Descargando… ${(progress * 100).toInt()} %"
                    else -> "No descargado"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (downloaded) MovaTheme.extra.success else MovaTheme.extra.textMuted
            )
            if (downloading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                if (downloaded) {
                    MovaSecondaryButton(text = "Borrar", icon = Icons.Filled.Delete, onClick = onDelete)
                } else {
                    MovaPrimaryButton(
                        text = "Descargar",
                        icon = Icons.Filled.Download,
                        enabled = !downloading,
                        onClick = onDownload
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
