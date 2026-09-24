package com.studiolexair.movaphone.wear

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cliente Bluetooth del reloj: busca el teléfono con MOVA, se conecta a su servicio propio
 * y mantiene el estado sincronizado (llamada, mensajes, batería del teléfono).
 *
 * No usa Google Play Services ni la nube: si no encuentra el teléfono, lo dice con claridad
 * en pantalla en lugar de fingir que está conectado.
 */
class WearBridgeClient(private val context: Context) {

    data class WatchState(
        val supported: Boolean = true,
        val scanning: Boolean = false,
        val connected: Boolean = false,
        val detail: String = "Buscando tu teléfono con MOVA…",
        val phone: PhoneState = PhoneState(),
        val lastSent: String? = null
    )

    private val _state = MutableStateFlow(WatchState())
    val state: StateFlow<WatchState> = _state.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var stateCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null

    /** MTU acordado con el teléfono (por defecto, el mínimo de Bluetooth de bajo consumo). */
    private var mtu: Int = WearProtocol.DEFAULT_MTU

    /** Cola de escritura: Bluetooth sólo admite una escritura en curso a la vez. */
    private val pendingWrites = ArrayDeque<ByteArray>()
    private var writeInFlight = false

    /** Reensambla los avisos que llegan troceados desde el teléfono. */
    private val assembler = WearProtocol.Reassembler()

    fun hasPermissions(): Boolean {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return needed.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _state.value = _state.value.copy(
                scanning = false,
                connected = false,
                detail = "Activa el Bluetooth del reloj para conectar con el teléfono"
            )
            return
        }
        if (!hasPermissions()) {
            _state.value = _state.value.copy(
                scanning = false,
                connected = false,
                detail = "Falta el permiso de Bluetooth en el reloj"
            )
            return
        }
        if (_state.value.connected || _state.value.scanning) return

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _state.value = _state.value.copy(detail = "Este reloj no puede buscar por Bluetooth")
            return
        }
        _state.value = _state.value.copy(scanning = true, detail = "Buscando MOVA en tu teléfono…")

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(WearProtocol.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { scanner.startScan(listOf(filter), settings, scanCallback) }
            .onFailure {
                _state.value = _state.value.copy(scanning = false, detail = "No se pudo iniciar la búsqueda: ${it.message}")
            }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        runCatching { gatt?.close() }
        gatt = null
        stateCharacteristic = null
        commandCharacteristic = null
        mtu = WearProtocol.DEFAULT_MTU
        pendingWrites.clear()
        writeInFlight = false
        _state.value = _state.value.copy(scanning = false, connected = false, detail = "Puente detenido")
    }

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            context.getSystemService(BluetoothManager::class.java)
                ?.adapter
                ?.bluetoothLeScanner
                ?.let { runCatching { it.stopScan(this) } }
            connect(device)
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = _state.value.copy(scanning = false, detail = "Error de búsqueda Bluetooth ($errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        _state.value = _state.value.copy(scanning = false, detail = "Conectando con ${device.address}…")
        gatt = device.connectGatt(context, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.value = _state.value.copy(detail = "Conectado. Preparando canales…")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _state.value = _state.value.copy(
                    connected = false,
                    detail = "Teléfono desconectado. Abre MOVA en el teléfono para reconectar."
                )
                runCatching { gatt.close() }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(WearProtocol.SERVICE_UUID)
            if (service == null) {
                _state.value = _state.value.copy(
                    connected = false,
                    detail = "El teléfono no tiene el puente de MOVA activado"
                )
                return
            }
            val stateChar = service.getCharacteristic(WearProtocol.STATE_UUID)
            val commandChar = service.getCharacteristic(WearProtocol.COMMAND_UUID)
            if (stateChar == null || commandChar == null) {
                _state.value = _state.value.copy(connected = false, detail = "Canales de MOVA incompletos en el teléfono")
                return
            }
            stateCharacteristic = stateChar
            commandCharacteristic = commandChar
            runCatching { gatt.setCharacteristicNotification(stateChar, true) }
            val descriptor = stateChar.getDescriptor(WearProtocol.CLIENT_CONFIG_DESCRIPTOR)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                runCatching { gatt.writeDescriptor(descriptor) }
            }
            _state.value = _state.value.copy(connected = true, detail = "Conectado a MOVA Phone")
            // Pedimos un paquete grande: así el estado del teléfono llega en un par de envíos.
            runCatching { gatt.requestMtu(517) }
            sendCommand(WearProtocol.ping())
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@WearBridgeClient.mtu = mtu
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            writeInFlight = false
            pumpWrites()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handlePayload(value)
        }

        @Deprecated("Compatibilidad con Android anteriores a 13")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handlePayload(characteristic.value ?: return)
        }

        private fun handlePayload(value: ByteArray) {
            val raw = assembler.accept(value) ?: return
            val phone = WearProtocol.parseState(raw) ?: return
            _state.value = _state.value.copy(phone = phone, connected = true)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(payload: String) {
        if (gatt == null || commandCharacteristic == null) return
        WearProtocol.frame(payload, mtu).forEach { pendingWrites.addLast(it) }
        _state.value = _state.value.copy(lastSent = payload)
        pumpWrites()
    }

    /** Envía el siguiente fragmento pendiente, uno detrás de otro. */
    @SuppressLint("MissingPermission")
    private fun pumpWrites() {
        if (writeInFlight) return
        val gatt = gatt ?: return
        val characteristic = commandCharacteristic ?: return
        val frame = pendingWrites.removeFirstOrNull() ?: return
        writeInFlight = true
        val sent = runCatching { writeFrame(gatt, characteristic, frame) }.getOrDefault(false)
        if (!sent) {
            // Si la escritura no arrancó, no bloqueamos la cola: se reintenta con el siguiente.
            writeInFlight = false
            pendingWrites.clear()
        }
    }

    /**
     * Escribe un fragmento, con el método propio de cada versión de Android.
     * Los permisos de Bluetooth se piden antes de conectar y la llamada va dentro de
     * runCatching, así que aquí no puede escaparse un SecurityException.
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun writeFrame(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        frame: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Desde Android 13 el método devuelve un código de estado, no un booleano.
            return gatt.writeCharacteristic(
                characteristic,
                frame,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
        }
        characteristic.value = frame
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return gatt.writeCharacteristic(characteristic)
    }
}
