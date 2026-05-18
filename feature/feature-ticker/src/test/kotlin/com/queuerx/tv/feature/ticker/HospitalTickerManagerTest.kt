package com.queuerx.tv.feature.ticker

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTickerManagerTest {

    private val scope = TestScope()
    private lateinit var manager: HospitalTickerManager

    private fun msg(id: String, text: String = "Message $id", priority: TickerPriority = TickerPriority.NORMAL) =
        TickerMessage(id, text, priority)

    @BeforeEach
    fun setUp() {
        manager = HospitalTickerManager(cycleIntervalMs = 1_000L, scope = scope)
    }

    @Test
    fun `initial state is Hidden`() {
        assertThat(manager.state.value).isEqualTo(TickerUiState.Hidden)
    }

    @Test
    fun `setMessages transitions to Scrolling`() = scope.runTest {
        manager.setMessages(listOf(msg("1"), msg("2")))
        assertThat(manager.state.value).isInstanceOf(TickerUiState.Scrolling::class.java)
    }

    @Test
    fun `setMessages with empty list transitions to Hidden`() = scope.runTest {
        manager.setMessages(emptyList())
        assertThat(manager.state.value).isEqualTo(TickerUiState.Hidden)
    }

    @Test
    fun `showEmergency shows Emergency state`() = scope.runTest {
        manager.setMessages(listOf(msg("1")))
        manager.showEmergency("FIRE ALARM — EVACUATE")
        assertThat(manager.state.value).isInstanceOf(TickerUiState.Emergency::class.java)
        val state = manager.state.value as TickerUiState.Emergency
        assertThat(state.message).contains("FIRE ALARM")
    }

    @Test
    fun `clearEmergency resumes Scrolling`() = scope.runTest {
        manager.setMessages(listOf(msg("1")))
        manager.showEmergency("ALERT")
        manager.clearEmergency()
        assertThat(manager.state.value).isInstanceOf(TickerUiState.Scrolling::class.java)
    }

    @Test
    fun `clear transitions to Hidden`() = scope.runTest {
        manager.setMessages(listOf(msg("1")))
        manager.clear()
        assertThat(manager.state.value).isEqualTo(TickerUiState.Hidden)
    }

    @Test
    fun `expired messages are not shown`() = scope.runTest {
        val expired = TickerMessage("exp-1", "Old news", expiresAtEpochMillis = 1L) // expired
        val fresh = msg("fresh-1")
        manager.setMessages(listOf(expired, fresh))
        val state = manager.state.value as TickerUiState.Scrolling
        assertThat(state.messages).hasSize(1)
        assertThat(state.messages.first().messageId).isEqualTo("fresh-1")
    }

    @Test
    fun `messages sorted by priority descending`() = scope.runTest {
        manager.setMessages(listOf(
            msg("low", priority = TickerPriority.LOW),
            msg("urgent", priority = TickerPriority.URGENT),
            msg("normal", priority = TickerPriority.NORMAL)
        ))
        val state = manager.state.value as TickerUiState.Scrolling
        assertThat(state.messages.first().messageId).isEqualTo("urgent")
    }

    @Test
    fun `addMessage adds to existing list`() = scope.runTest {
        manager.setMessages(listOf(msg("1")))
        manager.addMessage(msg("2"))
        val state = manager.state.value as TickerUiState.Scrolling
        assertThat(state.messages).hasSize(2)
    }

    @Test
    fun `removeMessage removes specific message`() = scope.runTest {
        manager.setMessages(listOf(msg("1"), msg("2")))
        manager.removeMessage("1")
        val state = manager.state.value as TickerUiState.Scrolling
        assertThat(state.messages).hasSize(1)
        assertThat(state.messages.first().messageId).isEqualTo("2")
    }
}
