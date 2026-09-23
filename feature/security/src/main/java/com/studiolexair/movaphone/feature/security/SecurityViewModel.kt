package com.studiolexair.movaphone.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.core.database.entity.BlockedNumberEntity
import com.studiolexair.movaphone.core.database.entity.SecurityEventEntity
import com.studiolexair.movaphone.core.database.entity.TrustedContactEntity
import com.studiolexair.movaphone.core.security.biometric.BiometricAuthManager
import com.studiolexair.movaphone.core.security.event.SecurityEventLogger
import com.studiolexair.movaphone.core.security.event.SecurityEventType
import com.studiolexair.movaphone.core.security.pin.PinManager
import com.studiolexair.movaphone.core.security.settings.MovaSettings
import com.studiolexair.movaphone.core.security.settings.MovaSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Centro de seguridad (requisitos 16 y 17): bloqueo de la app, PIN, biometría,
 * números bloqueados, contactos de confianza y registro de eventos.
 */
class SecurityViewModel(
    private val securityDao: SecurityDao,
    private val settingsStore: MovaSettingsStore,
    private val pinManager: PinManager,
    private val biometricManager: BiometricAuthManager,
    private val eventLogger: SecurityEventLogger
) : ViewModel() {

    private val messageState = MutableStateFlow<String?>(null)

    val settings: StateFlow<MovaSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovaSettings.DEFAULT)

    val blockedNumbers: StateFlow<List<BlockedNumberEntity>> = securityDao.observeBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trustedContacts: StateFlow<List<TrustedContactEntity>> = securityDao.observeTrusted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<SecurityEventEntity>> = eventLogger.observeEvents(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val statusMessage: StateFlow<String?> = messageState.asStateFlow()

    val pinIsSet: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        viewModelScope.launch { pinIsSet.value = pinManager.isPinSet() }
    }

    // ---------- Ajustes de seguridad ----------
    fun setAppLock(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setAppLock(enabled)
        eventLogger.log(SecurityEventType.PRIVATE_MODE_TOGGLED, if (enabled) "Bloqueo de app activado" else "Bloqueo de app desactivado")
    }

    fun setPrivateMode(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setPrivateMode(enabled)
        eventLogger.log(SecurityEventType.PRIVATE_MODE_TOGGLED, if (enabled) "Modo privado activado" else "Modo privado desactivado")
    }

    fun biometricAvailability() = biometricManager.availability()

    fun setBiometric(enabled: Boolean) = viewModelScope.launch {
        if (enabled && !biometricManager.isAvailable()) {
            messageState.value = when (biometricManager.availability()) {
                com.studiolexair.movaphone.core.security.biometric.BiometricAvailability.NO_HARDWARE ->
                    "Este dispositivo no tiene lector biométrico."
                com.studiolexair.movaphone.core.security.biometric.BiometricAvailability.NONE_ENROLLED ->
                    "No hay huellas o rostro registrados en el sistema. Configúralos en los ajustes del teléfono."
                else -> "La biometría no está disponible en este momento."
            }
            return@launch
        }
        settingsStore.setBiometric(enabled)
        eventLogger.log(SecurityEventType.BIOMETRIC_ENABLED, if (enabled) "Biometría activada" else "Biometría desactivada")
    }

    // ---------- PIN ----------
    fun setPin(pin: String) = viewModelScope.launch {
        val ok = pinManager.setPin(pin)
        pinIsSet.value = ok
        if (ok) {
            settingsStore.setPinEnabled(true)
            eventLogger.log(SecurityEventType.PIN_CHANGED, "PIN de la aplicación actualizado")
            messageState.value = "PIN guardado"
        } else {
            messageState.value = "El PIN debe tener al menos 4 dígitos."
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = pinManager.verifyPin(pin)
        eventLogger.log(
            if (ok) SecurityEventType.APP_UNLOCKED else SecurityEventType.PIN_FAILED,
            if (ok) "Desbloqueo correcto con PIN" else "Intento de desbloqueo fallido con PIN",
            if (ok) com.studiolexair.movaphone.core.security.event.Severity.INFO else com.studiolexair.movaphone.core.security.event.Severity.WARNING
        )
        onResult(ok)
    }

    fun clearPin() = viewModelScope.launch {
        pinManager.clearPin()
        pinIsSet.value = false
        settingsStore.setPinEnabled(false)
        messageState.value = "PIN eliminado"
    }

    // ---------- Números bloqueados ----------
    fun blockNumber(number: String, label: String? = null) = viewModelScope.launch {
        if (number.isBlank()) return@launch
        securityDao.block(
            BlockedNumberEntity(
                normalizedNumber = PhoneNumbers.normalize(number),
                phoneNumber = number,
                label = label,
                createdAt = System.currentTimeMillis()
            )
        )
        eventLogger.log(SecurityEventType.NUMBER_BLOCKED, "Número bloqueado: $number")
        messageState.value = "Número bloqueado"
    }

    fun unblock(entity: BlockedNumberEntity) = viewModelScope.launch {
        securityDao.unblock(entity)
        messageState.value = "Número desbloqueado"
    }

    // ---------- Contactos de confianza ----------
    fun addTrusted(name: String, phone: String) = viewModelScope.launch {
        if (name.isBlank() || phone.isBlank()) return@launch
        securityDao.addTrusted(
            TrustedContactEntity(
                name = name,
                phoneNumber = phone,
                normalizedNumber = PhoneNumbers.normalize(phone),
                createdAt = System.currentTimeMillis()
            )
        )
        messageState.value = "Contacto de confianza añadido"
    }

    fun removeTrusted(entity: TrustedContactEntity) = viewModelScope.launch {
        securityDao.removeTrusted(entity.id)
        messageState.value = "Contacto de confianza eliminado"
    }

    // ---------- Eventos ----------
    fun clearEvents() = viewModelScope.launch {
        eventLogger.clear()
        messageState.value = "Registro de eventos vaciado"
    }

    fun clearMessage() {
        messageState.value = null
    }

    companion object {
        fun factory(
            securityDao: SecurityDao,
            settingsStore: MovaSettingsStore,
            pinManager: PinManager,
            biometricManager: BiometricAuthManager,
            eventLogger: SecurityEventLogger
        ) = viewModelFactory {
            initializer { SecurityViewModel(securityDao, settingsStore, pinManager, biometricManager, eventLogger) }
        }
    }
}
