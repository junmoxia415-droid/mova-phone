package com.studiolexair.movaphone.feature.assistant

/**
 * Órdenes que MOVA Phone entiende. Se resuelven **en el dispositivo**:
 * el reconocimiento de voz lo hace el sistema y la interpretación es local,
 * así que ningún texto sale del teléfono (requisito 24 y privacidad).
 */
sealed interface SmartCommand {
    /** Abrir una pantalla de la aplicación. */
    data class Open(val destination: Destination) : SmartCommand
    /** Llamar a un contacto o a un número dictado. */
    data class Call(val target: String) : SmartCommand
    /** Enviar un SMS. */
    data class SendMessage(val target: String, val body: String) : SmartCommand
    /** Compartir la ubicación actual. */
    data object ShareLocation : SmartCommand
    /** Iniciar el protocolo de emergencia (requiere confirmación explícita). */
    data object StartEmergency : SmartCommand
    /** No se entendió la orden. */
    data class Unknown(val heard: String) : SmartCommand

    val isSensitive: Boolean
        get() = this is Call || this is SendMessage || this is ShareLocation || this is StartEmergency

    enum class Destination(val label: String) {
        HOME("inicio"),
        DIALER("marcador"),
        CALLS("historial"),
        CONTACTS("contactos"),
        MESSAGES("mensajes"),
        SECURITY("seguridad"),
        LOCATION("ubicación"),
        AUTOMATION("automatizaciones"),
        SETTINGS("ajustes"),
        SOS("sos"),
        DRIVING("modo conducción")
    }
}

/**
 * Intérprete de órdenes en lenguaje natural sencillo (español).
 * Es código puro: se puede probar sin Android y sin voz (ver pruebas unitarias).
 */
object SmartCommandParser {

    private val openPatterns = mapOf(
        SmartCommand.Destination.HOME to listOf("inicio", "casa", "home", "principal"),
        SmartCommand.Destination.DIALER to listOf("marcador", "teclado", "marcar"),
        SmartCommand.Destination.CALLS to listOf("historial", "llamadas", "registro"),
        SmartCommand.Destination.CONTACTS to listOf("contactos", "agenda"),
        SmartCommand.Destination.MESSAGES to listOf("mensajes", "sms", "conversaciones"),
        SmartCommand.Destination.SECURITY to listOf("seguridad", "bloqueo", "privacidad"),
        SmartCommand.Destination.LOCATION to listOf("ubicación", "ubicacion", "mapa", "dónde estoy"),
        SmartCommand.Destination.AUTOMATION to listOf("automatizaciones", "automatización", "reglas"),
        SmartCommand.Destination.SETTINGS to listOf("ajustes", "configuración", "configuracion"),
        SmartCommand.Destination.SOS to listOf("emergencia", "sos", "auxilio"),
        SmartCommand.Destination.DRIVING to listOf("conducción", "conduccion", "coche", "auto")
    )

    fun parse(raw: String): SmartCommand {
        val text = raw.trim().lowercase()
        if (text.isBlank()) return SmartCommand.Unknown(raw)

        // SOS y ubicación primero: son las órdenes más críticas.
        if (text.contains("sos") || text.contains("emergencia") || text.contains("auxilio")) {
            return SmartCommand.StartEmergency
        }
        if (text.contains("comparte") && (text.contains("ubicaci") || text.contains("dónde") || text.contains("donde"))) {
            return SmartCommand.ShareLocation
        }
        if (text.contains("dónde estoy") || text.contains("donde estoy")) {
            return SmartCommand.ShareLocation
        }

        if (text.startsWith("llama") || text.startsWith("llamar") || text.startsWith("marca") || text.startsWith("marcar")) {
            val target = extractTarget(text, listOf("llamar a", "llama a", "llamar", "llama", "marcar", "marca"))
            return if (target.isBlank()) SmartCommand.Unknown(raw) else SmartCommand.Call(target)
        }

        if (text.startsWith("envía") || text.startsWith("envia") || text.startsWith("envíar") ||
            text.startsWith("enviar") || text.startsWith("manda") || text.startsWith("manda un mensaje") ||
            text.contains("mensaje a")
        ) {
            val body = text.substringAfter(" diciendo", "").ifBlank { text.substringAfter("que diga", "") }.trim()
            val target = extractTarget(text, listOf("enviar mensaje a", "envía un mensaje a", "envia un mensaje a",
                "enviar un mensaje a", "manda un mensaje a", "manda mensaje a", "mensaje a", "envía a", "envia a", "manda a"))
                .substringBefore(" diciendo")
                .substringBefore(" que diga")
                .trim()
            return if (target.isBlank()) SmartCommand.Unknown(raw)
            else SmartCommand.SendMessage(target, body.ifBlank { "Te escribo desde MOVA Phone." })
        }

        if (text.startsWith("abre") || text.startsWith("abrir") || text.startsWith("ve a") || text.startsWith("ir a") ||
            text.startsWith("muestra") || text.startsWith("muéstrame") || text.contains("llévame a")
        ) {
            val destination = openPatterns.entries.firstOrNull { (_, words) -> words.any { text.contains(it) } }
            if (destination != null) return SmartCommand.Open(destination.key)
        }

        // "historial", "ajustes"… sin verbo también se aceptan.
        openPatterns.entries.firstOrNull { (_, words) -> words.any { text == it } }?.let {
            return SmartCommand.Open(it.key)
        }

        return SmartCommand.Unknown(raw)
    }

    private fun extractTarget(text: String, prefixes: List<String>): String {
        val prefix = prefixes.firstOrNull { text.startsWith(it) } ?: return ""
        return text.removePrefix(prefix).trim().trimStart('a', ' ').trim()
    }
}
