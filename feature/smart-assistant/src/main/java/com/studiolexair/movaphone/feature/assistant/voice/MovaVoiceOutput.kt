package com.studiolexair.movaphone.feature.assistant.voice

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.studiolexair.movaphone.core.logging.MovaLog
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * La voz de MOVA: habla como una persona, no como un robot.
 *
 * Usa el motor de voz del propio teléfono (el mismo que lee los libros), en español y con una
 * entonación natural: velocidad y tono ligeramente humanos, frases cortas y sin leer símbolos
 * ni URLs en alto. Si el teléfono no tiene voz instalada, se dice con claridad y **no se falla**:
 * el chat sigue funcionando por escrito.
 *
 * Todo ocurre sin conexión y sin enviar nada a ningún sitio.
 */
class MovaVoiceOutput(private val context: Context) {

    private var engine: TextToSpeech? = null
    private val ready = AtomicBoolean(false)
    private var pending: String? = null

    /** ¿Este teléfono puede hablar? */
    var available: Boolean = true
        private set

    /** Motivo por el que no puede hablar (para explicarlo sin tecnicismos). */
    var unavailableReason: String? = null
        private set

    fun prepare(onReady: (Boolean) -> Unit = {}) {
        if (engine != null) {
            onReady(ready.get())
            return
        }
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configure()
                ready.set(true)
                onReady(true)
                pending?.let { text ->
                    pending = null
                    speakNow(text)
                }
            } else {
                available = false
                unavailableReason =
                    "Este teléfono no tiene instalada ninguna voz en español. Puedes instalarla en " +
                        "Ajustes → Sistema → Idiomas → Salida de texto a voz."
                MovaLog.w(TAG, "Motor de voz no disponible (estado $status)")
                onReady(false)
            }
        }
    }

    private fun configure() {
        val tts = engine ?: return
        val spanish = Locale("es", "ES")
        val result = runCatching { tts.setLanguage(spanish) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Se intenta el español genérico antes de rendirse.
            val generic = runCatching { tts.setLanguage(Locale("es")) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (generic == TextToSpeech.LANG_MISSING_DATA || generic == TextToSpeech.LANG_NOT_SUPPORTED) {
                available = false
                unavailableReason =
                    "Falta el paquete de voz en español. Se instala en Ajustes → Sistema → Idiomas → " +
                        "Salida de texto a voz → Instalar datos de voz."
                MovaLog.w(TAG, "El motor de voz no tiene español instalado")
                return
            }
        }
        // Entonación humana: algo más lenta que la máquina y con tono natural.
        tts.setSpeechRate(0.98f)
        tts.setPitch(1.02f)
        chooseNaturalVoice(tts)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit
            @Deprecated("Compatibilidad")
            override fun onError(utteranceId: String?) = Unit
            override fun onError(utteranceId: String?, errorCode: Int) {
                MovaLog.w(TAG, "Error de voz (código $errorCode)")
            }
        })
    }

    /** Se busca la voz en español más natural disponible (si el motor no ofrece ninguna, se usa la de siempre). */
    private fun chooseNaturalVoice(tts: TextToSpeech) {
        val voices = runCatching { tts.voices }.getOrNull() ?: return
        val candidates = voices.filter { voice ->
            voice.locale.language.equals("es", ignoreCase = true) &&
                voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) == false
        }
        val best: Voice? = candidates
            .sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.latency }
            )
            .firstOrNull()
        if (best != null) runCatching { tts.voice = best }
    }

    /** Dice [text] en voz alta. Si el motor aún no está listo, se guarda y se dice al estarlo. */
    fun speak(text: String) {
        val clean = forSpeaking(text)
        if (clean.isBlank()) return
        if (engine == null) {
            pending = clean
            prepare()
            return
        }
        if (!ready.get()) {
            pending = clean
            return
        }
        speakNow(clean)
    }

    private fun speakNow(text: String) {
        val tts = engine ?: return
        val id = "mova-${System.currentTimeMillis()}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } else {
            @Suppress("DEPRECATION")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun stop() {
        runCatching { engine?.stop() }
    }

    fun release() {
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
        ready.set(false)
    }

    /**
     * Prepara el texto para que suene humano: se quitan direcciones web, se cambian los
     * símbolos que se leerían fatal («+34» → «más tres cuatro») y se acortan los silencios.
     */
    private fun forSpeaking(raw: String): String = raw
        .replace(Regex("https?://\\S+"), "un enlace de ubicación en el mapa")
        .replace("→", ", ")
        .replace("«", "").replace("»", "")
        .replace("·", ",")
        .replace("*", "")
        .replace("  ", " ")
        .trim()

    private companion object {
        const val TAG = "MovaVoiceOutput"
    }
}
