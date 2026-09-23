package com.studiolexair.movaphone.core.common.util

/**
 * Utilidades de normalización de números.
 * La comparación se hace por dígitos significativos para evitar falsos negativos
 * entre formatos (+53 5267 8747 / 52678747).
 */
object PhoneNumbers {

    private const val MIN_SIGNIFICANT_DIGITS = 7

    fun digitsOnly(raw: String?): String = raw.orEmpty().filter { it.isDigit() }

    fun normalize(raw: String?): String {
        val digits = digitsOnly(raw)
        return if (digits.length < MIN_SIGNIFICANT_DIGITS) digits else digits.takeLast(9)
    }

    fun sameNumber(a: String?, b: String?): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        return na.isNotEmpty() && na == nb
    }

    /** Agrupa el número para mostrarlo en el marcador: +53 5267 8747 */
    fun pretty(raw: String?): String {
        val digits = digitsOnly(raw)
        if (digits.isEmpty()) return ""
        val hasPrefix = raw.orEmpty().trimStart().startsWith("+")
        val body = if (hasPrefix) digits else digits
        return buildString {
            if (hasPrefix) append("+")
            body.chunked(4).forEachIndexed { index, chunk ->
                if (index > 0) append(' ')
                append(chunk)
            }
        }.trim()
    }

    fun isValidInput(raw: String?): Boolean = digitsOnly(raw).length >= 3

    fun canBeDialed(raw: String?): Boolean = digitsOnly(raw).length >= 5

    fun geoUri(latitude: Double, longitude: Double): String =
        "geo:$latitude,$longitude?q=$latitude,$longitude"

    fun mapsLink(latitude: Double, longitude: Double): String =
        "https://maps.google.com/?q=$latitude,$longitude"
}
