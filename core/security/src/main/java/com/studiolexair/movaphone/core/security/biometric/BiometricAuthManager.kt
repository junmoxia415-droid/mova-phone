package com.studiolexair.movaphone.core.security.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Autenticación biométrica con BiometricPrompt (requisito 16).
 * Se informa con claridad cuando el dispositivo no la admite:
 * nunca se simula una verificación.
 */
class BiometricAuthManager(private val context: Context) {

    fun availability(): BiometricAvailability = when (
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)
    ) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_BUSY
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
        else -> BiometricAvailability.UNAVAILABLE
    }

    fun isAvailable(): Boolean = availability() == BiometricAvailability.AVAILABLE

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    MovaLog.w(TAG, "Biometría cancelada/error: $errorCode")
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onFailed()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Usar PIN")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        const val TAG = "BiometricAuth"
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_BUSY,
    NONE_ENROLLED,
    UNAVAILABLE
}
