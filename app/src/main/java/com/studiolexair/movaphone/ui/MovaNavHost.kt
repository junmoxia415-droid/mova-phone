package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.studiolexair.movaphone.BuildConfig
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.navigation.MovaRoutes
import com.studiolexair.movaphone.core.security.settings.MovaSettings
import com.studiolexair.movaphone.di.MovaContainer
import com.studiolexair.movaphone.di.assistantFactory
import com.studiolexair.movaphone.di.automationFactory
import com.studiolexair.movaphone.di.callsFactory
import com.studiolexair.movaphone.di.contactsFactory
import com.studiolexair.movaphone.di.dialerFactory
import com.studiolexair.movaphone.di.emergencyFactory
import com.studiolexair.movaphone.di.homeFactory
import com.studiolexair.movaphone.di.locationFactory
import com.studiolexair.movaphone.di.messagesFactory
import com.studiolexair.movaphone.di.securityFactory
import com.studiolexair.movaphone.di.settingsFactory
import com.studiolexair.movaphone.feature.about.AboutRoute
import com.studiolexair.movaphone.feature.about.CreditsRoute
import com.studiolexair.movaphone.feature.about.PrivacyRoute
import com.studiolexair.movaphone.feature.assistant.SmartAssistantRoute
import com.studiolexair.movaphone.feature.automation.AutomationEditorRoute
import com.studiolexair.movaphone.feature.automation.AutomationHistoryRoute
import com.studiolexair.movaphone.feature.automation.AutomationRoute
import com.studiolexair.movaphone.feature.calls.CallsRoute
import com.studiolexair.movaphone.feature.contacts.ContactDetailRoute
import com.studiolexair.movaphone.feature.contacts.ContactEditRoute
import com.studiolexair.movaphone.feature.contacts.ContactsRoute
import com.studiolexair.movaphone.feature.dialer.DialerRoute
import com.studiolexair.movaphone.feature.driving.DrivingRoute
import com.studiolexair.movaphone.feature.emergency.EmergencyActiveScreen
import com.studiolexair.movaphone.feature.emergency.EmergencyContactsRoute
import com.studiolexair.movaphone.feature.emergency.EmergencyViewModel
import com.studiolexair.movaphone.feature.emergency.SosRoute
import com.studiolexair.movaphone.feature.favorites.FavoritesRoute
import com.studiolexair.movaphone.feature.home.HomeRoute
import com.studiolexair.movaphone.feature.location.LocationRoute
import com.studiolexair.movaphone.feature.messages.ConversationRoute
import com.studiolexair.movaphone.feature.messages.MessagesRoute
import com.studiolexair.movaphone.feature.messages.TemplatesRoute
import com.studiolexair.movaphone.feature.security.BlockedNumbersRoute
import com.studiolexair.movaphone.feature.security.SecurityEventsRoute
import com.studiolexair.movaphone.feature.security.SecurityRoute
import com.studiolexair.movaphone.feature.security.TrustedContactsRoute
import com.studiolexair.movaphone.feature.settings.SettingsRoute
import com.studiolexair.movaphone.feature.settings.SettingsSectionRoute

/**
 * Grafo de navegación completo de MOVA Phone: una ruta tipada por pantalla del mockup.
 * Cada ViewModel se crea con la fábrica que expone el contenedor de dependencias.
 */
@Composable
fun MovaNavHost(
    navController: androidx.navigation.NavHostController,
    container: MovaContainer,
    navigator: MovaNavigator,
    startDestination: String,
    settings: MovaSettings,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {

        composable(MovaRoutes.SPLASH) { SplashScreen(onReady = { navigator.replaceWith(MovaRoutes.HOME) }) }

        composable(MovaRoutes.HOME) { HomeRoute(navigator, viewModel(factory = container.homeFactory)) }

        composable(
            route = MovaRoutes.DIALER,
            arguments = listOf(navArgument("number") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val prefill = java.net.URLDecoder.decode(entry.arguments?.getString("number").orEmpty(), "UTF-8")
            DialerRoute(
                navigator = navigator,
                viewModel = viewModel(factory = container.dialerFactory),
                hapticEnabled = settings.hapticKeypad,
                prefill = prefill
            )
        }

        composable(MovaRoutes.CALLS) { CallsRoute(navigator, viewModel(factory = container.callsFactory)) }

        composable(MovaRoutes.CONTACTS) { ContactsRoute(navigator, viewModel(factory = container.contactsFactory)) }

        composable(
            route = MovaRoutes.CONTACT_DETAIL,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { entry ->
            ContactDetailRoute(
                contactId = entry.arguments?.getLong("contactId") ?: 0L,
                navigator = navigator,
                viewModel = viewModel(factory = container.contactsFactory)
            )
        }

        composable(
            route = MovaRoutes.CONTACT_EDIT,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            ContactEditRoute(
                contactId = entry.arguments?.getLong("contactId")?.takeIf { it > 0 },
                navigator = navigator,
                viewModel = viewModel(factory = container.contactsFactory)
            )
        }

        composable(MovaRoutes.FAVORITES) { FavoritesRoute(navigator, container.contactsRepository) }

        composable(MovaRoutes.SOS) {
            SosRoute(
                navigator = navigator,
                viewModel = viewModel(factory = container.emergencyFactory),
                holdMillis = settings.sosHoldSeconds * 1_000L
            )
        }

        composable(MovaRoutes.EMERGENCY_ACTIVE) {
            val emergency: EmergencyViewModel = viewModel(factory = container.emergencyFactory)
            val session by emergency.session.collectAsStateWithLifecycle()
            val active = session
            if (active != null) {
                EmergencyActiveScreen(session = active, viewModel = emergency, navigator = navigator)
            } else {
                MovaEmptyState(
                    title = "No hay emergencias activas",
                    description = "Pulsa SOS en el inicio cuando necesites activar el protocolo.",
                    icon = Icons.Filled.Emergency
                )
            }
        }

        composable(MovaRoutes.EMERGENCY_CONTACTS) {
            EmergencyContactsRoute(viewModel(factory = container.emergencyFactory))
        }

        composable(MovaRoutes.MESSAGES) { MessagesRoute(navigator, viewModel(factory = container.messagesFactory)) }

        composable(
            route = MovaRoutes.CONVERSATION,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { entry ->
            val address = java.net.URLDecoder.decode(entry.arguments?.getString("address").orEmpty(), "UTF-8")
            ConversationRoute(address = address, viewModel = viewModel(factory = container.messagesFactory))
        }

        composable(MovaRoutes.TEMPLATES) { TemplatesRoute(viewModel(factory = container.messagesFactory)) }

        composable(MovaRoutes.LOCATION) { LocationRoute(navigator, viewModel(factory = container.locationFactory)) }

        composable(MovaRoutes.LOCATION_HISTORY) {
            LocationHistoryRoute(container = container)
        }

        composable(MovaRoutes.AUTOMATION) {
            AutomationRoute(navigator, viewModel(factory = container.automationFactory))
        }

        composable(
            route = MovaRoutes.AUTOMATION_EDITOR,
            arguments = listOf(navArgument("ruleId") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            AutomationEditorRoute(
                ruleId = entry.arguments?.getLong("ruleId")?.takeIf { it > 0 },
                navigator = navigator,
                viewModel = viewModel(factory = container.automationFactory)
            )
        }

        composable(MovaRoutes.AUTOMATION_HISTORY) {
            AutomationHistoryRoute(viewModel(factory = container.automationFactory))
        }

        composable(MovaRoutes.SECURITY) { SecurityRoute(navigator, viewModel(factory = container.securityFactory)) }
        composable(MovaRoutes.BLOCKED_NUMBERS) { BlockedNumbersRoute(viewModel(factory = container.securityFactory)) }
        composable(MovaRoutes.SECURITY_EVENTS) { SecurityEventsRoute(viewModel(factory = container.securityFactory)) }
        composable(MovaRoutes.TRUSTED_CONTACTS) { TrustedContactsRoute(viewModel(factory = container.securityFactory)) }

        composable(MovaRoutes.DRIVING) {
            DrivingRoute(
                navigator = navigator,
                contactsRepository = container.contactsRepository,
                onExit = { navigator.back() }
            )
        }

        composable(MovaRoutes.ASSISTANT) {
            SmartAssistantRoute(navigator, viewModel(factory = container.assistantFactory))
        }

        composable(MovaRoutes.MORE) { MoreHubRoute(navigator) }

        composable(MovaRoutes.SETTINGS) { SettingsRoute(navigator, viewModel(factory = container.settingsFactory)) }

        composable(
            route = MovaRoutes.SETTINGS_SECTION,
            arguments = listOf(navArgument("section") { type = NavType.StringType })
        ) { entry ->
            SettingsSectionRoute(
                section = entry.arguments?.getString("section").orEmpty(),
                navigator = navigator,
                viewModel = viewModel(factory = container.settingsFactory)
            )
        }

        composable(MovaRoutes.ABOUT) {
            AboutRoute(
                navigator = navigator,
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                buildType = BuildConfig.BUILD_TYPE
            )
        }
        composable(MovaRoutes.CREDITS) { CreditsRoute() }
        composable(MovaRoutes.PRIVACY) { PrivacyRoute() }
    }
}

/** Historial de ubicaciones guardadas en el dispositivo (privacidad primero). */
@Composable
private fun LocationHistoryRoute(container: MovaContainer) {
    val history by container.locationRepository.observeHistory(50)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    AuroraBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg)
        ) {
            item {
                MovaScreenHeader(
                    title = "Historial de ubicaciones",
                    subtitle = "Se guarda sólo en tu dispositivo"
                )
            }
            if (history.isEmpty()) {
                item {
                    MovaEmptyState(
                        title = "Sin registros",
                        description = "Activa el historial en Ajustes → Privacidad para conservar tus ubicaciones.",
                        icon = Icons.Filled.History
                    )
                }
            } else {
                items(history, key = { it.id }) { record ->
                    MovaListRow(
                        title = TextFormatters.coordinates(record.latitude, record.longitude),
                        subtitle = TextFormatters.relativeDay(record.recordedAt) + " · " + record.source,
                        accent = MovaTheme.extra.success
                    )
                }
            }
        }
    }
}
