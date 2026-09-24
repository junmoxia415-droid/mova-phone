package com.studiolexair.movaphone.services.wear

import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Lo que la aplicación cuenta al reloj: llamadas, mensajes y batería.
 *
 * El reloj (Wear OS) se conecta por Bluetooth al teléfono, sin pasar por Google. Si no hay
 * reloj conectado, estas llamadas no hacen nada: cero coste cuando no se usa.
 */
object WearBridge {

    private const val TAG = "WearBridge"

    fun notifyIncomingCall(number: String, name: String?) {
        val previous = BridgeState.state.value.messages
        BridgeState.update { it.copy(call = WearProtocol.call(number = number, name = name, ringing = true)) }
        MovaLog.i(TAG, "Aviso de llamada enviado al reloj (${itSafe(name, number)})")
    }

    fun notifyCallEnded() {
        BridgeState.update { it.copy(call = null) }
    }

    fun notifyMessage(address: String, name: String?, body: String, at: Long = System.currentTimeMillis()) {
        val json = WearProtocol.message(from = address, name = name, body = body, at = at)
        BridgeState.pushMessage(json)
        BridgeState.update { it.copy(messages = BridgeState.state.value.messages) }
    }

    fun notifyBattery(percent: Int) {
        if (BridgeState.state.value.batteryPercent == percent) return
        BridgeState.update { it.copy(batteryPercent = percent) }
    }

    fun isConnected(): Boolean = BridgeState.state.value.connected

    private fun itSafe(name: String?, number: String) = name ?: number
}
