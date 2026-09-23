package com.studiolexair.movaphone.feature.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaDangerButton
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.domain.emergency.model.EmergencySession
import com.studiolexair.movaphone.domain.emergency.model.StepStatus
import kotlinx.coroutines.delay

/**
 * Emergencia activa: cronómetro, estado real de cada paso del protocolo y
 * botón de cancelación siempre accesible (el usuario mantiene el control).
 */
@Composable
fun EmergencyActiveScreen(
    session: EmergencySession,
    viewModel: EmergencyViewModel,
    navigator: MovaNavigator,
    modifier: Modifier = Modifier
) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(session.startedAt) {
        while (true) {
            elapsed = System.currentTimeMillis() - session.startedAt
            delay(1_000)
        }
    }

    AuroraBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            Text(
                text = "EMERGENCIA ACTIVA",
                style = MaterialTheme.typography.labelLarge,
                color = MovaTheme.extra.danger,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = TextFormatters.stopwatch(elapsed),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Contactando a tus contactos de emergencia...",
                style = MaterialTheme.typography.bodyMedium,
                color = MovaTheme.extra.textSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            MovaCard {
                session.steps.forEach { step ->
                    MovaListRow(
                        title = step.label,
                        subtitle = step.detail,
                        leading = {
                            Icon(
                                imageVector = when (step.status) {
                                    StepStatus.DONE -> Icons.Filled.CheckCircle
                                    StepStatus.FAILED -> Icons.Filled.Error
                                    StepStatus.RUNNING -> Icons.Filled.Sync
                                    else -> Icons.Filled.HourglassEmpty
                                },
                                contentDescription = null,
                                tint = when (step.status) {
                                    StepStatus.DONE -> MovaTheme.extra.success
                                    StepStatus.FAILED -> MovaTheme.extra.danger
                                    else -> MovaTheme.extra.textMuted
                                }
                            )
                        }
                    )
                }
            }

            if (session.steps.any { it.status == StepStatus.FAILED }) {
                MovaSecondaryButton(
                    text = "Reintentar los pasos que fallaron",
                    icon = Icons.Filled.Sync,
                    onClick = viewModel::retryFailedSteps
                )
            }

            MovaSecondaryButton(
                text = "Compartir ubicación y ver historial",
                onClick = { navigator.toLocation() }
            )

            MovaDangerButton(
                text = "CANCELAR EMERGENCIA",
                onClick = viewModel::cancelSos
            )

            session.batteryPercent?.let { battery ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Batería del dispositivo: ${TextFormatters.batteryLevel(battery)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary
                    )
                }
            }
        }
    }
}
