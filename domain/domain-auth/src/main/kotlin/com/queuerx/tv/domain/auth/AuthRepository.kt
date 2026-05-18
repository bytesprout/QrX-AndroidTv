package com.queuerx.tv.domain.auth

import com.queuerx.tv.core.common.ApiResult

/**
 * Repository interface for auth-token lifecycle management.
 *
 * Implemented in the data layer. The domain layer only depends on this interface.
 */
interface AuthRepository {
    /** Returns the currently persisted token or null if not available. */
    suspend fun getToken(): AuthToken?

    /** Persists a new token pair after successful login/activation. */
    suspend fun saveToken(token: AuthToken): ApiResult<Unit>

    /**
     * Exchanges a refresh token for a new [AuthToken].
     * Returns [com.queuerx.tv.core.common.QueueRxError.RefreshFailed] on failure.
     */
    suspend fun refreshToken(refreshToken: String): ApiResult<AuthToken>

    /** Clears stored tokens (logout / factory reset). */
    suspend fun clearToken(): ApiResult<Unit>
}
