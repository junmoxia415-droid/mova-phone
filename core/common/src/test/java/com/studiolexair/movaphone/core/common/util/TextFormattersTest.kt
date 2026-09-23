package com.studiolexair.movaphone.core.common.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Formatos visibles al usuario: duración, cronómetro, batería y precisión. */
class TextFormattersTest {

    @Test
    fun `duracion de llamada legible`() {
        assertThat(TextFormatters.duration(0)).isEqualTo("0 s")
        assertThat(TextFormatters.duration(45)).isEqualTo("45 s")
        assertThat(TextFormatters.duration(65)).isEqualTo("1 min 05 s")
        assertThat(TextFormatters.duration(3_725)).isEqualTo("1 h 02 min")
    }

    @Test
    fun `cronometro de emergencia en mm ss`() {
        assertThat(TextFormatters.stopwatch(0)).isEqualTo("00:00")
        assertThat(TextFormatters.stopwatch(61_000)).isEqualTo("01:01")
        assertThat(TextFormatters.stopwatch(3_661_000)).isEqualTo("61:01")
    }

    @Test
    fun `precisión en metros redondeada`() {
        assertThat(TextFormatters.accuracy(8.4f)).isEqualTo("±8 m")
        assertThat(TextFormatters.accuracy(1200f)).isEqualTo("±1200 m")
    }

    @Test
    fun `nivel de bateria con porcentaje`() {
        assertThat(TextFormatters.batteryLevel(73)).isEqualTo("73%")
    }

    @Test
    fun `mismo dia se muestra como hora`() {
        val now = 1_700_000_000_000L
        assertThat(TextFormatters.relativeDay(now - 60_000, now)).isNotEmpty()
    }
}
