package com.queuerx.tv.core.player

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaEngineCoordinatorTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private lateinit var coordinator: MediaEngineCoordinator

    private val config = PlayerConfig(
        autoplay = true,
        mute = false,
        loop = true,
        preferredQuality = "auto",
        subtitlesEnabled = false,
        retryCount = 3,
        maxBufferMs = 50_000
    )

    private fun imageItem(id: String = "img-1", durationMs: Long = 5_000L) = MediaItem(
        mediaId = id,
        type = MediaType.IMAGE,
        url = "https://example.com/$id.jpg",
        displayDurationMs = durationMs
    )

    @BeforeEach
    fun setUp() {
        coordinator = MediaEngineCoordinator(config, engine = null, scope = scope)
    }

    @Test
    fun `initial state is Idle`() = scope.runTest {
        coordinator.state.test {
            assertThat(awaitItem()).isEqualTo(PlaybackState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `play image item emits Playing`() = scope.runTest {
        coordinator.state.test {
            awaitItem() // Idle
            coordinator.play(imageItem())
            val state = awaitItem()
            assertThat(state).isInstanceOf(PlaybackState.Playing::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stop emits Idle`() = scope.runTest {
        coordinator.state.test {
            awaitItem() // Idle
            coordinator.play(imageItem())
            awaitItem() // Playing
            coordinator.stop()
            assertThat(awaitItem()).isEqualTo(PlaybackState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause emits Paused`() = scope.runTest {
        coordinator.state.test {
            awaitItem() // Idle
            coordinator.play(imageItem())
            awaitItem() // Playing
            coordinator.pause()
            assertThat(awaitItem()).isInstanceOf(PlaybackState.Paused::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resume after pause emits Playing`() = scope.runTest {
        coordinator.state.test {
            awaitItem() // Idle
            coordinator.play(imageItem())
            awaitItem() // Playing
            coordinator.pause()
            awaitItem() // Paused
            coordinator.resume()
            assertThat(awaitItem()).isInstanceOf(PlaybackState.Playing::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateConfig and currentConfig are consistent`() {
        val newConfig = config.copy(mute = true, retryCount = 5)
        coordinator.updateConfig(newConfig)
        assertThat(coordinator.currentConfig().mute).isTrue()
        assertThat(coordinator.currentConfig().retryCount).isEqualTo(5)
    }
}
