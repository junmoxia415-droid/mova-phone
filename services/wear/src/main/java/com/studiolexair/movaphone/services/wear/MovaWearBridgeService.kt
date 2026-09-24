package com.studiolexair.movaphone.services.wear

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.studiolexair.movaphone.core.logging.MovaLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Puente entre MOVA Phone y un **reloj Wear OS**, sin Google Play Services.
 *
 * El teléfono hace de servidor Bluetooth de bajo consumo (GATT): publica un servicio propio
 * y el reloj se conecta a él. Por ahí van los avisos que necesita la muñeca (llamada
 * entrante, últimos mensajes, batería del teléfono) y vuelven las órdenes (contestar,
 * colgar, responder con un mensaje corto, SOS).
 *
 * Nada se decide en el reloj por su cuenta: el SOS y las respuestas se confirman allí y se
 * ejecutan aquí, con las mismas reglas de seguridad de la aplicación.
 */
class MovaWearBridgeService : Service() {

    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private val devices = ConcurrentHashMap<String, BluetoothDevice>()
    private var running = false
    private var lastClientAt = 0L

    /** MTU acordado con cada reloj: no todos los aparatos aceptan notificaciones largas. */
    private val mtuByDevice = ConcurrentHashMap<String, Int>()

    /** Reensamblado de las órdenes que llegan troceadas desde el reloj. */
    private val commandAssemblers = ConcurrentHashMap<String, WearProtocol.Reassembler>()

    /** Último estado publicado (para quien lo lea directamente). */
    @Volatile private var lastPayload: ByteArray? = null

    /** Última petición de estado: descarta fragmentos de envíos antiguos. */
    private val sender = Handler(Looper.getMainLooper())
    private var generation = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // El estado del puente avisa al reloj cada vez que cambia algo (llamada, mensajes…).
        MovaWearBridgeServicePush.sender = { sendState() }
        MovaLog.i(TAG, "Puente con el reloj creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!running) startBridge()
        return START_STICKY
    }

    override fun onDestroy() {
        MovaWearBridgeServicePush.sender = null
        sender.removeCallbacksAndMessages(null)
        stopBridge()
        super.onDestroy()
    }

    // ---------------- Servidor GATT ----------------

    @SuppressLint("MissingPermission")
    private fun startBridge() {
        if (!hasBluetoothPermissions()) {
            MovaLog.w(TAG, "Faltan permisos de Bluetooth: el puente con el reloj no se inicia")
            BridgeState.update { it.copy(running = false, detail = "Faltan permisos de Bluetooth") }
            return
        }
        val manager = getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = manager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            MovaLog.w(TAG, "Bluetooth apagado: el reloj no podrá conectarse")
            BridgeState.update { it.copy(running = false, detail = "Bluetooth apagado") }
            return
        }

        val callback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                device ?: return
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    devices[device.address] = device
                    lastClientAt = System.currentTimeMillis()
                    MovaLog.i(TAG, "Reloj conectado: ${device.address}")
                    BridgeState.update { it.copy(connected = true, devices = devices.size, detail = "Reloj conectado") }
                    sendState()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    devices.remove(device.address)
                    mtuByDevice.remove(device.address)
                    commandAssemblers.remove(device.address)
                    BridgeState.update {
                        it.copy(connected = devices.isNotEmpty(), devices = devices.size, detail = "Reloj desconectado")
                    }
                }
            }

            /** El reloj nos dice qué tamaño de paquete acepta (normalmente 517). */
            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                device ?: return
                mtuByDevice[device.address] = mtu
                MovaLog.i(TAG, "MTU acordado con el reloj: $mtu")
                sendState()
            }

            /** Lectura directa del estado: responde con los bytes solicitados. */
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                val server = gattServer
                if (server == null || characteristic?.uuid != WearProtocol.STATE_UUID) {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    return
                }
                val bytes = lastPayload
                if (bytes == null) {
                    server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, ByteArray(0))
                } else {
                    val from = offset.coerceAtMost(bytes.size)
                    server.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        bytes.copyOfRange(from, bytes.size)
                    )
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
                if (descriptor?.uuid == WearProtocol.CLIENT_CONFIG_DESCRIPTOR) {
                    lastClientAt = System.currentTimeMillis()
                    MovaLog.i(TAG, "El reloj se ha suscrito a los avisos de MOVA")
                    sendState()
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                if (characteristic?.uuid != WearProtocol.COMMAND_UUID) {
                    if (responseNeeded) gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    return
                }
                if (responseNeeded) gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                lastClientAt = System.currentTimeMillis()
                val key = device?.address.orEmpty()
                val raw = commandAssemblers
                    .getOrPut(key) { WearProtocol.Reassembler() }
                    .accept(value)
                    ?: return
                val command = WearProtocol.parseCommand(raw)
                if (command == null) {
                    MovaLog.w(TAG, "Orden del reloj no válida")
                } else {
                    handleCommand(command)
                }
            }
        }

        val service = BluetoothGattService(
            WearProtocol.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val state = BluetoothGattCharacteristic(
            WearProtocol.STATE_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        state.addDescriptor(
            BluetoothGattDescriptor(
                WearProtocol.CLIENT_CONFIG_DESCRIPTOR,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
        )
        val commands = BluetoothGattCharacteristic(
            WearProtocol.COMMAND_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(state)
        service.addCharacteristic(commands)

        gattServer = manager.openGattServer(this, callback)?.apply {
            addService(service)
        }
        running = gattServer != null

        advertiser = adapter.bluetoothLeAdvertiser
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(android.os.ParcelUuid(WearProtocol.SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()
        runCatching { advertiser?.startAdvertising(settings, data, advertiseCallback) }
            .onFailure { MovaLog.e(TAG, "No se pudo anunciar el servicio de MOVA", it) }

        BridgeState.update {
            it.copy(
                running = running,
                detail = if (running) "Esperando al reloj…" else "No se pudo abrir el servidor Bluetooth"
            )
        }
        MovaLog.i(TAG, "Puente con el reloj iniciado (running=$running)")
    }

    @SuppressLint("MissingPermission")
    private fun stopBridge() {
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        runCatching { gattServer?.close() }
        gattServer = null
        advertiser = null
        devices.clear()
        running = false
        BridgeState.update { it.copy(running = false, connected = false, devices = 0, detail = "Puente detenido") }
        MovaLog.i(TAG, "Puente con el reloj detenido")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            MovaLog.e(TAG, "Fallo anunciando el servicio de MOVA (código $errorCode)")
        }
    }

    // ---------------- Órdenes del reloj ----------------

    private fun handleCommand(command: WearCommand) {
        MovaLog.i(TAG, "Orden del reloj: ${command.command}")
        when (command.command) {
            WearProtocol.CMD_ANSWER -> CallSessionHolderBridge.answer?.invoke()
            WearProtocol.CMD_REJECT, WearProtocol.CMD_HANGUP -> CallSessionHolderBridge.hangUp?.invoke()
            WearProtocol.CMD_SOS -> CallSessionHolderBridge.sos?.invoke()
            WearProtocol.CMD_REPLY -> {
                val to = command.to
                val body = command.body
                if (!to.isNullOrBlank() && !body.isNullOrBlank()) {
                    CallSessionHolderBridge.reply?.invoke(to, body)
                } else {
                    MovaLog.w(TAG, "Respuesta rápida incompleta del reloj")
                }
            }
            WearProtocol.CMD_PING -> sendState()
            else -> MovaLog.w(TAG, "Orden desconocida del reloj: ${command.command}")
        }
    }

    // ---------------- Envío de avisos al reloj ----------------

    private fun sendState() {
        val snapshot = BridgeState.state.value
        val payload = WearProtocol.state(
            batteryPercent = snapshot.batteryPercent,
            call = snapshot.call,
            messages = snapshot.messages
        )
        notifyWatch(payload)
    }

    @SuppressLint("MissingPermission")
    private fun notifyWatch(payload: String) {
        val server = gattServer ?: return
        if (devices.isEmpty()) return
        val service = server.getService(WearProtocol.SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(WearProtocol.STATE_UUID) ?: return
        val bytes = payload.toByteArray(Charsets.UTF_8)
        lastPayload = bytes
        // Lectura directa para quien pregunte.
        characteristic.value = bytes
        val ticket = ++generation
        devices.values.forEach { device ->
            val mtu = mtuByDevice[device.address] ?: WearProtocol.DEFAULT_MTU
            WearProtocol.frame(payload, mtu).forEachIndexed { index, frame ->
                sender.postDelayed({
                    // Si mientras tanto ha salido un estado más nuevo, estos fragmentos sobran.
                    if (ticket != generation) return@postDelayed
                    runCatching { notifyDevice(server, device, characteristic, frame) }
                        .onFailure { MovaLog.w(TAG, "No se pudo avisar al reloj: ${it.message}") }
                }, index * CHUNK_DELAY_MS)
            }
        }
    }

    /**
     * Notifica un fragmento al reloj.
     * En Android 13 y posteriores se usa el método con valor explícito; en versiones
     * anteriores el valor viaja dentro de la propia característica (compatible hasta Android 8).
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun notifyDevice(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        frame: ByteArray
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            server.notifyCharacteristicChanged(device, characteristic, false, frame)
        } else {
            characteristic.value = frame
            server.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reloj (Wear OS)",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Conexión de MOVA Phone con tu reloj" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(applicationInfo.loadLabel(packageManager).toString())
            .setContentText("Conectado con tu reloj (Wear OS)")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Auto-apagado: si nadie se conecta en 15 minutos, el puente se cierra solo. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val idleSince = System.currentTimeMillis() - lastClientAt
        if (devices.isEmpty() && idleSince > IDLE_TIMEOUT_MS) stopSelf()
    }

    private fun hasBluetoothPermissions(): Boolean {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        return needed.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    companion object {
        private const val TAG = "MovaWearBridge"
        private const val CHANNEL_ID = "mova_wear"
        private const val NOTIFICATION_ID = 7710
        private const val IDLE_TIMEOUT_MS = 15 * 60 * 1000L

        /** Pausa entre fragmentos: el reloj necesita digerir cada uno antes del siguiente. */
        private const val CHUNK_DELAY_MS = 35L

        fun start(context: Context) {
            WearBridgePreferences(context).enabled = true
            val intent = Intent(context, MovaWearBridgeService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure { MovaLog.w(TAG, "No se pudo arrancar el puente del reloj: ${it.message}") }
        }

        fun stop(context: Context) {
            WearBridgePreferences(context).enabled = false
            runCatching { context.stopService(Intent(context, MovaWearBridgeService::class.java)) }
        }

        /** Arranca el puente sólo si el usuario lo activó en Ajustes. */
        fun startIfEnabled(context: Context) {
            if (WearBridgePreferences(context).enabled) start(context)
        }
    }
}

/** Estado del puente, para mostrarlo en Ajustes y en el propio reloj. */
object BridgeState {
    data class State(
        val running: Boolean = false,
        val connected: Boolean = false,
        val devices: Int = 0,
        val batteryPercent: Int? = null,
        val detail: String = "Puente detenido",
        val call: JSONObject? = null,
        val messages: List<JSONObject> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    internal fun update(transform: (State) -> State) {
        val newState = transform(_state.value)
        _state.value = newState
        MovaWearBridgeServicePush.send(newState)
    }

    internal fun pushMessage(json: JSONObject) {
        update { it.copy(messages = (listOf(json) + it.messages).take(5)) }
    }
}

/** Empuja el estado al servicio sin crear dependencias circulares. */
internal object MovaWearBridgeServicePush {
    @Volatile var sender: ((BridgeState.State) -> Unit)? = null

    fun send(state: BridgeState.State) {
        runCatching { sender?.invoke(state) }
    }
}

/** Acciones que ejecuta la aplicación (se conectan desde el contenedor de dependencias). */
object CallSessionHolderBridge {
    @Volatile var answer: (() -> Unit)? = null
    @Volatile var hangUp: (() -> Unit)? = null
    @Volatile var sos: (() -> Unit)? = null
    @Volatile var reply: ((String, String) -> Unit)? = null
}
