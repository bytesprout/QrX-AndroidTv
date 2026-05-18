package com.queuerx.tv.feature.ticker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages the hospital ticker band (bottom of screen).
 *
 * Spec Part 7 responsibilities:
 * - Cycle through a list of messages at a configurable interval
 * - Support Emergency Override mode (full-width, high-priority message)
 * - Filter expired messages before display
 * - Emit [TickerUiState] for the UI to render
 */
class HospitalTickerManager(
    private val cycleIntervalMs: Long = 8_000L,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    private val _state = MutableStateFlow<TickerUiState>(TickerUiState.Hidden)
    val state: StateFlow<TickerUiState> = _state

    private val messages = mutableListOf<TickerMessage>()
    private var currentIndex = 0
    private var cycleJob: Job? = null
    private var emergencyActive = false

    // ── Public API ────────────────────────────────────────────────────────────

    /** Replaces all messages and starts cycling. */
    fun setMessages(newMessages: List<TickerMessage>) {
        messages.clear()
        messages.addAll(newMessages.filter { !it.isExpired(nowProvider()) })
        messages.sortByDescending { it.priority.ordinal }
        currentIndex = 0
        startCycling()
    }

    /** Adds a single message (if not already present and not expired). */
    fun addMessage(message: TickerMessage) {
        if (message.isExpired(nowProvider())) return
        if (messages.none { it.messageId == message.messageId }) {
            messages.add(message)
            messages.sortByDescending { it.priority.ordinal }
            if (!emergencyActive) emitCurrentState()
        }
    }

    /** Removes a message by ID. */
    fun removeMessage(messageId: String) {
        messages.removeIf { it.messageId == messageId }
        currentIndex = currentIndex.coerceAtMost((messages.size - 1).coerceAtLeast(0))
        if (!emergencyActive) emitCurrentState()
    }

    /** Shows an emergency message overlay — overrides normal cycling. */
    fun showEmergency(message: String) {
        emergencyActive = true
        cycleJob?.cancel()
        _state.value = TickerUiState.Emergency(message)
    }

    /** Clears emergency mode and resumes normal cycling. */
    fun clearEmergency() {
        emergencyActive = false
        startCycling()
    }

    /** Removes all messages and hides the ticker. */
    fun clear() {
        cycleJob?.cancel()
        messages.clear()
        currentIndex = 0
        emergencyActive = false
        _state.value = TickerUiState.Hidden
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun startCycling() {
        cycleJob?.cancel()
        if (messages.isEmpty()) {
            _state.value = TickerUiState.Hidden
            return
        }
        emitCurrentState()
        cycleJob = scope.launch {
            while (isActive) {
                delay(cycleIntervalMs)
                pruneExpired()
                if (messages.isEmpty()) {
                    _state.value = TickerUiState.Hidden
                    return@launch
                }
                currentIndex = (currentIndex + 1) % messages.size
                emitCurrentState()
            }
        }
    }

    private fun pruneExpired() {
        val now = nowProvider()
        val removed = messages.removeIf { it.isExpired(now) }
        if (removed) {
            currentIndex = currentIndex.coerceAtMost((messages.size - 1).coerceAtLeast(0))
        }
    }

    private fun emitCurrentState() {
        if (messages.isEmpty()) {
            _state.value = TickerUiState.Hidden
            return
        }
        _state.value = TickerUiState.Scrolling(
            messages = messages.toList(),
            currentIndex = currentIndex
        )
    }
}
