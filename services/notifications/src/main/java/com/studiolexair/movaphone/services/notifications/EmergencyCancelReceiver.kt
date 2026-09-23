package com.studiolexair.movaphone.services.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.emergency.repository.SosOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Permite cancelar la emergencia desde la notificación del SOS
 * (el usuario siempre conserva el control, requisito 12).
 */
class EmergencyCancelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MovaNotifications.ACTION_CANCEL_EMERGENCY) return
        val orchestrator = EmergencyServiceDependencies.sosOrchestrator ?: run {
            MovaLog.w(TAG, "Orquestador SOS no disponible para cancelar")
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                orchestrator.cancel()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "EmergencyCancel"
    }
}

/** Puente de dependencias para componentes creados por el sistema. */
object EmergencyServiceDependencies {
    @Volatile var sosOrchestrator: SosOrchestrator? = null
}
