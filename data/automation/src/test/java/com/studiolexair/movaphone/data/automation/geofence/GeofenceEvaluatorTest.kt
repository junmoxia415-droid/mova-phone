package com.studiolexair.movaphone.data.automation.geofence

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pruebas de la lógica de lugares: sin Android y sin Google Play Services.
 * Comprueban la distancia real y que entrar o salir se detecte una sola vez.
 */
class GeofenceEvaluatorTest {

    private fun place(
        radius: Double = 200.0,
        lastInside: Boolean? = null,
        lat: Double = 40.4168,
        lon: Double = -3.7038
    ) = GeofencePlace(
        id = 1,
        name = "Casa",
        latitude = lat,
        longitude = lon,
        radiusMeters = radius,
        lastInside = lastInside
    )

    @Test
    fun `la primera medida fija el estado sin disparar nada`() {
        val check = GeofenceEvaluator.evaluate(place(), 40.4168, -3.7038)
        assertThat(check.inside).isTrue()
        assertThat(check.transition).isEqualTo(GeofenceTransition.NONE)
    }

    @Test
    fun `entrar en el radio dispara ENTER una sola vez`() {
        val outside = place(lastInside = false)
        val insideCheck = GeofenceEvaluator.evaluate(outside, 40.4168 + 0.0005, -3.7038)
        assertThat(insideCheck.inside).isTrue()
        assertThat(insideCheck.transition).isEqualTo(GeofenceTransition.ENTER)

        val again = GeofenceEvaluator.evaluate(outside.copy(lastInside = true), 40.4168 + 0.0005, -3.7038)
        assertThat(again.transition).isEqualTo(GeofenceTransition.NONE)
    }

    @Test
    fun `salir del radio dispara EXIT y no repite avisos`() {
        val inside = place(lastInside = true)
        val outsideCheck = GeofenceEvaluator.evaluate(inside, 40.43, -3.7038)
        assertThat(outsideCheck.inside).isFalse()
        assertThat(outsideCheck.transition).isEqualTo(GeofenceTransition.EXIT)

        val again = GeofenceEvaluator.evaluate(inside.copy(lastInside = false), 40.43, -3.7038)
        assertThat(again.transition).isEqualTo(GeofenceTransition.NONE)
    }

    @Test
    fun `el margen de histéresis evita avisos en el borde`() {
        // A 195 m del centro con radio 200: sigue dentro aunque esté cerca del borde.
        val place = place(radius = 200.0, lastInside = true)
        val almostAtEdge = GeofenceEvaluator.distanceMeters(40.4168, -3.7038, 40.4185, -3.7038) // ~190 m
        assertThat(almostAtEdge).isLessThan(200.0)
        val check = GeofenceEvaluator.evaluate(place, 40.4185, -3.7038)
        assertThat(check.inside).isTrue()
        assertThat(check.transition).isEqualTo(GeofenceTransition.NONE)
    }

    @Test
    fun `la distancia entre dos puntos conocidos es correcta`() {
        // Puerta del Sol (Madrid) → Estadio Santiago Bernabéu: unos 5,2 km.
        val meters = GeofenceEvaluator.distanceMeters(40.4168, -3.7038, 40.4530, -3.6883)
        assertThat(meters).isGreaterThan(3900.0)
        assertThat(meters).isLessThan(4300.0)
    }

    @Test
    fun `un lugar lejano se considera fuera`() {
        val check = GeofenceEvaluator.evaluate(place(radius = 100.0, lastInside = true), 41.3874, 2.1686)
        assertThat(check.inside).isFalse()
        assertThat(check.transition).isEqualTo(GeofenceTransition.EXIT)
    }
}
