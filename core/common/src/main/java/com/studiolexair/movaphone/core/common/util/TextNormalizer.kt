package com.studiolexair.movaphone.core.common.util

import java.text.Normalizer
import java.util.Locale

/**
 * Normaliza texto escrito o dictado para comparar nombres de contactos.
 *
 * Nace de un caso real: el contacto se llama «Nena ❤️» y el usuario dice «llega a nena» /
 * «llama a nena». Sin normalizar, el emoji y los acentos impiden encontrarlo.
 *
 * Reglas: minúsculas · sin acentos · sin emoji · sin puntuación · espacios colapsados ·
 * se quitan artículos y posesivos iniciales («mi», «el», «la»…).
 */
object TextNormalizer {

    private val LEADING_FILLERS = setOf(
        "mi", "mis", "el", "la", "los", "las", "un", "una", "don", "doña", "dona",
        "señor", "senor", "señora", "senora", "a", "al", "de", "del"
    )

    /** Texto comparable: sin acentos, sin emoji, sin signos y en minúsculas. */
    fun normalize(raw: String?): String {
        val cleaned = stripEmoji(raw.orEmpty())
        val withoutAccents = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
            .replace(ACCENTS_REGEX, "")
        val lowercase = withoutAccents.lowercase(Locale.getDefault())
        val withoutPunctuation = lowercase.map { character ->
            when {
                character.isLetterOrDigit() -> character
                character == ' ' || character == '\n' || character == '\t' -> ' '
                else -> ' '
            }
        }.joinToString("")
        return withoutPunctuation.split(' ').filter { it.isNotBlank() }.joinToString(" ")
    }

    /** Igual que [normalize] pero además quita artículos y posesivos del principio. */
    fun normalizeQuery(raw: String?): String {
        val tokens = normalize(raw).split(' ').filter { it.isNotBlank() }.toMutableList()
        while (tokens.isNotEmpty() && tokens.first() in LEADING_FILLERS) tokens.removeAt(0)
        return tokens.joinToString(" ")
    }

    /** Palabras útiles de una consulta (sin vacías ni posesivos). */
    fun tokens(raw: String?): List<String> =
        normalizeQuery(raw).split(' ').filter { it.length >= MIN_TOKEN_LENGTH }

    /** Elimina emojis, banderas, símbolos y selectores de variación. */
    fun stripEmoji(raw: String): String {
        val builder = StringBuilder(raw.length)
        var index = 0
        while (index < raw.length) {
            val codePoint = raw.codePointAt(index)
            if (!isEmoji(codePoint)) builder.appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
        }
        return builder.toString()
    }

    /** Sólo los dígitos (para comparar números de teléfono). */
    fun digits(raw: String?): String = raw.orEmpty().filter { it.isDigit() }

    private fun isEmoji(codePoint: Int): Boolean = when {
        codePoint in 0x1F000..0x1FAFF -> true   // emojis, banderas, pictogramas
        codePoint in 0x2600..0x27BF -> true     // símbolos y dingbats (❤ ✔ ✨ …)
        codePoint in 0x2B00..0x2BFF -> true     // flechas y estrellas
        codePoint in 0xFE00..0xFE0F -> true     // selectores de variación
        codePoint in 0x1F1E6..0x1F1FF -> true   // letras de banderas
        codePoint == 0x200D -> true             // unión de secuencias (ZWJ)
        codePoint == 0x20E3 -> true             // combinación de teclas
        Character.getType(codePoint) == Character.OTHER_SYMBOL.toInt() -> true
        else -> false
    }

    private const val MIN_TOKEN_LENGTH = 2
    private val ACCENTS_REGEX = Regex("\\p{Mn}+")
}
