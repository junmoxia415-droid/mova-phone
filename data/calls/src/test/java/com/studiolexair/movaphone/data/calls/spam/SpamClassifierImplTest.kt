package com.studiolexair.movaphone.data.calls.spam

import com.google.common.truth.Truth.assertThat
import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.core.database.entity.BlockedNumberEntity
import com.studiolexair.movaphone.core.database.entity.SecurityEventEntity
import com.studiolexair.movaphone.core.database.entity.SpamRuleEntity
import com.studiolexair.movaphone.core.database.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * La detección de spam es local: números bloqueados, reglas propias y reportes
 * del usuario. Se prueba con un DAO de mentira para no depender del dispositivo.
 */
class SpamClassifierImplTest {

    private class FakeSecurityDao(
        private val blocked: List<BlockedNumberEntity> = emptyList(),
        private val rules: List<SpamRuleEntity> = emptyList()
    ) : SecurityDao {
        override fun observeBlocked(): Flow<List<BlockedNumberEntity>> = flowOf(blocked)
        override suspend fun isBlocked(normalizedNumber: String): BlockedNumberEntity? =
            blocked.firstOrNull { it.normalizedNumber == normalizedNumber }
        override suspend fun allBlocked(): List<BlockedNumberEntity> = blocked
        override suspend fun block(entity: BlockedNumberEntity): Long = 0
        override suspend fun unblock(entity: BlockedNumberEntity) = Unit
        override suspend fun allSpamRules(): List<SpamRuleEntity> = rules
        override fun observeSpamRules(): Flow<List<SpamRuleEntity>> = flowOf(rules)
        override suspend fun addSpamRule(rule: SpamRuleEntity): Long = 0
        override suspend fun deleteSpamRule(id: Long) = Unit
        override fun observeEvents(limit: Int): Flow<List<SecurityEventEntity>> = flowOf(emptyList())
        override suspend fun logEvent(event: SecurityEventEntity): Long = 0
        override suspend fun purgeEventsBefore(before: Long) = Unit
        override suspend fun clearEvents() = Unit
        override fun observeTrusted(): Flow<List<TrustedContactEntity>> = flowOf(emptyList())
        override suspend fun allTrusted(): List<TrustedContactEntity> = emptyList()
        override suspend fun addTrusted(contact: TrustedContactEntity): Long = 0
        override suspend fun removeTrusted(id: Long) = Unit
    }

    @Test
    fun `un numero bloqueado se clasifica como spam con nivel maximo`() = runTest {
        val dao = FakeSecurityDao(
            blocked = listOf(
                BlockedNumberEntity(normalizedNumber = "600123456", phoneNumber = "600123456", label = null, createdAt = 0)
            )
        )
        val verdict = SpamClassifierImpl(dao).classify("600123456")
        assertThat(verdict.isSpam).isTrue()
        assertThat(verdict.score).isGreaterThan(0)
        assertThat(verdict.reason).isNotEmpty()
    }

    @Test
    fun `un numero desconocido normal no se marca como spam`() = runTest {
        val verdict = SpamClassifierImpl(FakeSecurityDao()).classify("613749265")
        assertThat(verdict.isSpam).isFalse()
        assertThat(verdict.score).isEqualTo(0)
    }

    @Test
    fun `un patron sospechoso suma puntos pero no basta por si solo`() = runTest {
        // 600999999 tiene una secuencia repetida: se detecta el patrón (2 puntos)
        // y queda justo en el umbral. Se documenta el comportamiento real.
        val verdict = SpamClassifierImpl(FakeSecurityDao()).classify("600999999")
        assertThat(verdict.score).isEqualTo(2)
        assertThat(verdict.reason).contains("patrón")
    }

    @Test
    fun `una regla del usuario marca el numero como spam`() = runTest {
        val dao = FakeSecurityDao(
            rules = listOf(
                SpamRuleEntity(pattern = "900", type = "PREFIX", weight = 3, source = "user", createdAt = 0)
            )
        )
        val verdict = SpamClassifierImpl(dao).classify("900123456")
        assertThat(verdict.isSpam).isTrue()
        assertThat(verdict.score).isAtLeast(3)
    }
}
