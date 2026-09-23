package com.studiolexair.movaphone.data.calls.repository

import com.studiolexair.movaphone.core.database.dao.CallRecordDao
import com.studiolexair.movaphone.core.database.entity.CallRecordEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.calls.source.DeviceCallLogDataSource
import com.studiolexair.movaphone.domain.calls.model.CallRecord
import com.studiolexair.movaphone.domain.calls.model.CallType
import com.studiolexair.movaphone.domain.calls.repository.CallsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class CallsRepositoryImpl(
    private val callRecordDao: CallRecordDao,
    private val deviceCallLog: DeviceCallLogDataSource
) : CallsRepository {

    override fun observeCallLog(): Flow<List<CallRecord>> =
        callRecordDao.observeAll().map { list -> list.map(::toDomain) }

    override fun observeRecentCalls(limit: Int): Flow<List<CallRecord>> =
        callRecordDao.observeRecent(limit).map { list -> list.map(::toDomain) }

    override fun observeByType(type: CallType): Flow<List<CallRecord>> =
        callRecordDao.observeByType(type.name).map { list -> list.map(::toDomain) }

    override fun observeSpamCalls(): Flow<List<CallRecord>> =
        callRecordDao.observeSpam().map { list -> list.map(::toDomain) }

    override fun observeMissedCount(): Flow<Int> {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return callRecordDao.observeMissedCount(since)
    }

    override suspend fun registerCall(record: CallRecord): Long = callRecordDao.insert(
        CallRecordEntity(
            number = record.number,
            normalizedNumber = record.normalizedNumber,
            contactName = record.contactName,
            type = record.type.name,
            startedAt = record.startedAt,
            durationSeconds = record.durationSeconds,
            isSpam = record.isSpam,
            blockedByMova = record.blockedByMova
        )
    )

    /** Sincroniza el historial del sistema sin duplicar registros ya importados. */
    override suspend fun enrichFromDevice(): Int {
        val deviceRecords = deviceCallLog.readRecent()
        var imported = 0
        deviceRecords.forEach { entity ->
            val alreadyThere = entity.systemCallId?.let { callRecordDao.findBySystemId(it) }
            if (alreadyThere == null) {
                callRecordDao.insert(entity)
                imported++
            }
        }
        MovaLog.i(TAG, "Llamadas importadas: $imported de ${deviceRecords.size}")
        return imported
    }

    override suspend fun deleteCall(id: Long) = callRecordDao.deleteById(id)

    override suspend fun markAsSpam(normalizedNumber: String, isSpam: Boolean) =
        callRecordDao.markSpamByNumber(normalizedNumber, isSpam)

    private fun toDomain(entity: CallRecordEntity): CallRecord = CallRecord(
        id = entity.id,
        number = entity.number,
        normalizedNumber = entity.normalizedNumber,
        contactName = entity.contactName,
        type = runCatching { CallType.valueOf(entity.type) }.getOrDefault(CallType.INCOMING),
        startedAt = entity.startedAt,
        durationSeconds = entity.durationSeconds,
        isSpam = entity.isSpam,
        blockedByMova = entity.blockedByMova
    )

    private companion object {
        const val TAG = "CallsRepository"
    }
}
