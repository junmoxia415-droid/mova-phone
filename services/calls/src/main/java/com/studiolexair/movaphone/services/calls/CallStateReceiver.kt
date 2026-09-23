package com.studiolexair.movaphone.services.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.CallRecordEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Detecta el estado de las llamadas para:
 *  - registrar llamadas en el historial propio de MOVA,
 *  - disparar automatizaciones del tipo "entra una llamada".
 *
 * Nota técnica importante: desde Android 9/10 el número entrante ya no llega a los
 * receptores de PHONE_STATE. Cuando no está disponible se registra la llamada sin número
 * y se documenta; no se inventan datos.
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val deps = CallServiceDependencies
        val scope = CoroutineScope(Dispatchers.IO)
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                MovaLog.i(TAG, "Llamada entrante detectada")
                scope.launch {
                    deps.onIncomingCall?.invoke(number)
                    deps.automationEngine?.onTrigger(
                        TriggerType.CALL_INCOMING,
                        TriggerPayload(number = number, contactName = deps.contactNameResolver?.invoke(number))
                    )
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (number.isNullOrBlank()) return
                scope.launch {
                    deps.callRecordDao?.insert(
                        CallRecordEntity(
                            number = number,
                            normalizedNumber = PhoneNumbers.normalize(number),
                            contactName = deps.contactNameResolver?.invoke(number),
                            type = "OUTGOING",
                            startedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> Unit
        }
    }

    private companion object {
        const val TAG = "CallStateReceiver"
    }
}
