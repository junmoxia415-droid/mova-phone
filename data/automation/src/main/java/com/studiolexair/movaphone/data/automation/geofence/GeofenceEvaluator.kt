package com.studiolexair.movaphone.data.automation.geofence

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decide si entras o sales de un lugar.
 *
 * Es lógica pura (sin Android) y por eso está cubierta con pruebas: se le da un lugar y una
 * posición y devuelve la transición. Usa un margen del 15 % del radio (histéresis) para no
 * generar avisos en bucle cuando alguien está justo en el borde.
 */
object GeofenceEvaluator {

    private const val HYSTERESIS = 0.15
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun evaluate(place: GeofencePlace, latitude: Double, longitude: Double): GeofenceCheck {
        val distance = distanceMeters(latitude, longitude, place.latitude, place.longitude)
        val enterThreshold = place.radiusMeters * (1 - HYSTERESIS)
        val exitThreshold = place.radiusMeters * (1 + HYSTERESIS)

        val inside = when (place.lastInside) {
            null -> distance <= place.radiusMeters     // primera medida
            true -> distance <= exitThreshold          // ya estaba dentro: hay que salir del todo
            false -> distance <= enterThreshold        // estaba fuera: hay que entrar del todo
        }

        val transition = when {
            place.lastInside == null -> GeofenceTransition.NONE
            inside && !place.lastInside -> GeofenceTransition.ENTER
            !inside && place.lastInside -> GeofenceTransition.EXIT
            else -> GeofenceTransition.NONE
        }

        return GeofenceCheck(place = place, inside = inside, distanceMeters = distance, transition = transition)
    }

    /** Distancia real entre dos puntos en metros (fórmula del semiverseno). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
    }
}
