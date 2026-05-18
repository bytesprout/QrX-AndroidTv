package com.queuerx.tv.domain.auth

import kotlinx.serialization.Serializable

/**
 * Auth token pair stored securely after a successful login / activation.
 *
 * @param accessToken  Short-lived JWT used in Authorization header.
 * @param refreshToken Long-lived token used to obtain a new [accessToken].
 * @param expiresAtEpochMillis Epoch ms at which [accessToken] expires.
 */
@Serializable
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long
) {
    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        nowEpochMillis >= expiresAtEpochMillis

    /** Returns true if token will expire within the next [bufferMs] milliseconds. */
    fun isExpiringSoon(
        nowEpochMillis: Long = System.currentTimeMillis(),
        bufferMs: Long = 60_000L
    ): Boolean = nowEpochMillis >= (expiresAtEpochMillis - bufferMs)
}
