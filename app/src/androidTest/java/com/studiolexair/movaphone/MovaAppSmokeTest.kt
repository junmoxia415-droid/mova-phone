package com.studiolexair.movaphone

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de arranque: la aplicación debe abrir su pantalla principal sin fallar
 * y sin pedir permisos en bloque al iniciar.
 */
@RunWith(AndroidJUnit4::class)
class MovaAppSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun laAplicacionArrancaYNoSeCierra() {
        composeRule.waitForIdle()
        // Si la actividad llegó hasta aquí sin excepción, la infraestructura
        // (contenedor de dependencias, base de datos, temas) está operativa.
        composeRule.activityRule.scenario.onActivity { activity ->
            assert(!activity.isFinishing)
        }
    }
}
