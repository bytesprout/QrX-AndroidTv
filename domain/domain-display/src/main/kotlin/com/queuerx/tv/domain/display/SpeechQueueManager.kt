package com.queuerx.tv.domain.display

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic interface for TTS playback.
 *
 * On Android, implemented by a class that wraps [android.speech.tts.TextToSpeech].
 * In JVM tests, a test double is injected.
 */
interface TextToSpeechEngine {
    /** Speaks [text] in [language] (BCP-47 tag e.g. "en", "ar", "hi"). */
    suspend fun speak(text: String, language: String)

    /** Returns true if the engine is currently speaking. */
    val isSpeaking: Boolean

    /** Stops all pending and current speech. */
    fun stop()

    /** Releases resources. */
    fun release()
}

/**
 * Immutable announcement request queued for TTS playback.
 *
 * @param text          The text to announce.
 * @param language      BCP-47 language tag (spec supports en, ar, hi, ta, ml, te, kn, bn).
 * @param priority      Higher-priority announcements jump the queue.
 * @param tokenNumber   Optional — if present, this is a "token called" announcement.
 * @param roomLabel     Optional — room or counter label to include in the announcement.
 */
data class TtsAnnouncement(
    val id: String,
    val text: String,
    val language: String = "en",
    val priority: Int = 0,  // 0 = normal, higher = more urgent
    val tokenNumber: String? = null,
    val roomLabel: String? = null
) {
    companion object {
        /**
         * Constructs a "Token Called" announcement in the appropriate language.
         *
         * Spec requirement: announce token number + room/counter name.
         */
        fun tokenCalled(
            id: String,
            tokenNumber: String,
            roomLabel: String,
            language: String = "en"
        ): TtsAnnouncement {
            val text = buildAnnouncementText(tokenNumber, roomLabel, language)
            return TtsAnnouncement(
                id = id,
                text = text,
                language = language,
                priority = 1,
                tokenNumber = tokenNumber,
                roomLabel = roomLabel
            )
        }

        private fun buildAnnouncementText(
            tokenNumber: String,
            roomLabel: String,
            language: String
        ): String = when (language) {
            "ar" -> "رقم $tokenNumber إلى $roomLabel"
            "hi" -> "टोकन $tokenNumber, कृपया $roomLabel जाएं"
            "ta" -> "டோக்கன் $tokenNumber, $roomLabel க்கு வரவும்"
            "ml" -> "ടോക്കൺ $tokenNumber, $roomLabel ൽ ഹാജരാകൂ"
            "te" -> "టోకెన్ $tokenNumber, $roomLabel కి రండి"
            "kn" -> "ಟೋಕನ್ $tokenNumber, $roomLabel ಗೆ ಬನ್ನಿ"
            "bn" -> "টোকেন $tokenNumber, $roomLabel এ আসুন"
            else  -> "Token $tokenNumber, please proceed to $roomLabel"
        }
    }
}

/** Observed TTS state. */
sealed interface TtsState {
    data object Idle : TtsState
    data class Speaking(val announcement: TtsAnnouncement) : TtsState
    data class Queued(val pending: List<TtsAnnouncement>) : TtsState
}

/**
 * Manages the TTS announcement queue for the display.
 *
 * Spec Part 7 — TTS requirements:
 * - Queue token-called announcements in order
 * - Deduplicate announcements by token number
 * - Support priority (Emergency tokens jump the queue)
 * - Announce in the display's configured language
 * - Interface with audio ducking (delegate via [onSpeakStart] / [onSpeakEnd])
 */
class SpeechQueueManager(
    private val engine: TextToSpeechEngine,
    private val onSpeakStart: () -> Unit = {},
    private val onSpeakEnd: () -> Unit = {}
) {

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state

    // Priority queue: sorted descending by priority so urgent items come first
    private val queue = mutableListOf<TtsAnnouncement>()
    private val announcedTokens = mutableSetOf<String>()

    /**
     * Enqueues an announcement.
     *
     * If the same token number has already been announced in this session,
     * the duplicate is silently dropped.
     */
    fun enqueue(announcement: TtsAnnouncement) {
        val token = announcement.tokenNumber
        if (token != null && token in announcedTokens) return

        queue.add(announcement)
        queue.sortByDescending { it.priority }
        if (token != null) announcedTokens.add(token)

        updateQueuedState()
    }

    /**
     * Dequeues and speaks the next announcement.
     *
     * Callers must await completion and call this again via a loop or event.
     * Returns false if the queue was empty.
     */
    suspend fun speakNext(): Boolean {
        if (queue.isEmpty()) {
            _state.value = TtsState.Idle
            return false
        }

        val announcement = queue.removeFirst()
        _state.value = TtsState.Speaking(announcement)
        onSpeakStart()
        try {
            engine.speak(announcement.text, announcement.language)
        } finally {
            onSpeakEnd()
        }
        if (queue.isEmpty()) {
            _state.value = TtsState.Idle
        } else {
            updateQueuedState()
        }
        return true
    }

    /** Clears the queue and stops speaking. */
    fun clear() {
        queue.clear()
        engine.stop()
        _state.value = TtsState.Idle
    }

    /** Resets the announced-token dedup set (e.g. on session restart). */
    fun resetSession() {
        announcedTokens.clear()
    }

    val pendingCount: Int get() = queue.size
    val hasMore: Boolean get() = queue.isNotEmpty()

    private fun updateQueuedState() {
        _state.value = TtsState.Queued(queue.toList())
    }
}
