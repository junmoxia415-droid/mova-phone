package com.studiolexair.movaphone.services.location

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import com.studiolexair.movaphone.services.notifications.MovaNotificationChannels
import com.studiolexair.movaphone.services.notifications.MovaNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que registra la ubicación mientras hay una emergencia activa.
 * Se usa sólo durante el SOS (requisito 48: nada de servicios permanentes innecesarios)
 * y muestra una notificación visible para que el usuario sepa que está en marcha.
 */
class EmergencyLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startTracking()
            }
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        if (job != null) return
        val repository = LocationServiceDependencies.locationRepository ?: run {
            MovaLog.w(TAG, "Sin repositorio de ubicación: no se puede registrar el seguimiento")
            stopSelf()
            return
        }
        job = scope.launch {
            MovaLog.i(TAG, "Seguimiento de emergencia iniciado")
            while (isActive) {
                val result = repository.currentLocation()
                when (result) {
                    is com.studiolexair.movaphone.data.location.model.LocationResult.Available ->
                        repository.record(result.fix, source = "sos")
                    is com.studiolexair.movaphone.data.location.model.LocationResult.Unavailable ->
                        MovaLog.w(TAG, "Ubicación no disponible durante el seguimiento: ${result.reason}")
                }
                delay(TRACKING_INTERVAL_MS)
            }
        }
    }

    private fun stopTracking() {
        job?.cancel()
        job = null
        MovaLog.i(TAG, "Seguimiento de emergencia detenido")
    }

    private fun buildNotification(): Notification {
        val builder = Notification.Builder(this, MovaNotificationChannels.SOS)
            .setContentTitle("Emergencia activa")
            .setContentText("MOVA Phone está compartiendo tu ubicación con tus contactos de emergencia.")
            .setSmallIcon(com.studiolexair.movaphone.services.location.R.drawable.ic_notification_location)
            .setOngoing(true)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("Emergencia activa").setSmallIcon(com.studiolexair.movaphone.services.location.R.drawable.ic_notification_location).build()
        }
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.studiolexair.movaphone.action.START_EMERGENCY_LOCATION"
        const val ACTION_STOP = "com.studiolexair.movaphone.action.STOP_EMERGENCY_LOCATION"
        private const val NOTIFICATION_ID = 1002
        private const val TRACKING_INTERVAL_MS = 30_000L
        private const val TAG = "EmergencyLocationSvc"

        fun start(context: Context) {
            val intent = Intent(context, EmergencyLocationService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, EmergencyLocationService::class.java).setAction(ACTION_STOP))
        }
    }
}

object LocationServiceDependencies {
    @Volatile var locationRepository: LocationRepositoryImpl? = null
}
