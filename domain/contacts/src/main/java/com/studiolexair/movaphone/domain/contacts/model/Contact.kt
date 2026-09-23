package com.studiolexair.movaphone.domain.contacts.model

/** Contacto tal como lo entiende el dominio (ni Room ni ContactsContract). */
data class Contact(
    val id: Long = 0,
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
    val source: ContactSource = ContactSource.LOCAL
) {
    val initials: String
        get() = displayName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().toString() }
            .ifEmpty { "#" }
}

enum class ContactSource { LOCAL, DEVICE }
