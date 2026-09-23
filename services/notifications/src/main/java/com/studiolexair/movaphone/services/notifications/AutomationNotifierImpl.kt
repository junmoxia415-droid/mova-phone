package com.studiolexair.movaphone.services.notifications

import android.content.Context
import com.studiolexair.movaphone.domain.automation.repository.AutomationNotifier

/** Notificaciones de las automatizaciones (requisito 18). */
class AutomationNotifierImpl(private val context: Context) : AutomationNotifier {

    private val notifications = MovaNotifications(context)

    override suspend fun notify(title: String, message: String) {
        notifications.general(title, message, MovaNotificationChannels.AUTOMATIONS)
    }
}
