package com.studiolexair.movaphone.core.permissions

import android.Manifest
import android.os.Build

/**
 * Permisos que usa MOVA Phone, con su explicación para el usuario.
 * Requisito 33: sólo se piden los permisos necesarios, en contexto y explicando por qué.
 */
enum class MovaPermission(
    val androidPermission: String,
    val title: String,
    val rationale: String,
    val minSdk: Int = 23
) {
    CALL_PHONE(
        Manifest.permission.CALL_PHONE,
        "Realizar llamadas",
        "Para que MOVA Phone pueda marcar y llamar a tus contactos o a emergencias."
    ),
    READ_CONTACTS(
        Manifest.permission.READ_CONTACTS,
        "Leer contactos",
        "Para mostrar e importar tu agenda. MOVA Phone sólo lee: no modifica tus contactos."
    ),
    CALL_LOG(
        Manifest.permission.READ_CALL_LOG,
        "Historial de llamadas",
        "Para mostrar tu historial real y detectar llamadas perdidas o sospechosas."
    ),
    SEND_SMS(
        Manifest.permission.SEND_SMS,
        "Enviar SMS",
        "Necesario para los mensajes rápidos y para enviar tu mensaje de emergencia."
    ),
    READ_SMS(
        Manifest.permission.READ_SMS,
        "Leer SMS",
        "Para mostrar tus conversaciones dentro de MOVA Phone."
    ),
    RECEIVE_SMS(
        Manifest.permission.RECEIVE_SMS,
        "Recibir SMS",
        "Para avisarte de mensajes nuevos y de respuestas a tus alertas."
    ),
    FINE_LOCATION(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "Ubicación precisa",
        "Para el protocolo SOS: tu ubicación sólo se envía a tus contactos de emergencia."
    ),
    COARSE_LOCATION(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        "Ubicación aproximada",
        "Permite compartir una ubicación estimada cuando no se necesita precisión."
    ),
    BACKGROUND_LOCATION(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        "Ubicación en segundo plano",
        "Opcional: sólo si quieres registrar ubicación durante una emergencia con la pantalla apagada.",
        minSdk = Build.VERSION_CODES.Q
    ),
    READ_PHONE_STATE(
        Manifest.permission.READ_PHONE_STATE,
        "Estado del teléfono",
        "Para saber cuándo entra una llamada y aplicar tus reglas de bloqueo y de emergencia."
    ),
    POST_NOTIFICATIONS(
        "android.permission.POST_NOTIFICATIONS",
        "Notificaciones",
        "Para avisarte de emergencias, automatizaciones y mensajes importantes.",
        minSdk = Build.VERSION_CODES.TIRAMISU
    ),
    RECORD_AUDIO(
        Manifest.permission.RECORD_AUDIO,
        "Micrófono",
        "Sólo para los comandos de voz del modo conducción y del asistente. Nada se graba en secreto."
    ),
    ANSWER_PHONE_CALLS(
        Manifest.permission.ANSWER_PHONE_CALLS,
        "Atender llamadas",
        "Para que puedas contestar desde el modo conducción con un solo toque.",
        minSdk = Build.VERSION_CODES.O
    )
}
