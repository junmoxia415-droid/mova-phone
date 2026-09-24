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
 * Widget de **Favoritos**: hasta tres contactos con sus botones de llamar y mensaje.
 *
 * El widget enseña datos reales (los favoritos guardados o importados del teléfono) y al
 * pulsar abre MOVA con el número preparado: la llamada la hace el usuario desde la app.
 */
class MovaFavoritesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as MovaApplication).container
                val favorites = runCatching {
                    container.database.contactDao().observeFavorites().first()
                }.getOrElse { emptyList() }

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.mova_widget_favorites)
                    views.setOnClickPendingIntent(R.id.widget_open_app, WidgetIntents.openApp(context, widgetId))

                    val rows = listOf(
                        Triple(R.id.widget_row_1, R.id.widget_name_1, 1),
                        Triple(R.id.widget_row_2, R.id.widget_name_2, 2),
                        Triple(R.id.widget_row_3, R.id.widget_name_3, 3)
                    )
                    val callIds = listOf(R.id.widget_call_1, R.id.widget_call_2, R.id.widget_call_3)
                    val smsIds = listOf(R.id.widget_sms_1, R.id.widget_sms_2, R.id.widget_sms_3)

                    rows.forEachIndexed { index, (rowId, nameId, slot) ->
                        val contact = favorites.getOrNull(index)
                        if (contact == null) {
                            views.setViewVisibility(rowId, android.view.View.GONE)
                        } else {
                            views.setViewVisibility(rowId, android.view.View.VISIBLE)
                            views.setTextViewText(nameId, contact.displayName)
                            views.setOnClickPendingIntent(
                                callIds[index],
                                WidgetIntents.dial(context, contact.phoneNumber, widgetId * 10 + slot)
                            )
                            views.setOnClickPendingIntent(
                                smsIds[index],
                                WidgetIntents.message(context, contact.phoneNumber, widgetId * 10 + slot + 5)
                            )
                        }
                    }
                    views.setViewVisibility(
                        R.id.widget_empty,
                        if (favorites.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                    )
                    manager.updateAppWidget(widgetId, views)
                }
            } catch (t: Throwable) {
                MovaLog.e(TAG, "No se pudo actualizar el widget de favoritos", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "MovaFavoritesWidget"
    }
}
