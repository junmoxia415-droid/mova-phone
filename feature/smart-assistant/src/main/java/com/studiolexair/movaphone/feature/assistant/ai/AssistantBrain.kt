package com.studiolexair.movaphone.feature.assistant.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.studiolexair.movaphone.core.logging.MovaLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * «Cerebro» del asistente: convierte lo que dice el usuario en una orden entendible.
 *
 * Hay dos implementaciones:
 *  - [RuleAssistantBrain]: intérprete de reglas local, siempre disponible y sin coste.
 *  - [LlmAssistantBrain]: modelo de lenguaje **en el propio teléfono** (MediaPipe LLM
 *    Inference + un modelo pequeño descargado por el usuario). Nada sale del dispositivo.
 *
 * Lo que devuelve el modelo se valida siempre con las reglas antes de hacer nada: si el
 * modelo se equivoca o responde algo raro, MOVA no ejecuta una acción rara, y cualquier
 * acción sensible sigue pidiendo confirmación al usuario.
 */
interface AssistantBrain {
    val id: String
    suspend fun understand(input: String, contactNames: List<String> = emptyList()): BrainResult
}

data class BrainResult(
    /** Orden en texto sencillo que entiende el intérprete de reglas ("llamar a Ana"). */
    val normalizedCommand: String?,
    val explanation: String?,
    val source: String
)

/** Intérprete de reglas: la base de MOVA, sin modelo y sin conexión. */
class RuleAssistantBrain : AssistantBrain {
    override val id: String = "reglas"

    override suspend fun understand(input: String, contactNames: List<String>): BrainResult =
        BrainResult(normalizedCommand = input, explanation = null, source = id)
}

/**
 * Modelo local con MediaPipe. Se le pide **una sola cosa**: devolver la orden de MOVA en una
 * línea. El modelo no inventa acciones nuevas: sólo reescribe lo que ha dicho el usuario con
 * las palabras que MOVA entiende.
 */
class LlmAssistantBrain(
    private val context: Context,
    private val modelPath: String
) : AssistantBrain {

    override val id: String = "modelo-local"

    private var inference: LlmInference? = null

    private fun engine(): LlmInference? {
        inference?.let { return it }
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                // En 0.10.27 topK/temperatura viven en la sesión; aquí sólo fijamos
                // el tokenizador de muestreo global para que la respuesta no divague.
                .setMaxTopK(40)
                .build()
            LlmInference.createFromOptions(context, options).also { inference = it }
        } catch (t: Throwable) {
            MovaLog.e(TAG, "No fue posible cargar el modelo local", t)
            null
        }
    }

    override suspend fun understand(input: String, contactNames: List<String>): BrainResult =
        withContext(Dispatchers.Default) {
            val model = engine()
                ?: return@withContext BrainResult(
                    normalizedCommand = null,
                    explanation = "El modelo local no pudo cargarse; se usa el intérprete de reglas.",
                    source = id
                )
            val answer = runCatching { model.generateResponse(buildPrompt(input, contactNames)) }
                .getOrElse { error ->
                    MovaLog.e(TAG, "Fallo del modelo local", error)
                    return@withContext BrainResult(null, "El modelo local falló; se usa el intérprete de reglas.", id)
                }
            BrainResult(
                normalizedCommand = parseAnswer(answer),
                explanation = "Interpretado por el modelo local del teléfono.",
                source = id
            )
        }

    fun close() {
        runCatching { inference?.close() }
        inference = null
    }

    private fun buildPrompt(input: String, contactNames: List<String>): String {
        val contacts = if (contactNames.isEmpty()) {
            "no hace falta: usa el nombre tal y como lo diga el usuario"
        } else {
            contactNames.joinToString(", ")
        }
        return """
            Eres el intérprete de órdenes de MOVA Phone (español de España).
            Órdenes válidas: llamar a <nombre>, enviar mensaje a <nombre>, compartir ubicación,
            abrir inicio, abrir marcador, abrir historial, abrir contactos, abrir mensajes,
            abrir seguridad, abrir ubicación, abrir automatizaciones, abrir ajustes, abrir sos,
            abrir conducción, emergencia.
            Nombres conocidos: $contacts
            Frase del usuario: "$input"
            Responde SOLO con la orden en una línea, sin explicaciones ni comillas.
        """.trimIndent()
    }

    /** El modelo puede añadir texto: se limpia y se elige la primera línea válida. */
    private fun parseAnswer(answer: String): String? {
        val line = answer.lineSequence()
            .map { it.trim().trim('"', '.', '-', '*', ' ') }
            .firstOrNull { it.isNotBlank() }
            ?: return null
        val cleaned = line.substringAfter("Orden:", line).trim().trim('"')
        return cleaned.takeIf { it.length in 3..120 }
    }

    private companion object {
        const val TAG = "LlmAssistantBrain"
        const val MAX_TOKENS = 128
    }
}
