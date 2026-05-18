package com.queuerx.tv.core.common

/**
 * Universal result wrapper for all API/repository operations.
 *
 * All repository and use-case return types use this sealed class to propagate
 * success values, typed errors, and unexpected failures without relying on
 * exceptions crossing layer boundaries.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val error: QueueRxError) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw error.toException()
    }

    fun onSuccess(block: (T) -> Unit): ApiResult<T> {
        if (this is Success) block(data)
        return this
    }

    fun onError(block: (QueueRxError) -> Unit): ApiResult<T> {
        if (this is Error) block(error)
        return this
    }

    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    fun <R> flatMap(transform: (T) -> ApiResult<R>): ApiResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }
}

/** Wraps a suspend block and converts exceptions to [ApiResult.Error]. */
suspend fun <T> runCatchingApiResult(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: QueueRxException) {
    ApiResult.Error(e.error)
} catch (e: Exception) {
    ApiResult.Error(QueueRxError.UnexpectedError(e.message ?: "Unexpected error", e))
}
