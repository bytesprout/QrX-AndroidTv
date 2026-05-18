package com.queuerx.tv.domain.queue

enum class QueueTokenState {
    WAITING,
    CALLED,
    ACTIVE,
    COMPLETED,
    REFERRED_TO_PHARMACY,
    PREPARING,
    READY,
    COLLECTED
}

class QueueStateMachine {
    private val transitions = mapOf(
        QueueTokenState.WAITING to setOf(QueueTokenState.CALLED),
        QueueTokenState.CALLED to setOf(QueueTokenState.ACTIVE),
        QueueTokenState.ACTIVE to setOf(QueueTokenState.COMPLETED),
        QueueTokenState.COMPLETED to setOf(QueueTokenState.REFERRED_TO_PHARMACY),
        QueueTokenState.REFERRED_TO_PHARMACY to setOf(QueueTokenState.PREPARING),
        QueueTokenState.PREPARING to setOf(QueueTokenState.READY),
        QueueTokenState.READY to setOf(QueueTokenState.COLLECTED),
        QueueTokenState.COLLECTED to emptySet()
    )

    fun transition(from: QueueTokenState, to: QueueTokenState): QueueTokenState {
        val allowed = transitions.getValue(from)
        require(to in allowed) { "Invalid queue transition from $from to $to" }
        return to
    }
}
