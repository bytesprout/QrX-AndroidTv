package com.queuerx.tv.data.auth

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import com.queuerx.tv.core.common.runCatchingApiResult
import com.queuerx.tv.core.network.ApiEndpoints
import com.queuerx.tv.core.network.TokenProvider
import com.queuerx.tv.core.network.TokenPair
import com.queuerx.tv.core.storage.PreferenceKeys
import com.queuerx.tv.core.storage.SecureKeyValueStore
import com.queuerx.tv.domain.auth.AuthRepository
import com.queuerx.tv.domain.auth.AuthToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

/**
 * [AuthRepository] + [TokenProvider] implementation.
 *
 * Stores tokens in encrypted key-value storage.
 * Exposes a synchronous in-memory cache of the latest access/refresh tokens so
 * that [TokenProvider.accessToken] and [TokenProvider.refreshToken] can be called
 * from non-suspend contexts (e.g. Ktor's [DefaultRequest] block).
 */
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val store: SecureKeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : AuthRepository, TokenProvider {

    // In-memory cache for synchronous reads
    private val cachedAccessToken = AtomicReference<String?>(null)
    private val cachedRefreshToken = AtomicReference<String?>(null)

    // ── AuthRepository ────────────────────────────────────────────────────────

    override suspend fun getToken(): AuthToken? {
        val raw = store.get(PREF_AUTH_TOKEN) ?: return null
        return runCatching { json.decodeFromString<AuthTokenDto>(raw) }
            .map { it.toDomain() }
            .onSuccess { token ->
                cachedAccessToken.set(token.accessToken)
                cachedRefreshToken.set(token.refreshToken)
            }
            .getOrNull()
    }

    override suspend fun saveToken(token: AuthToken): ApiResult<Unit> =
        runCatchingApiResult {
            val dto = AuthTokenDto.fromDomain(token)
            store.set(PREF_AUTH_TOKEN, json.encodeToString(dto))
            store.set(PreferenceKeys.AUTH_TOKEN, token.accessToken)
            store.set(PreferenceKeys.REFRESH_TOKEN, token.refreshToken)
            cachedAccessToken.set(token.accessToken)
            cachedRefreshToken.set(token.refreshToken)
        }

    override suspend fun refreshToken(refreshToken: String): ApiResult<AuthToken> =
        runCatchingApiResult {
            val response: RefreshTokenResponse = httpClient.post(ApiEndpoints.AUTH_REFRESH) {
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }.body()
            AuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAtEpochMillis = response.expiresAtEpochMillis
            )
        }.also { result ->
            if (result is ApiResult.Success) {
                cachedAccessToken.set(result.data.accessToken)
                cachedRefreshToken.set(result.data.refreshToken)
            }
        }

    override suspend fun clearToken(): ApiResult<Unit> = runCatchingApiResult {
        store.remove(PREF_AUTH_TOKEN)
        store.remove(PreferenceKeys.AUTH_TOKEN)
        store.remove(PreferenceKeys.REFRESH_TOKEN)
        cachedAccessToken.set(null)
        cachedRefreshToken.set(null)
    }

    // ── TokenProvider (synchronous) ───────────────────────────────────────────

    override fun accessToken(): String? = cachedAccessToken.get()

    override fun refreshToken(): String? = cachedRefreshToken.get()

    override suspend fun refresh(refreshToken: String): TokenPair? {
        val result = this.refreshToken(refreshToken)
        return if (result is ApiResult.Success) {
            TokenPair(result.data.accessToken, result.data.refreshToken)
        } else {
            null
        }
    }

    /** Warm the in-memory cache from persisted storage. Call on app start. */
    suspend fun warmCache() {
        getToken() // side-effects populate the AtomicReferences
    }

    companion object {
        private const val PREF_AUTH_TOKEN = "auth_token_full_json"
    }
}

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
private data class AuthTokenDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long
) {
    fun toDomain() = AuthToken(accessToken, refreshToken, expiresAtEpochMillis)

    companion object {
        fun fromDomain(token: AuthToken) = AuthTokenDto(
            token.accessToken,
            token.refreshToken,
            token.expiresAtEpochMillis
        )
    }
}

@Serializable
private data class RefreshTokenRequest(val refreshToken: String)

@Serializable
private data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long
)
