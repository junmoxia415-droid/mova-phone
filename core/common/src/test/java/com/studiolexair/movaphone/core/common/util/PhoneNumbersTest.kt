package com.studiolexair.movaphone.core.common.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** El marcador y el filtrado dependen de normalizar bien los números. */
class PhoneNumbersTest {

    @Test
    fun `normaliza quitando espacios guiones y parentesis`() {
        assertThat(PhoneNumbers.normalize("+34 600-12 34 56")).isEqualTo("600123456")
        assertThat(PhoneNumbers.normalize("(91) 555 44 33")).isEqualTo("915554433")
    }

    @Test
    fun `normaliza entrada vacia o nula sin fallar`() {
        assertThat(PhoneNumbers.normalize(null)).isEmpty()
        assertThat(PhoneNumbers.normalize("")).isEmpty()
    }

    @Test
    fun `compara numeros con distinto formato`() {
        assertThat(PhoneNumbers.sameNumber("600 12 34 56", "+34600123456")).isTrue()
        assertThat(PhoneNumbers.sameNumber("600123456", "600123457")).isFalse()
    }

    @Test
    fun `exige longitud minima para poder marcar`() {
        assertThat(PhoneNumbers.canBeDialed("123")).isFalse()
        assertThat(PhoneNumbers.canBeDialed("60012")).isTrue()
        assertThat(PhoneNumbers.canBeDialed("+34 600 12 34 56")).isTrue()
    }

    @Test
    fun `genera enlace de mapas con las coordenadas`() {
        val link = PhoneNumbers.mapsLink(40.4168, -3.7038)
        assertThat(link).contains("40.4168")
        assertThat(link).contains("-3.7038")
    }
}
