package com.studiolexair.movaphone.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.studiolexair.movaphone.MovaApplication
import com.studiolexair.movaphone.R
import com.studiolexair.movaphone.core.logging.MovaLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Widget «Te han escrito»: resumen **real** de la base de datos de MOVA.
 *
 * Muestra cuántos mensajes han llegado sin leer y cuántas llamadas se han perdido hoy,
 * y abre las conversaciones al tocarlo. Los datos salen de la app, no de una estimación.
 */
class MovaSummaryWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as MovaApplication).container
                val unread = runCatching {
                    container.database.messageDao().observeLatest(200).first()
                        .count { it.isIncoming && it.state == "RECEIVED" }
                }.getOrElse { 0 }
                val sinceMidnight = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val missed = runCatching {
                    container.database.callRecordDao().observeMissedCount(sinceMidnight).first()
                }.getOrElse { 0 }

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.mova_widget_summary)
                    views.setTextViewText(
                        R.id.widget_messages,
                        if (unread == 0) "Sin mensajes pendientes" else "Te han escrito: $unread mensaje(s) sin leer"
                    )
                    views.setTextViewText(
                        R.id.widget_calls,
                        if (missed == 0) "Sin llamadas perdidas hoy" else "Llamadas perdidas hoy: $missed"
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_refresh,
                        refreshIntent(context, widgetId)
                    )
                    val openConversations: PendingIntent =
                        WidgetIntents.route(context, WidgetDeepLink.ROUTE_MESSAGES, widgetId + 500)
                    views.setOnClickPendingIntent(R.id.widget_messages, openConversations)
                    views.setOnClickPendingIntent(R.id.widget_calls, openConversations)
                    views.setOnClickPendingIntent(R.id.widget_hint, openConversations)
                    manager.updateAppWidget(widgetId, views)
                }
            } catch (t: Throwable) {
                MovaLog.e(TAG, "No se pudo actualizar el widget de resumen", t)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action == ACTION_REFRESH) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, MovaSummaryWidget::class.java))
            onUpdate(context, AppWidgetManager.getInstance(context), ids)
            return
        }
        super.onReceive(context, intent)
    }

    private fun refreshIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = android.content.Intent(context, MovaSummaryWidget::class.java)
            .setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.studiolexair.movaphone.widget.REFRESH_SUMMARY"
        private const val TAG = "MovaSummaryWidget"
    }
}
