package com.studiolexair.movaphone.data.messages.repository

import com.studiolexair.movaphone.core.database.dao.MessageDao
import com.studiolexair.movaphone.core.database.entity.SmsTemplateEntity
import com.studiolexair.movaphone.domain.emergency.usecase.RenderEmergencySmsUseCase
import kotlinx.coroutines.flow.Flow

/** Plantillas rápidas y de emergencia (requisito 14 y 21). */
class SmsTemplateRepositoryImpl(private val messageDao: MessageDao) {

    fun observeTemplates(): Flow<List<SmsTemplateEntity>> = messageDao.observeTemplates()

    suspend fun emergencyTemplate(): String =
        messageDao.templatesByCategory("emergency").firstOrNull()?.body
            ?: RenderEmergencySmsUseCase.DEFAULT_TEMPLATE

    suspend fun quickTemplates(): List<SmsTemplateEntity> = messageDao.templatesByCategory("quick")

    suspend fun saveTemplate(id: Long, title: String, body: String) {
        if (id == 0L) {
            messageDao.insertTemplate(
                SmsTemplateEntity(
                    title = title,
                    body = body,
                    category = if (title.lowercase().contains("sos")) "emergency" else "quick",
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            messageDao.updateTemplate(id, title, body)
        }
    }

    /** Crea las plantillas por defecto una única vez. */
    suspend fun ensureDefaults() {
        if (messageDao.observeTemplates().let { false }) return
        val existing = messageDao.templatesByCategory("quick")
        val hasEmergency = messageDao.templatesByCategory("emergency").isNotEmpty()
        if (!hasEmergency) {
            messageDao.insertTemplate(
                SmsTemplateEntity(
                    title = "Plantilla de emergencia (SOS)",
                    body = RenderEmergencySmsUseCase.DEFAULT_TEMPLATE,
                    category = "emergency",
                    isDefault = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        if (existing.isEmpty()) {
            listOf(
                "Llego bien" to "Llegué bien. Te aviso cuando salga.",
                "Estoy en camino" to "Voy en camino, llego en unos minutos.",
                "No puedo atender" to "No puedo atender ahora. Te llamo en cuanto pueda.",
                "Necesito que me llamen" to "¿Puedes llamarme cuando puedas? Es importante."
            ).forEach { (title, body) ->
                messageDao.insertTemplate(
                    SmsTemplateEntity(title = title, body = body, category = "quick", createdAt = System.currentTimeMillis())
                )
            }
        }
    }
}
