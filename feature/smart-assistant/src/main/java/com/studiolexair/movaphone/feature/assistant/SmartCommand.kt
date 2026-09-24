package com.studiolexair.movaphone.feature.assistant

import com.studiolexair.movaphone.core.common.util.TextNormalizer

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
    /** Contar en voz alta los mensajes sin leer. */
    data object ReadMessages : SmartCommand
    /** Contar en voz alta las llamadas perdidas de hoy. */
    data object ReadMissedCalls : SmartCommand
    /** Silenciar los avisos del teléfono. */
    data object Silence : SmartCommand
    /** Cambiar el nombre con el que MOVA se dirige al usuario (el perfil). */
    data class RenameUser(val name: String) : SmartCommand
    /** Encender o apagar el modo conducción. */
    data class Driving(val enabled: Boolean) : SmartCommand
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
        DRIVING("modo conducción"),
        PROFILE("mi perfil"),
        FAVORITES("favoritos"),
        TEMPLATES("plantillas"),
        BLOCKED("números bloqueados")
    }
}

/**
 * Intérprete de órdenes en lenguaje natural sencillo (español).
 *
 * Compara **sin acentos ni mayúsculas** (así «llamar a mamá» y «llamar a mama» son lo mismo)
 * y acepta varias formas de decir lo mismo. Es código puro: se puede probar sin Android y
 * sin voz (ver pruebas unitarias).
 */
object SmartCommandParser {

    private val openPatterns = mapOf(
        SmartCommand.Destination.HOME to listOf("inicio", "casa", "home", "principal"),
        SmartCommand.Destination.DIALER to listOf("marcador", "teclado", "marcar"),
        SmartCommand.Destination.CALLS to listOf("historial", "llamadas", "registro"),
        SmartCommand.Destination.CONTACTS to listOf("contactos", "agenda"),
        SmartCommand.Destination.MESSAGES to listOf("mensajes", "sms", "conversaciones"),
        SmartCommand.Destination.SECURITY to listOf("seguridad", "bloqueo", "privacidad"),
        SmartCommand.Destination.LOCATION to listOf("ubicacion", "mapa", "donde estoy"),
        SmartCommand.Destination.AUTOMATION to listOf("automatizaciones", "automatizacion", "reglas"),
        SmartCommand.Destination.SETTINGS to listOf("ajustes", "configuracion"),
        SmartCommand.Destination.SOS to listOf("emergencia", "sos", "auxilio"),
        SmartCommand.Destination.DRIVING to listOf("conduccion"),
        SmartCommand.Destination.PROFILE to listOf("mi perfil", "perfil"),
        SmartCommand.Destination.FAVORITES to listOf("favoritos"),
        SmartCommand.Destination.TEMPLATES to listOf("plantillas"),
        SmartCommand.Destination.BLOCKED to listOf("bloqueados", "numeros bloqueados")
    )

    fun parse(raw: String): SmartCommand {
        // Se compara en minúsculas y sin acentos: la voz y el teclado no son perfectos.
        val text = TextNormalizer.normalize(raw)
        // …pero el nombre del contacto que se dice se conserva **tal cual** («Mamá», «Nena»),
        // porque luego hay que buscarlo en la agenda con sus tildes y su emoji.
        val spoken = TextNormalizer.stripEmoji(raw).trim()
        if (text.isBlank()) return SmartCommand.Unknown(raw)

        // ---------- Lo más crítico primero ----------
        // «ayuda» a secas también cuenta: es lo que se grita cuando hace falta, y el SOS
        // siempre pide confirmación antes de avisar a nadie.
        if (text.contains("sos") || text.contains("emergencia") || text.contains("auxilio") ||
            text == "ayuda" || text.contains("necesito ayuda") || text.contains("ayudame")
        ) {
            return SmartCommand.StartEmergency
        }
        if (text.contains("comparte") || text.contains("compartir") || text.contains("manda mi ubicacion")) {
            if (text.contains("ubicaci") || text.contains("posicion") || text.contains("donde estoy")) {
                return SmartCommand.ShareLocation
            }
        }
        if (text.contains("donde estoy")) return SmartCommand.ShareLocation

        // ---------- Leer en voz alta ----------
        if (text.contains("me han llamado") || text.contains("quien me ha llamado") ||
            text.contains("llamadas perdidas") || (text.contains("perdid") && text.contains("llamad"))
        ) {
            return SmartCommand.ReadMissedCalls
        }
        if (text.contains("mensaje") || text.contains("sms") || text.contains("whatsapp")) {
            if (text.startsWith("lee") || text.startsWith("leeme") || text.startsWith("leer") ||
                text.contains("tengo mensaje") || text.contains("mensajes nuevo") ||
                text.contains("mensajes sin leer") || text.contains("dime si tengo")
            ) {
                return SmartCommand.ReadMessages
            }
        }

        // ---------- Modo conducción y silencio ----------
        if (text.contains("conduccion") || text.contains("conducir") || text.contains("coche") || text.contains("auto")) {
            val off = listOf("quita", "desactiva", "apaga", "sal de", "salir")
            if (off.any { text.contains(it) }) return SmartCommand.Driving(false)
            if (listOf("activa", "pon", "enciende", "modo").any { text.contains(it) }) {
                return SmartCommand.Driving(true)
            }
        }
        if (text.contains("silencio") || text.contains("silencia") || text.contains("silenciar")) {
            return SmartCommand.Silence
        }

        // ---------- Llamar ----------
        val callTarget = extractTarget(
            spoken,
            listOf("llamar a", "llama a", "llamarle a", "llamar", "llama", "marcar a", "marcar", "marca a", "marca")
        )
        if (callTarget.isNotBlank()) return SmartCommand.Call(callTarget)

        // ---------- Mandar mensajes ----------
        if (text.startsWith("envia") || text.startsWith("enviar") || text.startsWith("manda") ||
            text.startsWith("escribe") || text.contains("mensaje a") || text.contains("mensaje para")
        ) {
            val spokenLower = spoken.lowercase(java.util.Locale.getDefault())
            val afterDiciendo = spokenLower.substringAfter(" diciendo", "")
            val afterQueDiga = spokenLower.substringAfter(" que diga", "")
            val afterQue = spokenLower.substringAfter(" que ", "")
            val cut = listOf(" diciendo", " que diga", " que ")
            val body = cutAt(spoken, cut, after = true)
                .ifBlank { afterQueDiga.ifBlank { afterQue }.trim() }
            val target = cutAt(
                extractTarget(
                    spoken,
                    listOf(
                        "enviar mensaje a", "envia un mensaje a", "enviar un mensaje a", "manda un mensaje a",
                        "manda mensaje a", "mandar un mensaje a", "escribir a", "escribe a", "escribele a",
                        "mensaje a", "mensaje para", "envia a", "enviar a", "manda a", "envia", "manda"
                    )
                ),
                cut
            )
            return if (target.isBlank()) SmartCommand.Unknown(raw)
            else SmartCommand.SendMessage(target, body.ifBlank { "Te escribo desde MOVA Phone." })
        }

        // ---------- Cambiar el nombre del perfil ----------
        val newName = extractTarget(
            spoken,
            listOf("cambia mi nombre a", "cambia mi nombre por", "mi nombre es", "llamame", "me llamo")
        )
        if (newName.isNotBlank()) return SmartCommand.RenameUser(newName)
        // «cambia mi nombre» sin decir cuál: se abre el perfil para escribirlo con calma.
        if (text.contains("cambia mi nombre") || text.contains("mi nombre") || text.contains("mi perfil")) {
            return SmartCommand.Open(SmartCommand.Destination.PROFILE)
        }

        // ---------- Abrir pantallas ----------
        if (text.startsWith("abre") || text.startsWith("abrir") || text.startsWith("ve a") || text.startsWith("ir a") ||
            text.startsWith("muestra") || text.startsWith("muestrame") || text.contains("llevame a") ||
            text.contains("abre ") || text.startsWith("entra")
        ) {
            val destination = openPatterns.entries.firstOrNull { (_, words) -> words.any { text.contains(it) } }
            if (destination != null) return SmartCommand.Open(destination.key)
        }

        // «historial», «ajustes»… sin verbo también se aceptan.
        openPatterns.entries.firstOrNull { (_, words) -> words.any { text == it } }?.let {
            return SmartCommand.Open(it.key)
        }

        return SmartCommand.Unknown(raw)
    }

    /**
     * Devuelve lo que el usuario dijo **después del verbo**, conservando tildes y mayúsculas.
     *
     * Se compara palabra a palabra y sin acentos («Llamar a Mamá» = «llamar a mama»), pero el
     * valor devuelto es el original: así «Llamar a Mamá» llama a «Mamá» y no a «mama».
     */
    private fun extractTarget(spoken: String, prefixes: List<String>): String {
        val tokens = tokenize(spoken)
        if (tokens.isEmpty()) return ""
        val normalized = tokens.map { TextNormalizer.normalize(it) }
        val candidates = prefixes
            .map { it.split(' ').filter { word -> word.isNotBlank() } }
            .sortedByDescending { it.size }
        val match = candidates.firstOrNull { prefix ->
            normalized.size > prefix.size && normalized.take(prefix.size) == prefix
        } ?: return ""
        val rest = tokens.drop(match.size).toMutableList()
        // «llamar a la nena» → «nena»: fuera artículos y posesivos del principio.
        while (rest.size > 1 && TextNormalizer.normalize(rest.first()) in LEADING_FILLERS) {
            rest.removeAt(0)
        }
        return rest.joinToString(" ").trim()
    }

    /** Corta [spoken] justo antes (o justo después) del primer marcador que aparezca. */
    private fun cutAt(spoken: String, markers: List<String>, after: Boolean = false): String {
        val lower = spoken.lowercase(java.util.Locale.getDefault())
        val hit = markers.mapNotNull { marker ->
            val index = lower.indexOf(marker)
            if (index < 0) null else index to marker
        }.minByOrNull { it.first } ?: return spoken.trim()
        return if (after) {
            spoken.substring(hit.first + hit.second.length).trim()
        } else {
            spoken.substring(0, hit.first).trim()
        }
    }

    /** Artículos y posesivos que se caen del nombre: «a la nena» = «nena». */
    private val LEADING_FILLERS = setOf(
        "a", "al", "de", "del", "el", "la", "los", "las", "un", "una", "mi", "mis", "don", "dona"
    )

    /** Palabras útiles de la frase, sin signos de puntuación. */
    private fun tokenize(spoken: String): List<String> = spoken
        .split(' ', '\n', '\t')
        .map { word -> word.trim { character -> !character.isLetterOrDigit() && character != '@' && character != '+' } }
        .filter { it.isNotBlank() }
}
