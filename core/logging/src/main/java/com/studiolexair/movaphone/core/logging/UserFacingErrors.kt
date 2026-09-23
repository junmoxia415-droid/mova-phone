package com.studiolexair.movaphone.core.logging

import java.io.IOException

/**
 * Traduce cualquier fallo técnico a un mensaje apto para el usuario final.
 * Requisito del proyecto: nunca mostrar NullPointerException, SQLiteException, etc.
 */
object UserFacingErrors {

    const val GENERIC = "No fue posible completar esta operación. Inténtalo nuevamente."
    const val NO_PERMISSION = "MOVA Phone necesita permiso para realizar esta acción. Puedes concederlo en Ajustes."
    const val NO_TELEPHONY = "Este dispositivo no admite llamadas telefónicas."
    const val NO_SMS = "Este dispositivo no puede enviar SMS."
    const val NO_LOCATION = "No se pudo obtener la ubicación. Verifica el GPS y los permisos."
    const val NO_CONNECTION = "Sin conexión a Internet. Algunas funciones no están disponibles."
    const val NOT_AVAILABLE = "Esta función no está disponible en este dispositivo o versión de Android."

    fun messageFor(throwable: Throwable?, fallback: String = GENERIC): String = when {
        throwable is SecurityException -> NO_PERMISSION
        throwable is IOException -> NO_CONNECTION
        throwable is UnsupportedOperationException -> NOT_AVAILABLE
        else -> fallback
    }
}
