package com.queuerx.tv.data.realtime

import com.queuerx.tv.core.network.ApiEndpoints
import com.queuerx.tv.core.realtime.HeartbeatMessage
import com.queuerx.tv.core.realtime.RealtimeEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages the WebSocket connection for realtime command + heartbeat channel.
 *
 * Responsibilities (spec Part 3):
 * - Send heartbeat every 25 seconds
 * - Receive and dispatch [RealtimeEvent] messages from server
 * - Auto-reconnect with backoff on disconnect
 * - Handle PING/PONG for keep-alive
 */
class WebSocketConnectionManager(
    private val httpClient: HttpClient,
    private val deviceId: String,
    private val uptimeProvider: () -> Long = { System.currentTimeMillis() / 1000 },
    private val json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true },
    private val scope: CoroutineScope
) {

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private val _connectionState = MutableSharedFlow<WebSocketConnectionState>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val connectionState: SharedFlow<WebSocketConnectionState> = _connectionState.asSharedFlow()

    private var connectionJob: Job? = null
    private var reconnectAttempt = 0

    private val backoffDelaysSeconds = listOf(1L, 2L, 5L, 10L, 20L, 60L)

    fun connect() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch { runConnectionLoop() }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        reconnectAttempt = 0
        _connectionState.tryEmit(WebSocketConnectionState.Disconnected)
    }

    private suspend fun runConnectionLoop() {
        while (true) {
            try {
                _connectionState.emit(WebSocketConnectionState.Connecting(reconnectAttempt))
                openWebSocket()
                reconnectAttempt = 0
            } catch (e: CancellationException) {
                _connectionState.emit(WebSocketConnectionState.Disconnected)
                return
            } catch (e: Exception) {
                reconnectAttempt++
                val delayIdx = (reconnectAttempt - 1).coerceAtMost(backoffDelaysSeconds.lastIndex)
                val delaySeconds = backoffDelaysSeconds[delayIdx]
                _connectionState.emit(
                    WebSocketConnectionState.Reconnecting(
                        attempt = reconnectAttempt,
                        delaySeconds = delaySeconds,
                        reason = e.message ?: "Unknown"
                    )
                )
                delay(delaySeconds * 1_000L)
            }
        }
    }

    private suspend fun openWebSocket() {
        httpClient.webSocket(ApiEndpoints.REALTIME_WS) {
            _connectionState.emit(WebSocketConnectionState.Connected)
            reconnectAttempt = 0

            val heartbeatJob = launch { sendHeartbeatLoop() }
            try {
                receiveLoop()
            } finally {
                heartbeatJob.cancel()
                close()
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        for (frame in incoming) {
            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()
                    runCatching {
                        val event = json.decodeFromString<RealtimeEvent>(text)
                        _events.emit(event)
                    }
                }
                is Frame.Close -> return
                else -> { /* PING/PONG handled by Ktor automatically */ }
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendHeartbeatLoop() {
        while (true) {
            delay(HEARTBEAT_INTERVAL_MS)
            val heartbeat = HeartbeatMessage(
                deviceId = deviceId,
                status = "alive",
                uptimeSeconds = uptimeProvider()
            )
            runCatching {
                send(Frame.Text(json.encodeToString(heartbeat)))
            }
        }
    }

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 25_000L
    }
}

sealed class WebSocketConnectionState {
    data object Disconnected : WebSocketConnectionState()
    data class Connecting(val attempt: Int) : WebSocketConnectionState()
    data object Connected : WebSocketConnectionState()
    data class Reconnecting(
        val attempt: Int,
        val delaySeconds: Long,
        val reason: String
    ) : WebSocketConnectionState()
}
