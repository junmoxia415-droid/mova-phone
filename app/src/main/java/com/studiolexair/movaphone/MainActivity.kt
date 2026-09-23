package com.studiolexair.movaphone

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.ui.MovaApp

/**
 * Única actividad de MOVA Phone: hospeda la interfaz Compose.
 * Se usa [FragmentActivity] porque BiometricPrompt necesita un anfitrión de fragmentos
 * para mostrar el diálogo biométrico del sistema.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MovaApplication).container

        setContent {
            MovaApp(container = container)
        }
    }

    override fun onResume() {
        super.onResume()
        MovaLog.d(TAG, "MainActivity en primer plano")
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
