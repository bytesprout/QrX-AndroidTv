package com.queuerx.tv.data.realtime

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.realtime.RealtimeEvent
import com.queuerx.tv.core.realtime.RealtimeEventType
import com.queuerx.tv.core.realtime.RealtimeSseManager
import org.junit.jupiter.api.Test

/**
 * Unit tests for SSE and WebSocket reconnect / deduplication logic.
 *
 * Integration tests that verify actual Ktor connectivity run against a test
 * mock engine and are located in the integration-test source set.
 */
class SseReconnectBackoffTest {

    @Test
    fun `backoff delays are within spec bounds (1s to 60s max)`() {
        val manager = RealtimeSseManager()
        val delays = (1..10).map { manager.nextReconnectDelaySeconds(it) }
        assertThat(delays.first()).isAtLeast(1L)
        assertThat(delays.last()).isAtMost(60L)
    }

    @Test
    fun `duplicate events are filtered out`() {
        val manager = RealtimeSseManager()
        val event = RealtimeEvent("id-001", RealtimeEventType.TOKEN_CALLED, "{}", 1000L)
        assertThat(manager.handleIncomingEvent(event)).isTrue()
        assertThat(manager.handleIncomingEvent(event)).isFalse()
    }

    @Test
    fun `last event id is tracked after first event`() {
        val manager = RealtimeSseManager()
        assertThat(manager.lastEventId.value).isNull()
        manager.handleIncomingEvent(
            RealtimeEvent("evt-XYZ", RealtimeEventType.QUEUE_UPDATED, "{}", 999L)
        )
        assertThat(manager.lastEventId.value).isEqualTo("evt-XYZ")
    }

    @Test
    fun `last event id advances to most recent non-duplicate event`() {
        val manager = RealtimeSseManager()
        manager.handleIncomingEvent(RealtimeEvent("id-1", RealtimeEventType.QUEUE_UPDATED, "{}", 1L))
        manager.handleIncomingEvent(RealtimeEvent("id-2", RealtimeEventType.TOKEN_CALLED, "{}", 2L))
        assertThat(manager.lastEventId.value).isEqualTo("id-2")
    }
}
