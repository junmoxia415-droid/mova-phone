package com.studiolexair.movaphone.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens

@Composable
fun MovaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MovaDimens.minTouchTarget),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(MovaDimens.iconSm))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun MovaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color? = null,
    enabled: Boolean = true
) {
    val tint = accent ?: MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MovaDimens.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(MovaDimens.iconSm))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun MovaDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MovaDimens.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = com.studiolexair.movaphone.core.designsystem.theme.MovaPalette.DangerDeep,
            contentColor = Color.White
        )
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(MovaDimens.iconSm))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Fila de ajuste con interruptor, reutilizada en todos los módulos de configuración. */
@Composable
fun MovaSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MovaDimens.spaceMd, vertical = MovaDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.studiolexair.movaphone.core.designsystem.theme.MovaTheme.extra.textSecondary
                    )
                }
            }
            // El interruptor responde al instante: se pinta el cambio antes de que termine
            // el guardado real y se sincroniza cuando llega el valor definitivo.
            val localState = androidx.compose.runtime.remember(checked) {
                androidx.compose.runtime.mutableStateOf(checked)
            }
            androidx.compose.material3.Switch(
                checked = localState.value,
                onCheckedChange = { value ->
                    localState.value = value
                    onCheckedChange(value)
                },
                enabled = enabled
            )
        }
    }
}
