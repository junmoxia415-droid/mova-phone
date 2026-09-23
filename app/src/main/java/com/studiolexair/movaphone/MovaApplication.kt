package com.studiolexair.movaphone

import android.app.Application
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.di.MovaContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Punto de entrada de la aplicación.
 * Prepara la infraestructura común: contenedor de dependencias, canales de
 * notificación, trabajos periódicos y registro de eventos. La lógica de negocio
 * vive en los módulos, no aquí.
 */
class MovaApplication : Application() {

    /** Alcance de vida de la aplicación para tareas de fondo del contenedor. */
    private val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var container: MovaContainer
        private set

    override fun onCreate() {
        super.onCreate()
        MovaLog.initialize(isDebug = BuildConfig.DEBUG)
        MovaLog.i(TAG, "MOVA Phone iniciado · build ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

        container = MovaContainer(this, applicationScope)

        // Conecta servicios y receptores con las dependencias reales y arranca
        // las tareas periódicas (automatizaciones, historial de ubicación).
        container.wireSystemComponents()
        container.startBackgroundWork()

        MovaLog.i(TAG, "Infraestructura lista · SDK ${container.capabilities.sdkInt} · teléfono: ${container.capabilities.hasTelephony}")
    }

    private companion object {
        const val TAG = "MovaApplication"
    }
}
