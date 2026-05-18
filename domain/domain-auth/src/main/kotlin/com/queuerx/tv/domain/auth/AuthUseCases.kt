package com.queuerx.tv.domain.auth

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError

/**
 * Returns the stored [AuthToken] if it exists and is not expired.
 *
 * Returns [QueueRxError.TokenExpired] if the token is past its expiry.
 * Returns [QueueRxError.TokenInvalid] if no token is stored.
 */
class GetValidAuthTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): ApiResult<AuthToken> {
        val token = repository.getToken()
            ?: return ApiResult.Error(QueueRxError.TokenInvalid)
        if (token.isExpired()) {
            return ApiResult.Error(QueueRxError.TokenExpired)
        }
        return ApiResult.Success(token)
    }
}

/**
 * Proactively refreshes the access token if it is expiring soon (within 60 s).
 *
 * Always returns a valid token or an error — callers do not need to manage
 * expiry logic themselves.
 */
class EnsureFreshTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): ApiResult<AuthToken> {
        val token = repository.getToken()
            ?: return ApiResult.Error(QueueRxError.TokenInvalid)

        if (!token.isExpiringSoon()) {
            return ApiResult.Success(token)
        }

        val refreshResult = repository.refreshToken(token.refreshToken)
        if (refreshResult is ApiResult.Error) return refreshResult

        val newToken = (refreshResult as ApiResult.Success).data
        val saveResult = repository.saveToken(newToken)
        if (saveResult is ApiResult.Error) return saveResult

        return ApiResult.Success(newToken)
    }
}

/**
 * Clears all stored auth data (used on logout and factory reset).
 */
class ClearAuthTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): ApiResult<Unit> = repository.clearToken()
}
