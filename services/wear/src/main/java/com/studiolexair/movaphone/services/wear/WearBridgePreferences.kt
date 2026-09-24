package com.studiolexair.movaphone.services.wear

import android.content.Context

/**
 * Recuerda si el usuario quiere el puente con el reloj encendido.
 * Si lo apagó en Ajustes, MOVA no vuelve a encenderlo solo.
 */
class WearBridgePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("mova_wear_bridge", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    private companion object {
        const val KEY_ENABLED = "bridge_enabled"
    }
}
