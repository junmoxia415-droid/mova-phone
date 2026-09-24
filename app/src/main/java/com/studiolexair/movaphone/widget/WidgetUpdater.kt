package com.studiolexair.movaphone.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Refresco de los widgets de la pantalla de inicio.
 *
 * Se llama cuando MOVA trae datos nuevos (mensajes, llamadas, favoritos) y al abrir o
 * cerrar la app, para que el resumen no se quede viejo entre las actualizaciones del sistema.
 */
object WidgetUpdater {

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            MovaFavoritesWidget::class.java,
            MovaSummaryWidget::class.java,
            MovaSosWidget::class.java
        ).forEach { provider ->
            val component = ComponentName(context, provider)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return@forEach
            val intent = android.content.Intent(context, provider).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
