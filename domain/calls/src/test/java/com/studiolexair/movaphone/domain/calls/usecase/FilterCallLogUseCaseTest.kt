package com.studiolexair.movaphone.domain.calls.usecase

import com.google.common.truth.Truth.assertThat
import com.studiolexair.movaphone.domain.calls.model.CallRecord
import com.studiolexair.movaphone.domain.calls.model.CallType
import org.junit.Test

/** Las pestañas del historial deben filtrar exactamente lo que prometen. */
class FilterCallLogUseCaseTest {

    private val filter = FilterCallLogUseCase()

    private fun record(id: Long, type: CallType, spam: Boolean = false) = CallRecord(
        id = id,
        number = "60000000$id",
        normalizedNumber = "60000000$id",
        contactName = null,
        type = type,
        startedAt = 1_700_000_000_000L + id,
        durationSeconds = 0,
        isSpam = spam
    )

    private val records = listOf(
        record(1, CallType.INCOMING),
        record(2, CallType.OUTGOING),
        record(3, CallType.MISSED),
        record(4, CallType.REJECTED),
        record(5, CallType.INCOMING, spam = true)
    )

    @Test
    fun `todas devuelve el historial completo`() {
        assertThat(filter(records, CallFilter.ALL)).hasSize(5)
    }

    @Test
    fun `perdidas sólo incluye llamadas perdidas`() {
        assertThat(filter(records, CallFilter.MISSED).map { it.id }).containsExactly(3L)
    }

    @Test
    fun `entrantes y salientes se separan`() {
        assertThat(filter(records, CallFilter.INCOMING).map { it.id }).containsExactly(1L, 5L)
        assertThat(filter(records, CallFilter.OUTGOING).map { it.id }).containsExactly(2L)
    }

    @Test
    fun `spam usa la marca del clasificador`() {
        assertThat(filter(records, CallFilter.SPAM).map { it.id }).containsExactly(5L)
    }
}
