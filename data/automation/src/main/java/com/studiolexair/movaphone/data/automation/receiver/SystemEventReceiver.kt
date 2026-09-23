package com.studiolexair.movaphone.data.automation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Disparadores de automatización que dependen del estado del sistema.
 *
 * Android avisa a la app de estos cambios y aquí se traducen a los disparadores que el
 * usuario ve en el editor de automatizaciones: cargador conectado, Wi-Fi, Bluetooth y
 * batería. Así el motor no depende de trabajo periódico para estos casos y responde al
 * instante, sin servicios permanentes.
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val engine = AutomationEventBridge.engine ?: return
        val type = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> TriggerType.CHARGING
            Intent.ACTION_POWER_DISCONNECTED -> null
            ConnectivityManager.CONNECTIVITY_ACTION -> if (hasWifi(context)) TriggerType.WIFI_CONNECTED else null
            android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED -> TriggerType.BLUETOOTH_CONNECTED
            Intent.ACTION_BATTERY_CHANGED -> if (intent.isBatteryLow()) TriggerType.BATTERY_LOW else null
            else -> null
        } ?: return

        val payload = TriggerPayload(batteryPercent = intent.batteryPercent())
        MovaLog.i(TAG, "Disparador de sistema: $type")
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                engine.onTrigger(type, payload)
            } catch (t: Throwable) {
                MovaLog.e(TAG, "Fallo al evaluar automatizaciones de sistema", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun hasWifi(context: Context): Boolean = try {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } catch (t: Throwable) {
        false
    }

    private fun Intent.isBatteryLow(): Boolean {
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val level = batteryPercent() ?: return false
        return !charging && level <= LOW_BATTERY_PERCENT
    }

    private fun Intent.batteryPercent(): Int? {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return level * 100 / scale
    }

    companion object {
        private const val TAG = "SystemEventReceiver"
        private const val LOW_BATTERY_PERCENT = 15
    }
}

/**
 * Registra en tiempo de ejecución los avisos del sistema que Android **no** entrega a los
 * receptores declarados en el manifiesto. `ACTION_BATTERY_CHANGED` es un broadcast *sticky*:
 * la documentación de Android lo dice expresamente, así que se registra aquí con la app viva.
 */
class SystemEventsRegistrar(private val context: Context) {

    private var receiver: BroadcastReceiver? = null

    fun register() {
        if (receiver != null) return
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                SystemEventReceiver().onReceive(context, intent)
            }
        }
        val filter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        try {
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                batteryReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiver = batteryReceiver
            MovaLog.i(TAG, "Avisos de batería registrados en tiempo de ejecución")
        } catch (t: Throwable) {
            MovaLog.e(TAG, "No fue posible registrar los avisos de batería", t)
        }
    }

    private companion object {
        const val TAG = "SystemEventsRegistrar"
    }
}

/**
 * Puente entre los receptores del sistema y el motor de automatizaciones.
 * Lo rellena el contenedor de la aplicación al arrancar (patrón usado también en
 * llamadas y SMS, donde Android instancia los componentes por sí mismo).
 */
object AutomationEventBridge {
    @Volatile var engine: com.studiolexair.movaphone.domain.automation.repository.AutomationEngine? = null

    /** Lanza un disparador desde cualquier punto de la app (conducción, SOS, desbloqueo). */
    fun fire(type: TriggerType, payload: TriggerPayload = TriggerPayload.EMPTY) {
        val engine = engine ?: return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                engine.onTrigger(type, payload)
            } catch (t: Throwable) {
                MovaLog.e("AutomationEventBridge", "Fallo lanzando $type", t)
            }
        }
    }

    /** Android 12+ exige el permiso de Bluetooth para leer el estado de los dispositivos. */
    fun bluetoothPermission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.Manifest.permission.BLUETOOTH_CONNECT else null
}
