package com.queuerx.tv.core.realtime

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RealtimeSseManager(
    private val backoffPolicy: ReconnectBackoffPolicy = ReconnectBackoffPolicy()
) {
    private val processedEventIds = ConcurrentHashMap.newKeySet<String>()
    private val _lastEventId = MutableStateFlow<String?>(null)

    val lastEventId: StateFlow<String?> = _lastEventId

    fun nextReconnectDelaySeconds(attempt: Int): Long = backoffPolicy.delayForAttempt(attempt)

    fun handleIncomingEvent(event: RealtimeEvent): Boolean {
        val inserted = processedEventIds.add(event.eventId)
        if (inserted) {
            _lastEventId.value = event.eventId
        }
        return inserted
    }
}
