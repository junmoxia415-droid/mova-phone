package com.studiolexair.movaphone.core.security.settings

/**
 * Todos los ajustes de la aplicación en un único modelo inmutable.
 * Se persisten con DataStore (requisito 36: sistema de configuración completo).
 */
data class MovaSettings(
    // General
    val userName: String = "Usuario",
    val language: String = "es",
    val onboardingCompleted: Boolean = false,
    // Llamadas
    val hapticKeypad: Boolean = true,
    val confirmBeforeCalling: Boolean = false,
    val blockUnknownNumbers: Boolean = false,
    val spamDetectionEnabled: Boolean = true,
    // Contactos
    val showDeviceContacts: Boolean = true,
    val mergeDuplicates: Boolean = true,
    // Mensajes
    val quickRepliesEnabled: Boolean = true,
    val emergencyTemplateId: Long = 0,
    // SOS
    val sosHoldSeconds: Int = 3,
    val sosSendSms: Boolean = true,
    val sosShareLocation: Boolean = true,
    val sosPlaceCall: Boolean = true,
    val sosCountdownVisible: Boolean = true,
    // Seguridad
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val lockTimeoutSeconds: Int = 30,
    val privateModeEnabled: Boolean = false,
    // Privacidad
    val storeCallHistory: Boolean = true,
    val storeLocationHistory: Boolean = false,
    val anonymousDiagnostics: Boolean = false,
    // Automatizaciones
    val automationsEnabled: Boolean = true,
    val automationHistoryEnabled: Boolean = true,
    // Ubicación
    val highAccuracyLocation: Boolean = true,
    val shareLocationOnSos: Boolean = true,
    // Notificaciones
    val notificationsEnabled: Boolean = true,
    val sosNotifications: Boolean = true,
    val messageNotifications: Boolean = true,
    val automationNotifications: Boolean = true,
    val securityNotifications: Boolean = true,
    val silentMode: Boolean = false,
    // Accesibilidad
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    // Modo conducción
    val drivingModeEnabled: Boolean = false,
    val drivingAutoDetect: Boolean = false,
    val drivingVoiceCommands: Boolean = true,
    // Apariencia
    val darkTheme: Boolean = true,
    val followSystemTheme: Boolean = true,
    // Datos
    val lastBackupAt: Long = 0L
) {
    companion object {
        val DEFAULT = MovaSettings()
    }
}
