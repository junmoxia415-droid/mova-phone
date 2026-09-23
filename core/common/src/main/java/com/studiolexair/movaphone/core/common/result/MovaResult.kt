package com.studiolexair.movaphone.core.common.result

/**
 * Resultado de negocio. Nunca expone excepciones técnicas a la capa de UI:
 * [MovaError] transporta un mensaje ya apto para el usuario final.
 */
sealed interface MovaResult<out T> {
    data class Success<T>(val data: T) : MovaResult<T>
    data class Failure(val error: MovaError) : MovaResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.data
    val isSuccess: Boolean get() = this is Success
}

data class MovaError(
    val code: ErrorCode,
    val userMessage: String,
    val technicalDetail: String? = null
)

enum class ErrorCode {
    PERMISSION_DENIED,
    NOT_SUPPORTED,
    NO_CONNECTION,
    NO_DATA,
    VALIDATION,
    SECURITY,
    UNKNOWN
}

inline fun <T, R> MovaResult<T>.map(transform: (T) -> R): MovaResult<R> = when (this) {
    is MovaResult.Success -> MovaResult.Success(transform(data))
    is MovaResult.Failure -> this
}

fun <T> runCatchingMova(
    unknownMessage: String = "No fue posible completar esta operación. Inténtalo nuevamente.",
    block: () -> T
): MovaResult<T> = try {
    MovaResult.Success(block())
} catch (t: Throwable) {
    MovaResult.Failure(
        MovaError(
            code = ErrorCode.UNKNOWN,
            userMessage = unknownMessage,
            technicalDetail = t.message
        )
    )
}

/** Ayuda para construir fallos de negocio sin repetir tipos. */
fun <T> failure(code: ErrorCode, userMessage: String, detail: String? = null): MovaResult<T> =
    MovaResult.Failure(MovaError(code, userMessage, detail))
