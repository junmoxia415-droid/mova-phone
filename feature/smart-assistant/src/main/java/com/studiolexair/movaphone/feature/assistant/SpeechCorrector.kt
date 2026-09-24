package com.studiolexair.movaphone.feature.assistant

import com.studiolexair.movaphone.core.common.util.TextNormalizer

/**
 * Repara lo que se oye mal.
 *
 * El dictado se equivoca: «yamar a juan», «llamar a nene» (por Nena), «manda un mesaje»,
 * «comparte mi ubicasion». Aquí se corrige **antes** de interpretar la orden:
 *
 *  1. se normaliza el texto (sin acentos, sin emoji, en minúsculas);
 *  2. cada palabra se compara con el vocabulario de MOVA y con los nombres reales de la
 *     agenda del usuario (los contactos son los que más se confunden);
 *  3. si una palabra está a una o dos letras de una conocida, se cambia;
 *  4. los números dictados en palabras («nueve tres uno») se convierten en dígitos.
 *
 * Todo es local y explicable: la corrección se puede mostrar al usuario ("entendí X").
 */
object SpeechCorrector {

    private val numberWords = mapOf(
        "cero" to "0", "uno" to "1", "una" to "1", "dos" to "2", "tres" to "3", "cuatro" to "4",
        "cinco" to "5", "seis" to "6", "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10",
        "once" to "11", "doce" to "12", "trece" to "13", "catorce" to "14", "quince" to "15",
        "veinte" to "20", "treinta" to "30", "cuarenta" to "40", "cincuenta" to "50",
        "sesenta" to "60", "setenta" to "70", "ochenta" to "80", "noventa" to "90",
        "cien" to "100", "mil" to "1000"
    )

    /** Resultado de la reparación: el texto corregido y qué se cambió (para poder contarlo). */
    data class Repair(val text: String, val corrections: List<Pair<String, String>>) {
        val changed: Boolean get() = corrections.isNotEmpty()
    }

    /**
     * Corrige [raw] usando el vocabulario de MOVA y los nombres de los contactos.
     *
     * [contactNames] son los nombres tal cual están guardados (con emoji y tildes):
     * se normalizan aquí para compararlos.
     */
    fun repair(raw: String, contactNames: List<String> = emptyList()): Repair {
        val normalized = TextNormalizer.normalize(raw)
        if (normalized.isBlank()) return Repair(raw, emptyList())

        val known = buildSet {
            addAll(CommandLibrary.vocabulary)
            contactNames.forEach { name ->
                addAll(TextNormalizer.normalize(name).split(' ').filter { it.length >= 3 })
            }
        }

        val corrections = mutableListOf<Pair<String, String>>()
        val words = normalized.split(' ').filter { it.isNotBlank() }
        val repairedWords = words.map { word ->
            // 1) Números dictados en palabras.
            numberWords[word]?.let { digit -> return@map digit }
            // 2) Palabras que ya existen: no se tocan.
            if (word in known || word.length <= 2 || word.count { it.isDigit() } > 0) return@map word
            // 3) Se busca la palabra conocida más parecida.
            val best = known
                .filter { it.length >= 3 && kotlin.math.abs(it.length - word.length) <= 2 }
                .mapNotNull { candidate ->
                    val distance = editDistance(word, candidate)
                    val limit = allowedDistance(word)
                    if (distance in 1..limit) Triple(candidate, distance, true) else null
                }
                .minByOrNull { (_, distance, _) -> distance }
            if (best != null) {
                corrections += word to best.first
                best.first
            } else {
                word
            }
        }

        // Se reconstruye con acentos y mayúsculas razonables (el intérprete no los necesita).
        val text = repairedWords.joinToString(" ")
        return if (corrections.isEmpty()) Repair(raw, emptyList()) else Repair(text, corrections)
    }

    /** Distancia de edición admisible: más permisiva en palabras largas. */
    private fun allowedDistance(word: String): Int = when {
        word.length <= 4 -> 1
        word.length <= 7 -> 2
        else -> 2
    }

    /** Distancia de Levenshtein clásica con dos filas (memoria constante). */
    internal fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
