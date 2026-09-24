package com.studiolexair.movaphone.data.automation.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.automation.geofence.GeofenceServiceDependencies

/**
 * Tarea periódica del sistema que revisa los lugares aunque MOVA esté cerrada.
 *
 * Android limita estas tareas a una cada 15 minutos como mínimo; MOVA comprueba además en
 * cada posición nueva, así que en la práctica el aviso llega con el margen suficiente para
 * automatismos del día a día (llegar a casa, salir del trabajo…).
 */
class GeofenceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val monitor = GeofenceServiceDependencies.monitor
            ?: return Result.success() // la app aún no ha preparado el motor: nada que hacer
        return try {
            monitor.check()
            Result.success()
        } catch (t: Throwable) {
            MovaLog.e(TAG, "Fallo comprobando los lugares guardados", t)
            Result.retry()
        }
    }

    companion object {
        const val NAME = "mova_geofence_check"
        private const val TAG = "GeofenceCheckWorker"

        /** Revisión periódica: 15 minutos es el mínimo que permite Android. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GeofenceCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            MovaLog.i(TAG, "Comprobación de lugares programada cada 15 minutos")
        }

        /** Comprobación inmediata (botón «Comprobar ahora» o al recibir una posición). */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<GeofenceCheckWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(NAME + "_now", ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
            WorkManager.getInstance(context).cancelUniqueWork(NAME + "_now")
        }
    }
}
