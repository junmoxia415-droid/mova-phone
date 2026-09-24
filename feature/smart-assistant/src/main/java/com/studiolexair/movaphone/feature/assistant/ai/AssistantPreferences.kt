package com.studiolexair.movaphone.feature.assistant.ai

import android.content.Context

/**
 * Preferencias del asistente: si se usa el modelo local y con qué idioma se escucha.
 * Se guardan aparte de los ajustes generales para no migrar la base de datos.
 */
class AssistantPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("mova_assistant", Context.MODE_PRIVATE)

    var useLocalModel: Boolean
        get() = prefs.getBoolean(KEY_USE_MODEL, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_MODEL, value).apply()

    /** El modelo local sólo se activa si hay uno descargado. */
    fun enableModelIfAvailable(available: Boolean): Boolean {
        if (!available && useLocalModel) useLocalModel = false
        return useLocalModel
    }

    private companion object {
        const val KEY_USE_MODEL = "use_local_model"
    }
}
