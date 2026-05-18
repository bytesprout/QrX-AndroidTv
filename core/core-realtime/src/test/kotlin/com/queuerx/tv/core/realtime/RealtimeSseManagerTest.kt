package com.queuerx.tv.core.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealtimeSseManagerTest {
    @Test
    fun `backoff policy follows required retry sequence`() {
        val manager = RealtimeSseManager()
        val expected = listOf(1L, 2L, 5L, 10L, 20L, 60L, 60L)

        val actual = (1..7).map(manager::nextReconnectDelaySeconds)

        assertEquals(expected, actual)
    }

    @Test
    fun `deduplicates events by event id`() {
        val manager = RealtimeSseManager()
        val first = RealtimeEvent("evt-1", RealtimeEventType.QUEUE_UPDATED, "{}", 1L)
        val duplicate = RealtimeEvent("evt-1", RealtimeEventType.QUEUE_UPDATED, "{}", 2L)

        assertTrue(manager.handleIncomingEvent(first))
        assertFalse(manager.handleIncomingEvent(duplicate))
        assertEquals("evt-1", manager.lastEventId.value)
    }
}
