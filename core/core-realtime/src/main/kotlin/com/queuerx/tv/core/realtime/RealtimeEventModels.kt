package com.queuerx.tv.core.realtime

import kotlinx.serialization.Serializable

@Serializable
enum class RealtimeEventType {
    QUEUE_UPDATED,
    TOKEN_CALLED,
    TOKEN_ACTIVE,
    TOKEN_COMPLETED,
    TOKEN_READY,
    PHARMACY_PREPARING,
    EMERGENCY_ALERT,
    EMERGENCY_CLEAR,
    DISPLAY_LAYOUT_CHANGED,
    PLAYLIST_UPDATED,
    TICKER_UPDATED,
    DEVICE_RESTART,
    FORCE_SYNC
}

@Serializable
data class RealtimeEvent(
    val eventId: String,
    val type: RealtimeEventType,
    val payload: String,
    val timestampEpochMillis: Long
)

data class HeartbeatMessage(
    val type: String = "heartbeat",
    val deviceId: String,
    val status: String,
    val uptimeSeconds: Long
)
