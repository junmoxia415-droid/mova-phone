package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Contacto local de MOVA Phone. Es independiente de la agenda del sistema:
 * permite notas, grupos y favoritos propios sin alterar los contactos del usuario
 * (la agenda del sistema se consulta en modo lectura a través de ContactsContract).
 */
@Entity(
    tableName = "contacts",
    indices = [Index(value = ["normalizedNumber"]), Index(value = ["isFavorite"])]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceContactId: String? = null,
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val secondaryNumber: String? = null,
    val email: String? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val groupName: String? = null,
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
