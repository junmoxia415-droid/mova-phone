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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/**
 * Ayuda del asistente: **todo** lo que MOVA sabe hacer, con la frase exacta que entiende.
 *
 * Antes esto estaba mezclado en la pantalla del chat; ahora vive aquí, para que el chat sea
 * sólo la conversación y aquí puedas consultar la lista completa cuando quieras.
 */
@Composable
fun AssistantHelpRoute(navigator: MovaNavigator, modifier: Modifier = Modifier) {
    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Column {
                        Text(
                            text = "Lo que MOVA sabe hacer",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Dilo hablando o escríbelo en el chat. Da igual el orden de las palabras.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MovaTheme.extra.textSecondary
                        )
                    }
                }
            }

            item {
                MovaInfoBanner(
                    message = "MOVA entiende frases normales y también corrige lo que se oye mal: " +
                        "«yamar a nena» acaba encontrando a «Nena ❤️». Y si hay dos contactos con el " +
                        "mismo nombre, pregunta cuál antes de llamar.",
                    tone = PillTone.Brand
                )
            }

            items(CommandLibrary.capabilities, key = { it.id }) { capability ->
                MovaCard {
                    Text(
                        text = capability.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = capability.what,
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceXs)
                    )
                    Text(
                        text = "Se dice así:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MovaTheme.extra.textMuted,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceXs),
                        modifier = Modifier.padding(top = MovaDimens.spaceXs)
                    ) {
                        capability.examples.forEach { example ->
                            ExamplePhrase(
                                text = example,
                                onClick = { navigator.toAssistant() }
                            )
                        }
                    }
                }
            }

            item {
                MovaInfoBanner(
                    message = "Consejo: en el chat, toca el micrófono y habla normal. Si el teléfono no " +
                        "tiene instalado el idioma sin conexión, MOVA te lo dice y usa el reconocedor del " +
                        "sistema para que puedas seguir hablando.",
                    tone = PillTone.Neutral
                )
            }
        }
    }
}

/** Frase de ejemplo tocables: llevan al chat para probarla. */
@Composable
private fun ExamplePhrase(text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
