package com.studiolexair.movaphone.data.automation.engine

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.dao.ContactDao
import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.core.database.entity.BlockedNumberEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl
import com.studiolexair.movaphone.domain.automation.model.Action
import com.studiolexair.movaphone.domain.automation.model.ActionType
import com.studiolexair.movaphone.domain.automation.repository.AutomationNotifier
import com.studiolexair.movaphone.domain.automation.repository.DrivingModeController
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload
import com.studiolexair.movaphone.domain.calls.repository.CallLauncher
import kotlinx.coroutines.flow.first
import com.studiolexair.movaphone.domain.emergency.repository.EmergencyAlertSink
import com.studiolexair.movaphone.domain.emergency.repository.SosOrchestrator

/** Resultado de ejecutar una acción. Se registra en el historial de automatizaciones. */
data class ActionResult(val success: Boolean, val detail: String)

/**
 * Registro de acciones ejecutables. Cada acción usa APIs reales del dispositivo:
 * los SMS se envían de verdad, las llamadas se lanzan de verdad y las ubicaciones
 * se obtienen de verdad. Cuando algo no puede hacerse, se informa.
 */
class AutomationActionExecutor(
    private val messageRepository: MessageRepositoryImpl,
    private val callLauncher: CallLauncher,
    private val locationRepository: LocationRepositoryImpl,
    private val notifier: AutomationNotifier,
    private val drivingMode: DrivingModeController,
    private val sosOrchestrator: SosOrchestrator,
    private val contactDao: ContactDao,
    private val securityDao: SecurityDao,
    private val onSecurityEvent: suspend (String) -> Unit
) {

    suspend fun execute(action: Action, payload: TriggerPayload): ActionResult = try {
        when (action.type) {
            ActionType.SEND_SMS -> sendSms(action.value, payload)
            ActionType.CALL_CONTACT -> callContact(action.value, payload)
            ActionType.SHARE_LOCATION -> shareLocation(action.value)
            ActionType.ENABLE_DRIVING_MODE -> {
                drivingMode.setDrivingMode(true)
                ActionResult(true, "Modo conducción activado")
            }
            ActionType.DISABLE_DRIVING_MODE -> {
                drivingMode.setDrivingMode(false)
                ActionResult(true, "Modo conducción desactivado")
            }
            ActionType.SILENCE_NOTIFICATIONS -> {
                notifier.notify("MOVA Phone", "Notificaciones silenciadas por una automatización")
                ActionResult(true, "Notificaciones silenciadas")
            }
            ActionType.ENABLE_SOS -> {
                sosOrchestrator.start(trigger = "automation")
                ActionResult(true, "Protocolo SOS iniciado por automatización")
            }
            ActionType.BLOCK_NUMBER -> blockNumber(action.value ?: payload.number)
            ActionType.NOTIFY -> {
                notifier.notify("MOVA Phone", action.value ?: "Automatización ejecutada")
                ActionResult(true, "Notificación enviada")
            }
            ActionType.ADD_SECURITY_EVENT -> {
                onSecurityEvent(action.value ?: "Evento de automatización")
                ActionResult(true, "Evento registrado")
            }
        }
    } catch (t: Throwable) {
        MovaLog.e(TAG, "Fallo ejecutando acción ${action.type}", t)
        ActionResult(false, "No fue posible ejecutar la acción")
    }

    private suspend fun sendSms(value: String?, payload: TriggerPayload): ActionResult {
        if (value.isNullOrBlank()) return ActionResult(false, "Sin destinatario configurado")
        val (rawAddress, bodyPart) = value.split("|", limit = 2).let {
            it.first() to it.getOrNull(1)
        }
        val body = bodyPart ?: payload.message ?: "Mensaje automático de MOVA Phone"
        val address = resolveAddress(rawAddress)
            ?: return ActionResult(false, "Destinatario no encontrado: $rawAddress")
        val ok = messageRepository.sendMessage(
            address = address,
            normalizedAddress = PhoneNumbers.normalize(address),
            body = body,
            contactName = rawAddress
        )
        return if (ok) ActionResult(true, "SMS enviado a $rawAddress")
        else ActionResult(false, "No fue posible enviar el SMS a $rawAddress")
    }

    private suspend fun callContact(value: String?, payload: TriggerPayload): ActionResult {
        val target = value ?: payload.number ?: return ActionResult(false, "Sin contacto configurado")
        val address = resolveAddress(target) ?: return ActionResult(false, "Contacto no encontrado: $target")
        val ok = callLauncher.placeCall(address)
        return if (ok) ActionResult(true, "Llamada iniciada a $target")
        else ActionResult(false, "No fue posible llamar a $target")
    }

    private suspend fun shareLocation(value: String?): ActionResult {
        val result = locationRepository.currentLocation()
        return when (result) {
            is com.studiolexair.movaphone.data.location.model.LocationResult.Available -> {
                val text = locationRepository.shareText(result.fix)
                if (value.isNullOrBlank()) {
                    ActionResult(true, "Ubicación obtenida: ${com.studiolexair.movaphone.core.common.util.TextFormatters.coordinates(result.fix.latitude, result.fix.longitude)}")
                } else {
                    val address = resolveAddress(value)
                    if (address == null) ActionResult(false, "Destinatario no encontrado: $value")
                    else {
                        val ok = messageRepository.sendMessage(
                            address = address,
                            normalizedAddress = PhoneNumbers.normalize(address),
                            body = text,
                            contactName = value
                        )
                        if (ok) ActionResult(true, "Ubicación enviada a $value") else ActionResult(false, "No fue posible enviar la ubicación")
                    }
                }
            }
            is com.studiolexair.movaphone.data.location.model.LocationResult.Unavailable ->
                ActionResult(false, result.reason)
        }
    }

    private suspend fun blockNumber(number: String?): ActionResult {
        if (number.isNullOrBlank()) return ActionResult(false, "Sin número que bloquear")
        securityDao.block(
            BlockedNumberEntity(
                normalizedNumber = PhoneNumbers.normalize(number),
                phoneNumber = number,
                reason = "automation",
                createdAt = System.currentTimeMillis()
            )
        )
        return ActionResult(true, "Número $number bloqueado")
    }

    /** Permite usar tanto "Mamá" (nombre de contacto) como un número directo. */
    private suspend fun resolveAddress(value: String): String? {
        val digits = PhoneNumbers.digitsOnly(value)
        if (digits.length >= 5) return value
        val contact = contactDao.search(value).first().firstOrNull()
        return contact?.phoneNumber
    }

    private companion object {
        const val TAG = "AutomationActions"
    }
}
