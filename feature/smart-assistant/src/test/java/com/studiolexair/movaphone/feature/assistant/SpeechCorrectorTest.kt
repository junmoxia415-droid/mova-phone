package com.studiolexair.movaphone.feature.assistant

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pruebas del «oído» de MOVA: cuando el reconocedor oye mal una palabra, se arregla con el
 * vocabulario de MOVA y con los nombres reales de la agenda antes de interpretar la orden.
 */
class SpeechCorrectorTest {

    @Test
    fun `arregla el verbo mal oido`() {
        val repair = SpeechCorrector.repair("yamar a nena")
        assertThat(repair.text).startsWith("llamar")
    }

    @Test
    fun `arregla el nombre usando la agenda`() {
        val repair = SpeechCorrector.repair("llamar a nen", contactNames = listOf("Nena ❤️"))
        assertThat(repair.corrections.map { it.second }).contains("nena")
    }

    @Test
    fun `no toca las frases que ya estan bien`() {
        val repair = SpeechCorrector.repair("llama a mamá")
        assertThat(repair.text).isEqualTo("llama a mamá")
        assertThat(repair.corrections).isEmpty()
    }

    @Test
    fun `una palabra desconocida se deja en paz`() {
        val repair = SpeechCorrector.repair("hazme un cafe")
        assertThat(repair.text).isEqualTo("hazme un cafe")
    }
}
