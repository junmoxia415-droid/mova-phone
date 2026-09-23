package com.studiolexair.movaphone.feature.messages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.studiolexair.movaphone.core.database.entity.MessageEntity

/**
 * Significado de cada estado de mensaje, tal y como se lo contamos al usuario.
 *
 * Es la base de la palomita de la conversación y de la ficha que aparece al tocar un
 * mensaje: [icon], [label] y [explanation] salen siempre del estado real guardado en Room,
 * nunca de un texto fijo.
 */
data class MessageStateUi(
    val key: String,
    val label: String,
    val explanation: String,
    val icon: ImageVector,
    val tint: Color
)

object MessageStates {

    const val SENDING = "SENDING"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val READ = "READ"
    const val FAILED = "FAILED"
    const val RECEIVED = "RECEIVED"

    /** Estado de un mensaje saliente o entrante con su icono (palomita) y color. */
    fun of(message: MessageEntity, accent: Color, successColor: Color, dangerColor: Color, muted: Color): MessageStateUi =
        of(message.state, message.isIncoming, accent, successColor, dangerColor, muted)

    fun of(
        state: String,
        isIncoming: Boolean,
        accent: Color,
        successColor: Color,
        dangerColor: Color,
        muted: Color
    ): MessageStateUi {
        if (isIncoming) {
            return MessageStateUi(
                key = RECEIVED,
                label = "Recibido",
                explanation = "Este mensaje lo has recibido tú. Se queda guardado en tu teléfono.",
                icon = Icons.Filled.Sms,
                tint = muted
            )
        }
        return when (state) {
            SENDING -> MessageStateUi(
                SENDING,
                "Enviando",
                "El mensaje está saliendo de tu teléfono ahora mismo.",
                Icons.Filled.Schedule,
                muted
            )
            SENT -> MessageStateUi(
                SENT,
                "Enviado",
                "El mensaje salió de tu teléfono correctamente. Una palomita.",
                Icons.Filled.Done,
                muted
            )
            DELIVERED -> MessageStateUi(
                DELIVERED,
                "Entregado",
                "El teléfono del destinatario ha confirmado que lo recibió. Dos palomitas.",
                Icons.Filled.DoneAll,
                accent
            )
            READ -> MessageStateUi(
                READ,
                "Leído",
                "El destinatario ha abierto la conversación y lo ha leído. Dos palomitas marcadas.",
                Icons.Filled.MarkChatRead,
                successColor
            )
            FAILED -> MessageStateUi(
                FAILED,
                "No enviado",
                "El mensaje no salió. Revisa la cobertura y vuelve a intentarlo con el botón Reintentar.",
                Icons.Filled.ErrorOutline,
                dangerColor
            )
            else -> MessageStateUi(
                SENT,
                "Enviado",
                "El mensaje salió de tu teléfono correctamente.",
                Icons.Filled.Done,
                muted
            )
        }
    }
}
