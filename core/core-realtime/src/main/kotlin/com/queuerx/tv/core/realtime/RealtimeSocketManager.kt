package com.queuerx.tv.core.realtime

class RealtimeSocketManager {
    fun heartbeat(deviceId: String, uptimeSeconds: Long, online: Boolean = true): HeartbeatMessage {
        return HeartbeatMessage(
            deviceId = deviceId,
            status = if (online) "online" else "degraded",
            uptimeSeconds = uptimeSeconds
        )
    }
}
