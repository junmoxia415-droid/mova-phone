package com.studiolexair.movaphone.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme

/** Estado vacío coherente: nunca se muestra una pantalla en blanco. */
@Composable
fun MovaEmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MovaDimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MovaTheme.extra.textMuted,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MovaTheme.extra.textSecondary,
            textAlign = TextAlign.Center
        )
        action?.invoke()
    }
}

/** Aviso informativo usado cuando una función no está disponible en el dispositivo. */
@Composable
fun MovaInfoBanner(
    message: String,
    tone: PillTone = PillTone.Brand,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val color = when (tone) {
        PillTone.Success -> MovaTheme.extra.success
        PillTone.Warning -> MovaTheme.extra.warning
        PillTone.Danger -> MovaTheme.extra.danger
        PillTone.Neutral -> MovaTheme.extra.textSecondary
        PillTone.Brand -> MaterialTheme.colorScheme.primary
    }
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(MovaDimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(MovaDimens.iconSm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
