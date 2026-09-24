package com.studiolexair.movaphone.services.wear

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Protocolo propio entre el teléfono y el reloj (Wear OS).
 *
 * MOVA no usa Google Play Services, así que la comunicación va por **Bluetooth de bajo
 * consumo** con un servicio GATT propio: el teléfono hace de servidor y el reloj de cliente.
 * Los mensajes son líneas JSON pequeñas (el reloj sólo necesita datos mínimos).
 */
object WearProtocol {

    /** Servicio GATT de MOVA en el teléfono. */
    val SERVICE_UUID: UUID = UUID.fromString("7a4d0f10-8f2b-4c6a-9c3e-1d2f5b7a9c01")

    /** Estado del teléfono → reloj (notificaciones). */
    val STATE_UUID: UUID = UUID.fromString("7a4d0f10-8f2b-4c6a-9c3e-1d2f5b7a9c02")

    /** Órdenes del reloj → teléfono (escritura). */
    val COMMAND_UUID: UUID = UUID.fromString("7a4d0f10-8f2b-4c6a-9c3e-1d2f5b7a9c03")

    val CLIENT_CONFIG_DESCRIPTOR: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Órdenes que el reloj puede enviar.
    const val CMD_ANSWER = "answer"
    const val CMD_REJECT = "reject"
    const val CMD_HANGUP = "hangup"
    const val CMD_SOS = "sos"
    const val CMD_PING = "ping"
    const val CMD_REPLY = "reply"

    // Tipos de mensaje que viajan del teléfono al reloj.
    const val TYPE_STATE = "state"
    const val TYPE_CALL = "call"
    const val TYPE_MESSAGE = "message"

    fun state(
        batteryPercent: Int?,
        call: JSONObject?,
        messages: List<JSONObject>
    ): String = JSONObject().apply {
        put("type", TYPE_STATE)
        put("battery", batteryPercent ?: -1)
        put("call", call ?: JSONObject.NULL)
        put("messages", JSONArray(messages))
        put("at", System.currentTimeMillis())
    }.toString()

    fun call(number: String, name: String?, ringing: Boolean): JSONObject = JSONObject().apply {
        put("type", TYPE_CALL)
        put("number", number)
        put("name", name ?: number)
        put("ringing", ringing)
    }

    fun message(from: String, name: String?, body: String, at: Long): JSONObject = JSONObject().apply {
        put("type", TYPE_MESSAGE)
        put("from", from)
        put("name", name ?: from)
        put("body", body.take(160))
        put("at", at)
    }

    // ---------------- Fragmentación (MTU) ----------------

    /** MTU ATT por defecto: 23 bytes, de los que 3 son de cabecera ATT. */
    const val DEFAULT_MTU = 23

    /** Cada fragmento lleva delante [índice, total] en dos bytes. */
    const val FRAME_HEADER = 2

    /** Bytes útiles por fragmento según el MTU negociado. */
    fun chunkSize(mtu: Int): Int = (mtu - 3 - FRAME_HEADER).coerceAtLeast(1)

    /**
     * Trocea un texto en fragmentos que quepan en el MTU negociado.
     * Sin esto, una notificación larga (por ejemplo con nombre de contacto y mensajes) se
     * cortaría en el aire y el reloj no podría leerla.
     */
    fun frame(payload: String, mtu: Int): List<ByteArray> {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val size = chunkSize(mtu)
        val total = ((bytes.size + size - 1) / size).coerceAtLeast(1)
        return (0 until total).map { index ->
            val from = minOf(index * size, bytes.size)
            val to = minOf(bytes.size, from + size)
            val slice = bytes.copyOfRange(from, to)
            ByteArray(FRAME_HEADER + slice.size).also { frame ->
                frame[0] = index.toByte()
                frame[1] = total.toByte()
                slice.copyInto(frame, FRAME_HEADER)
            }
        }
    }

    /**
     * Reensambla los fragmentos recibidos. Devuelve el texto cuando ya está completo.
     * Si lo que llega no lleva cabecera de fragmento válida, se devuelve tal cual
     * (compatibilidad con un emisor que mande el texto entero).
     */
    class Reassembler {
        private val buffer = java.io.ByteArrayOutputStream()
        private var expected = 0

        @Synchronized
        fun accept(frame: ByteArray?): String? {
            if (frame == null || frame.isEmpty()) return null
            if (frame.size < FRAME_HEADER) return frame.toString(Charsets.UTF_8)
            val index = frame[0].toInt() and 0xFF
            val total = frame[1].toInt() and 0xFF
            if (total <= 0 || index >= total) return frame.toString(Charsets.UTF_8)
            if (index == 0) {
                buffer.reset()
                expected = total
            }
            if (total != expected) return null
            buffer.write(frame, FRAME_HEADER, frame.size - FRAME_HEADER)
            if (index != total - 1) return null
            val text = buffer.toByteArray().toString(Charsets.UTF_8)
            buffer.reset()
            expected = 0
            return text
        }
    }

    /** Lee una orden del reloj. Devuelve null si no es válida. */
    fun parseCommand(raw: String): WearCommand? = try {
        val json = JSONObject(raw)
        val cmd = json.optString("cmd")
        if (cmd.isBlank()) null else WearCommand(
            command = cmd,
            to = json.optString("to").takeIf { it.isNotBlank() },
            body = json.optString("body").takeIf { it.isNotBlank() }
        )
    } catch (t: Throwable) {
        null
    }
}

data class WearCommand(val command: String, val to: String?, val body: String?)
