package com.studiolexair.movaphone.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.studiolexair.movaphone.core.database.entity.BlockedNumberEntity
import com.studiolexair.movaphone.core.database.entity.ContactEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de integración de la base de datos local: se verifica que los DAO
 * guardan, consultan y borran de verdad (Room en memoria, sin tocar el dispositivo).
 */
@RunWith(AndroidJUnit4::class)
class MovaDatabaseTest {

    private lateinit var database: MovaDatabase
    private val now = System.currentTimeMillis()

    @Before
    fun crearBaseDeDatos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun cerrarBaseDeDatos() {
        database.close()
    }

    @Test
    fun guardaYRecuperaContactos() = runTest {
        val dao = database.contactDao()
        val id = dao.insert(
            ContactEntity(
                displayName = "Ana Pérez",
                phoneNumber = "600 11 22 33",
                normalizedNumber = "600112233",
                isFavorite = true,
                createdAt = now,
                updatedAt = now
            )
        )
        assertThat(id).isGreaterThan(0)

        val todos = dao.observeAll().first()
        assertThat(todos).hasSize(1)
        assertThat(todos.first().displayName).isEqualTo("Ana Pérez")
        assertThat(dao.observeFavorites().first()).hasSize(1)
        assertThat(dao.findByNormalizedNumber("600112233")?.displayName).isEqualTo("Ana Pérez")
    }

    @Test
    fun marcaYDesmarcaNumerosBloqueados() = runTest {
        val dao = database.securityDao()
        dao.block(BlockedNumberEntity(normalizedNumber = "600000000", phoneNumber = "600000000", label = "spam", createdAt = now))
        assertThat(dao.observeBlocked().first()).hasSize(1)
        assertThat(dao.isBlocked("600000000")).isNotNull()

        dao.unblock(dao.allBlocked().first())
        assertThat(dao.observeBlocked().first()).isEmpty()
    }

    @Test
    fun registraEventosDeSeguridad() = runTest {
        val dao = database.securityDao()
        dao.logEvent(
            com.studiolexair.movaphone.core.database.entity.SecurityEventEntity(
                type = "SOS_TRIGGERED",
                description = "Prueba de integración",
                occurredAt = now
            )
        )
        assertThat(dao.observeEvents(10).first()).hasSize(1)
        dao.clearEvents()
        assertThat(dao.observeEvents(10).first()).isEmpty()
    }
}
