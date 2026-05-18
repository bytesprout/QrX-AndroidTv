package com.queuerx.tv.domain.display

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SpeechQueueManagerTest {

    private lateinit var engine: TextToSpeechEngine
    private lateinit var manager: SpeechQueueManager

    private var speakStartCount = 0
    private var speakEndCount = 0

    @BeforeEach
    fun setUp() {
        engine = mockk(relaxed = true)
        speakStartCount = 0
        speakEndCount = 0
        manager = SpeechQueueManager(
            engine = engine,
            onSpeakStart = { speakStartCount++ },
            onSpeakEnd = { speakEndCount++ }
        )
    }

    private fun announcement(
        id: String = "ann-1",
        text: String = "Token A-001, please proceed to Room 1",
        tokenNumber: String? = "A-001",
        priority: Int = 0,
        language: String = "en"
    ) = TtsAnnouncement(id, text, language, priority, tokenNumber)

    // ── Enqueue / dequeue ────────────────────────────────────────────────────

    @Nested
    inner class QueueManagement {
        @Test
        fun `enqueue adds to queue`() {
            manager.enqueue(announcement("a1", tokenNumber = "A-001"))
            assertThat(manager.pendingCount).isEqualTo(1)
        }

        @Test
        fun `duplicate token is silently dropped`() {
            manager.enqueue(announcement("a1", tokenNumber = "T-001"))
            manager.enqueue(announcement("a2", tokenNumber = "T-001"))
            assertThat(manager.pendingCount).isEqualTo(1)
        }

        @Test
        fun `different tokens are both enqueued`() {
            manager.enqueue(announcement("a1", tokenNumber = "T-001"))
            manager.enqueue(announcement("a2", tokenNumber = "T-002"))
            assertThat(manager.pendingCount).isEqualTo(2)
        }

        @Test
        fun `high priority item is sorted to front`() {
            manager.enqueue(announcement("a1", tokenNumber = "T-001", priority = 0))
            manager.enqueue(announcement("a2", tokenNumber = "T-002", priority = 5))
            val state = manager.state.value
            assertThat(state).isInstanceOf(TtsState.Queued::class.java)
            val pending = (state as TtsState.Queued).pending
            assertThat(pending.first().id).isEqualTo("a2")
        }

        @Test
        fun `clear empties queue and stops engine`() {
            manager.enqueue(announcement("a1"))
            manager.clear()
            assertThat(manager.pendingCount).isEqualTo(0)
            assertThat(manager.state.value).isEqualTo(TtsState.Idle)
            verify { engine.stop() }
        }
    }

    // ── SpeakNext ─────────────────────────────────────────────────────────────

    @Nested
    inner class SpeakNext {
        @Test
        fun `speakNext returns false when queue empty`() = runTest {
            val result = manager.speakNext()
            assertThat(result).isFalse()
        }

        @Test
        fun `speakNext speaks item and calls engine`() = runTest {
            manager.enqueue(announcement())
            manager.speakNext()
            coVerify { engine.speak(any(), "en") }
        }

        @Test
        fun `speakNext invokes onSpeakStart and onSpeakEnd`() = runTest {
            manager.enqueue(announcement())
            manager.speakNext()
            assertThat(speakStartCount).isEqualTo(1)
            assertThat(speakEndCount).isEqualTo(1)
        }

        @Test
        fun `speakNext transitions to Idle when queue empties`() = runTest {
            manager.enqueue(announcement())
            manager.speakNext()
            assertThat(manager.state.value).isEqualTo(TtsState.Idle)
            assertThat(manager.hasMore).isFalse()
        }
    }

    // ── TtsAnnouncement builder ───────────────────────────────────────────────

    @Nested
    inner class AnnouncementBuilder {
        @Test
        fun `tokenCalled builds English text`() {
            val ann = TtsAnnouncement.tokenCalled("id-1", "A-101", "Room 3", "en")
            assertThat(ann.text).contains("A-101")
            assertThat(ann.text).contains("Room 3")
        }

        @Test
        fun `tokenCalled builds Arabic text`() {
            val ann = TtsAnnouncement.tokenCalled("id-2", "A-102", "Room 5", "ar")
            assertThat(ann.text).contains("A-102")
            assertThat(ann.language).isEqualTo("ar")
        }

        @Test
        fun `resetSession allows re-announcing same token`() = runTest {
            manager.enqueue(announcement(tokenNumber = "R-001"))
            manager.speakNext()
            manager.enqueue(announcement("a2", tokenNumber = "R-001"))
            assertThat(manager.pendingCount).isEqualTo(0) // still deduped

            manager.resetSession()
            manager.enqueue(announcement("a3", tokenNumber = "R-001"))
            assertThat(manager.pendingCount).isEqualTo(1) // allowed after reset
        }
    }
}
