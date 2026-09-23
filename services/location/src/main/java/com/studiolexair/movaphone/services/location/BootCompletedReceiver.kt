package com.studiolexair.movaphone.services.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Reagenda el trabajo periódico tras reiniciar el dispositivo.
 * MOVA Phone no arranca servicios ocultos: sólo reprograma el trabajo de WorkManager
 * que el propio usuario haya habilitado.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        MovaLog.i(TAG, "Dispositivo reiniciado: reprogramando trabajo periódico")
        BackgroundLocationWorker.schedule(context)
        try {
            com.studiolexair.movaphone.data.automation.worker.AutomationWorker.schedule(context)
        } catch (t: Throwable) {
            MovaLog.w(TAG, "No se pudo reprogramar el motor de automatizaciones")
        }
    }

    private companion object {
        const val TAG = "BootCompleted"
    }
}
