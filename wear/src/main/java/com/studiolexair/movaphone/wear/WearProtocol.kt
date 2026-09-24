package com.studiolexair.movaphone.wear

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Protocolo entre el reloj y el teléfono de MOVA.
 *
 * Es el mismo que implementa el teléfono: servicio Bluetooth propio (GATT) con dos canales,
 * uno de avisos (teléfono → reloj) y otro de órdenes (reloj → teléfono). Sin Google Play
 * Services y sin Internet: los datos van y vienen por Bluetooth, entre tus propios aparatos.
 */
object WearProtocol {

    val SERVICE_UUID: UUID = UUID.fromString("7a4d0f10-8f2b-4c6a-9c3e-1d2f5b7a9c01")
    val STATE_UUID: UUID = UUID.fromString("7a4d0f10-8f2b-4c6a-9c3e-1d2f5b7a9c02")
    val COMMAND_UUID: UUID = UUID.fromString("7a4d0f10-8f2b-4c6a-9c3e-1d2f5b7a9c03")
    val CLIENT_CONFIG_DESCRIPTOR: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val CMD_ANSWER = "answer"
    const val CMD_REJECT = "reject"
    const val CMD_HANGUP = "hangup"
    const val CMD_SOS = "sos"
    const val CMD_PING = "ping"
    const val CMD_REPLY = "reply"

    fun answer(): String = JSONObject().put("cmd", CMD_ANSWER).toString()
    fun reject(): String = JSONObject().put("cmd", CMD_REJECT).toString()
    fun hangUp(): String = JSONObject().put("cmd", CMD_HANGUP).toString()
    fun sos(): String = JSONObject().put("cmd", CMD_SOS).toString()
    fun ping(): String = JSONObject().put("cmd", CMD_PING).toString()
    fun reply(to: String, body: String): String = JSONObject()
        .put("cmd", CMD_REPLY)
        .put("to", to)
        .put("body", body)
        .toString()

    // ---------------- Fragmentación (MTU) ----------------

    /** MTU ATT por defecto: 23 bytes (20 útiles). */
    const val DEFAULT_MTU = 23

    /** Cada fragmento lleva delante [índice, total] en dos bytes. */
    const val FRAME_HEADER = 2

    fun chunkSize(mtu: Int): Int = (mtu - 3 - FRAME_HEADER).coerceAtLeast(1)

    /** Trocea un texto en fragmentos que quepan en el MTU acordado con el teléfono. */
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

    /** Reensambla lo que llega del teléfono; devuelve el texto cuando está completo. */
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

    /** Lee el estado que envía el teléfono. */
    fun parseState(raw: String): PhoneState? = try {
        val json = JSONObject(raw)
        val callJson = json.optJSONObject("call")
        val messages = json.optJSONArray("messages") ?: JSONArray()
        PhoneState(
            battery = json.optInt("battery", -1).takeIf { it >= 0 },
            call = callJson?.let {
                IncomingCall(
                    number = it.optString("number"),
                    name = it.optString("name"),
                    ringing = it.optBoolean("ringing", true)
                )
            },
            messages = (0 until messages.length()).mapNotNull { index ->
                messages.optJSONObject(index)?.let { item ->
                    LastMessage(
                        from = item.optString("from"),
                        name = item.optString("name"),
                        body = item.optString("body"),
                        at = item.optLong("at")
                    )
                }
            }
        )
    } catch (t: Throwable) {
        null
    }
}

data class PhoneState(
    val battery: Int? = null,
    val call: IncomingCall? = null,
    val messages: List<LastMessage> = emptyList()
)

data class IncomingCall(val number: String, val name: String, val ringing: Boolean)

data class LastMessage(val from: String, val name: String, val body: String, val at: Long)
