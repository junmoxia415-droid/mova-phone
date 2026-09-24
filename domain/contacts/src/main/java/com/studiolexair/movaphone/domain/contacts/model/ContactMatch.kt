package com.studiolexair.movaphone.domain.contacts.model

/** Resultado de buscar un contacto por nombre dictado: a quién y por qué. */
data class ContactMatch(
    val contact: Contact,
    val reason: String
)
