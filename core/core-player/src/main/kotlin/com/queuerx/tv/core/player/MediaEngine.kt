package com.queuerx.tv.core.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic interface for the media player engine.
 *
 * On Android this is implemented via ExoPlayer 3.x.
 * In the JVM scaffold this interface ensures all playlist logic
 * (ordering, scheduling, retry) is fully testable without ExoPlayer.
 *
 * All implementations must emit [playbackState] as a hot [StateFlow]
 * so observers always have the latest state.
 */
interface MediaEngine {
    /** Observable playback state. */
    val playbackState: StateFlow<PlaybackState>

    /** Loads and plays a single [MediaItem]. */
    suspend fun play(item: MediaItem)

    /** Pauses playback. */
    fun pause()

    /** Resumes a paused item. */
    fun resume()

    /** Stops playback and resets to [PlaybackState.Idle]. */
    fun stop()

    /** Seeks to [positionMs] (videos only; no-op for images/streams). */
    fun seek(positionMs: Long)

    /** Returns true if the engine is currently playing. */
    val isPlaying: Boolean

    /** Releases all resources. Called when the screen is destroyed. */
    fun release()
}
