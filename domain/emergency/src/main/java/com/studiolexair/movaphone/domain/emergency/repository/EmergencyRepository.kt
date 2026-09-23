package com.studiolexair.movaphone.domain.emergency.repository

import com.studiolexair.movaphone.domain.emergency.model.EmergencyContact
import com.studiolexair.movaphone.domain.emergency.model.EmergencySession
import kotlinx.coroutines.flow.Flow

interface EmergencyRepository {
    fun observeContacts(): Flow<List<EmergencyContact>>
    suspend fun contactsByPriority(): List<EmergencyContact>
    suspend fun saveContact(contact: EmergencyContact): Long
    suspend fun deleteContact(contact: EmergencyContact)
    suspend fun reorder(ordered: List<EmergencyContact>)
    suspend fun contactCount(): Int

    fun observeSessions(): Flow<List<EmergencySession>>
    suspend fun lastSession(): EmergencySession?
    suspend fun saveSession(session: EmergencySession): Long
    suspend fun updateSession(session: EmergencySession)
}

/** Contrato del orquestador SOS: coordina pasos reales y reporta su estado. */
interface SosOrchestrator {
    val session: Flow<EmergencySession?>
    /** Inicia el protocolo. Devuelve el identificador de la sesión. */
    suspend fun start(trigger: String): Long
    /** Cancela la emergencia en curso (el usuario conserva el control). */
    suspend fun cancel()
    /** Reintenta los pasos que fallaron (por ejemplo, por falta de cobertura). */
    suspend fun retryFailedSteps()
}
