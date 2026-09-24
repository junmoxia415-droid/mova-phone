package com.studiolexair.movaphone.data.automation.geofence

/**
 * Lugar guardado por el usuario («casa», «trabajo», «gimnasio»…) para las automatizaciones
 * de entrada y salida.
 *
 * Implementación propia, **sin Google Play Services**: MOVA calcula por su cuenta si estás
 * dentro o fuera (distancia real, fórmula del semiverseno) y dispara el automatismo
 * correspondiente cuando cruzas el borde del lugar.
 */
data class GeofencePlace(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Radio en metros. */
    val radiusMeters: Double,
    val enabled: Boolean = true,
    /** Último estado conocido: true si la última comprobación estaba dentro. */
    val lastInside: Boolean? = null,
    val lastCheckedAt: Long = 0L,
    val createdAt: Long = 0L
)

/** Qué ha pasado al comprobar un lugar con la ubicación actual. */
enum class GeofenceTransition { ENTER, EXIT, NONE }

data class GeofenceCheck(
    val place: GeofencePlace,
    val inside: Boolean,
    val distanceMeters: Double,
    val transition: GeofenceTransition
)
