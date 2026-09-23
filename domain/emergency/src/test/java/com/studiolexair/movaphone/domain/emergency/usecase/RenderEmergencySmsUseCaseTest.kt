package com.studiolexair.movaphone.domain.emergency.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El SMS de emergencia es el mensaje más importante de la aplicación:
 * debe respetar la plantilla del usuario y sustituir todos los marcadores.
 */
class RenderEmergencySmsUseCaseTest {

    private val render = RenderEmergencySmsUseCase()

    @Test
    fun `sustituye nombre ubicacion bateria y precision`() {
        val mensaje = render(
            template = "SOS de {nombre}. Estoy en {ubicacion} ({precision}). Batería {bateria}.",
            contactName = "Ana",
            latitude = 40.4168,
            longitude = -3.7038,
            accuracyMeters = 15.0f,
            batteryPercent = 42,
            timestamp = 1_700_000_000_000L,
            timeLabel = "12:30"
        )
        assertThat(mensaje).contains("Ana")
        assertThat(mensaje).contains("40.4168")
        assertThat(mensaje).contains("15")
        assertThat(mensaje).contains("42")
        assertThat(mensaje).doesNotContain("{")
    }

    @Test
    fun `usa texto explícito cuando no hay ubicación`() {
        val mensaje = render(
            template = "Ayuda desde {ubicacion}",
            contactName = null,
            latitude = null,
            longitude = null,
            accuracyMeters = null,
            batteryPercent = null,
            timestamp = 1_700_000_000_000L,
            timeLabel = "12:30"
        )
        assertThat(mensaje.lowercase()).contains("no disponible")
    }

    @Test
    fun `plantilla vacía cae en la plantilla por defecto`() {
        val mensaje = render(
            template = "   ",
            contactName = "Luis",
            latitude = 1.0,
            longitude = 2.0,
            accuracyMeters = 5f,
            batteryPercent = 10,
            timestamp = 1_700_000_000_000L,
            timeLabel = "08:00"
        )
        assertThat(mensaje).isNotEmpty()
        assertThat(mensaje.trim()).isNotEmpty()
    }
}
