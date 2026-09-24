package com.studiolexair.movaphone.feature.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Voz **en el dispositivo**: MOVA transcribe sin depender de Internet.
 *
 * Android 12 (API 31) incorpora el reconocimiento en el propio teléfono
 * (`createOnDeviceSpeechRecognizer`). En versiones anteriores se pide al reconocedor del
 * sistema que trabaje sin conexión (`EXTRA_PREFER_OFFLINE`). El audio no sale del teléfono:
 * la transcripción la hace el propio Android.
 *
 * Si el teléfono no tiene instalado el paquete de idioma, se avisa con claridad y se
 * explica cómo instalarlo (Ajustes → Sistema → Idiomas → Voz), o se usa el teclado.
 */
class OfflineVoiceInput(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var running = false

    /** ¿Este teléfono puede transcribir sin conexión? */
    fun isOnDeviceAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    /** ¿Hay algún reconocedor disponible (aunque sea el clásico del sistema)? */
    fun isAnyRecognizerAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun isListening(): Boolean = running

    fun start(
        languageTag: String = "es-ES",
        onPartial: (String) -> Unit = {},
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        val instance = createRecognizer()
        if (instance == null) {
            onError("Este teléfono no tiene reconocimiento de voz. Puedes escribir la orden o instalar el paquete de voz del sistema.")
            return
        }
        recognizer = instance
        instance.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(onPartial)
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                running = false
                if (text.isBlank()) {
                    onError("No se entendió nada. Prueba otra vez, más cerca del micrófono.")
                } else {
                    onResult(text)
                }
                stop()
            }

            override fun onError(error: Int) {
                running = false
                onError(describe(error))
                stop()
            }
        })

        running = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            // La clave del modo sin conexión: el reconocedor no debe tirar de Internet.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        runCatching { instance.startListening(intent) }
            .onFailure {
                running = false
                onError("No se pudo iniciar el micrófono: ${it.message}")
            }
    }

    fun stop() {
        recognizer?.let { instance ->
            runCatching { instance.stopListening() }
            runCatching { instance.cancel() }
            runCatching { instance.destroy() }
        }
        recognizer = null
        running = false
    }

    private fun createRecognizer(): SpeechRecognizer? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            // Reconocimiento en el propio teléfono: funciona sin datos móviles ni Wi-Fi.
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context).also {
                MovaLog.i(TAG, "Reconocedor del sistema en modo sin conexión preferente")
            }
        }
    } catch (t: Throwable) {
        MovaLog.e(TAG, "No fue posible crear el reconocedor de voz", t)
        null
    }

    private fun describe(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "No te entendí. Inténtalo otra vez."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No oí nada. Pulsa de nuevo y habla."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso de micrófono."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Tu teléfono no tiene instalado el idioma para reconocer sin conexión. " +
                "Instálalo en Ajustes → Sistema → Idiomas → Voz, o escribe la orden."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado. Espera un segundo."
        SpeechRecognizer.ERROR_AUDIO -> "Fallo al leer el micrófono."
        SpeechRecognizer.ERROR_SERVER -> "El reconocedor sin conexión no respondió. Vuelve a intentarlo."
        else -> "No pude usar la voz (código $error). Puedes escribir la orden."
    }

    private companion object {
        const val TAG = "OfflineVoiceInput"
    }
}
