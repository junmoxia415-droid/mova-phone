package com.studiolexair.movaphone.feature.assistant

/**
 * Biblioteca de órdenes de MOVA: **una sola lista** de todo lo que el asistente sabe hacer.
 *
 * De aquí salen cuatro cosas, para que nunca se contradigan entre ellas:
 *  1. la ayuda que ve el usuario («¿Qué sabes hacer?»),
 *  2. los ejemplos rápidos del chat,
 *  3. el vocabulario con el que se corrigen las palabras mal oídas,
 *  4. las órdenes que se le explican al modelo local para que traduzca lo que dices.
 *
 * Si algún día se añade una orden nueva, se añade **aquí** y aparece en los cuatro sitios.
 */
object CommandLibrary {

    /** Una capacidad del asistente tal como se le explica a una persona. */
    data class Capability(
        val id: String,
        val title: String,
        val what: String,
        val examples: List<String>,
        val phrase: String
    )

    val capabilities: List<Capability> = listOf(
        Capability(
            id = "llamar",
            title = "Llamar",
            what = "Busca el contacto por nombre (aunque tenga emoji o tilde) o marca un número. " +
                "Si hay dos contactos con el mismo nombre, MOVA pregunta cuál antes de llamar.",
            examples = listOf("llama a mamá", "llamar a Nena", "marca el 535267 8747", "llama a Juan Pérez"),
            phrase = "llamar a <contacto o número>"
        ),
        Capability(
            id = "mensaje",
            title = "Mandar mensajes",
            what = "Escribe el SMS y te dice si salió. Si el nombre no está claro, pregunta a quién.",
            examples = listOf(
                "manda un mensaje a Luis diciendo llego tarde",
                "envía a mamá que ya voy",
                "escribe a Ana diciendo te llamo luego"
            ),
            phrase = "manda un mensaje a <contacto> diciendo <texto>"
        ),
        Capability(
            id = "leer",
            title = "Leer lo último",
            what = "Te cuenta en voz alta los mensajes sin leer o las llamadas perdidas, sin abrir nada.",
            examples = listOf("léeme los mensajes", "¿me han llamado?", "¿tengo mensajes nuevos?"),
            phrase = "leer mensajes o llamadas perdidas"
        ),
        Capability(
            id = "ubicacion",
            title = "Compartir ubicación",
            what = "Coge tu posición real (GPS) y la comparte con quien le digas.",
            examples = listOf("comparte mi ubicación", "dónde estoy", "manda mi ubicación a mamá"),
            phrase = "compartir mi ubicación"
        ),
        Capability(
            id = "sos",
            title = "Emergencia",
            what = "Lanza el protocolo SOS completo (avisa a tus contactos de confianza y comparte ubicación). " +
                "Siempre pide confirmación antes.",
            examples = listOf("emergencia", "sos", "necesito ayuda", "auxilio"),
            phrase = "iniciar emergencia"
        ),
        Capability(
            id = "abrir",
            title = "Abrir pantallas",
            what = "Te lleva dentro de MOVA a donde pidas.",
            examples = listOf("abre contactos", "ve a ajustes", "muéstrame el historial", "abre el historial"),
            phrase = "abrir <pantalla>"
        ),
        Capability(
            id = "silenciar",
            title = "Silenciar y modo conducción",
            what = "Silencia los avisos o activa el modo conducción.",
            examples = listOf("silencia el teléfono", "modo conducción", "quita el modo conducción"),
            phrase = "silenciar o modo conducción"
        ),
        Capability(
            id = "perfil",
            title = "Tu perfil y los ajustes",
            what = "Te lleva a tu perfil, donde cambias tu nombre y cómo quieres que MOVA te llame.",
            examples = listOf("abre mi perfil", "cambia mi nombre"),
            phrase = "abrir mi perfil"
        ),
        Capability(
            id = "marcador",
            title = "Marcar números",
            what = "Abre el teclado para marcar a mano cuando prefieras escribir el número.",
            examples = listOf("abre el marcador", "muéstrame el teclado"),
            phrase = "abrir el marcador"
        ),
        Capability(
            id = "favoritos",
            title = "Favoritos",
            what = "Lleva directo a tus personas de siempre, para llamarlas en dos toques.",
            examples = listOf("abre favoritos"),
            phrase = "abrir favoritos"
        ),
        Capability(
            id = "bloqueados",
            title = "Números bloqueados",
            what = "Muestra a quién tienes bloqueado y deja desbloquear desde ahí.",
            examples = listOf("abre los números bloqueados"),
            phrase = "abrir los números bloqueados"
        ),
        Capability(
            id = "desambiguar",
            title = "Cuando hay dudas, pregunta",
            what = "Si oye mal un nombre o hay varios contactos parecidos, MOVA **pregunta** " +
                "en vez de llamar al primero que encuentre.",
            examples = listOf("llama a Juan  →  ¿a cuál de los dos?"),
            phrase = "MOVA pregunta antes de equivocarse"
        )
    )

    /** Todas las frases de ejemplo (para los botones rápidos del chat). */
    val quickExamples: List<String> = listOf(
        "llama a mamá",
        "léeme los mensajes",
        "¿me han llamado?",
        "comparte mi ubicación",
        "abre contactos",
        "manda un mensaje a Luis diciendo llego tarde"
    )

    /**
     * Vocabulario para corregir el dictado: los verbos y palabras clave de MOVA más las
     * pantallas de la aplicación. Si el reconocedor oye «yamar a juan», aquí se arregla.
     */
    val vocabulary: Set<String> = buildSet {
        addAll(
            listOf(
                "llama", "llamar", "marca", "marcar", "llamada", "llamadas", "contacto", "contactos",
                "mensaje", "mensajes", "manda", "mandar", "envia", "enviar", "escribe", "escribir",
                "diciendo", "diga", "texto", "sms", "lee", "leer", "léeme", "leeme", "nuevos", "nuevo",
                "perdidas", "perdida", "han", "llamado", "ubicacion", "ubicación", "comparte", "compartir",
                "comparto", "donde", "dónde", "estoy", "emergencia", "sos", "auxilio", "ayuda",
                "abre", "abrir", "ve", "ir", "muestra", "muestrame", "muéstrame", "llevame", "llévame",
                "ajustes", "configuracion", "configuración", "historial", "inicio", "casa", "marcador",
                "teclado", "agenda", "seguridad", "bloqueo", "privacidad", "mapa", "automatizaciones",
                "automatización", "reglas", "conduccion", "conducción", "coche", "auto", "silencio",
                "silencia", "silenciar", "perfil", "nombre", "favoritos", "sospechoso", "bloqueados",
                "plantillas", "modo", "conducir", "pasos", "mamá", "mama", "papá", "papa", "hermano",
                "hermana", "hijo", "hija", "abuela", "abuelo", "trabajo", "oficina", "casa", "novia",
                "novio", "esposa", "esposo", "marido", "mujer", "doctor", "doctora"
            )
        )
        addAll(SmartCommand.Destination.values().map { it.label.lowercase() })
    }

    /** Texto que se le da al modelo local para que sepa qué puede pedir el usuario. */
    fun promptFor(): String = capabilities.joinToString("\n") { "- ${it.phrase}" }
}
