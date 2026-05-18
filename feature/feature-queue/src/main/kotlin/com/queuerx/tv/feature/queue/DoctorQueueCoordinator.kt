package com.queuerx.tv.feature.queue

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.realtime.RealtimeEvent
import com.queuerx.tv.core.realtime.RealtimeEventType
import com.queuerx.tv.domain.queue.DoctorQueue
import com.queuerx.tv.domain.queue.GetCurrentTokenUseCase
import com.queuerx.tv.domain.queue.GetUpcomingTokensUseCase
import com.queuerx.tv.domain.queue.QueueRepository
import com.queuerx.tv.domain.queue.TokenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Feature coordinator for the doctor queue board.
 *
 * Subscribes to [RealtimeEvent] emissions and updates [uiState] accordingly.
 * Also handles the emergency override display.
 *
 * Spec Part 5 responsibilities:
 * - Render now-serving token prominently
 * - Show up to N upcoming tokens in a list
 * - Trigger Emergency Override banner on EMERGENCY state
 * - Show stale-data warning if the last update was > 5 min ago
 */
class DoctorQueueCoordinator(
    private val repository: QueueRepository,
    private val departmentId: String,
    private val realtimeEvents: SharedFlow<RealtimeEvent>,
    private val getCurrentToken: GetCurrentTokenUseCase = GetCurrentTokenUseCase(repository),
    private val getUpcoming: GetUpcomingTokensUseCase = GetUpcomingTokensUseCase(repository),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _uiState = MutableStateFlow<DoctorQueueUiState>(DoctorQueueUiState.Loading)
    val uiState: StateFlow<DoctorQueueUiState> = _uiState

    init {
        // Realtime subscription is started lazily via startRealtimeSubscription()
        // so tests can control when this begins.
    }

    /** Loads initial queue state from cache. */
    fun load() {
        scope.launch { refreshDisplay() }
    }

    /** Starts consuming realtime events. Call after [load]. */
    fun startRealtimeSubscription() {
        subscribeToRealtime()
    }

    /** Operator presses retry after an error. */
    fun retry() {
        _uiState.value = DoctorQueueUiState.Loading
        scope.launch { refreshDisplay() }
    }

    /** Called when new queue data has been saved to the repository. */
    fun onQueueUpdated(queues: List<DoctorQueue>) {
        scope.launch {
            val queue = queues.firstOrNull { it.departmentId == departmentId }
            if (queue != null) {
                checkForEmergency(queue)
                if (_uiState.value !is DoctorQueueUiState.EmergencyOverride) {
                    _uiState.value = queue.toUiState()
                }
            }
        }
    }

    private fun subscribeToRealtime() {
        scope.launch {
            realtimeEvents.collect { event ->
                when (event.type) {
                    RealtimeEventType.QUEUE_UPDATED,
                    RealtimeEventType.TOKEN_CALLED,
                    RealtimeEventType.TOKEN_STATE_CHANGED -> refreshDisplay()
                    RealtimeEventType.EMERGENCY_ALERT -> handleEmergencyEvent(event)
                    else -> Unit
                }
            }
        }
    }

    private suspend fun refreshDisplay() {
        val currentResult = getCurrentToken(departmentId)
        if (currentResult is ApiResult.Error) {
            // Check if stale
            val stale = currentResult.error
            val queues = repository.getDoctorQueues()
            val queue = queues.firstOrNull { it.departmentId == departmentId }
            val lastUpdate = repository.getLastUpdateTimestamp()
            val ageMinutes = lastUpdate?.let {
                (System.currentTimeMillis() - it) / 60_000L
            } ?: 0L
            _uiState.value = DoctorQueueUiState.Stale(
                lastShowing = queue?.toUiState(),
                ageMinutes = ageMinutes
            )
            return
        }

        val queues = repository.getDoctorQueues()
        val queue = queues.firstOrNull { it.departmentId == departmentId }
        if (queue == null) {
            _uiState.value = DoctorQueueUiState.Idle
            return
        }

        checkForEmergency(queue)
        // Only emit Showing if emergency didn't take over the state
        if (_uiState.value !is DoctorQueueUiState.EmergencyOverride) {
            _uiState.value = queue.toUiState()
        }
    }

    private fun checkForEmergency(queue: DoctorQueue) {
        val emergency = (listOfNotNull(queue.nowServing) + queue.upcoming)
            .firstOrNull { it.state == TokenState.EMERGENCY }
        if (emergency != null) {
            _uiState.value = DoctorQueueUiState.EmergencyOverride(emergency.toDisplayModel())
        }
    }

    private fun handleEmergencyEvent(event: RealtimeEvent) {
        scope.launch { refreshDisplay() }
    }
}
