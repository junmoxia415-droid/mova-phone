package com.studiolexair.movaphone.domain.emergency.usecase

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.common.util.TextFormatters
import java.util.Locale

/**
 * Construye el SMS de emergencia a partir de la plantilla configurable por el usuario.
 * Marcadores soportados: {nombre} {ubicacion} {enlace} {bateria} {hora} {precision}
 */
class RenderEmergencySmsUseCase(
    private val defaultTemplate: String = DEFAULT_TEMPLATE
) {
    operator fun invoke(
        template: String?,
        contactName: String?,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Float?,
        batteryPercent: Int?,
        timestamp: Long,
        timeLabel: String
    ): String {
        val source = template?.takeIf { it.isNotBlank() } ?: defaultTemplate

        val location = if (latitude != null && longitude != null) {
            TextFormatters.coordinates(latitude, longitude)
        } else {
            "no disponible"
        }
        val link = if (latitude != null && longitude != null) {
            PhoneNumbers.mapsLink(latitude, longitude)
        } else {
            "sin enlace"
        }

        return source
            .replace("{nombre}", contactName ?: "sin nombre")
            .replace("{ubicacion}", location)
            .replace("{enlace}", link)
            .replace("{bateria}", batteryPercent?.let { "$it%" } ?: "desconocida")
            .replace("{precision}", accuracyMeters?.let { String.format(Locale.US, "%.0f m", it) } ?: "sin precisión")
            .replace("{hora}", timeLabel)
            .replace("{timestamp}", timestamp.toString())
    }

    companion object {
        const val DEFAULT_TEMPLATE = """SOS ACTIVADO. Necesito ayuda.

Ubicación: {ubicacion}
Precisión: {precision}
Batería: {bateria}
Hora: {hora}

Enlace: {enlace}"""
    }
}
