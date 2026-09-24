package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoMark
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.security.settings.MovaSettings
import com.studiolexair.movaphone.core.security.settings.MovaSettingsStore
import kotlinx.coroutines.launch

/**
 * Tu perfil en MOVA.
 *
 * El usuario pidió «que al principio te pida crear cuenta, nombre y eso… para el perfil»:
 * aquí está, pero **sin servidores ni contraseñas**. MOVA no crea cuentas en Internet;
 * el «perfil» es tu nombre en este teléfono, con el que el asistente te saluda y te llama.
 *
 * Se usa en dos momentos:
 *  - **Primer arranque** ([firstRun] = true): tras conceder los permisos, para que la app
 *    arranque sabiendo cómo llamarte.
 *  - **Cuando quieras** (Ajustes → Tu perfil): para cambiarlo.
 */
@Composable
fun ProfileRoute(
    settingsStore: MovaSettingsStore,
    navigator: MovaNavigator,
    firstRun: Boolean = false,
    onFinish: () -> Unit = { navigator.back() },
    modifier: Modifier = Modifier
) {
    val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = MovaSettings.DEFAULT)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(settings.userName) {
        if (!loaded && settings.userName != MovaSettings.DEFAULT.userName) {
            name = settings.userName
            loaded = true
        }
    }

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    MovaLogoMark(size = MovaDimens.iconXl * 2)
                    Text(
                        text = if (firstRun) "Crea tu perfil" else "Tu perfil",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = MovaDimens.spaceMd)
                    )
                    Text(
                        text = if (firstRun) {
                            "Así MOVA sabe cómo llamarte y cómo saludarte. Todo se queda en este teléfono."
                        } else {
                            "El nombre con el que MOVA te saluda y te llama."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                }
            }

            item {
                MovaCard {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        label = { Text("¿Cómo te llamas?") },
                        placeholder = { Text("Por ejemplo: Luis") },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (name.isBlank()) {
                            "Si lo dejas vacío, MOVA te hablará sin nombre."
                        } else {
                            "MOVA te saludará así: «Buenas tardes, $name»."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                }
            }

            item {
                MovaInfoBanner(
                    message = "MOVA no crea cuentas ni pide contraseñas: no hay servidores donde " +
                        "registrarte y nada de tu perfil sale del teléfono. Si algún día quieres " +
                        "sincronizar entre aparatos, hará falta un servidor propio y te lo diré " +
                        "antes de pedirte nada.",
                    tone = PillTone.Neutral
                )
            }

            if (!firstRun) {
                item {
                    MovaListRow(
                        title = "Permisos de la aplicación",
                        subtitle = "Revisa y concede lo que falte",
                        onClick = { navigator.toPermissions() }
                    )
                }
            }

            item {
                MovaPrimaryButton(
                    text = if (firstRun) "Guardar y entrar en MOVA" else "Guardar",
                    onClick = {
                        scope.launch {
                            settingsStore.setUserName(name.trim().ifBlank { MovaSettings.DEFAULT.userName })
                            if (firstRun) settingsStore.setOnboardingCompleted(true)
                            onFinish()
                        }
                    }
                )
            }

            if (firstRun) {
                item {
                    MovaSecondaryButton(
                        text = "Prefiero hacerlo luego",
                        onClick = {
                            scope.launch {
                                settingsStore.setOnboardingCompleted(true)
                                onFinish()
                            }
                        }
                    )
                }
            }
        }
    }
}
