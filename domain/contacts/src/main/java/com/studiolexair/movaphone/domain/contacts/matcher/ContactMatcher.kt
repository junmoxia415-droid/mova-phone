package com.studiolexair.movaphone.domain.contacts.matcher

import com.studiolexair.movaphone.core.common.util.TextNormalizer
import com.studiolexair.movaphone.domain.contacts.model.Contact

/**
 * Busca contactos «como los llamaría una persona».
 *
 * Resuelve el caso real: buscar «nena» encuentra «Nena ❤️»; buscar «maria jose» encuentra
 * «María José»; buscar «8747» encuentra el número +53 5267 8747; y un nombre con una
 * letra cambiada por el dictado sigue encontrándose.
 */
object ContactMatcher {

    data class Match(val contact: Contact, val score: Int, val reason: String)

    fun score(query: String, contact: Contact): Match? {
        val normalizedQuery = TextNormalizer.normalizeQuery(query)
        if (normalizedQuery.isBlank()) return null

        val name = TextNormalizer.normalize(contact.displayName)
        val queryDigits = TextNormalizer.digits(normalizedQuery)
        val phoneDigits = TextNormalizer.digits(contact.phoneNumber)
        val normalizedPhone = TextNormalizer.digits(contact.normalizedNumber)

        // 1) Número de teléfono: coincidencia de dígitos (completa o por el final).
        if (queryDigits.length >= MIN_PHONE_DIGITS) {
            if (phoneDigits == queryDigits || normalizedPhone == queryDigits) {
                return Match(contact, SCORE_PHONE_EXACT, "número exacto")
            }
            if (phoneDigits.endsWith(queryDigits) || normalizedPhone.endsWith(queryDigits)) {
                return Match(contact, SCORE_PHONE_SUFFIX, "últimos dígitos del número")
            }
        }

        if (name.isBlank()) return null

        // 2) Nombre exacto.
        if (name == normalizedQuery) return Match(contact, SCORE_NAME_EXACT, "nombre exacto")

        val nameTokens = name.split(' ').filter { it.isNotBlank() }
        val queryTokens = normalizedQuery.split(' ').filter { it.isNotBlank() }

        // 3) Una sola palabra que coincide con una palabra del nombre («nena» → «Nena ❤️»).
        if (queryTokens.size == 1) {
            val token = queryTokens.first()
            if (nameTokens.any { it == token }) return Match(contact, SCORE_TOKEN_EXACT, "coincide con «$token»")
            if (nameTokens.any { it.startsWith(token) && token.length >= MIN_PREFIX }) {
                return Match(contact, SCORE_TOKEN_PREFIX, "empieza por «$token»")
            }
            if (name.contains(token) && token.length >= MIN_PREFIX) {
                return Match(contact, SCORE_NAME_CONTAINS, "contiene «$token»")
            }
        }

        // 4) Todas las palabras de la consulta están en el nombre, en cualquier orden.
        val remaining = nameTokens.toMutableList()
        var matchedTokens = 0
        queryTokens.forEach { token ->
            val hit = remaining.firstOrNull { it == token }
                ?: remaining.firstOrNull { it.startsWith(token) && token.length >= MIN_PREFIX }
            if (hit != null) {
                remaining.remove(hit)
                matchedTokens++
            }
        }
        if (queryTokens.isNotEmpty() && matchedTokens == queryTokens.size) {
            return Match(
                contact,
                if (queryTokens.size > 1) SCORE_ALL_TOKENS else SCORE_NAME_CONTAINS,
                "todas las palabras coinciden"
            )
        }

        // 5) Una palabra del nombre aparece dentro de la frase dictada.
        val partial = nameTokens.firstOrNull { it.length >= MIN_PREFIX && normalizedQuery.contains(it) }
        if (partial != null) return Match(contact, SCORE_QUERY_CONTAINS_NAME, "la frase contiene «$partial»")

        // 6) Errores de dictado: distancia de edición pequeña.
        distanceOf(normalizedQuery, name)?.let { return Match(contact, it.second, it.first) }
        nameTokens.forEach { nameToken ->
            queryTokens.forEach { queryToken ->
                distanceOf(queryToken, nameToken)?.let { return Match(contact, it.second, it.first) }
            }
        }

        // 7) Iniciales: «j p» encuentra «Juan Pérez».
        if (queryTokens.size >= 2) {
            val initials = nameTokens.mapNotNull { it.firstOrNull() }.joinToString("")
            val queryInitials = queryTokens.mapNotNull { it.firstOrNull() }.joinToString("")
            if (initials == queryInitials) return Match(contact, SCORE_INITIALS, "iniciales del nombre")
        }

        return null
    }

    /** Mejores coincidencias ordenadas (favoritos y nombres cortos primero en caso de empate). */
    fun rank(query: String, contacts: List<Contact>, limit: Int = 6): List<Contact> =
        contacts.mapNotNull { score(query, it) }
            .sortedWith(
                compareByDescending<Match> { it.score }
                    .thenByDescending { it.contact.isFavorite }
                    .thenBy { it.contact.displayName.length }
            )
            .map { it.contact }
            .distinctBy { it.phoneNumber }
            .take(limit)

    /** Mejor coincidencia o `null` si ninguna se parece lo suficiente. */
    fun best(query: String, contacts: List<Contact>): Match? =
        contacts.mapNotNull { score(query, it) }
            .filter { it.score >= SCORE_MINIMUM }
            .maxWithOrNull(
                compareBy<Match> { it.score }
                    .thenBy { it.contact.isFavorite }
                    .thenByDescending { it.contact.displayName.length }
            )

    /** Distancia de edición admisible según la longitud (devuelve motivo y puntuación). */
    private fun distanceOf(queryToken: String, nameToken: String): Pair<String, Int>? {
        if (queryToken.length < MIN_FUZZY_LENGTH || nameToken.length < MIN_FUZZY_LENGTH) return null
        val distance = levenshtein(queryToken, nameToken)
        return when {
            distance <= 1 -> "muy parecido a «$nameToken»" to SCORE_FUZZY_NEAR
            distance <= 2 && queryToken.length >= MIN_FUZZY_LONG -> "parecido a «$nameToken»" to SCORE_FUZZY_FAR
            else -> null
        }
    }

    /** Distancia de Levenshtein (dos filas, sin dependencias externas). */
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        a.forEachIndexed { i, charA ->
            current[0] = i + 1
            b.forEachIndexed { j, charB ->
                val cost = if (charA == charB) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    const val SCORE_MINIMUM = 45
    private const val SCORE_PHONE_EXACT = 100
    private const val SCORE_NAME_EXACT = 95
    private const val SCORE_PHONE_SUFFIX = 85
    private const val SCORE_TOKEN_EXACT = 80
    private const val SCORE_ALL_TOKENS = 72
    private const val SCORE_TOKEN_PREFIX = 68
    private const val SCORE_NAME_CONTAINS = 60
    private const val SCORE_QUERY_CONTAINS_NAME = 55
    private const val SCORE_FUZZY_NEAR = 52
    private const val SCORE_FUZZY_FAR = 46
    private const val SCORE_INITIALS = 45
    private const val MIN_PHONE_DIGITS = 3
    private const val MIN_PREFIX = 3
    private const val MIN_FUZZY_LENGTH = 4
    private const val MIN_FUZZY_LONG = 7
}
