package com.studiolexair.movaphone.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.studiolexair.movaphone.MainActivity

/**
 * Enlaces de los widgets hacia la aplicación.
 *
 * Los widgets **nunca llaman ni envían nada por su cuenta**: abren MOVA con el número o la
 * conversación preparados y el usuario confirma allí. Es la misma regla que en el resto de
 * la app (nada de marcaciones silenciosas).
 */
data class WidgetDeepLink(
    val route: String? = null,
    val number: String? = null,
    val address: String? = null
) {
    companion object {
        const val EXTRA_ROUTE = "mova_widget_route"
        const val EXTRA_NUMBER = "mova_widget_number"
        const val EXTRA_ADDRESS = "mova_widget_address"
        const val ROUTE_SOS = "sos"
        const val ROUTE_MESSAGES = "messages"

        fun from(intent: Intent?): WidgetDeepLink? {
            intent ?: return null
            val route = intent.getStringExtra(EXTRA_ROUTE)
            val number = intent.getStringExtra(EXTRA_NUMBER)
            val address = intent.getStringExtra(EXTRA_ADDRESS)
            if (route == null && number == null && address == null) return null
            return WidgetDeepLink(route = route, number = number, address = address)
        }
    }
}

internal object WidgetIntents {

    private const val REQUEST_BASE = 4200

    fun openApp(context: Context, requestCode: Int): PendingIntent = pendingIntent(
        context = context,
        requestCode = requestCode,
        deepLink = WidgetDeepLink()
    )

    fun dial(context: Context, number: String, requestCode: Int): PendingIntent = pendingIntent(
        context = context,
        requestCode = requestCode,
        deepLink = WidgetDeepLink(number = number)
    )

    fun message(context: Context, address: String, requestCode: Int): PendingIntent = pendingIntent(
        context = context,
        requestCode = requestCode,
        deepLink = WidgetDeepLink(address = address)
    )

    fun route(context: Context, route: String, requestCode: Int): PendingIntent = pendingIntent(
        context = context,
        requestCode = requestCode,
        deepLink = WidgetDeepLink(route = route)
    )

    private fun pendingIntent(context: Context, requestCode: Int, deepLink: WidgetDeepLink): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            deepLink.route?.let { putExtra(WidgetDeepLink.EXTRA_ROUTE, it) }
            deepLink.number?.let { putExtra(WidgetDeepLink.EXTRA_NUMBER, it) }
            deepLink.address?.let { putExtra(WidgetDeepLink.EXTRA_ADDRESS, it) }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, REQUEST_BASE + requestCode, intent, flags)
    }
}
