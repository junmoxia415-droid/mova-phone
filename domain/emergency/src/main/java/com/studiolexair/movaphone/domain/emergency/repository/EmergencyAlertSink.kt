package com.studiolexair.movaphone.domain.emergency.repository

import com.studiolexair.movaphone.domain.emergency.model.EmergencySession

/**
 * Salida de alertas del protocolo SOS. La implementación vive en services:notifications
 * para que la capa de datos no dependa de la capa de servicios.
 */
interface EmergencyAlertSink {
    suspend fun onEmergencyStarted(session: EmergencySession)
    suspend fun onSessionUpdated(session: EmergencySession)
    suspend fun onEmergencyCancelled(session: EmergencySession)
}
