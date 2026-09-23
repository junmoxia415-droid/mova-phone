package com.studiolexair.movaphone.domain.calls.model

/** Llamada del historial (dominio). */
data class CallRecord(
    val id: Long = 0,
    val number: String,
    val normalizedNumber: String,
    val contactName: String? = null,
    val type: CallType,
    val startedAt: Long,
    val durationSeconds: Long = 0,
    val isSpam: Boolean = false,
    val blockedByMova: Boolean = false
) {
    val displayName: String get() = contactName ?: number
    val isKnownContact: Boolean get() = !contactName.isNullOrBlank()
}

enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED;

    companion object {
        fun fromSystemType(systemType: Int): CallType = when (systemType) {
            1 -> INCOMING
            2 -> OUTGOING
            3 -> MISSED
            5 -> REJECTED
            6 -> BLOCKED
            else -> INCOMING
        }
    }
}

/** Resultado del clasificador local de spam. */
data class SpamVerdict(
    val isSpam: Boolean,
    val score: Int,
    val reason: String
) {
    companion object {
        val NOT_SPAM = SpamVerdict(false, 0, "sin coincidencias")
    }
}
