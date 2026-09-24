package com.studiolexair.movaphone.core.navigation

import androidx.navigation.NavController

/**
 * Fachada de navegación. Las pantallas piden acciones de negocio
 * ("abrir el marcador") en lugar de manipular rutas a mano, lo que mantiene
 * la navegación en un único lugar y facilita cambiar rutas sin tocar la UI.
 */
class MovaNavigator(private val navController: NavController) {

    /** Abre los ajustes de la aplicación en el sistema (para permisos denegados "no volver a preguntar"). */
    var onOpenSystemSettings: (() -> Unit)? = null

    /** Controlador de navegación para el anfitrión del grafo (módulo app). */
    val controller: NavController get() = navController

    /** Reemplaza la pantalla actual sin destruir el grafo de navegación. */
    fun replaceWith(route: String) {
        navController.navigate(route) {
            popUpTo(MovaRoutes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }


    fun back(): Boolean = navController.navigateUp()

    /** Navega a una ruta de la aplicación (la usan los accesos directos personalizados). */
    fun navigateRoute(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    /**
     * Vuelve a Inicio dejando la pila limpia.
     *
     * Antes esto *empujaba* otra pantalla de Inicio encima, así que al pulsar "atrás" el
     * usuario volvía a la pantalla anterior (el fallo de navegación del 1.1). Ahora se
     * vacía hasta el inicio del grafo: "atrás" siempre sale de la aplicación desde Inicio.
     */
    fun toHome() = toTopLevel(TopLevelDestination.HOME)
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
    fun toAutomationPlaces() = navController.navigate(MovaRoutes.AUTOMATION_PLACES)

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
    /** Abre el chat del asistente. `launchSingleTop` evita apilar un chat por cada toque. */
    fun toAssistant() = navController.navigate(MovaRoutes.ASSISTANT) { launchSingleTop = true }
    fun toPermissions() = navController.navigate(MovaRoutes.PERMISSIONS)
    fun toMore() = navController.navigate(MovaRoutes.MORE) { launchSingleTop = true }

    /**
     * Cambio de pestaña de la barra inferior.
     *
     * Se vacía la pila hasta el **primer destino del grafo** (Inicio) y se restaura el estado
     * de cada pestaña: así el botón "atrás" del teléfono nunca devuelve a una pantalla ya
     * cerrada ni se queda atrapado en un submenú, que era el fallo de navegación del 1.1.
     */
    fun toTopLevel(destination: TopLevelDestination) {
        val start = navController.graph.startDestinationId
        navController.navigate(destination.route) {
            popUpTo(start) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Abre el chat del asistente desde cualquier parte de la aplicación. */
    fun toAssistantSettings() = navController.navigate(MovaRoutes.ASSISTANT_SETTINGS)
    fun toAssistantHelp() = navController.navigate(MovaRoutes.ASSISTANT_HELP)
    fun toProfile() = navController.navigate(MovaRoutes.PROFILE)
}
