package com.studiolexair.movaphone.feature.assistant.ai

import android.content.Context

/**
 * Elige el cerebro del asistente: modelo local si está descargado y activado, y si no,
 * el intérprete de reglas. El usuario nunca se queda sin asistente.
 */
object BrainFactory {

    fun create(context: Context, modelManager: LocalModelManager, useModel: Boolean): AssistantBrain {
        if (!useModel) return RuleAssistantBrain()
        val model = modelManager.downloadedModel() ?: return RuleAssistantBrain()
        return LlmAssistantBrain(context, modelManager.modelFile(model).absolutePath)
    }
}
