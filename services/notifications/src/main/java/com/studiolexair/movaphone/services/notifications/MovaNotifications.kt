package com.studiolexair.movaphone.services.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.studiolexair.movaphone.services.notifications.R

/**
 * Constructor central de notificaciones.
 * Respeta el canal configurado y comprueba el permiso real de notificaciones
 * antes de publicar (en Android 13+ puede estar denegado).
 */
class MovaNotifications(private val context: Context) {

    fun general(title: String, message: String, channel: String = MovaNotificationChannels.GENERAL) {
        if (!canPost()) return
        val notification = base(channel)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_mova)
            .setAutoCancel(true)
            .build()
        notifySafely(NOTIFICATION_ID_GENERAL, notification)
    }

    fun ongoingEmergency(sessionLabel: String, cancelIntent: PendingIntent?): Int {
        if (!canPost()) return NOTIFICATION_ID_SOS
        val builder = base(MovaNotificationChannels.SOS)
            .setContentTitle("EMERGENCIA ACTIVA")
            .setContentText(sessionLabel)
            .setSmallIcon(R.drawable.ic_notification_mova)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
        if (cancelIntent != null) {
            builder.addAction(0, "Cancelar emergencia", cancelIntent)
        }
        notifySafely(NOTIFICATION_ID_SOS, builder.build())
        return NOTIFICATION_ID_SOS
    }

    fun cancelEmergency() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SOS)
    }

    private fun base(channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun notifySafely(id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (t: SecurityException) {
            // El usuario denegó el permiso de notificaciones: se informa por la UI, no se insiste.
        }
    }

    companion object {
        const val NOTIFICATION_ID_GENERAL = 1000
        const val NOTIFICATION_ID_SOS = 1001
        const val NOTIFICATION_ID_LOCATION = 1002
        const val NOTIFICATION_ID_CALLS = 1003

        /** Intent de cancelación de emergencia usado por la notificación del SOS. */
        fun cancelEmergencyIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            Intent(ACTION_CANCEL_EMERGENCY).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        const val ACTION_CANCEL_EMERGENCY = "com.studiolexair.movaphone.action.CANCEL_EMERGENCY"
    }
}
