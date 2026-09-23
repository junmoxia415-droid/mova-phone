package com.studiolexair.movaphone.data.messages.model

/**
 * Resumen de una persona que ha escrito al usuario.
 *
 * Alimenta la sección "Te han escrito" de Mensajes: muestra quién escribió, el último
 * mensaje, cuándo y cuántos mensajes siguen sin leer, y permite entrar directo al chat.
 */
data class SenderSummary(
    val address: String,
    val normalizedAddress: String,
    val displayName: String,
    val lastBody: String,
    val lastAt: Long,
    val totalMessages: Int,
    val unreadCount: Int
)
