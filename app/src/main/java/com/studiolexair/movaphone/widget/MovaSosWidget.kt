package com.studiolexair.movaphone.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.studiolexair.movaphone.R

/**
 * Widget **SOS**: un toque abre el protocolo de emergencia de MOVA.
 *
 * Por seguridad, el widget no lanza el SOS por sí solo: abre la pantalla de emergencia,
 * donde el usuario confirma (o donde el temporizador del protocolo se encarga si así lo
 * tiene configurado). Nunca hay envíos silenciosos.
 */
class MovaSosWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.mova_widget_sos)
            views.setOnClickPendingIntent(
                R.id.widget_sos_button,
                WidgetIntents.route(context, WidgetDeepLink.ROUTE_SOS, widgetId)
            )
            manager.updateAppWidget(widgetId, views)
        }
    }
}
