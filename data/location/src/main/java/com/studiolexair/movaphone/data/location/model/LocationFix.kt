package com.studiolexair.movaphone.data.location.model

/** Posición obtenida del dispositivo. */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val timestamp: Long
)

/** Resultado de un intento de localización: nunca se inventa una posición. */
sealed interface LocationResult {
    data class Available(val fix: LocationFix) : LocationResult
    data class Unavailable(val reason: String) : LocationResult
}
