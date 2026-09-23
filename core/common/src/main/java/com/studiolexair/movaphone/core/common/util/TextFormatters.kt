package com.studiolexair.movaphone.core.common.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Formateo de datos para la UI. Centralizado para mantener coherencia visual. */
object TextFormatters {

    fun duration(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return "0 s"
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> String.format(Locale.US, "%d h %02d min", hours, minutes)
            minutes > 0 -> String.format(Locale.US, "%d min %02d s", minutes, seconds)
            else -> String.format(Locale.US, "%d s", seconds)
        }
    }

    fun clock(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    fun stopwatch(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /** Etiqueta relativa usada en historial y mensajes (Hoy 09:12 · Ayer · 12 mar). */
    fun relativeDay(millis: Long, now: Long = System.currentTimeMillis()): String {
        val today = startOfDay(now)
        val target = startOfDay(millis)
        val diffDays = TimeUnit.MILLISECONDS.toDays(today - target)
        return when {
            diffDays == 0L -> "Hoy ${clock(millis)}"
            diffDays == 1L -> "Ayer ${clock(millis)}"
            diffDays in 2..6 -> "${daysAgo(diffDays)} ${clock(millis)}"
            else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
        }
    }

    private fun daysAgo(days: Long): String = when (days) {
        2L -> "Anteayer"
        3L -> "Hace 3 días"
        else -> "Hace $days días"
    }

    fun batteryLevel(percent: Int): String = "$percent%"

    fun batteryPercent(level: Int, scale: Int): Int {
        if (scale <= 0) return 0
        return ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun coordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

    fun accuracy(meters: Float): String =
        String.format(Locale.US, "±%.0f m", meters)

    fun maskedNumber(number: String): String {
        if (number.length <= 4) return number
        return "•".repeat((number.length - 4).coerceAtMost(8)) + number.takeLast(4)
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
