package com.studiolexair.movaphone.domain.emergency.usecase

import com.studiolexair.movaphone.core.common.result.ErrorCode
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.core.common.result.failure
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.domain.emergency.model.EmergencyContact
import com.studiolexair.movaphone.domain.emergency.repository.EmergencyRepository

class GetEmergencyContactsUseCase(private val repository: EmergencyRepository) {
    operator fun invoke() = repository.observeContacts()
}

class SaveEmergencyContactUseCase(private val repository: EmergencyRepository) {
    suspend operator fun invoke(contact: EmergencyContact): MovaResult<Long> {
        if (contact.name.isBlank()) return failure(ErrorCode.VALIDATION, "El contacto necesita un nombre.")
        if (!PhoneNumbers.canBeDialed(contact.phoneNumber)) {
            return failure(ErrorCode.VALIDATION, "Introduce un número de teléfono válido.")
        }
        return try {
            val priority = if (contact.priority <= 0) repository.contactCount() + 1 else contact.priority
            val id = repository.saveContact(
                contact.copy(
                    priority = priority,
                    normalizedNumber = PhoneNumbers.normalize(contact.phoneNumber)
                )
            )
            MovaResult.Success(id)
        } catch (t: Throwable) {
            failure(ErrorCode.UNKNOWN, "No fue posible guardar el contacto de emergencia.", t.message)
        }
    }
}

class DeleteEmergencyContactUseCase(private val repository: EmergencyRepository) {
    suspend operator fun invoke(contact: EmergencyContact): MovaResult<Unit> = try {
        repository.deleteContact(contact)
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible eliminar el contacto.", t.message)
    }
}

/** Reordena prioridades respetando el orden indicado por el usuario (1 = primero). */
class ReorderEmergencyContactsUseCase(private val repository: EmergencyRepository) {
    suspend operator fun invoke(ordered: List<EmergencyContact>): MovaResult<Unit> = try {
        repository.reorder(ordered.mapIndexed { index, contact -> contact.copy(priority = index + 1) })
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible reordenar los contactos.", t.message)
    }
}

/** ¿Está configurado el protocolo mínimo de emergencia? */
class EmergencyReadinessUseCase(private val repository: EmergencyRepository) {
    suspend operator fun invoke(): Boolean = repository.contactCount() > 0
}
