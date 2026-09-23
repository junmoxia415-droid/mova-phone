package com.studiolexair.movaphone.core.security.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mova_settings")

/**
 * Persistencia de ajustes con DataStore (asíncrono, sin bloqueos y sin ANR).
 * Ningún secreto se guarda aquí en texto plano: ver SecretStore y Keystore.
 */
class MovaSettingsStore(private val context: Context) {

    val settings: Flow<MovaSettings> = context.settingsDataStore.data.map { prefs ->
        MovaSettings(
            userName = prefs[Keys.USER_NAME] ?: MovaSettings.DEFAULT.userName,
            onboardingCompleted = prefs[Keys.ONBOARDING] ?: false,
            hapticKeypad = prefs[Keys.HAPTIC] ?: true,
            confirmBeforeCalling = prefs[Keys.CONFIRM_CALL] ?: false,
            blockUnknownNumbers = prefs[Keys.BLOCK_UNKNOWN] ?: false,
            spamDetectionEnabled = prefs[Keys.SPAM] ?: true,
            showDeviceContacts = prefs[Keys.SHOW_DEVICE_CONTACTS] ?: true,
            quickRepliesEnabled = prefs[Keys.QUICK_REPLIES] ?: true,
            sosHoldSeconds = prefs[Keys.SOS_HOLD] ?: 3,
            sosSendSms = prefs[Keys.SOS_SMS] ?: true,
            sosShareLocation = prefs[Keys.SOS_LOCATION] ?: true,
            sosPlaceCall = prefs[Keys.SOS_CALL] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            lockTimeoutSeconds = prefs[Keys.LOCK_TIMEOUT] ?: 30,
            privateModeEnabled = prefs[Keys.PRIVATE_MODE] ?: false,
            storeCallHistory = prefs[Keys.STORE_CALLS] ?: true,
            storeLocationHistory = prefs[Keys.STORE_LOCATION] ?: false,
            automationsEnabled = prefs[Keys.AUTOMATIONS] ?: true,
            highAccuracyLocation = prefs[Keys.HIGH_ACCURACY] ?: true,
            shareLocationOnSos = prefs[Keys.SHARE_LOCATION_SOS] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            sosNotifications = prefs[Keys.NOTIF_SOS] ?: true,
            messageNotifications = prefs[Keys.NOTIF_MESSAGES] ?: true,
            automationNotifications = prefs[Keys.NOTIF_AUTOMATION] ?: true,
            securityNotifications = prefs[Keys.NOTIF_SECURITY] ?: true,
            silentMode = prefs[Keys.SILENT_MODE] ?: false,
            largeText = prefs[Keys.LARGE_TEXT] ?: false,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            drivingModeEnabled = prefs[Keys.DRIVING] ?: false,
            drivingAutoDetect = prefs[Keys.DRIVING_AUTO] ?: false,
            drivingVoiceCommands = prefs[Keys.DRIVING_VOICE] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            followSystemTheme = prefs[Keys.FOLLOW_SYSTEM] ?: true,
            lastBackupAt = prefs[Keys.LAST_BACKUP] ?: 0L
        )
    }

    suspend fun setUserName(value: String) = put(Keys.USER_NAME, value)
    suspend fun setOnboardingCompleted(value: Boolean) = put(Keys.ONBOARDING, value)
    suspend fun setHapticKeypad(value: Boolean) = put(Keys.HAPTIC, value)
    suspend fun setConfirmBeforeCalling(value: Boolean) = put(Keys.CONFIRM_CALL, value)
    suspend fun setBlockUnknownNumbers(value: Boolean) = put(Keys.BLOCK_UNKNOWN, value)
    suspend fun setSpamDetection(value: Boolean) = put(Keys.SPAM, value)
    suspend fun setSosHoldSeconds(value: Int) = put(Keys.SOS_HOLD, value)
    suspend fun setSosSendSms(value: Boolean) = put(Keys.SOS_SMS, value)
    suspend fun setSosShareLocation(value: Boolean) = put(Keys.SOS_LOCATION, value)
    suspend fun setSosPlaceCall(value: Boolean) = put(Keys.SOS_CALL, value)
    suspend fun setAppLock(value: Boolean) = put(Keys.APP_LOCK, value)
    suspend fun setBiometric(value: Boolean) = put(Keys.BIOMETRIC, value)
    suspend fun setPinEnabled(value: Boolean) = put(Keys.PIN_ENABLED, value)
    suspend fun setLockTimeout(seconds: Int) = put(Keys.LOCK_TIMEOUT, seconds)
    suspend fun setPrivateMode(value: Boolean) = put(Keys.PRIVATE_MODE, value)
    suspend fun setStoreCallHistory(value: Boolean) = put(Keys.STORE_CALLS, value)
    suspend fun setStoreLocationHistory(value: Boolean) = put(Keys.STORE_LOCATION, value)
    suspend fun setAutomations(value: Boolean) = put(Keys.AUTOMATIONS, value)
    suspend fun setHighAccuracyLocation(value: Boolean) = put(Keys.HIGH_ACCURACY, value)
    suspend fun setShareLocationOnSos(value: Boolean) = put(Keys.SHARE_LOCATION_SOS, value)
    suspend fun setNotifications(value: Boolean) = put(Keys.NOTIFICATIONS, value)
    suspend fun setSosNotifications(value: Boolean) = put(Keys.NOTIF_SOS, value)
    suspend fun setMessageNotifications(value: Boolean) = put(Keys.NOTIF_MESSAGES, value)
    suspend fun setAutomationNotifications(value: Boolean) = put(Keys.NOTIF_AUTOMATION, value)
    suspend fun setSecurityNotifications(value: Boolean) = put(Keys.NOTIF_SECURITY, value)
    suspend fun setSilentMode(value: Boolean) = put(Keys.SILENT_MODE, value)
    suspend fun setLargeText(value: Boolean) = put(Keys.LARGE_TEXT, value)
    suspend fun setHighContrast(value: Boolean) = put(Keys.HIGH_CONTRAST, value)
    suspend fun setReduceMotion(value: Boolean) = put(Keys.REDUCE_MOTION, value)
    suspend fun setDrivingMode(value: Boolean) = put(Keys.DRIVING, value)
    suspend fun setDrivingAutoDetect(value: Boolean) = put(Keys.DRIVING_AUTO, value)
    suspend fun setDrivingVoiceCommands(value: Boolean) = put(Keys.DRIVING_VOICE, value)
    suspend fun setDarkTheme(value: Boolean) = put(Keys.DARK_THEME, value)
    suspend fun setFollowSystemTheme(value: Boolean) = put(Keys.FOLLOW_SYSTEM, value)
    suspend fun setLastBackupAt(value: Long) = put(Keys.LAST_BACKUP, value)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val HAPTIC = booleanPreferencesKey("haptic_keypad")
        val CONFIRM_CALL = booleanPreferencesKey("confirm_before_calling")
        val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown_numbers")
        val SPAM = booleanPreferencesKey("spam_detection")
        val SHOW_DEVICE_CONTACTS = booleanPreferencesKey("show_device_contacts")
        val QUICK_REPLIES = booleanPreferencesKey("quick_replies")
        val SOS_HOLD = intPreferencesKey("sos_hold_seconds")
        val SOS_SMS = booleanPreferencesKey("sos_send_sms")
        val SOS_LOCATION = booleanPreferencesKey("sos_share_location")
        val SOS_CALL = booleanPreferencesKey("sos_place_call")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout_seconds")
        val PRIVATE_MODE = booleanPreferencesKey("private_mode")
        val STORE_CALLS = booleanPreferencesKey("store_call_history")
        val STORE_LOCATION = booleanPreferencesKey("store_location_history")
        val AUTOMATIONS = booleanPreferencesKey("automations_enabled")
        val HIGH_ACCURACY = booleanPreferencesKey("high_accuracy_location")
        val SHARE_LOCATION_SOS = booleanPreferencesKey("share_location_sos")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val NOTIF_SOS = booleanPreferencesKey("notif_sos")
        val NOTIF_MESSAGES = booleanPreferencesKey("notif_messages")
        val NOTIF_AUTOMATION = booleanPreferencesKey("notif_automation")
        val NOTIF_SECURITY = booleanPreferencesKey("notif_security")
        val SILENT_MODE = booleanPreferencesKey("silent_mode")
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val DRIVING = booleanPreferencesKey("driving_mode")
        val DRIVING_AUTO = booleanPreferencesKey("driving_auto_detect")
        val DRIVING_VOICE = booleanPreferencesKey("driving_voice")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val FOLLOW_SYSTEM = booleanPreferencesKey("follow_system_theme")
        val LAST_BACKUP = longPreferencesKey("last_backup_at")
    }
}
