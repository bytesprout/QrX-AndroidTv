package com.queuerx.tv.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory that creates configured [HttpClient] instances for different use cases.
 *
 * Separate clients are created for:
 * - REST API (with auth + JSON + retry)
 * - SSE (long-lived streaming connection, no timeout)
 * - WebSocket (long-lived, no timeout)
 */
object HttpClientFactory {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    /**
     * Creates the primary REST API client with bearer auth, retry, and JSON negotiation.
     *
     * @param config       Network configuration (timeouts, base URL, etc.)
     * @param tokenProvider Provides [BearerTokens] for the Auth plugin.
     */
    fun createRestClient(
        config: NetworkConfig,
        tokenProvider: TokenProvider
    ): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenProvider.accessToken() ?: return@loadTokens null
                    val refresh = tokenProvider.refreshToken() ?: return@loadTokens null
                    BearerTokens(access, refresh)
                }
                refreshTokens {
                    val newTokens = tokenProvider.refresh(oldTokens?.refreshToken ?: "")
                        ?: return@refreshTokens null
                    BearerTokens(newTokens.accessToken, newTokens.refreshToken)
                }
                sendWithoutRequest { request ->
                    // Do not attach auth header to activation or login endpoints
                    val path = request.url.pathSegments.joinToString("/")
                    !path.contains("activate") && !path.contains("auth/login")
                }
            }
        }

        install(HttpTimeout) {
            connectTimeoutMillis = config.connectTimeoutSeconds.inWholeMilliseconds
            requestTimeoutMillis = config.requestTimeoutSeconds.inWholeMilliseconds
            socketTimeoutMillis = config.socketTimeoutSeconds.inWholeMilliseconds
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = config.maxRetries)
            retryOnException(maxRetries = config.maxRetries, retryOnTimeout = true)
            exponentialDelay(
                base = config.retryBackoffFactor,
                maxDelayMs = config.retryMaxDelaySeconds.inWholeMilliseconds
            )
        }

        if (config.enableRequestLogging) {
            install(Logging) {
                level = LogLevel.ALL
                logger = object : Logger {
                    override fun log(message: String) {
                        println("[QueueRx HTTP] $message")
                    }
                }
            }
        }

        install(DefaultRequest) {
            url(config.baseUrl)
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }

    /**
     * Creates an SSE-capable client. Long timeouts; manages its own reconnect loop.
     * In Ktor 3.x, SSE is accessed via [io.ktor.client.request.get] + accept-sse
     * headers — no separate plugin install required.
     */
    fun createSseClient(config: NetworkConfig, tokenProvider: TokenProvider): HttpClient =
        HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutSeconds.inWholeMilliseconds
                // No request / socket timeout — SSE is a long-lived stream
                requestTimeoutMillis = 0
                socketTimeoutMillis = 0
            }

            install(DefaultRequest) {
                url(config.baseUrl)
                val token = tokenProvider.accessToken()
                if (token != null) {
                    headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }

    /**
     * Creates a WebSocket client for the realtime command channel.
     */
    fun createWebSocketClient(config: NetworkConfig, tokenProvider: TokenProvider): HttpClient =
        HttpClient(CIO) {
            install(WebSockets) {
                pingIntervalMillis = 25_000
            }

            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutSeconds.inWholeMilliseconds
                requestTimeoutMillis = 0
                socketTimeoutMillis = 0
            }

            install(DefaultRequest) {
                url(config.baseUrl)
                val token = tokenProvider.accessToken()
                if (token != null) {
                    headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
}

/** Provides current auth tokens to the [HttpClientFactory]. */
interface TokenProvider {
    /** Returns the cached access token synchronously (may return null before first login). */
    fun accessToken(): String?
    /** Returns the cached refresh token synchronously. */
    fun refreshToken(): String?
    /** Performs an async network refresh and returns the new token pair or null on failure. */
    suspend fun refresh(refreshToken: String): TokenPair?
}

data class TokenPair(val accessToken: String, val refreshToken: String)
