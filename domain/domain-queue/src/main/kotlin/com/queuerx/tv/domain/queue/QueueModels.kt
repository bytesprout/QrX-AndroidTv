package com.queuerx.tv.domain.queue

import kotlinx.serialization.Serializable

/**
 * All possible states a queue token can be in, per spec Part 5.
 *
 * Extended from the earlier [QueueTokenState] to add NO_SHOW, REFERRED, and EMERGENCY.
 * The state machine in [QueueStateMachine] enforces valid transitions.
 */
@Serializable
enum class TokenState {
    WAITING,
    CALLED,
    ACTIVE,
    COMPLETED,
    NO_SHOW,
    REFERRED,      // Referred to pharmacy
    PREPARING,     // Pharmacy: medicine being prepared
    READY,         // Pharmacy: medicine ready for collection
    COLLECTED,     // Pharmacy: collected by patient
    EMERGENCY
}

/**
 * A single patient token in the doctor or pharmacy queue.
 *
 * @param tokenNumber  Display number shown on screen (e.g. "A-102", "P-401", "E-001").
 * @param state        Current lifecycle state.
 * @param roomNumber   Target room / counter (e.g. "Room 5", "Counter 2").
 * @param doctorName   Doctor's name (null for pharmacy tokens).
 * @param department   Department name (e.g. "Cardiology").
 * @param counterName  Pharmacy counter name (null for doctor tokens).
 * @param calledAtEpochMillis  Epoch ms when the token was called (null if not yet called).
 * @param updatedAtEpochMillis Last state-change timestamp.
 */
@Serializable
data class QueueToken(
    val tokenNumber: String,
    val state: TokenState,
    val roomNumber: String,
    val doctorName: String? = null,
    val department: String? = null,
    val counterName: String? = null,
    val calledAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long = 0L
) {
    val isEmergency: Boolean get() = state == TokenState.EMERGENCY
    val isCalled: Boolean get() = state == TokenState.CALLED
    val isActive: Boolean get() = state == TokenState.ACTIVE
    val isReady: Boolean get() = state == TokenState.READY
    val isWaiting: Boolean get() = state == TokenState.WAITING

    /**
     * Visual priority: lower number = higher priority.
     * Used to sort tokens for display (Emergency first, then Called, etc.).
     */
    val displayPriority: Int get() = when (state) {
        TokenState.EMERGENCY -> 0
        TokenState.CALLED -> 1
        TokenState.ACTIVE -> 2
        TokenState.READY -> 3
        TokenState.WAITING -> 4
        TokenState.PREPARING -> 5
        TokenState.REFERRED -> 6
        TokenState.COMPLETED -> 7
        TokenState.COLLECTED -> 8
        TokenState.NO_SHOW -> 9
    }
}

/**
 * Complete snapshot of a department's doctor queue.
 *
 * This is what the queue board renders.
 */
@Serializable
data class DoctorQueue(
    val departmentId: String,
    val departmentName: String,
    val doctorName: String,
    val roomNumber: String,
    val nowServing: QueueToken?,
    val upcoming: List<QueueToken>,
    val recentlyCalled: List<QueueToken>,
    val updatedAtEpochMillis: Long = 0L
)

/**
 * Complete snapshot of the pharmacy queue.
 */
@Serializable
data class PharmacyQueue(
    val preparing: List<QueueToken>,
    val ready: List<QueueToken>,
    val counters: List<PharmacyCounter>,
    val updatedAtEpochMillis: Long = 0L
)

@Serializable
data class PharmacyCounter(
    val counterId: String,
    val counterName: String,
    val currentToken: QueueToken?
)

/**
 * Repository interface for queue data.
 */
interface QueueRepository {
    /** Returns all active doctor queues for this display. */
    suspend fun getDoctorQueues(): List<DoctorQueue>

    /** Returns the pharmacy queue state. */
    suspend fun getPharmacyQueue(): PharmacyQueue?

    /** Returns a single token by number, or null if not found. */
    suspend fun getToken(tokenNumber: String): QueueToken?

    /** Persists a batch of doctor queues (called after SSE QUEUE_UPDATED). */
    suspend fun saveDoctorQueues(queues: List<DoctorQueue>)

    /** Persists the pharmacy queue. */
    suspend fun savePharmacyQueue(queue: PharmacyQueue)

    /** Updates a single token's state. Triggers local cache update. */
    suspend fun updateTokenState(tokenNumber: String, newState: TokenState)

    /** Deletes all cached queue data (reset). */
    suspend fun clearAll()

    /** Returns timestamp (epoch ms) of the last queue update. */
    suspend fun getLastUpdateTimestamp(): Long?
}
