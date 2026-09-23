package com.studiolexair.movaphone.core.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme

/** Tarjeta que explica por qué se necesita un permiso y permite solicitarlo. */
@Composable
fun PermissionRequestCard(
    permission: MovaPermission,
    granted: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (granted) MovaTheme.extra.success.copy(alpha = 0.5f) else MovaTheme.extra.border
        )
    ) {
        Column(modifier = Modifier.padding(MovaDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(MovaDimens.iconSm))
                Text(text = permission.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = permission.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MovaTheme.extra.textSecondary
            )
            if (granted) {
                Text(
                    text = "Permiso concedido",
                    style = MaterialTheme.typography.labelMedium,
                    color = MovaTheme.extra.success
                )
            } else {
                MovaSecondaryButton(text = "Conceder permiso", onClick = onRequest)
            }
        }
    }
}

/**
 * Aviso con varios permisos a la vez: se usa cuando una pantalla necesita más de uno
 * (por ejemplo Mensajes: enviar, recibir y leer SMS).
 */
@Composable
fun PermissionPrompt(
    permissions: List<MovaPermission>,
    title: String,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (permissions.isEmpty()) return
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MovaTheme.extra.border)
    ) {
        Column(
            modifier = Modifier.padding(MovaDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MovaDimens.iconSm)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            permissions.forEach { permission ->
                Text(
                    text = "• ${permission.title}: ${permission.rationale}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MovaTheme.extra.textSecondary
                )
            }
            MovaSecondaryButton(text = "Conceder permisos", onClick = onRequest)
        }
    }
}

/** Aviso de que una función concreta no está disponible (requisito 35). */
@Composable
fun UnavailableFeatureDialog(
    featureName: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = featureName) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        }
    )
}
