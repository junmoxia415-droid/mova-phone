package com.studiolexair.movaphone.services.calls

import android.telecom.Call
import android.telecom.CallAudioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de una llamada, tal y como lo necesita la pantalla de llamada de MOVA. */
data class CallSnapshot(
    val id: String,
    val number: String,
    val contactName: String?,
    val state: Int,
    val direction: Int,
    val isIncoming: Boolean,
    val isConference: Boolean,
    val startedAt: Long,
    val connectTimeMillis: Long,
    val muted: Boolean,
    val held: Boolean
) {
    val isRinging: Boolean get() = state == Call.STATE_RINGING
    val isActive: Boolean get() = state == Call.STATE_ACTIVE
    val isDialing: Boolean get() = state == Call.STATE_DIALING || state == Call.STATE_CONNECTING
    val isHolding: Boolean get() = state == Call.STATE_HOLDING
    val label: String
        get() = when (state) {
            Call.STATE_RINGING -> "Llamada entrante"
            Call.STATE_DIALING -> "Llamando…"
            Call.STATE_CONNECTING -> "Conectando…"
            Call.STATE_ACTIVE -> "En llamada"
            Call.STATE_HOLDING -> "En espera"
            Call.STATE_DISCONNECTED -> "Finalizada"
            else -> "Llamada"
        }
}

/** Estado global de las llamadas + acciones, para que la interfaz no toque Telecom directamente. */
object CallSessionHolder {

    data class UiState(
        val calls: List<CallSnapshot> = emptyList(),
        val audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
        val muted: Boolean = false,
        val secondaryCall: CallSnapshot? = null
    ) {
        val primary: CallSnapshot? get() = calls.firstOrNull { it.isActive || it.isRinging || it.isDialing || it.isHolding }
        val isEmpty: Boolean get() = calls.isEmpty()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** La interfaz rellena estas acciones; el servicio las ejecuta sobre los objetos Call. */
    var answer: (String) -> Unit = {}
    var hangUp: (String) -> Unit = {}
    var hold: (String, Boolean) -> Unit = { _, _ -> }
    var mute: (Boolean) -> Unit = {}
    var speaker: (Boolean) -> Unit = {}
    var playDtmf: (String, Char) -> Unit = { _, _ -> }
    var stopDtmf: () -> Unit = {}
    /** Acciones de la segunda llamada (espera/recuperar) y colgado general. */
    var hangUpAll: () -> Unit = {}
    var swapCalls: () -> Unit = {}

    internal fun update(transform: (UiState) -> UiState) {
        _state.value = transform(_state.value)
    }

    internal fun reset() {
        _state.value = UiState()
    }
}
