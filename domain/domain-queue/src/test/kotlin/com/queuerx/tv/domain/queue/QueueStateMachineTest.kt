package com.queuerx.tv.domain.queue

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QueueStateMachineTest {
    private val stateMachine = QueueStateMachine()

    @Test
    fun `accepts valid consultation transition sequence`() {
        val called = stateMachine.transition(TokenState.WAITING, TokenState.CALLED)
        val active = stateMachine.transition(called, TokenState.ACTIVE)
        val completed = stateMachine.transition(active, TokenState.COMPLETED)

        assertEquals(TokenState.COMPLETED, completed)
    }

    @Test
    fun `accepts full pharmacy referral sequence`() {
        var state = TokenState.WAITING
        state = stateMachine.transition(state, TokenState.CALLED)
        state = stateMachine.transition(state, TokenState.ACTIVE)
        state = stateMachine.transition(state, TokenState.REFERRED)
        state = stateMachine.transition(state, TokenState.PREPARING)
        state = stateMachine.transition(state, TokenState.READY)
        state = stateMachine.transition(state, TokenState.COLLECTED)

        assertEquals(TokenState.COLLECTED, state)
    }

    @Test
    fun `rejects invalid transition WAITING to ACTIVE`() {
        assertFailsWith<IllegalArgumentException> {
            stateMachine.transition(TokenState.WAITING, TokenState.ACTIVE)
        }
    }

    @Test
    fun `rejects invalid transition COLLECTED to anything`() {
        assertFailsWith<IllegalArgumentException> {
            stateMachine.transition(TokenState.COLLECTED, TokenState.WAITING)
        }
    }

    @Test
    fun `allows WAITING to NO_SHOW`() {
        val result = stateMachine.transition(TokenState.WAITING, TokenState.NO_SHOW)
        assertEquals(TokenState.NO_SHOW, result)
    }

    @Test
    fun `allows CALLED to EMERGENCY`() {
        val result = stateMachine.transition(TokenState.CALLED, TokenState.EMERGENCY)
        assertEquals(TokenState.EMERGENCY, result)
    }

    @Test
    fun `canTransition returns false for invalid path`() {
        assertThat(stateMachine.canTransition(TokenState.WAITING, TokenState.COLLECTED)).isFalse()
    }

    @Test
    fun `canTransition returns true for valid path`() {
        assertThat(stateMachine.canTransition(TokenState.WAITING, TokenState.CALLED)).isTrue()
    }
}
