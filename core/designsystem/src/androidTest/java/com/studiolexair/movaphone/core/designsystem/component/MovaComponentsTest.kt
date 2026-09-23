package com.studiolexair.movaphone.core.designsystem.component

import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas de interfaz de los componentes base del Design System. */
@RunWith(AndroidJUnit4::class)
class MovaComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_muestraTituloYDescripcion() {
        composeRule.setContent {
            MovaTheme {
                MovaEmptyState(
                    title = "Sin contactos",
                    description = "Añade tu primer contacto",
                    icon = androidx.compose.material.icons.Icons.Filled.Phone
                )
            }
        }
        composeRule.onNodeWithText("Sin contactos").assertIsDisplayed()
        composeRule.onNodeWithText("Añade tu primer contacto").assertIsDisplayed()
    }

    @Test
    fun botonPrimario_invocaLaAccion() {
        var pulsado = false
        composeRule.setContent {
            MovaTheme {
                MovaPrimaryButton(text = "Llamar", onClick = { pulsado = true })
            }
        }
        composeRule.onNodeWithText("Llamar").performClick()
        assertThat(pulsado).isTrue()
    }

    @Test
    fun botonDeshabilitado_noInvocaLaAccion() {
        var pulsado = false
        composeRule.setContent {
            MovaTheme {
                MovaPrimaryButton(text = "Llamar", enabled = false, onClick = { pulsado = true })
            }
        }
        composeRule.onNodeWithText("Llamar").performClick()
        assertThat(pulsado).isFalse()
    }

    @Test
    fun infoBanner_muestraElMensaje() {
        composeRule.setContent {
            MovaTheme { MovaInfoBanner(message = "Permiso necesario para llamar") }
        }
        composeRule.onNodeWithText("Permiso necesario para llamar").assertIsDisplayed()
    }
}
