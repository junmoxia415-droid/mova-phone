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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaDangerButton
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSectionHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSwitchRow
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.feature.assistant.ai.LocalModel

/**
 * Ajustes del asistente (el engranaje del chat).
 *
 * Aquí vive **todo lo técnico**, que antes aparecía mezclado con la conversación:
 * el modelo local (tres tamaños, con lo que ocupa cada uno y si tu teléfono puede con él)
 * y la voz de MOVA (si te contesta hablando, y si tiene el idioma instalado).
 */
@Composable
fun AssistantSettingsRoute(
    navigator: MovaNavigator,
    viewModel: SmartAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.modelState.collectAsStateWithLifecycle()

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Column {
                        Text(
                            text = "Ajustes del asistente",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "El modelo y la voz se quedan en tu teléfono",
                            style = MaterialTheme.typography.bodySmall,
                            color = MovaTheme.extra.textSecondary
                        )
                    }
                }
            }

            // ---------------- Modelo local ----------------
            item { MovaSectionHeader(text = "Inteligencia artificial en el teléfono") }
            item {
                MovaInfoBanner(
                    message = "MOVA funciona sin ningún modelo descargado: su intérprete de reglas " +
                        "entiende las órdenes de la ayuda y no necesita Internet. Un modelo hace que " +
                        "entienda además frases largas y dichas de cualquier manera.",
                    tone = PillTone.Brand
                )
            }

            items(state.models, key = { it.id }) { model ->
                ModelTierCard(
                    model = model,
                    availability = state.availability[model.id].orEmpty(),
                    downloaded = state.downloadedId == model.id,
                    downloading = state.downloadingId == model.id,
                    progress = state.progress,
                    onDownload = { viewModel.downloadModel(model) },
                    onDelete = { viewModel.deleteModel(model) }
                )
            }

            item {
                MovaSwitchRow(
                    title = "Usar el modelo para entenderte",
                    subtitle = if (state.downloadedId == null) {
                        "Descarga primero un modelo. Sin él, MOVA usa su intérprete de reglas (también sin Internet)."
                    } else {
                        "Todo el razonamiento ocurre dentro del teléfono: nada sale a Internet."
                    },
                    checked = state.useModel,
                    onCheckedChange = { viewModel.setUseModel(it) },
                    enabled = state.downloadedId != null
                )
            }

            // ---------------- Voz ----------------
            item { MovaSectionHeader(text = "La voz de MOVA") }
            item {
                MovaSwitchRow(
                    title = "Que MOVA me conteste hablando",
                    subtitle = "Lee sus respuestas en voz alta, con entonación natural, usando la voz " +
                        "del propio teléfono. Si no hay voz instalada, te lo dice y sigue por escrito.",
                    checked = state.speakReplies,
                    onCheckedChange = { viewModel.setSpeakReplies(it) }
                )
            }
            item {
                MovaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = MovaTheme.extra.violet,
                            modifier = Modifier.padding(end = MovaDimens.spaceSm)
                        )
                        Text(
                            text = "Si no te oye bien",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "MOVA compara lo que oye con su vocabulario y con los nombres de tu " +
                            "agenda, así que un dictado imperfecto sigue funcionando. Si Android no " +
                            "tiene el idioma sin conexión instalado (error 12 o 13), MOVA lo explica " +
                            "y sigue escuchando con el reconocedor del sistema, o puedes escribir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceXs)
                    )
                }
            }

            state.message?.let { message ->
                item { MovaInfoBanner(message = message, tone = PillTone.Neutral) }
            }

            item {
                MovaPrimaryButton(
                    text = "Volver al chat",
                    onClick = { navigator.back() }
                )
            }
        }
    }
}

/** Un modelo con su tamaño real, lo que aporta y si el teléfono puede con él. */
@Composable
private fun ModelTierCard(
    model: LocalModel,
    availability: String,
    downloaded: Boolean,
    downloading: Boolean,
    progress: Float,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    MovaCard {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = model.description,
            style = MaterialTheme.typography.bodySmall,
            color = MovaTheme.extra.textSecondary,
            modifier = Modifier.padding(top = MovaDimens.spaceXs)
        )
        Text(
            text = when {
                downloaded -> "Descargado y listo · se queda sólo en tu teléfono"
                downloading -> "Descargando… ${(progress * 100).toInt()} %"
                else -> availability
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (downloaded) MovaTheme.extra.success else MovaTheme.extra.textMuted,
            modifier = Modifier.padding(top = MovaDimens.spaceSm)
        )

        if (downloading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MovaDimens.spaceSm)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm),
            modifier = Modifier.padding(top = MovaDimens.spaceMd)
        ) {
            if (downloaded) {
                MovaDangerButton(
                    text = "Borrar",
                    icon = Icons.Filled.Delete,
                    onClick = onDelete
                )
            } else if (!downloading) {
                MovaPrimaryButton(
                    text = "Descargar (${model.sizeMb} MB)",
                    icon = Icons.Filled.Download,
                    onClick = onDownload
                )
            }
        }
    }
}
