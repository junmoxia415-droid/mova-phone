package com.studiolexair.movaphone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.security.settings.MovaSettings
import com.studiolexair.movaphone.core.security.settings.MovaSettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sistema de configuración (requisito 36).
 * Todas las secciones comparten el mismo almacén (DataStore) y se aplican al instante.
 */
class SettingsViewModel(private val store: MovaSettingsStore) : ViewModel() {

    val settings: StateFlow<MovaSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovaSettings.DEFAULT)

    fun setUserName(value: String) = viewModelScope.launch { store.setUserName(value) }
    fun setHaptic(value: Boolean) = viewModelScope.launch { store.setHapticKeypad(value) }
    fun setConfirmCall(value: Boolean) = viewModelScope.launch { store.setConfirmBeforeCalling(value) }
    fun setBlockUnknown(value: Boolean) = viewModelScope.launch { store.setBlockUnknownNumbers(value) }
    fun setSpamDetection(value: Boolean) = viewModelScope.launch { store.setSpamDetection(value) }
    fun setSosSms(value: Boolean) = viewModelScope.launch { store.setSosSendSms(value) }
    fun setSosLocation(value: Boolean) = viewModelScope.launch { store.setSosShareLocation(value) }
    fun setSosCall(value: Boolean) = viewModelScope.launch { store.setSosPlaceCall(value) }
    fun setStoreCalls(value: Boolean) = viewModelScope.launch { store.setStoreCallHistory(value) }
    fun setStoreLocation(value: Boolean) = viewModelScope.launch { store.setStoreLocationHistory(value) }
    fun setAutomations(value: Boolean) = viewModelScope.launch { store.setAutomations(value) }
    fun setHighAccuracy(value: Boolean) = viewModelScope.launch { store.setHighAccuracyLocation(value) }
    fun setNotifications(value: Boolean) = viewModelScope.launch { store.setNotifications(value) }
    fun setSosNotifications(value: Boolean) = viewModelScope.launch { store.setSosNotifications(value) }
    fun setMessageNotifications(value: Boolean) = viewModelScope.launch { store.setMessageNotifications(value) }
    fun setAutomationNotifications(value: Boolean) = viewModelScope.launch { store.setAutomationNotifications(value) }
    fun setSecurityNotifications(value: Boolean) = viewModelScope.launch { store.setSecurityNotifications(value) }
    fun setLargeText(value: Boolean) = viewModelScope.launch { store.setLargeText(value) }
    fun setHighContrast(value: Boolean) = viewModelScope.launch { store.setHighContrast(value) }
    fun setReduceMotion(value: Boolean) = viewModelScope.launch { store.setReduceMotion(value) }
    fun setDrivingMode(value: Boolean) = viewModelScope.launch { store.setDrivingMode(value) }
    fun setDrivingAutoDetect(value: Boolean) = viewModelScope.launch { store.setDrivingAutoDetect(value) }
    fun setDrivingVoice(value: Boolean) = viewModelScope.launch { store.setDrivingVoiceCommands(value) }
    fun setDarkTheme(value: Boolean) = viewModelScope.launch { store.setDarkTheme(value) }
    fun setFollowSystemTheme(value: Boolean) = viewModelScope.launch { store.setFollowSystemTheme(value) }
    fun setLastBackup(value: Long) = viewModelScope.launch { store.setLastBackupAt(value) }

    companion object {
        fun factory(store: MovaSettingsStore) = viewModelFactory {
            initializer { SettingsViewModel(store) }
        }
    }
}
