package com.studiolexair.movaphone.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studiolexair.movaphone.MovaApplication
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.di.messagesFactory
import com.studiolexair.movaphone.feature.messages.ConversationRoute

/**
 * Pantalla de envío de SMS del sistema (`sms:`, `smsto:`, `mms:`, `mmsto:`).
 *
 * Cuando MOVA Phone es la aplicación de mensajes predeterminada, Android abre **esto** al
 * pulsar “enviar mensaje” desde cualquier otra app o desde un enlace: la conversación se
 * escribe y se envía con MOVA, sin saltar a la aplicación de mensajes del sistema.
 *
 * Vive en el módulo `:app` porque necesita el contenedor de dependencias de la aplicación
 * (los módulos de función no pueden depender de `:app`).
 */
class SendToActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MovaApplication).container

        val address = extractAddress(intent)
        val body = intent?.getStringExtra("sms_body")
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

        setContent {
            MovaTheme {
                ConversationRoute(
                    address = address,
                    viewModel = viewModel(factory = container.messagesFactory),
                    navigator = null,
                    contactName = container.contactNameCache.resolve(address),
                    prefill = body,
                    onClose = { finish() },
                    modifier = Modifier
                )
            }
        }
    }

    /** Extrae el destinatario de `smsto:+34600111222?body=...`. */
    private fun extractAddress(intent: Intent?): String {
        val data = intent?.data ?: return ""
        return data.schemeSpecificPart.orEmpty().substringBefore('?').trim()
    }
}
