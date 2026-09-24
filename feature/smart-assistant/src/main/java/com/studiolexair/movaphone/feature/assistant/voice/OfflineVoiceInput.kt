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
 * Voz de MOVA, con red de seguridad.
 *
 * Se intenta primero el reconocimiento **en el propio teléfono** (sin Internet). Si el
 * teléfono no tiene el paquete de idioma descargado, Android responde con los códigos 12
 * («idioma no disponible») o 13 («idioma no instalado») y mucha gente se queda sin voz.
 * Aquí, cuando eso pasa:
 *
 *  1. se explica **qué hacer** para instalarlo (ruta exacta en Ajustes),
 *  2. se **reintenta solo** con el reconocedor del sistema, para que el usuario pueda habar
 *     igualmente,
 *  3. y todo lo que venga después se corrige con [SpeechCorrector], así que un dictado
 *     imperfecto sigue sirviendo.
 *
 * Nunca se inventa una transcripción: si no se oye nada, se dice.
 */
class OfflineVoiceInput(private val context: Context) {

    /** Cómo se está escuchando en este momento. */
    enum class Mode { ON_DEVICE, SYSTEM }

    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var currentMode: Mode = Mode.SYSTEM
    private var retriedWithSystem = false

    /** Callback que se llama cuando cambia la forma de escuchar (por ejemplo, al caer al sistema). */
    var onModeChanged: ((Mode) -> Unit)? = null

    private var languageTag: String = "es-ES"
    private var onPartial: (String) -> Unit = {}
    private var onResult: (String) -> Unit = {}
    private var onError: (String) -> Unit = {}

    fun isOnDeviceAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun isAnyRecognizerAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun isListening(): Boolean = running

    /** Explicación útil de por qué no hay voz, con la ruta para arreglarlo. */
    fun missingLanguageAdvice(): String =
        "Tu teléfono no tiene descargado el idioma para dictado sin conexión. " +
            "Para tenerlo: Ajustes → Sistema → Idiomas e introducción de texto → Voz → " +
            "Salida de texto a voz / Reconocimiento de voz → descarga Español. " +
            "Mientras tanto MOVA usará el reconocedor del sistema (puede necesitar Internet la primera vez), " +
            "y siempre puedes escribir la orden en el chat."

    fun start(
        languageTag: String = "es-ES",
        preferOnDevice: Boolean = true,
        onPartial: (String) -> Unit = {},
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        this.languageTag = languageTag
        this.onPartial = onPartial
        this.onResult = onResult
        this.onError = onError
        retriedWithSystem = false

        val wantsOnDevice = preferOnDevice &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        launch(mode = if (wantsOnDevice) Mode.ON_DEVICE else Mode.SYSTEM)
    }

    private fun launch(mode: Mode, isRetry: Boolean = false) {
        val instance = createRecognizer(mode)
        if (instance == null) {
            onError(
                "Este teléfono no tiene reconocimiento de voz. Escribe la orden en el chat " +
                    "o instala el paquete de voz del sistema."
            )
            return
        }
        currentMode = mode
        onModeChanged?.invoke(mode)
        recognizer = instance

        instance.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                text?.let(onPartial)
            }

            override fun onResults(results: Bundle?) {
                val alternatives = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                    .filter { it.isNotBlank() }
                running = false
                if (alternatives.isEmpty()) {
                    // Se cortó sin oír nada: se reintenta el del sistema si aún no se ha probado.
                    if (!retriedWithSystem && currentMode == Mode.ON_DEVICE) {
                        retriedWithSystem = true
                        destroy()
                        launch(Mode.SYSTEM, isRetry = true)
                        return
                    }
                    onError("No se oyó nada. Acércate al micrófono y vuelve a intentarlo.")
                } else {
                    // Se devuelve la primera; el corrector de MOVA arregla el resto.
                    onResult(alternatives.first())
                }
                destroy()
            }

            override fun onError(error: Int) {
                running = false
                destroy()
                // 12 = idioma no disponible · 13 = idioma no instalado · 2 = sin red
                val languageProblem = error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                if (!retriedWithSystem && (languageProblem || currentMode == Mode.ON_DEVICE)) {
                    retriedWithSystem = true
                    if (languageProblem) {
                        MovaLog.w(TAG, "Sin idioma offline ($error): se prueba el reconocedor del sistema")
                    }
                    launch(Mode.SYSTEM, isRetry = true)
                    return
                }
                if (!retriedWithSystem && isRetry && error == SpeechRecognizer.ERROR_NETWORK) {
                    // El del sistema necesita Internet: se avisa sin tecnicismos.
                    onError(
                        "El reconocedor del teléfono necesita Internet para este idioma. " +
                            "Puedes escribir la orden en el chat: funciona igual."
                    )
                    return
                }
                onError(describe(error))
            }
        })

        running = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            // La clave del modo sin conexión: que no tire de Internet.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, currentMode == Mode.ON_DEVICE)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        runCatching { instance.startListening(intent) }
            .onFailure {
                running = false
                onError("No se pudo abrir el micrófono: ${it.message}")
                destroy()
            }
    }

    fun stop() {
        destroy()
        running = false
    }

    private fun destroy() {
        recognizer?.let { instance ->
            runCatching { instance.stopListening() }
            runCatching { instance.cancel() }
            runCatching { instance.destroy() }
        }
        recognizer = null
    }

    private fun createRecognizer(mode: Mode): SpeechRecognizer? = try {
        if (mode == Mode.ON_DEVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    } catch (t: Throwable) {
        MovaLog.e(TAG, "No se pudo crear el reconocedor (modo $mode)", t)
        null
    }

    /** Explicación en español de cada código de error del reconocedor. */
    private fun describe(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Se agotó el tiempo de espera de la red."
        SpeechRecognizer.ERROR_NETWORK -> "Sin conexión para reconocer la voz. Prueba a escribir."
        SpeechRecognizer.ERROR_AUDIO -> "Hubo un problema con el micrófono. Cierra otras apps que lo usen y reintenta."
        SpeechRecognizer.ERROR_SERVER -> "El servicio de voz del sistema falló. Reintenta en un momento."
        SpeechRecognizer.ERROR_CLIENT -> "El micrófono se interrumpió. Vuelve a pulsar Hablar."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se oyó nada. Acércate al micrófono y vuelve a intentarlo."
        SpeechRecognizer.ERROR_NO_MATCH -> "No entendí lo que dijiste. Repítelo más despacio."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado. Espera un segundo y reintenta."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso de micrófono. Concédelo en Ajustes → Permisos."
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Demasiados intentos seguidos. Espera unos segundos."
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Se perdió la conexión con el servicio de voz."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> missingLanguageAdvice()
        else -> "No se pudo escuchar (código $error). Puedes escribir la orden en el chat."
    }

    private companion object {
        const val TAG = "OfflineVoiceInput"
    }
}
