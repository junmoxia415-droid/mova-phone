package com.studiolexair.movaphone.core.security.pin

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studiolexair.movaphone.core.security.crypto.CryptoManager
import kotlinx.coroutines.flow.first
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.secureDataStore by preferencesDataStore(name = "mova_secure")

/**
 * PIN de la aplicación.
 * El PIN nunca se almacena: se guarda un hash PBKDF2 con sal aleatoria,
 * cifrado con clave del Android Keystore (requisito 30 y 45).
 */
class PinManager(
    private val context: Context,
    private val crypto: CryptoManager
) {

    suspend fun isPinSet(): Boolean {
        val prefs = context.secureDataStore.data.first()
        return !prefs[SALT].isNullOrBlank() && !prefs[HASH].isNullOrBlank()
    }

    suspend fun setPin(pin: String): Boolean {
        if (pin.length < MIN_LENGTH) return false
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iterations = iterations()
        val hash = hash(pin, salt, iterations)
        val encryptedHash = crypto.encrypt(hash) ?: return false
        val encryptedSalt = crypto.encrypt(Base64.encodeToString(salt, Base64.NO_WRAP))
            ?: return false

        context.secureDataStore.edit { prefs ->
            prefs[SALT] = encryptedSalt
            prefs[HASH] = encryptedHash
            prefs[ITERATIONS] = iterations.toString()
        }
        return true
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.secureDataStore.data.first()
        val encryptedSalt = prefs[SALT] ?: return false
        val encryptedHash = prefs[HASH] ?: return false
        val storedIterations = prefs[ITERATIONS]?.toIntOrNull() ?: iterations()

        val salt = crypto.decrypt(encryptedSalt)?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: return false
        val expected = crypto.decrypt(encryptedHash) ?: return false
        val candidate = hash(pin, salt, storedIterations)
        return constantTimeEquals(expected, candidate)
    }

    suspend fun clearPin() {
        context.secureDataStore.edit { prefs ->
            prefs.remove(SALT)
            prefs.remove(HASH)
            prefs.remove(ITERATIONS)
        }
    }

    private fun hash(pin: String, salt: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return Base64.encodeToString(factory.generateSecret(spec).encoded, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    private fun iterations(): Int = 120_000

    private companion object {
        const val MIN_LENGTH = 4
        const val SALT_BYTES = 16
        const val KEY_BITS = 256
        val SALT = stringPreferencesKey("pin_salt_enc")
        val HASH = stringPreferencesKey("pin_hash_enc")
        val ITERATIONS = stringPreferencesKey("pin_iterations")
    }
}
