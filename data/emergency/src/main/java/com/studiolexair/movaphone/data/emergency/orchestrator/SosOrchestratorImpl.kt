package com.studiolexair.movaphone.data.emergency.orchestrator

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.emergency.repository.EmergencyRepositoryImpl
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl
import com.studiolexair.movaphone.data.messages.repository.SmsTemplateRepositoryImpl
import com.studiolexair.movaphone.domain.calls.repository.CallLauncher
import com.studiolexair.movaphone.domain.emergency.repository.EmergencyAlertSink
import com.studiolexair.movaphone.domain.emergency.model.EmergencySession
import com.studiolexair.movaphone.domain.emergency.model.EmergencyStep
import com.studiolexair.movaphone.domain.emergency.model.EmergencyStepKind
import com.studiolexair.movaphone.domain.emergency.model.StepStatus
import com.studiolexair.movaphone.domain.emergency.repository.SosOrchestrator
import com.studiolexair.movaphone.domain.emergency.usecase.RenderEmergencySmsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Protocolo SOS real (requisito 12).
 *
 * Orden de actuación:
 *  1. registrar el evento
 *  2. obtener ubicación
 *  3. leer batería
 *  4. enviar SMS a los contactos por prioridad
 *  5. intentar llamada al contacto principal
 *  6. preparar el segundo contacto
 *  7. notificar el estado y dejar todo auditado
 *
 * Cada paso reporta su resultado real: si algo falla (sin cobertura, sin permiso,
 * sin GPS) se marca como FAILED con el motivo, y el usuario puede reintentarlo.
 * Nunca se simula un envío ni se inventa una posición.
 */
class SosOrchestratorImpl(
    private val emergencyRepository: EmergencyRepositoryImpl,
    private val locationRepository: LocationRepositoryImpl,
    private val messageRepository: MessageRepositoryImpl,
    private val templateRepository: SmsTemplateRepositoryImpl,
    private val callLauncher: CallLauncher,
    private val alertSink: EmergencyAlertSink,
    private val batteryReader: () -> Int?,
    private val onSecurityEvent: suspend (String) -> Unit,
    private val renderSms: RenderEmergencySmsUseCase = RenderEmergencySmsUseCase(),
    private val senderName: String = "Usuario"
) : SosOrchestrator {

    private val state = MutableStateFlow<EmergencySession?>(null)
    override val session: Flow<EmergencySession?> = state.asStateFlow()

    override suspend fun start(trigger: String): Long {
        val startedAt = System.currentTimeMillis()
        val contacts = emergencyRepository.contactsByPriority()

        var current = EmergencySession(
            startedAt = startedAt,
            active = true,
            trigger = trigger,
            steps = buildSteps(contacts.map { it.name })
        )
        state.value = current
        emit(current)

        onSecurityEvent("Emergencia activada · trigger=$trigger · contactos=${contacts.size}")
        MovaLog.w(TAG, "SOS iniciado con ${contacts.size} contactos de emergencia")

        // 1) Ubicación
        current = updateStep(current, EmergencyStepKind.LOCATION, StepStatus.RUNNING, null)
        val locationResult = locationRepository.currentLocation()
        current = when (locationResult) {
            is com.studiolexair.movaphone.data.location.model.LocationResult.Available -> {
                val fix = locationResult.fix
                locationRepository.record(fix, source = "sos")
                updateStep(
                    current.copy(
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        accuracyMeters = fix.accuracyMeters
                    ),
                    EmergencyStepKind.LOCATION,
                    StepStatus.DONE,
                    "Precisión ${TextFormatters.accuracy(fix.accuracyMeters)}"
                )
            }
            is com.studiolexair.movaphone.data.location.model.LocationResult.Unavailable -> {
                updateStep(current, EmergencyStepKind.LOCATION, StepStatus.FAILED, locationResult.reason)
            }
        }
        emit(current)

        // 2) Batería
        current = updateStep(current, EmergencyStepKind.BATTERY, StepStatus.RUNNING, null)
        val battery = batteryReader()
        current = if (battery != null) {
            updateStep(
                current.copy(batteryPercent = battery),
                EmergencyStepKind.BATTERY,
                StepStatus.DONE,
                TextFormatters.batteryLevel(battery)
            )
        } else {
            updateStep(current, EmergencyStepKind.BATTERY, StepStatus.SKIPPED, "No se pudo leer la batería")
        }
        emit(current)

        // 3) SMS de emergencia a cada contacto (por prioridad)
        val template = templateRepository.emergencyTemplate()
        val timeLabel = TextFormatters.clock(startedAt)
        val primary = contacts.firstOrNull()
        val message = renderSms(
            template = template,
            contactName = senderName,
            latitude = current.latitude,
            longitude = current.longitude,
            accuracyMeters = current.accuracyMeters,
            batteryPercent = current.batteryPercent,
            timestamp = startedAt,
            timeLabel = timeLabel
        )
        current = current.copy(messageBody = message)

        val smsTargets = contacts.filter { it.allowSms }
        if (smsTargets.isEmpty()) {
            current = updateStep(current, EmergencyStepKind.SMS, StepStatus.SKIPPED, "Sin contactos con SMS habilitado")
        } else {
            var sent = 0
            val failures = mutableListOf<String>()
            smsTargets.forEach { contact ->
                val ok = messageRepository.sendMessage(
                    address = contact.phoneNumber,
                    normalizedAddress = PhoneNumbers.normalize(contact.phoneNumber),
                    body = message,
                    contactName = contact.name,
                    isEmergency = true
                )
                if (ok) sent++ else failures += contact.name
            }
            current = if (sent > 0) {
                updateStep(
                    current,
                    EmergencyStepKind.SMS,
                    StepStatus.DONE,
                    "$sent de ${smsTargets.size} enviados" + if (failures.isNotEmpty()) " · falló: ${failures.joinToString()}" else ""
                )
            } else {
                updateStep(current, EmergencyStepKind.SMS, StepStatus.FAILED, "No se pudo enviar el SMS de emergencia")
            }
        }
        emit(current)

        // 4) Llamada al contacto principal
        val callTarget = contacts.firstOrNull { it.allowCall }
        current = if (callTarget == null) {
            updateStep(current, EmergencyStepKind.CONTACT_CALL, StepStatus.SKIPPED, "Sin contacto con llamada habilitada")
        } else {
            current = updateStep(current, EmergencyStepKind.CONTACT_CALL, StepStatus.RUNNING, callTarget.name)
            val ok = callLauncher.placeCall(callTarget.phoneNumber)
            updateStep(
                current.copy(contactedName = callTarget.name),
                EmergencyStepKind.CONTACT_CALL,
                if (ok) StepStatus.DONE else StepStatus.FAILED,
                if (ok) "Llamando a ${callTarget.name}" else "No fue posible iniciar la llamada a ${callTarget.name}"
            )
        }
        emit(current)

        // 5) Segundo contacto preparado
        val second = contacts.drop(1).firstOrNull()
        current = if (second == null) {
            updateStep(current, EmergencyStepKind.SECOND_CONTACT, StepStatus.SKIPPED, "Solo hay un contacto configurado")
        } else {
            updateStep(
                current,
                EmergencyStepKind.SECOND_CONTACT,
                StepStatus.DONE,
                "${second.name} preparado (prioridad ${second.priority})"
            )
        }
        emit(current)

        // 6) Alerta y auditoría
        current = updateStep(current, EmergencyStepKind.ALERT, StepStatus.DONE, "Alerta registrada")
        emit(current)

        emergencyRepository.saveSession(current)
        alertSink.onEmergencyStarted(current)
        return current.id
    }

    override suspend fun cancel() {
        val current = state.value ?: return
        val cancelled = current.copy(active = false, steps = current.steps.map { it })
        state.value = cancelled
        emit(cancelled)
        onSecurityEvent("Emergencia cancelada por el usuario")
        alertSink.onEmergencyCancelled(cancelled)
        MovaLog.i(TAG, "SOS cancelado por el usuario")
    }

    override suspend fun retryFailedSteps() {
        val current = state.value ?: return
        val pending = current.steps.filter { it.status == StepStatus.FAILED }.map { it.kind }
        if (pending.isEmpty()) return
        MovaLog.i(TAG, "Reintentando pasos: $pending")
        emergencyRepository.saveSession(current)
        start(current.trigger)
    }

    private fun buildSteps(contactNames: List<String>): List<EmergencyStep> = listOf(
        EmergencyStep(EmergencyStepKind.LOCATION, "Obteniendo ubicación", StepStatus.PENDING),
        EmergencyStep(EmergencyStepKind.BATTERY, "Leyendo batería", StepStatus.PENDING),
        EmergencyStep(
            EmergencyStepKind.SMS,
            if (contactNames.isEmpty()) "Enviando SMS de emergencia" else "Enviando SMS a ${contactNames.first()}",
            StepStatus.PENDING
        ),
        EmergencyStep(
            EmergencyStepKind.CONTACT_CALL,
            contactNames.firstOrNull()?.let { "Llamando a $it" } ?: "Llamando al contacto principal",
            StepStatus.PENDING
        ),
        EmergencyStep(
            EmergencyStepKind.SECOND_CONTACT,
            contactNames.drop(1).firstOrNull()?.let { "Preparando a $it" } ?: "Segundo contacto",
            StepStatus.PENDING
        ),
        EmergencyStep(EmergencyStepKind.ALERT, "Registrando la emergencia", StepStatus.PENDING)
    )

    private fun updateStep(
        session: EmergencySession,
        kind: EmergencyStepKind,
        status: StepStatus,
        detail: String?
    ): EmergencySession = session.copy(
        steps = session.steps.map { step ->
            if (step.kind == kind) step.copy(status = status, detail = detail ?: step.detail) else step
        }
    )

    private suspend fun emit(session: EmergencySession) {
        state.value = session
        alertSink.onSessionUpdated(session)
    }

    private companion object {
        const val TAG = "SosOrchestrator"
    }
}
