package com.queuerx.tv.feature.queue

import com.queuerx.tv.domain.queue.DoctorQueue
import com.queuerx.tv.domain.queue.PharmacyQueue
import com.queuerx.tv.domain.queue.QueueToken
import com.queuerx.tv.domain.queue.TokenState

/**
 * Observable display state for the doctor queue board.
 *
 * All UI transitions flow through this sealed class:
 * - Skeleton loading view while data is being fetched.
 * - Normal display with now-serving token + upcoming list.
 * - Emergency banner overlay (spec Part 5 — Emergency Override).
 * - Error/stale state with retry prompt.
 */
sealed interface DoctorQueueUiState {

    /** Initial load / data refresh in progress. */
    data object Loading : DoctorQueueUiState

    /** No queue data available and display is idle. */
    data object Idle : DoctorQueueUiState

    /** Active queue display. */
    data class Showing(
        val departmentName: String,
        val doctorName: String,
        val roomNumber: String,
        val nowServing: TokenDisplayModel?,
        val upcoming: List<TokenDisplayModel>,
        val recentlyCalled: List<TokenDisplayModel>,
        val updatedAt: Long
    ) : DoctorQueueUiState

    /** Emergency token override — full-screen banner. */
    data class EmergencyOverride(
        val token: TokenDisplayModel,
        val message: String = "EMERGENCY — Please proceed immediately"
    ) : DoctorQueueUiState

    /** Data is stale (> 5 min since last update). Show warning. */
    data class Stale(
        val lastShowing: Showing?,
        val ageMinutes: Long
    ) : DoctorQueueUiState

    /** Network/backend error. */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : DoctorQueueUiState
}

/**
 * Observable display state for the pharmacy queue board.
 */
sealed interface PharmacyQueueUiState {
    data object Loading : PharmacyQueueUiState
    data object Idle : PharmacyQueueUiState

    data class Showing(
        val preparing: List<TokenDisplayModel>,
        val ready: List<TokenDisplayModel>,
        val counters: List<CounterDisplayModel>,
        val updatedAt: Long
    ) : PharmacyQueueUiState

    data class Error(val message: String) : PharmacyQueueUiState
}

/**
 * Flat model derived from [QueueToken] for rendering.
 *
 * Decoupled from the domain model so the UI layer doesn't depend on
 * [TokenState] logic directly.
 */
data class TokenDisplayModel(
    val tokenNumber: String,
    val displayLabel: String,
    val roomLabel: String,
    val isEmergency: Boolean,
    val isCalled: Boolean,
    val state: String
)

data class CounterDisplayModel(
    val counterId: String,
    val counterName: String,
    val currentToken: TokenDisplayModel?
)

// ── Mapping extensions ────────────────────────────────────────────────────────

fun QueueToken.toDisplayModel(): TokenDisplayModel = TokenDisplayModel(
    tokenNumber = tokenNumber,
    displayLabel = tokenNumber,
    roomLabel = if (doctorName != null) "${doctorName} — ${roomNumber}" else roomNumber,
    isEmergency = state == TokenState.EMERGENCY,
    isCalled = state == TokenState.CALLED || state == TokenState.ACTIVE,
    state = state.name
)

fun DoctorQueue.toUiState(): DoctorQueueUiState.Showing = DoctorQueueUiState.Showing(
    departmentName = departmentName,
    doctorName = doctorName,
    roomNumber = roomNumber,
    nowServing = nowServing?.toDisplayModel(),
    upcoming = upcoming.map { it.toDisplayModel() },
    recentlyCalled = recentlyCalled.map { it.toDisplayModel() },
    updatedAt = updatedAtEpochMillis
)

fun PharmacyQueue.toUiState(): PharmacyQueueUiState.Showing = PharmacyQueueUiState.Showing(
    preparing = preparing.map { it.toDisplayModel() },
    ready = ready.map { it.toDisplayModel() },
    counters = counters.map { c ->
        CounterDisplayModel(
            counterId = c.counterId,
            counterName = c.counterName,
            currentToken = c.currentToken?.toDisplayModel()
        )
    },
    updatedAt = updatedAtEpochMillis
)
