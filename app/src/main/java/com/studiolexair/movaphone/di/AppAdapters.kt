package com.studiolexair.movaphone.di

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.dao.ContactDao
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.core.security.settings.MovaSettingsStore
import com.studiolexair.movaphone.domain.automation.repository.AutomationClock
import com.studiolexair.movaphone.domain.automation.repository.BatteryLevelReader
import com.studiolexair.movaphone.domain.automation.repository.DrivingModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Implementaciones de los puertos de dominio que necesitan Android.
 * Viven en el módulo app porque son el pegamento entre capas:
 * el dominio declara la necesidad y aquí se resuelve con APIs reales.
 */

/** Modo conducción persistido en los ajustes del usuario. */
class SettingsDrivingModeController(
    private val store: MovaSettingsStore,
    scope: CoroutineScope
) : DrivingModeController {

    @Volatile
    private var active: Boolean = false

    init {
        // Se mantiene sincronizado con DataStore sin bloquear el hilo principal.
        scope.launch {
            store.settings.collect { settings -> active = settings.drivingModeEnabled }
        }
    }

    override fun isDrivingModeActive(): Boolean = active

    override suspend fun setDrivingMode(enabled: Boolean) {
        store.setDrivingMode(enabled)
        active = enabled
    }
}

/** Reloj del sistema para el motor de automatización (inyectable en pruebas). */
class SystemAutomationClock : AutomationClock {
    override fun now(): Long = System.currentTimeMillis()
    override fun hourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}

/** Lectura real del nivel de batería. Devuelve null si el sistema no lo informa. */
class BatteryReader(private val context: Context) : BatteryLevelReader {

    override fun batteryPercent(): Int? = try {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (level in 0..100) {
            level
        } else {
            val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val raw = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (raw >= 0 && scale > 0) (raw * 100) / scale else null
        }
    } catch (t: Throwable) {
        MovaLog.w(TAG, "No fue posible leer la batería: ${t.message}")
        null
    }

    private companion object {
        const val TAG = "BatteryReader"
    }
}

/**
 * Caché de nombres de contacto para componentes que no pueden usar suspensiones
 * (por ejemplo el receptor del estado de llamada: debe responder de inmediato).
 * Se rellena al arrancar y se refresca cada vez que cambia la agenda local.
 */
class ContactNameCache(
    private val contactDao: ContactDao,
    scope: CoroutineScope
) {

    private val names = java.util.concurrent.ConcurrentHashMap<String, String>()

    init {
        scope.launch {
            runCatching {
                contactDao.observeAll().collect { contacts ->
                    names.clear()
                    contacts.forEach { contact ->
                        names[contact.normalizedNumber] = contact.displayName
                    }
                }
            }.onFailure { MovaLog.w(TAG, "No se pudo refrescar la caché de contactos") }
        }
    }

    /** Resuelve el nombre de un número. Devuelve null cuando no se conoce. */
    fun resolve(rawNumber: String?): String? {
        val normalized = PhoneNumbers.normalize(rawNumber)
        if (normalized.isBlank()) return null
        return names[normalized] ?: names.entries.firstOrNull { PhoneNumbers.sameNumber(it.key, normalized) }?.value
    }

    private companion object {
        const val TAG = "ContactNameCache"
    }
}

/** Utilidad: leer un ajuste puntual sin bloquear (para lambdas de conveniencia). */
suspend fun MovaSettingsStore.snapshot(): com.studiolexair.movaphone.core.security.settings.MovaSettings =
    settings.first()
