package com.studiolexair.movaphone.data.emergency.repository

import com.studiolexair.movaphone.core.database.dao.EmergencyContactDao
import com.studiolexair.movaphone.core.database.dao.LocationDao
import com.studiolexair.movaphone.data.emergency.mapper.EmergencyMapper
import com.studiolexair.movaphone.domain.emergency.model.EmergencyContact
import com.studiolexair.movaphone.domain.emergency.model.EmergencySession
import com.studiolexair.movaphone.domain.emergency.model.StepStatus
import com.studiolexair.movaphone.domain.emergency.repository.EmergencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistencia de contactos de emergencia y de las sesiones SOS.
 * La trazabilidad de una emergencia se guarda como punto de ubicación
 * (source = "sos") y como evento de seguridad, para poder auditarla después.
 */
class EmergencyRepositoryImpl(
    private val emergencyDao: EmergencyContactDao,
    private val locationDao: LocationDao
) : EmergencyRepository {

    override fun observeContacts(): Flow<List<EmergencyContact>> =
        emergencyDao.observeAll().map { list -> list.map(EmergencyMapper::toDomain) }

    override suspend fun contactsByPriority(): List<EmergencyContact> =
        emergencyDao.getAll().map(EmergencyMapper::toDomain)

    override suspend fun saveContact(contact: EmergencyContact): Long =
        emergencyDao.insert(EmergencyMapper.toEntity(contact))

    override suspend fun deleteContact(contact: EmergencyContact) =
        emergencyDao.delete(EmergencyMapper.toEntity(contact))

    override suspend fun reorder(ordered: List<EmergencyContact>) {
        ordered.forEach { contact ->
            emergencyDao.updatePriority(contact.id, contact.priority)
        }
    }

    override suspend fun contactCount(): Int = emergencyDao.count()

    override fun observeSessions(): Flow<List<EmergencySession>> = locationDao
        .observeBySource("sos", 50)
        .map { records ->
            records.map { record ->
                EmergencySession(
                    id = record.eventId ?: record.id,
                    startedAt = record.recordedAt,
                    active = false,
                    steps = listOf(
                        com.studiolexair.movaphone.domain.emergency.model.EmergencyStep(
                            kind = com.studiolexair.movaphone.domain.emergency.model.EmergencyStepKind.LOCATION,
                            label = "Ubicación registrada",
                            status = StepStatus.DONE
                        )
                    ),
                    latitude = record.latitude,
                    longitude = record.longitude,
                    accuracyMeters = record.accuracyMeters
                )
            }
        }

    override suspend fun lastSession(): EmergencySession? = locationDao.lastKnown()?.let { record ->
        EmergencySession(
            id = record.id,
            startedAt = record.recordedAt,
            active = false,
            latitude = record.latitude,
            longitude = record.longitude,
            accuracyMeters = record.accuracyMeters
        )
    }

    /** Las sesiones se persisten como puntos de ubicación con origen SOS. */
    override suspend fun saveSession(session: EmergencySession): Long = locationDao.insert(
        com.studiolexair.movaphone.core.database.entity.LocationRecordEntity(
            latitude = session.latitude ?: 0.0,
            longitude = session.longitude ?: 0.0,
            accuracyMeters = session.accuracyMeters ?: 0f,
            provider = "emergency",
            source = "sos",
            eventId = session.id,
            recordedAt = session.startedAt
        )
    )

    override suspend fun updateSession(session: EmergencySession) {
        // La sesión activa se mantiene en memoria (SosOrchestrator) y se persiste al final.
    }
}
