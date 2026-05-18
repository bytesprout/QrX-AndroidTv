package com.queuerx.tv.domain.queue

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Returns the current active token for a given department.
 *
 * If no token is currently being served, returns Success(null).
 * If queue data is stale (> 5 minutes old), returns QueueStale error.
 */
class GetCurrentTokenUseCase(
    private val repository: QueueRepository,
    private val staleness: Long = 5 * 60 * 1_000L
) {
    suspend operator fun invoke(departmentId: String): ApiResult<QueueToken?> {
        val lastUpdate = repository.getLastUpdateTimestamp()
        if (lastUpdate != null) {
            val ageMs = System.currentTimeMillis() - lastUpdate
            if (ageMs > staleness) {
                return ApiResult.Error(QueueRxError.QueueStale)
            }
        }
        val queues = repository.getDoctorQueues()
        val queue = queues.firstOrNull { it.departmentId == departmentId }
        return ApiResult.Success(queue?.nowServing)
    }
}

/**
 * Returns all upcoming tokens for a department, sorted by display priority.
 */
class GetUpcomingTokensUseCase(private val repository: QueueRepository) {
    suspend operator fun invoke(departmentId: String): ApiResult<List<QueueToken>> {
        val queues = repository.getDoctorQueues()
        val queue = queues.firstOrNull { it.departmentId == departmentId }
            ?: return ApiResult.Success(emptyList())
        return ApiResult.Success(queue.upcoming.sortedBy { it.displayPriority })
    }
}

/**
 * Validates a token state transition and applies it via the repository.
 *
 * Returns [QueueRxError.InvalidStateTransition] if the transition is not valid.
 */
class TransitionTokenStateUseCase(
    private val repository: QueueRepository,
    private val stateMachine: QueueStateMachine = QueueStateMachine()
) {
    suspend operator fun invoke(
        tokenNumber: String,
        newState: TokenState
    ): ApiResult<Unit> {
        val token = repository.getToken(tokenNumber)
            ?: return ApiResult.Error(QueueRxError.UnexpectedError("Token $tokenNumber not found"))

        return if (stateMachine.canTransition(token.state, newState)) {
            repository.updateTokenState(tokenNumber, newState)
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error(
                QueueRxError.InvalidStateTransition(
                    from = token.state.name,
                    to = newState.name
                )
            )
        }
    }
}

/**
 * Streams the complete doctor queue state as a [Flow].
 *
 * Emits once immediately with the cached state, and then emits again
 * whenever the cache is refreshed via [GetDoctorQueuesUseCase].
 */
class ObserveDoctorQueuesUseCase(private val repository: QueueRepository) {
    fun invoke(): Flow<List<DoctorQueue>> = flow {
        emit(repository.getDoctorQueues())
    }
}

/**
 * Returns the pharmacy queue state.
 */
class GetPharmacyQueueUseCase(private val repository: QueueRepository) {
    suspend operator fun invoke(): ApiResult<PharmacyQueue?> {
        return ApiResult.Success(repository.getPharmacyQueue())
    }
}
