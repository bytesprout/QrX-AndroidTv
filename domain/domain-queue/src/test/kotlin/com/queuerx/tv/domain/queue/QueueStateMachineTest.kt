package com.queuerx.tv.domain.queue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QueueStateMachineTest {
    private val stateMachine = QueueStateMachine()

    @Test
    fun `accepts valid consultation transition sequence`() {
        val called = stateMachine.transition(QueueTokenState.WAITING, QueueTokenState.CALLED)
        val active = stateMachine.transition(called, QueueTokenState.ACTIVE)
        val completed = stateMachine.transition(active, QueueTokenState.COMPLETED)

        assertEquals(QueueTokenState.COMPLETED, completed)
    }

    @Test
    fun `rejects invalid transition`() {
        assertFailsWith<IllegalArgumentException> {
            stateMachine.transition(QueueTokenState.WAITING, QueueTokenState.ACTIVE)
        }
    }
}
