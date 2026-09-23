package com.studiolexair.movaphone.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme

/**
 * Fila estándar de listas (contactos, historial, mensajes, ajustes).
 * Se usa en toda la aplicación para que cada lista se sienta parte del mismo producto.
 */
@Composable
fun MovaListRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MovaTheme.extra.border.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { base ->
                    if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base
                }
                .padding(horizontal = MovaDimens.spaceMd, vertical = MovaDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            if (leading != null) leading()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (accent != null) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/** Avatar circular con iniciales (cuando no hay foto disponible). */
@Composable
fun MovaAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    photoUri: String? = null
) {
    Surface(
        modifier = modifier.size(46.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

/** Etiqueta de estado: correcto (verde), advertencia (ámbar), error/spam (rojo), neutral. */
@Composable
fun MovaStatusPill(
    text: String,
    tone: PillTone,
    modifier: Modifier = Modifier
) {
    val color = when (tone) {
        PillTone.Success -> MovaTheme.extra.success
        PillTone.Warning -> MovaTheme.extra.warning
        PillTone.Danger -> MovaTheme.extra.danger
        PillTone.Neutral -> MovaTheme.extra.textSecondary
        PillTone.Brand -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

enum class PillTone { Success, Warning, Danger, Neutral, Brand }
