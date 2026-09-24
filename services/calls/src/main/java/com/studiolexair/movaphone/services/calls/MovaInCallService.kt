package com.studiolexair.movaphone.services.calls

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * MOVA Phone **sustituye a la aplicación de teléfono del sistema**.
 *
 * Android entrega aquí todas las llamadas (entrantes y salientes) cuando MOVA Phone tiene el
 * rol de teléfono. Desde este servicio se controlan de verdad: contestar, colgar, espera,
 * silenciar, altavoz y tonos DTMF, y se publica el estado en [CallSessionHolder] para que la
 * pantalla de llamada propia de MOVA (`InCallActivity`) lo muestre.
 *
 * No es una simulación: son las APIs oficiales de `android.telecom`.
 */
class MovaInCallService : InCallService() {

    private val callbacks = mutableMapOf<Call, Call.Callback>()

    override fun onCreate() {
        super.onCreate()
        registerActions()
        MovaLog.i(TAG, "Servicio de llamadas de MOVA activo")
    }

    override fun onDestroy() {
        callbacks.keys.forEach { call -> callbacks[call]?.let(call::unregisterCallback) }
        callbacks.clear()
        CallSessionHolder.reset()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) = publish()
            override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
            override fun onCallDestroyed(call: Call) {
                call.unregisterCallback(this)
                callbacks.remove(call)
                publish()
            }
        }
        call.registerCallback(callback, null)
        callbacks[call] = callback
        publish()
        // Se avisa a la capa de aplicación para que abra la pantalla de llamada de MOVA.
        CallServiceDependencies.onShowInCallUi?.invoke()
        MovaLog.i(TAG, "Llamada añadida al servicio de MOVA")
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        callbacks.remove(call)?.let { callback -> call.unregisterCallback(callback) }
        publish()
        if (calls.isEmpty()) CallServiceDependencies.onHideInCallUi?.invoke()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallSessionHolder.update { it.copy(audioRoute = audioState.route, muted = audioState.isMuted) }
    }

    /** Vuelca el estado real de Telecom en el contenedor observable de la interfaz. */
    private fun publish() {
        val snapshots = calls.map { call ->
            val details = call.details
            val number = details?.handle?.schemeSpecificPart.orEmpty()
            CallSnapshot(
                id = call.toString(),
                number = number,
                contactName = CallServiceDependencies.contactNameResolver?.invoke(number),
                state = call.state,
                direction = details?.callDirection ?: Call.Details.DIRECTION_UNKNOWN,
                isIncoming = details?.callDirection == Call.Details.DIRECTION_INCOMING,
                isConference = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    ((details?.callProperties ?: 0) and Call.Details.PROPERTY_CONFERENCE) != 0
                } else {
                    false
                },
                startedAt = System.currentTimeMillis(),
                connectTimeMillis = details?.connectTimeMillis ?: 0L,
                muted = callAudioState?.isMuted == true,
                held = call.state == Call.STATE_HOLDING
            )
        }
        CallSessionHolder.update { current ->
            current.copy(
                calls = snapshots,
                muted = callAudioState?.isMuted ?: current.muted,
                audioRoute = callAudioState?.route ?: current.audioRoute,
                secondaryCall = if (snapshots.size > 1) snapshots[1] else null
            )
        }
    }

    /** Puente entre los botones de la pantalla y las llamadas reales. */
    private fun registerActions() {
        CallSessionHolder.answer = { id -> findCall(id)?.let { call -> call.answer(VideoProfile.STATE_AUDIO_ONLY) } }
        CallSessionHolder.hangUp = { id -> findCall(id)?.let { call -> call.disconnect() } }
        CallSessionHolder.hold = { id, hold ->
            findCall(id)?.let { call -> if (hold) call.hold() else call.unhold() }
        }
        CallSessionHolder.mute = { muted -> setMuted(muted) }
        CallSessionHolder.speaker = { speakerOn ->
            setAudioRoute(
                if (speakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            )
        }
        CallSessionHolder.playDtmf = { id, tone -> findCall(id)?.playDtmfTone(tone) }
        CallSessionHolder.stopDtmf = { calls.forEach { call -> call.stopDtmfTone() } }
        CallSessionHolder.hangUpAll = { calls.toList().forEach { call -> call.disconnect() } }
        CallSessionHolder.swapCalls = {
            calls.forEach { call ->
                if (call.state == Call.STATE_HOLDING) call.unhold() else call.hold()
            }
        }
    }

    private fun findCall(id: String): Call? = calls.firstOrNull { it.toString() == id }

    private companion object {
        const val TAG = "MovaInCallService"
    }
}
