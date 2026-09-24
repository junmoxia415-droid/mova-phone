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

    /**
     * ¿MOVA contesta en voz alta? Activado de serie: la gracia de un asistente es que te
     * conteste sin tener que mirar la pantalla. Se apaga desde el engranaje del chat.
     */
    var speakReplies: Boolean
        get() = prefs.getBoolean(KEY_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_SPEAK, value).apply()

    /**
     * ¿Ya se le ofreció al usuario descargar el modelo la primera vez que abrió el chat?
     * Así la oferta de los tres tamaños sale una vez y no molesta más.
     */
    var offeredModelDownload: Boolean
        get() = prefs.getBoolean(KEY_OFFERED, false)
        set(value) = prefs.edit().putBoolean(KEY_OFFERED, value).apply()

    /** El modelo local sólo se activa si hay uno descargado. */
    fun enableModelIfAvailable(available: Boolean): Boolean {
        if (!available && useLocalModel) useLocalModel = false
        return useLocalModel
    }

    private companion object {
        const val KEY_USE_MODEL = "use_local_model"
        const val KEY_SPEAK = "speak_replies"
        const val KEY_OFFERED = "offered_model_download"
    }
}
