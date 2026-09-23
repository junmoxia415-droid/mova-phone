package com.studiolexair.movaphone.services.notifications

import android.content.Context
import android.os.Build
import com.studiolexair.movaphone.domain.emergency.model.EmergencySession
import com.studiolexair.movaphone.domain.emergency.model.StepStatus
import com.studiolexair.movaphone.domain.emergency.repository.EmergencyAlertSink
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Implementación del sumidero de alertas SOS: mantiene una notificación
 * permanente visible mientras hay una emergencia activa, con acción de cancelar.
 */
class EmergencyAlertSinkImpl(private val context: Context) : EmergencyAlertSink {

    private val notifications = MovaNotifications(context)

    override suspend fun onEmergencyStarted(session: EmergencySession) {
        MovaLog.w(TAG, "Notificación de emergencia publicada")
        notifications.ongoingEmergency(session.describe(), MovaNotifications.cancelEmergencyIntent(context))
    }

    override suspend fun onSessionUpdated(session: EmergencySession) {
        notifications.ongoingEmergency(session.describe(), MovaNotifications.cancelEmergencyIntent(context))
    }

    override suspend fun onEmergencyCancelled(session: EmergencySession) {
        notifications.cancelEmergency()
    }

    private fun EmergencySession.describe(): String {
        val failed = steps.count { it.status == StepStatus.FAILED }
        val done = steps.count { it.status == StepStatus.DONE }
        val base = "Pasos completados: $done/${steps.size}"
        return if (failed > 0) "$base · $failed con error (revisa la app)" else base
    }

    private companion object {
        const val TAG = "EmergencyAlertSink"
    }
}
