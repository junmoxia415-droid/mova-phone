package com.studiolexair.movaphone.domain.calls.repository

import com.studiolexair.movaphone.domain.calls.model.CallRecord
import com.studiolexair.movaphone.domain.calls.model.CallType
import com.studiolexair.movaphone.domain.calls.model.SpamVerdict
import kotlinx.coroutines.flow.Flow

/** Historial de llamadas. */
interface CallsRepository {
    fun observeCallLog(): Flow<List<CallRecord>>
    fun observeRecentCalls(limit: Int = 10): Flow<List<CallRecord>>
    fun observeByType(type: CallType): Flow<List<CallRecord>>
    fun observeSpamCalls(): Flow<List<CallRecord>>
    fun observeMissedCount(): Flow<Int>

    suspend fun registerCall(record: CallRecord): Long
    suspend fun enrichFromDevice(): Int
    suspend fun deleteCall(id: Long)
    suspend fun markAsSpam(normalizedNumber: String, isSpam: Boolean)
}

/** Clasificación de spam (local en V1, ampliable a reputación remota sin tocar la UI). */
interface SpamClassifier {
    suspend fun classify(number: String): SpamVerdict
    suspend fun reportSpam(number: String, category: String)
}

/** Marcación mediante APIs oficiales de Android (ACTION_CALL / ACTION_DIAL). */
interface CallLauncher {
    suspend fun placeCall(number: String): Boolean
    fun openDialer(number: String): Boolean
    fun canPlaceCalls(): Boolean
}
