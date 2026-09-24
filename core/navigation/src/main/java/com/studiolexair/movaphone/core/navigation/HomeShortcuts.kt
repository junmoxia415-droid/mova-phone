package com.studiolexair.movaphone.core.navigation

/**
 * Accesos directos de la pantalla de inicio.
 *
 * El usuario pidió poder «personalizar al máximo la apk» y ajustar sus accesos directos.
 * Esta es la lista única de atajos disponibles: la pantalla de inicio los pinta en el orden
 * y con los que el usuario haya elegido (Ajustes → Apariencia → Accesos directos), y los
 * ajustes usan la misma lista, así que nunca se desincronizan.
 */
object HomeShortcuts {

    data class Shortcut(
        val id: String,
        val label: String,
        val route: String,
        val description: String
    )

    val all: List<Shortcut> = listOf(
        Shortcut("llamar", "Llamar", MovaRoutes.DIALER, "Abrir el teclado para marcar"),
        Shortcut("mensajes", "Mensajes", MovaRoutes.MESSAGES, "Tus conversaciones"),
        Shortcut("contactos", "Contactos", MovaRoutes.CONTACTS, "Tu agenda"),
        Shortcut("favoritos", "Favoritos", MovaRoutes.FAVORITES, "Tus personas de siempre"),
        Shortcut("historial", "Historial", MovaRoutes.CALLS, "Llamadas recientes"),
        Shortcut("seguridad", "Seguridad", MovaRoutes.SECURITY, "Bloqueo, spam y privacidad"),
        Shortcut("ubicacion", "Ubicación", MovaRoutes.LOCATION, "Dónde estás y compartir"),
        Shortcut("automatizar", "Automatizar", MovaRoutes.AUTOMATION, "Reglas y lugares"),
        Shortcut("sos", "SOS", MovaRoutes.SOS, "Emergencia"),
        Shortcut("conduccion", "Conducción", MovaRoutes.DRIVING, "Modo manos libres")
    )

    val defaultIds: List<String> = listOf("seguridad", "ubicacion", "automatizar", "mensajes")

    /** Los identificadores guardados en ajustes, saneados (máximo 6 y sin repetidos). */
    fun parse(value: String): List<String> {
        val ids = value.split(',').map { it.trim() }.filter { it.isNotBlank() }
        val known = ids.filter { id -> all.any { it.id == id } }.distinct().take(MAX)
        return known.ifEmpty { defaultIds }
    }

    fun resolve(value: String): List<Shortcut> =
        parse(value).mapNotNull { id -> all.firstOrNull { it.id == id } }

    /** Texto que se guarda en los ajustes a partir de los identificadores elegidos. */
    fun serialize(ids: List<String>): String =
        ids.filter { id -> all.any { it.id == id } }.distinct().take(MAX).joinToString(",")

    const val MAX = 6
}
