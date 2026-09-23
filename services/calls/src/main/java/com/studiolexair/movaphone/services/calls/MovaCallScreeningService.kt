package com.studiolexair.movaphone.services.calls

import android.telecom.Call
import android.telecom.CallScreeningService
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.dao.CallRecordDao
import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.core.database.entity.CallRecordEntity
import com.studiolexair.movaphone.core.database.entity.SecurityEventEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.calls.repository.SpamClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bloqueo y filtrado de llamadas con la API oficial de Android (CallScreeningService).
 * Requiere que el usuario conceda el rol de filtrado de llamadas (ROLE_CALL_SCREENING);
 * si no se concede, MOVA Phone funciona igual pero sin bloquear en el sistema.
 *
 * Nunca se usan técnicas clandestinas para saltarse restricciones de Android.
 */
class MovaCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        val normalized = PhoneNumbers.normalize(number)
        val deps = CallServiceDependencies
        val securityDao = deps.securityDao
        val classifier = deps.spamClassifier

        if (normalized.isEmpty() || securityDao == null) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val blocked = securityDao.isBlocked(normalized) != null
                val verdict = classifier?.classify(normalized)
                val shouldReject = blocked || (deps.spamDetectionEnabled?.invoke() == true && verdict?.isSpam == true)

                if (shouldReject) {
                    MovaLog.i(TAG, "Llamada bloqueada por MOVA Phone")
                    deps.callRecordDao?.insert(
                        CallRecordEntity(
                            number = number.orEmpty(),
                            normalizedNumber = normalized,
                            type = "BLOCKED",
                            startedAt = System.currentTimeMillis(),
                            isSpam = verdict?.isSpam == true,
                            blockedByMova = true
                        )
                    )
                    securityDao.logEvent(
                        SecurityEventEntity(
                            type = "BLOCKED_CALL",
                            description = "Llamada bloqueada desde $number" + (verdict?.reason?.let { " · $it" } ?: ""),
                            severity = "warning",
                            occurredAt = System.currentTimeMillis()
                        )
                    )
                    val response = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(true)
                        .build()
                    respondToCall(callDetails, response)
                } else {
                    respondToCall(callDetails, CallResponse.Builder().build())
                }
            } catch (t: Throwable) {
                MovaLog.e(TAG, "Fallo en el filtrado de llamadas", t)
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private companion object {
        const val TAG = "CallScreening"
    }
}
