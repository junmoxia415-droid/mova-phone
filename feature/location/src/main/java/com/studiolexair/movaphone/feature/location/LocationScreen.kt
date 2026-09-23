package com.studiolexair.movaphone.feature.location

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaPalette
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/**
 * Pantalla de ubicación sin dependencia de Google Maps: se muestra la posición real
 * con un radar propio y se ofrece copiar coordenadas o compartirlas por cualquier app.
 * Cuando exista integración de mapas (Google/OSM) se añadirá aquí sin reescribir el módulo.
 */
@Composable
fun LocationRoute(
    navigator: MovaNavigator,
    viewModel: LocationViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MovaDimens.spaceLg,
                end = MovaDimens.spaceLg,
                bottom = MovaDimens.spaceXxl
            ),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                MovaScreenHeader(
                    title = "Mi ubicación",
                    subtitle = if (state.hasFix) "Precisión: ${TextFormatters.accuracy(state.accuracyMeters ?: 0f)}" else "Sin posición todavía"
                )
            }

            if (!state.permissionGranted) {
                item {
                    MovaInfoBanner(
                        message = "MOVA Phone necesita permiso de ubicación para esta función. Puedes concederlo cuando la uses.",
                        tone = PillTone.Warning,
                        icon = Icons.Filled.LocationOn
                    )
                }
            } else if (!state.isLocationEnabled) {
                item {
                    MovaInfoBanner(
                        message = "La ubicación del dispositivo está desactivada. Actívala en los ajustes del sistema.",
                        tone = PillTone.Warning,
                        icon = Icons.Filled.LocationOn
                    )
                }
            }

            item {
                MovaCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f),
                        contentAlignment = Alignment.Center
                    ) {
                        RadarView(hasFix = state.hasFix)
                    }
                    if (state.hasFix) {
                        Text(
                            text = state.coordinates,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Proveedor: ${state.provider ?: "-"} · actualizado ${TextFormatters.clock(state.updatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MovaTheme.extra.textSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = state.errorMessage ?: "Pulsa «Obtener ubicación» para localizarte.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MovaTheme.extra.textSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                MovaPrimaryButton(
                    text = if (state.loading) "Obteniendo ubicación..." else "Obtener ubicación",
                    icon = Icons.Filled.MyLocation,
                    enabled = !state.loading,
                    onClick = viewModel::requestFreshLocation
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                    MovaSecondaryButton(
                        text = "Copiar",
                        icon = Icons.Filled.ContentCopy,
                        modifier = Modifier.weight(1f),
                        enabled = state.hasFix,
                        onClick = { copyToClipboard(context, state.coordinates) }
                    )
                    MovaSecondaryButton(
                        text = "Compartir",
                        icon = Icons.Filled.Share,
                        modifier = Modifier.weight(1f),
                        enabled = state.hasFix,
                        onClick = { shareLocation(context, viewModel.shareText()) }
                    )
                }
            }

            item {
                MovaSecondaryButton(
                    text = "Ver historial de ubicaciones",
                    onClick = { navigator.toLocationHistory() }
                )
            }

            item { Text(text = "HISTORIAL RECIENTE", style = MaterialTheme.typography.labelMedium, color = MovaTheme.extra.textMuted) }

            if (history.isEmpty()) {
                item {
                    MovaListRow(
                        title = "Sin registros",
                        subtitle = "El historial de ubicación es opcional y está desactivado por defecto."
                    )
                }
            } else {
                items(history, key = { it.id }) { record ->
                    MovaListRow(
                        title = TextFormatters.coordinates(record.latitude, record.longitude),
                        subtitle = "${record.source} · ${TextFormatters.accuracy(record.accuracyMeters)} · ${TextFormatters.relativeDay(record.recordedAt)}"
                    )
                }
            }
        }
    }
}

/** Radar propio: sin mapas externos, pero mostrando que hay o no señal real. */
@Composable
private fun RadarView(hasFix: Boolean) {
    val extra = MovaTheme.extra
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2.2f
        val center = Offset(size.width / 2f, size.height / 2f)
        (1..3).forEach { ring ->
            drawCircle(
                color = MovaPalette.SkyBlue.copy(alpha = 0.18f),
                radius = radius * ring / 3f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
        drawCircle(
            color = if (hasFix) extra.success.copy(alpha = 0.85f) else extra.danger.copy(alpha = 0.8f),
            radius = if (hasFix) 18f else 12f,
            center = center
        )
        drawLine(
            color = MovaPalette.Cyan.copy(alpha = 0.5f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 2f
        )
        drawLine(
            color = MovaPalette.Cyan.copy(alpha = 0.5f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 2f
        )
    }
}

private fun copyToClipboard(context: Context, value: String) {
    if (value.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Ubicación MOVA Phone", value))
}

private fun shareLocation(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir ubicación"))
}
