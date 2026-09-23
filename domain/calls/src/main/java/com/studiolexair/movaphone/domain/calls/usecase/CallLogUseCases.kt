package com.studiolexair.movaphone.domain.calls.usecase

import com.studiolexair.movaphone.core.common.result.ErrorCode
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.core.common.result.failure
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.domain.calls.model.CallRecord
import com.studiolexair.movaphone.domain.calls.model.CallType
import com.studiolexair.movaphone.domain.calls.repository.CallLauncher
import com.studiolexair.movaphone.domain.calls.repository.CallsRepository
import com.studiolexair.movaphone.domain.calls.repository.SpamClassifier
import kotlinx.coroutines.flow.Flow

class GetCallLogUseCase(private val repository: CallsRepository) {
    operator fun invoke(): Flow<List<CallRecord>> = repository.observeCallLog()
}

class GetRecentCallsUseCase(private val repository: CallsRepository) {
    operator fun invoke(limit: Int = 10): Flow<List<CallRecord>> = repository.observeRecentCalls(limit)
}

/** Filtro de pestañas del historial: Todas · Perdidas · Entrantes · Salientes · Spam. */
class FilterCallLogUseCase {
    operator fun invoke(records: List<CallRecord>, filter: CallFilter): List<CallRecord> = when (filter) {
        CallFilter.ALL -> records
        CallFilter.MISSED -> records.filter { it.type == CallType.MISSED }
        CallFilter.INCOMING -> records.filter { it.type == CallType.INCOMING }
        CallFilter.OUTGOING -> records.filter { it.type == CallType.OUTGOING }
        CallFilter.SPAM -> records.filter { it.isSpam }
    }
}

enum class CallFilter { ALL, MISSED, INCOMING, OUTGOING, SPAM }

class DeleteCallUseCase(private val repository: CallsRepository) {
    suspend operator fun invoke(id: Long): MovaResult<Unit> = try {
        repository.deleteCall(id)
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible eliminar el registro.", t.message)
    }
}

class ClassifyNumberUseCase(private val classifier: SpamClassifier) {
    suspend operator fun invoke(number: String) = classifier.classify(number)
}

class PlaceCallUseCase(private val launcher: CallLauncher) {
    suspend operator fun invoke(number: String): MovaResult<Unit> {
        if (number.isBlank()) return failure(ErrorCode.VALIDATION, "Introduce un número para llamar.")
        if (!launcher.canPlaceCalls()) {
            return failure(ErrorCode.NOT_SUPPORTED, "Este dispositivo no admite llamadas telefónicas.")
        }
        if (!launcher.hasCallPermission()) {
            return failure(
                ErrorCode.PERMISSION_DENIED,
                "Concede el permiso de llamadas para marcar desde MOVA Phone."
            )
        }
        return try {
            if (launcher.placeCall(number)) MovaResult.Success(Unit)
            else failure(ErrorCode.PERMISSION_DENIED, "MOVA Phone necesita permiso para realizar llamadas.")
        } catch (t: Throwable) {
            failure(ErrorCode.UNKNOWN, "No fue posible iniciar la llamada.", t.message)
        }
    }
}

class MarkSpamUseCase(
    private val repository: CallsRepository,
    private val classifier: SpamClassifier
) {
    suspend operator fun invoke(number: String): MovaResult<Unit> = try {
        classifier.reportSpam(number, "user_report")
        repository.markAsSpam(PhoneNumbers.normalize(number), true)
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible reportar el número.", t.message)
    }
}
