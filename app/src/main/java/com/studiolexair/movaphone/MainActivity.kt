package com.studiolexair.movaphone

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.ui.MovaApp
import com.studiolexair.movaphone.widget.WidgetDeepLink

/**
 * Única actividad de MOVA Phone: hospeda la interfaz Compose.
 * Se usa [FragmentActivity] porque BiometricPrompt necesita un anfitrión de fragmentos
 * para mostrar el diálogo biométrico del sistema.
 *
 * También recibe los toques de los widgets de la pantalla de inicio (llamar, escribir,
 * SOS) y los convierte en navegación real dentro de la app.
 */
class MainActivity : FragmentActivity() {

    private var deepLink by mutableStateOf<WidgetDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MovaApplication).container
        deepLink = WidgetDeepLink.from(intent)

        setContent {
            MovaApp(
                container = container,
                deepLink = deepLink,
                onDeepLinkHandled = { deepLink = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = WidgetDeepLink.from(intent)
    }

    override fun onResume() {
        super.onResume()
        MovaLog.d(TAG, "MainActivity en primer plano")
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
