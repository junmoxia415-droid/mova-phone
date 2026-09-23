package com.studiolexair.movaphone.core.security.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de bloqueo de la aplicación.
 * El bloqueo se activa al salir y se levanta al autenticarse; nunca se guarda
 * el estado de desbloqueo en disco.
 */
class AppLockController {

    private val unlockedState = MutableStateFlow(false)
    private var lastBackgroundAt: Long = 0L

    val isUnlocked: StateFlow<Boolean> = unlockedState.asStateFlow()

    fun unlock() {
        unlockedState.value = true
    }

    fun lockNow() {
        unlockedState.value = false
    }

    fun onAppBackgrounded(timestamp: Long = System.currentTimeMillis()) {
        lastBackgroundAt = timestamp
    }

    /** Al volver a primer plano, se relockea si se superó el tiempo configurado. */
    fun onAppForegrounded(timeoutSeconds: Int, now: Long = System.currentTimeMillis()) {
        if (timeoutSeconds <= 0) return
        val elapsed = now - lastBackgroundAt
        if (lastBackgroundAt > 0 && elapsed >= timeoutSeconds * 1000L) {
            unlockedState.value = false
        }
    }
}
