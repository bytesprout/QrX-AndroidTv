package com.queuerx.tv.core.realtime

class ReconnectBackoffPolicy(
    private val delaysSeconds: List<Long> = listOf(1L, 2L, 5L, 10L, 20L, 60L)
) {
    fun delayForAttempt(attempt: Int): Long {
        require(attempt >= 1) { "attempt must be >= 1" }
        val index = (attempt - 1).coerceAtMost(delaysSeconds.lastIndex)
        return delaysSeconds[index]
    }
}
