package com.queuerx.tv.data.realtime

import com.queuerx.tv.core.network.ApiEndpoints
import com.queuerx.tv.core.realtime.RealtimeEvent
import com.queuerx.tv.core.realtime.RealtimeSseManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Manages the SSE connection to the realtime event endpoint.
 *
 * Features (spec Part 3 + Part 7):
 * - Auto-reconnect with exponential backoff
 * - Last-Event-ID header for replay on reconnect
 * - Duplicate event deduplication via [RealtimeSseManager]
 * - Observable connection state
 * - Graceful cancellation
 *
 * SSE protocol: parsed from a line-framed text/event-stream HTTP response.
 * The raw response body is consumed as a String and split on double-newline
 * event boundaries. This avoids a dependency on the optional ktor-client-sse
 * artifact while staying fully spec-compliant.
 */
class SseConnectionManager(
    private val httpClient: HttpClient,
    private val deviceId: String,
    private val sseManager: RealtimeSseManager = RealtimeSseManager(),
    private val json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true },
    private val scope: CoroutineScope
) {

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private val _connectionState = MutableSharedFlow<SseConnectionState>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val connectionState: SharedFlow<SseConnectionState> = _connectionState.asSharedFlow()

    private var connectionJob: Job? = null
    private var reconnectAttempt = 0

    /** Starts the SSE connection. Safe to call multiple times (idempotent). */
    fun connect() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch { runConnectionLoop() }
    }

    /** Permanently closes the SSE connection. */
    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        reconnectAttempt = 0
        _connectionState.tryEmit(SseConnectionState.Disconnected)
    }

    private suspend fun runConnectionLoop() {
        while (true) {
            try {
                _connectionState.emit(SseConnectionState.Connecting(reconnectAttempt))
                pollSseEndpoint()
                reconnectAttempt = 0
            } catch (e: CancellationException) {
                _connectionState.emit(SseConnectionState.Disconnected)
                return
            } catch (e: Exception) {
                reconnectAttempt++
                val delaySeconds = sseManager.nextReconnectDelaySeconds(reconnectAttempt)
                _connectionState.emit(
                    SseConnectionState.Reconnecting(
                        attempt = reconnectAttempt,
                        delaySeconds = delaySeconds,
                        reason = e.message ?: "Unknown error"
                    )
                )
                delay(delaySeconds * 1_000L)
            }
        }
    }

    /**
     * Performs a single SSE GET, parses the event-stream body, and dispatches
     * unique [RealtimeEvent] objects via [_events].
     *
     * This method blocks until the response body is fully consumed (stream closed
     * by server) or an exception is thrown.
     */
    private suspend fun pollSseEndpoint() {
        val lastId = sseManager.lastEventId.value

        val responseBody: String = httpClient.get(ApiEndpoints.REALTIME_SSE) {
            parameter("deviceId", deviceId)
            header(HttpHeaders.Accept, "text/event-stream")
            header(HttpHeaders.CacheControl, "no-cache")
            if (lastId != null) {
                header("Last-Event-ID", lastId)
            }
        }.body()

        _connectionState.emit(SseConnectionState.Connected)

        parseAndDispatchEvents(responseBody)
    }

    /**
     * Parses an SSE event-stream body.
     *
     * Format:
     * ```
     * id: evt-001\n
     * data: {...json...}\n
     * \n
     * id: evt-002\n
     * data: {...json...}\n
     * \n
     * ```
     */
    private suspend fun parseAndDispatchEvents(body: String) {
        val eventBlocks = body.split("\n\n")
        for (block in eventBlocks) {
            if (block.isBlank()) continue
            var eventId: String? = null
            val dataLines = mutableListOf<String>()

            for (line in block.lines()) {
                when {
                    line.startsWith("id:") -> eventId = line.removePrefix("id:").trim()
                    line.startsWith("data:") -> dataLines.add(line.removePrefix("data:").trim())
                }
            }

            val dataStr = dataLines.joinToString("")
            if (eventId != null && dataStr.isNotBlank()) {
                runCatching {
                    val event = json.decodeFromString<RealtimeEvent>(dataStr)
                    if (sseManager.handleIncomingEvent(event)) {
                        _events.emit(event)
                    }
                }
            }
        }
    }
}

/** Observable SSE connection states. */
sealed class SseConnectionState {
    data object Disconnected : SseConnectionState()
    data class Connecting(val attempt: Int) : SseConnectionState()
    data object Connected : SseConnectionState()
    data class Reconnecting(
        val attempt: Int,
        val delaySeconds: Long,
        val reason: String
    ) : SseConnectionState()
}

