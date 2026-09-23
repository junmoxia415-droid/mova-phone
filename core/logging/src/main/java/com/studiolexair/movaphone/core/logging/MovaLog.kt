package com.studiolexair.movaphone.core.logging

import android.util.Log

/**
 * Registro centralizado.
 * - En release se silencian los niveles informativos (R8 los elimina por completo).
 * - Los errores técnicos se guardan para diagnóstico, nunca se muestran al usuario.
 */
object MovaLog {

    private var debugEnabled = true
    private var minLevel = Level.DEBUG

    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    fun initialize(isDebug: Boolean) {
        debugEnabled = isDebug
        minLevel = if (isDebug) Level.DEBUG else Level.WARN
    }

    fun d(tag: String, message: String) = log(Level.DEBUG, tag, message, null)
    fun i(tag: String, message: String) = log(Level.INFO, tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(Level.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(Level.ERROR, tag, message, throwable)

    private fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
        if (!debugEnabled && level.ordinal < minLevel.ordinal) return
        val safeTag = tag.take(23)
        when (level) {
            Level.VERBOSE -> Log.v(safeTag, message, throwable)
            Level.DEBUG -> Log.d(safeTag, message, throwable)
            Level.INFO -> Log.i(safeTag, message, throwable)
            Level.WARN -> Log.w(safeTag, message, throwable)
            Level.ERROR -> Log.e(safeTag, message, throwable)
        }
    }
}
