package com.studiolexair.movaphone.core.navigation

import androidx.navigation.NavController

/**
 * Fachada de navegación. Las pantallas piden acciones de negocio
 * ("abrir el marcador") en lugar de manipular rutas a mano, lo que mantiene
 * la navegación en un único lugar y facilita cambiar rutas sin tocar la UI.
 */
class MovaNavigator(private val navController: NavController) {

    /** Controlador de navegación para el anfitrión del grafo (módulo app). */
    val controller: NavController get() = navController

    /** Reemplaza la pantalla actual: usado por el splash para no dejar historial. */
    fun replaceWith(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }


    fun back(): Boolean = navController.navigateUp()

    fun toHome() = navController.navigate(MovaRoutes.HOME) { launchSingleTop = true }
    fun toDialer(prefill: String? = null) {
        val route = "dialer?number=${java.net.URLEncoder.encode(prefill.orEmpty(), "UTF-8")}"
        navController.navigate(route) { launchSingleTop = true }
    }
    fun toCalls() = navController.navigate(MovaRoutes.CALLS) { launchSingleTop = true }
    fun toContacts() = navController.navigate(MovaRoutes.CONTACTS) { launchSingleTop = true }
    fun toFavorites() = navController.navigate(MovaRoutes.FAVORITES)
    fun toContactDetail(contactId: Long) = navController.navigate(MovaRoutes.contactDetail(contactId))
    fun toContactEdit(contactId: Long? = null) = navController.navigate(MovaRoutes.contactEdit(contactId))

    fun toSos() = navController.navigate(MovaRoutes.SOS)
    fun toEmergencyActive() = navController.navigate(MovaRoutes.EMERGENCY_ACTIVE)
    fun toEmergencyContacts() = navController.navigate(MovaRoutes.EMERGENCY_CONTACTS)

    fun toMessages() = navController.navigate(MovaRoutes.MESSAGES) { launchSingleTop = true }
    fun toConversation(address: String) = navController.navigate(MovaRoutes.conversation(address))
    fun toTemplates() = navController.navigate(MovaRoutes.TEMPLATES)

    fun toLocation() = navController.navigate(MovaRoutes.LOCATION)
    fun toLocationHistory() = navController.navigate(MovaRoutes.LOCATION_HISTORY)

    fun toAutomation() = navController.navigate(MovaRoutes.AUTOMATION)
    fun toAutomationEditor(ruleId: Long? = null) = navController.navigate(MovaRoutes.automationEditor(ruleId))
    fun toAutomationHistory() = navController.navigate(MovaRoutes.AUTOMATION_HISTORY)

    fun toSecurity() = navController.navigate(MovaRoutes.SECURITY)
    fun toBlockedNumbers() = navController.navigate(MovaRoutes.BLOCKED_NUMBERS)
    fun toSecurityEvents() = navController.navigate(MovaRoutes.SECURITY_EVENTS)
    fun toTrustedContacts() = navController.navigate(MovaRoutes.TRUSTED_CONTACTS)

    fun toDriving() = navController.navigate(MovaRoutes.DRIVING)
    fun toSettings() = navController.navigate(MovaRoutes.SETTINGS) { launchSingleTop = true }
    fun toSettingsSection(section: String) = navController.navigate(MovaRoutes.settingsSection(section))
    fun toAbout() = navController.navigate(MovaRoutes.ABOUT)
    fun toCredits() = navController.navigate(MovaRoutes.CREDITS)
    fun toPrivacy() = navController.navigate(MovaRoutes.PRIVACY)
    fun toAssistant() = navController.navigate(MovaRoutes.ASSISTANT)
    fun toMore() = navController.navigate(MovaRoutes.MORE) { launchSingleTop = true }

    fun toTopLevel(destination: TopLevelDestination) = navController.navigate(destination.route) {
        popUpTo(MovaRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
