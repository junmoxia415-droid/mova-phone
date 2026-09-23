package com.studiolexair.movaphone.domain.emergency.model

/** Contacto de emergencia con prioridad (1 = primero). */
data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String = "",
    val priority: Int = 0,
    val relationship: String? = null,
    val allowCall: Boolean = true,
    val allowSms: Boolean = true,
    val shareLocation: Boolean = true,
    val isMedical: Boolean = false
)

/** Paso del protocolo SOS con su resultado real (nunca simulado). */
data class EmergencyStep(
    val kind: EmergencyStepKind,
    val label: String,
    val status: StepStatus,
    val detail: String? = null
)

enum class EmergencyStepKind { CONTACT_CALL, SMS, LOCATION, BATTERY, SECOND_CONTACT, ALERT }

enum class StepStatus { PENDING, RUNNING, DONE, FAILED, SKIPPED }

/** Estado completo de una emergencia activa. */
data class EmergencySession(
    val id: Long = 0,
    val startedAt: Long,
    val active: Boolean,
    val trigger: String = "manual",
    val steps: List<EmergencyStep> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val batteryPercent: Int? = null,
    val messageBody: String? = null,
    val contactedName: String? = null
)
