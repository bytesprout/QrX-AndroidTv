package com.queuerx.tv.feature.ticker

import kotlinx.serialization.Serializable

/**
 * A single message item for the hospital ticker strip.
 *
 * Can be a static text message, a news headline, or a promotional notice.
 */
@Serializable
data class TickerMessage(
    val messageId: String,
    val text: String,
    val priority: TickerPriority = TickerPriority.NORMAL,
    val language: String = "en",
    val expiresAtEpochMillis: Long? = null
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        expiresAtEpochMillis != null && now > expiresAtEpochMillis
}

@Serializable
enum class TickerPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Observed state for the hospital ticker band UI component.
 */
sealed interface TickerUiState {
    data object Hidden : TickerUiState
    data class Scrolling(
        val messages: List<TickerMessage>,
        val currentIndex: Int,
        val scrollSpeedDp: Float = 60f
    ) : TickerUiState
    data class Emergency(val message: String) : TickerUiState
}
