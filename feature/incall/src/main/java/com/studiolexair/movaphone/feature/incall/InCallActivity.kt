package com.studiolexair.movaphone.feature.incall

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.services.calls.CallSessionHolder

/**
 * Pantalla de llamada **de MOVA**.
 *
 * Cuando MOVA Phone tiene el rol de teléfono, Telecom entrega las llamadas a
 * `MovaInCallService` y esta actividad muestra la interfaz propia: quién llama, contestar,
 * colgar, altavoz, silenciar, teclado DTMF y cambio de llamada. La del sistema no aparece.
 */
class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Se muestra sobre la pantalla de bloqueo y enciende la pantalla: una llamada no puede esperar.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MovaTheme(darkTheme = true) {
                val state by CallSessionHolder.state.collectAsStateWithLifecycle()
                InCallScreen(
                    state = state,
                    onAnswer = { id -> CallSessionHolder.answer(id) },
                    onHangUp = { id -> CallSessionHolder.hangUp(id) },
                    onHangUpAll = { CallSessionHolder.hangUpAll() },
                    onHoldToggle = { id, hold -> CallSessionHolder.hold(id, hold) },
                    onMuteToggle = { muted -> CallSessionHolder.mute(muted) },
                    onSpeakerToggle = { speaker -> CallSessionHolder.speaker(speaker) },
                    onDtmf = { id, tone -> CallSessionHolder.playDtmf(id, tone) },
                    onDtmfStop = { CallSessionHolder.stopDtmf() },
                    onSwapCalls = { CallSessionHolder.swapCalls() },
                    onDismiss = { if (state.isEmpty) finish() }
                )
            }
        }
    }

    override fun onBackPressed() {
        // No se cierra la pantalla de llamada con el botón atrás: una llamada sigue en curso.
        val state = CallSessionHolder.state.value
        if (state.isEmpty) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
