package com.studiolexair.movaphone.core.database

import android.content.Context
import androidx.room.Room
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Construcción de la base de datos.
 * Se expone como fábrica (no singleton global) para que el contenedor de dependencias
 * de la aplicación controle su ciclo de vida y las pruebas puedan inyectar otra instancia.
 */
object DatabaseFactory {

    private const val TAG = "DatabaseFactory"

    fun create(context: Context): MovaDatabase {
        MovaLog.i(TAG, "Creando base de datos local v${MovaDatabase.VERSION}")
        return Room.databaseBuilder(
            context.applicationContext,
            MovaDatabase::class.java,
            MovaDatabase.NAME
        )
            // No se usa fallback destructivo: cada cambio de esquema requiere migración explícita
            // para no perder datos del usuario (requisito de calidad del proyecto).
            .build()
    }
}
