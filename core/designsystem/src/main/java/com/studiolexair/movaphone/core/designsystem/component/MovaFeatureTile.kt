package com.studiolexair.movaphone.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens

/**
 * Tarjeta de acceso rápido del inicio (Llamar, Contactos, Historial, Favoritos).
 * Color e icono vienen del llamador para mantener consistencia con el mockup.
 */
@Composable
fun MovaFeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MovaDimens.tileHeight)
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(gradient))
                .padding(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(MovaDimens.iconMd)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Acción cuadrada de la fila "Funciones rápidas". */
@Composable
fun MovaQuickAction(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Column(
        modifier = modifier
            .width(MovaDimens.quickActionSize)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = accent.copy(alpha = 0.16f),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(MovaDimens.iconMd))
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(text = badge, style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
