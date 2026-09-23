package com.studiolexair.movaphone.services.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Canales de notificación (requisito 26).
 * Cada tipo de aviso tiene su canal para que el usuario pueda configurar
 * prioridades por separado, como exige Android 8+.
 */
object MovaNotificationChannels {

    const val CALLS = "mova_calls"
    const val SOS = "mova_sos"
    const val MESSAGES = "mova_messages"
    const val AUTOMATIONS = "mova_automations"
    const val SECURITY = "mova_security"
    const val GENERAL = "mova_general"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(CALLS, "Llamadas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Llamadas entrantes, perdidas y bloqueadas"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(SOS, "Emergencias SOS", NotificationManager.IMPORTANCE_MAX).apply {
                description = "Alertas del protocolo de emergencia"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(MESSAGES, "Mensajes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Mensajes SMS y mensajes rápidos"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(AUTOMATIONS, "Automatizaciones", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Resultado de tus reglas automáticas"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(SECURITY, "Seguridad", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Bloqueos, accesos y eventos de seguridad"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(GENERAL, "General", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Avisos generales de MOVA Phone"
            }
        )
    }
}
