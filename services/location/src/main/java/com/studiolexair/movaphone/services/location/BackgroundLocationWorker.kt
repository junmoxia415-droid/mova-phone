package com.studiolexair.movaphone.services.location

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studiolexair.movaphone.core.logging.MovaLog
import java.util.concurrent.TimeUnit

/**
 * Registro de ubicación en segundo plano (opcional y desactivado por defecto).
 * Usa WorkManager con intervalo amplio: 15 minutos como mínimo razonable
 * para históricos de ubicación sin comprometer la batería.
 */
class BackgroundLocationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1) Los lugares guardados se comprueban siempre que haya una posición:
        //    es lo que hace que «llegar a casa» o «salir del trabajo» se disparen de verdad.
        runCatching { com.studiolexair.movaphone.data.automation.geofence.GeofenceServiceDependencies.monitor?.check() }
            .onFailure { MovaLog.w(TAG, "No se pudieron comprobar los lugares en segundo plano") }

        val repository = LocationServiceDependencies.locationRepository ?: return Result.success()
        val enabled = LocationServiceDependencies.locationHistoryEnabled?.invoke() ?: false
        if (!enabled) {
            MovaLog.i(TAG, "Historial de ubicación desactivado: no se registra nada")
            return Result.success()
        }
        return when (val result = repository.currentLocation()) {
            is com.studiolexair.movaphone.data.location.model.LocationResult.Available -> {
                repository.record(result.fix, source = "background")
                Result.success()
            }
            is com.studiolexair.movaphone.data.location.model.LocationResult.Unavailable -> {
                MovaLog.w(TAG, "Sin ubicación: ${result.reason}")
                Result.success()
            }
        }
    }

    companion object {
        private const val TAG = "BgLocationWorker"
        private const val WORK_NAME = "mova_location_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackgroundLocationWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

/** Amplía el puente de dependencias con la preferencia del historial. */
val LocationServiceDependencies.locationHistoryEnabled: (() -> Boolean)?
    get() = LocationDepsHolder.locationHistoryEnabled

object LocationDepsHolder {
    @Volatile var locationHistoryEnabled: (() -> Boolean)? = null
}
