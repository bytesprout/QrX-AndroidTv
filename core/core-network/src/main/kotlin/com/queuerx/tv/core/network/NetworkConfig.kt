package com.queuerx.tv.core.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Networking configuration shared across all HTTP / SSE / WebSocket connections.
 *
 * Production defaults are tuned for hospital LAN environments where latency can
 * be variable but bandwidth is generally adequate.
 */
data class NetworkConfig(
    /** Base API URL. Resolved at runtime from [com.queuerx.tv.core.storage.QueueRxEnvironment]. */
    val baseUrl: String,

    /** Timeout for establishing a TCP connection. */
    val connectTimeoutSeconds: Duration = 15.seconds,

    /** Timeout waiting for a server response after the request is sent. */
    val requestTimeoutSeconds: Duration = 30.seconds,

    /** Timeout for reading response body data. */
    val socketTimeoutSeconds: Duration = 30.seconds,

    /** Maximum number of automatic retries on transient failures. */
    val maxRetries: Int = 3,

    /** Initial delay before the first retry. Subsequent retries use exponential backoff. */
    val retryInitialDelaySeconds: Duration = 2.seconds,

    /** Maximum backoff delay between retries. */
    val retryMaxDelaySeconds: Duration = 30.seconds,

    /** Factor by which the delay grows on each retry. */
    val retryBackoffFactor: Double = 2.0,

    /** Whether to log HTTP request/response bodies (disable in production). */
    val enableRequestLogging: Boolean = false,

    /** Maximum number of simultaneous HTTP connections. */
    val maxConnections: Int = 10
)

/** Default configs per environment. */
object NetworkConfigs {
    fun forEnvironment(baseUrl: String, isDebug: Boolean = false): NetworkConfig =
        NetworkConfig(
            baseUrl = baseUrl,
            enableRequestLogging = isDebug
        )
}
