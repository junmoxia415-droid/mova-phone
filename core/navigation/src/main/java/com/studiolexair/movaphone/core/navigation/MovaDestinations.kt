package com.studiolexair.movaphone.core.navigation

/**
 * Rutas de navegación centralizadas.
 * Las features no conocen cadenas sueltas: piden navegar a un destino tipado.
 */
object MovaRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val DIALER = "dialer?number={number}"

    const val CALLS = "calls"
    const val CONTACTS = "contacts"
    const val CONTACT_DETAIL = "contacts/{contactId}"
    const val CONTACT_EDIT = "contacts/edit?contactId={contactId}"
    const val FAVORITES = "favorites"

    const val SOS = "sos"
    const val EMERGENCY_ACTIVE = "emergency/active"
    const val EMERGENCY_CONTACTS = "emergency/contacts"

    const val MESSAGES = "messages"
    const val CONVERSATION = "messages/{address}"
    const val TEMPLATES = "messages/templates"

    const val LOCATION = "location"
    const val LOCATION_HISTORY = "location/history"

    const val AUTOMATION = "automation"
    const val AUTOMATION_EDITOR = "automation/editor?ruleId={ruleId}"
    const val AUTOMATION_HISTORY = "automation/history"

    const val SECURITY = "security"
    const val BLOCKED_NUMBERS = "security/blocked"
    const val SECURITY_EVENTS = "security/events"
    const val TRUSTED_CONTACTS = "security/trusted"

    const val DRIVING = "driving"
    const val SETTINGS = "settings"
    const val SETTINGS_SECTION = "settings/{section}"
    const val ABOUT = "about"
    const val CREDITS = "credits"
    const val PRIVACY = "privacy"
    const val ASSISTANT = "assistant"
    const val MORE = "more"

    fun contactDetail(id: Long) = "contacts/$id"
    fun conversation(address: String) = "messages/${java.net.URLEncoder.encode(address, "UTF-8")}"
    fun settingsSection(section: String) = "settings/$section"
    fun automationEditor(ruleId: Long?) = "automation/editor?ruleId=${ruleId ?: 0L}"
    fun contactEdit(contactId: Long?) = "contacts/edit?contactId=${contactId ?: 0L}"
}

/** Destinos de la barra inferior (Inicio · Contactos · Historial · Más). */
enum class TopLevelDestination(val route: String, val label: String) {
    HOME(MovaRoutes.HOME, "Inicio"),
    CONTACTS(MovaRoutes.CONTACTS, "Contactos"),
    CALLS(MovaRoutes.CALLS, "Historial"),
    MORE(MovaRoutes.MORE, "Más")
}
