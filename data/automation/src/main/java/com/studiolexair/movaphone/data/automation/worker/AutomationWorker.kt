package com.studiolexair.movaphone.data.automation.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Evaluación periódica de automatizaciones basadas en tiempo y batería.
 * Se usa WorkManager (requisito 48): sin servicios permanentes y con intervalos
 * razonables para no consumir batería.
 */
class AutomationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val engine = AutomationWorkerDependencies.engine ?: return Result.success()
        return try {
            val calendar = Calendar.getInstance()
            val payload = com.studiolexair.movaphone.domain.automation.repository.TriggerPayload(
                hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
                batteryPercent = AutomationWorkerDependencies.batteryReader?.invoke()
            )
            engine.onTrigger(TriggerType.TIME_OF_DAY, payload)
            engine.onTrigger(TriggerType.BATTERY_LOW, payload)
            Result.success()
        } catch (t: Throwable) {
            MovaLog.e(TAG, "Fallo evaluando automatizaciones periódicas", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AutomationWorker"
        private const val WORK_NAME = "mova_automation_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutomationWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            MovaLog.i(TAG, "Automatizaciones periódicas programadas cada 30 minutos")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

/**
 * Puente de dependencias entre el contenedor de la app y el Worker
 * (WorkManager instancia los Worker por reflexión: no admite inyección directa).
 */
object AutomationWorkerDependencies {
    @Volatile var engine: AutomationEngine? = null
    @Volatile var batteryReader: (() -> Int?)? = null
}
