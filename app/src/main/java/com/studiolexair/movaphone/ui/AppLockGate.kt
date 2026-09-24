package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoMark
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.security.settings.MovaSettings
import com.studiolexair.movaphone.di.MovaContainer
import kotlinx.coroutines.launch

/**
 * Puerta de bloqueo de MOVA Phone (requisito 16).
 * Se muestra cuando el usuario activó el bloqueo y la sesión está cerrada:
 * desbloquea con PIN o con biometría real del dispositivo.
 */
@Composable
fun AppLockGate(container: MovaContainer, settings: MovaSettings) {
    val unlocked by container.appLockController.isUnlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()) }

    // ¿Hay alguna forma real de desbloquear? PIN guardado o biometría disponible.
    var pinIsSet by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(settings.appLockEnabled, settings.biometricEnabled) {
        pinIsSet = container.pinManager.isPinSet()
    }
    val biometricUsable = settings.biometricEnabled && container.biometricManager.isAvailable()
    val canUnlock = pinIsSet == true || biometricUsable

    // Red de seguridad: bloqueo activado sin PIN ni biometría -> se desactiva y se entra igual.
    LaunchedEffect(settings.appLockEnabled, canUnlock) {
        if (settings.appLockEnabled && !canUnlock) {
            container.settingsStore.setAppLock(false)
            container.appLockController.unlock()
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("¿Restablecer el bloqueo?") },
            text = {
                Text(
                    "Se desactivará el bloqueo y se borrará el PIN guardado para que puedas entrar. " +
                        "Tus contactos, llamadas y mensajes NO se borran. Después podrás crear un PIN nuevo."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showResetDialog = false
                    scope.launch {
                        container.pinManager.clearPin()
                        container.settingsStore.setPinEnabled(false)
                        container.settingsStore.setBiometric(false)
                        container.settingsStore.setAppLock(false)
                        container.appLockController.unlock()
                    }
                }) { Text("Restablecer") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (!settings.appLockEnabled || !canUnlock || unlocked) return

    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MovaDimens.spaceXl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MovaLogoMark(size = MovaDimens.logoMarkSize)
            Text(
                text = "MOVA Phone está bloqueado",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = MovaDimens.spaceLg)
            )
            Text(
                text = "Verifica tu identidad. Coloca tu huella o mira a la cámara.",
                style = MaterialTheme.typography.bodySmall,
                color = MovaTheme.extra.textSecondary,
                modifier = Modifier.padding(top = MovaDimens.spaceSm)
            )

            message?.let {
                MovaInfoBanner(
                    message = it,
                    tone = PillTone.Warning,
                    icon = Icons.Filled.Lock,
                    modifier = Modifier.padding(top = MovaDimens.spaceLg)
                )
            }

            OutlinedTextField(
                value = pin,
                onValueChange = { value -> pin = value.filter { it.isDigit() }.take(8) },
                label = { Text("PIN") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MovaDimens.spaceLg)
            )
            MovaPrimaryButton(
                text = "Desbloquear",
                icon = Icons.Filled.Lock,
                enabled = pin.length >= 4,
                modifier = Modifier.padding(top = MovaDimens.spaceMd),
                onClick = {
                    scope.launch {
                        if (container.pinManager.verifyPin(pin)) {
                            container.securityEventLogger.log(
                                com.studiolexair.movaphone.core.security.event.SecurityEventType.APP_UNLOCKED,
                                "Desbloqueo correcto con PIN"
                            )
                            container.appLockController.unlock()
                            container.onAppUnlocked()
                        } else {
                            container.securityEventLogger.log(
                                com.studiolexair.movaphone.core.security.event.SecurityEventType.PIN_FAILED,
                                "PIN incorrecto",
                                com.studiolexair.movaphone.core.security.event.Severity.WARNING
                            )
                            message = "PIN incorrecto. Inténtalo de nuevo."
                            pin = ""
                        }
                    }
                }
            )

            androidx.compose.material3.TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.padding(top = MovaDimens.spaceSm)
            ) {
                Text("He olvidado el PIN")
            }

            if (settings.biometricEnabled && container.biometricManager.isAvailable()) {
                MovaSecondaryButton(
                    text = "Usar huella o rostro",
                    icon = Icons.Filled.Fingerprint,
                    modifier = Modifier.padding(top = MovaDimens.spaceSm),
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity == null) {
                            message = "La biometría necesita una actividad compatible."
                        } else {
                            container.biometricManager.prompt(
                                activity = activity,
                                title = "Desbloquear MOVA Phone",
                                subtitle = "Confirma tu identidad para continuar",
                                onSuccess = {
                                    container.appLockController.unlock()
                                    container.onAppUnlocked()
                                },
                                onError = { message = it }
                            )
                        }
                    }
                )
            }
        }
    }
}
