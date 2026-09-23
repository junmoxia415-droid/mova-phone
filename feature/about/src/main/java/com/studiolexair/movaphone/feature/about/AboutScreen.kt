package com.studiolexair.movaphone.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.studiolexair.movaphone.core.designsystem.branding.BrandConfig
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoLockup
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/**
 * Acerca de MOVA Phone: versión, desarrollador, tecnologías y licencias.
 * Requisito del proyecto: Ajustes → Acerca de → Créditos con versión, build, año,
 * tecnologías, licencias y política de privacidad.
 */
@Composable
fun AboutRoute(
    navigator: MovaNavigator,
    versionName: String,
    versionCode: Int,
    buildType: String,
    modifier: Modifier = Modifier
) {
    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item { MovaScreenHeader(title = "Acerca de", subtitle = "Información de la aplicación") }

            item {
                MovaLogoLockup(showTagline = false)
            }

            item {
                MovaCard {
                    Text(
                        text = BrandConfig.DISPLAY_NAME,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Versión $versionName (build $versionCode)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MovaTheme.extra.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Compilación: $buildType",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = BrandConfig.TAGLINE.replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                }
            }

            item {
                MovaCard {
                    Text(
                        text = BrandConfig.DEVELOPER_CREDIT,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "© ${BrandConfig.COPYRIGHT_YEAR} ${BrandConfig.DEVELOPER}. Todos los derechos reservados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                MovaListRow(
                    title = "Créditos y tecnologías",
                    subtitle = "Kotlin, Compose, Room, WorkManager…",
                    leading = { androidx.compose.material3.Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { navigator.toCredits() }
                )
            }
            item {
                MovaListRow(
                    title = "Política de privacidad",
                    subtitle = "Qué datos usamos y por qué",
                    leading = { androidx.compose.material3.Icon(Icons.Filled.Policy, contentDescription = null, tint = MovaTheme.extra.success) },
                    onClick = { navigator.toPrivacy() }
                )
            }
            item {
                MovaListRow(
                    title = "Soporte",
                    subtitle = BrandConfig.SUPPORT_EMAIL,
                    leading = { androidx.compose.material3.Icon(Icons.Filled.Email, contentDescription = null, tint = MovaTheme.extra.violet) }
                )
            }
            item {
                MovaInfoBanner(
                    message = "MOVA Phone funciona sin conexión y sin cuentas: tus datos se quedan en tu teléfono.",
                    tone = PillTone.Success,
                    icon = Icons.Filled.Verified
                )
            }
        }
    }
}

/** Créditos: tecnologías reales usadas y licencias de código abierto. */
@Composable
fun CreditsRoute(modifier: Modifier = Modifier) {
    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
        ) {
            item { MovaScreenHeader(title = "Créditos", subtitle = "Tecnologías y licencias") }

            item {
                MovaCard {
                    Text(
                        text = "Desarrollado por Studio Lexair",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MOVA Phone se construye con tecnología nativa de Android. Sin WebView, sin capas ocultas: cada función usa la API del sistema que corresponde.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                }
            }

            item { Text("Tecnologías", style = MaterialTheme.typography.labelMedium, color = MovaTheme.extra.textMuted) }
            items(technologies) { entry ->
                MovaListRow(title = entry.name, subtitle = entry.role)
            }

            item { Text("Licencias de terceros", style = MaterialTheme.typography.labelMedium, color = MovaTheme.extra.textMuted) }
            items(licenses) { entry ->
                MovaListRow(
                    title = entry.name,
                    subtitle = entry.license,
                    leading = { androidx.compose.material3.Icon(Icons.Filled.Lock, contentDescription = null, tint = MovaTheme.extra.textMuted) }
                )
            }

            item {
                MovaInfoBanner(
                    message = "Este proyecto respeta las licencias Apache 2.0 de las bibliotecas de AndroidX y las condiciones de uso de cada componente.",
                    tone = PillTone.Neutral,
                    icon = Icons.Filled.Info
                )
            }
            item {
                MovaInfoBanner(
                    message = "Gracias por confiar en MOVA Phone para tus llamadas, tu seguridad y tus emergencias.",
                    tone = PillTone.Brand,
                    icon = Icons.Filled.Favorite
                )
            }
        }
    }
}

/** Política de privacidad en lenguaje claro. */
@Composable
fun PrivacyRoute(modifier: Modifier = Modifier) {
    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
        ) {
            item { MovaScreenHeader(title = "Privacidad", subtitle = "Sin cuentas, sin rastreo, sin servidores") }
            items(privacyPoints) { point ->
                MovaListRow(
                    title = point.title,
                    subtitle = point.detail,
                    leading = { androidx.compose.material3.Icon(Icons.Filled.Lock, contentDescription = null, tint = MovaTheme.extra.success) }
                )
            }
            item {
                MovaCard {
                    Text(
                        text = "Contacto del desarrollador",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${BrandConfig.DEVELOPER} · ${BrandConfig.SUPPORT_EMAIL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary
                    )
                    Text(
                        text = "© ${BrandConfig.COPYRIGHT_YEAR} ${BrandConfig.DEVELOPER}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MovaTheme.extra.textMuted
                    )
                }
            }
        }
    }
}

private data class Entry(val name: String, val role: String)
private data class LicenseEntry(val name: String, val license: String)
private data class PrivacyPoint(val title: String, val detail: String)

private val technologies = listOf(
    Entry("Kotlin", "Lenguaje principal 100 % nativo"),
    Entry("Jetpack Compose + Material 3", "Interfaz declarativa del mockup"),
    Entry("Coroutines y Flow", "Concurrencia y datos reactivos"),
    Entry("ViewModel + Navigation", "MVVM y navegación tipada"),
    Entry("Room (SQLite)", "Base de datos local cifrable"),
    Entry("DataStore", "Preferencias del usuario"),
    Entry("WorkManager", "Tareas en segundo plano con batería responsable"),
    Entry("Telephony, SmsManager, LocationManager, CallLog", "APIs reales del sistema"),
    Entry("Android Keystore + BiometricPrompt", "Seguridad del PIN y bloqueo de la app"),
    Entry("Arquitectura modular", "core/, domain/, data/, feature/, services/, build-logic/")
)

private val licenses = listOf(
    LicenseEntry("AndroidX Core, Lifecycle, Activity", "Apache License 2.0"),
    LicenseEntry("Jetpack Compose (UI, Material 3)", "Apache License 2.0"),
    LicenseEntry("Room, WorkManager, DataStore", "Apache License 2.0"),
    LicenseEntry("Kotlin stdlib y kotlinx.coroutines", "Apache License 2.0"),
    LicenseEntry("Biometric Prompt", "Apache License 2.0")
)

private val privacyPoints = listOf(
    PrivacyPoint("Todo se guarda en tu dispositivo", "Contactos, historial, ubicaciones y eventos viven en una base de datos local de MOVA Phone."),
    PrivacyPoint("No hay servidores propios", "La aplicación no envía datos personales a Internet. El módulo de ubicación es agnóstico del proveedor."),
    PrivacyPoint("Permisos mínimos y justificados", "Cada permiso se pide en el momento de usarlo, con explicación, y puedes denegarlo."),
    PrivacyPoint("SOS transparente", "Antes de enviar un SMS o compartir ubicación, la aplicación te muestra exactamente qué va a ocurrir."),
    PrivacyPoint("Puedes borrar todo", "Historial de llamadas, mensajes, ubicaciones y eventos de seguridad se eliminan desde los ajustes."),
    PrivacyPoint("Sin publicidad ni analítica", "MOVA Phone no incluye SDK de anuncios ni rastreadores de terceros.")
)
