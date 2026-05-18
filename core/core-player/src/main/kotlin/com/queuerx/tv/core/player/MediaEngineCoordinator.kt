package com.queuerx.tv.core.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Coordinates media playback on behalf of the display feature.
 *
 * Responsibilities:
 * - Delegates actual playback to a [MediaEngine] implementation.
 * - Handles image/web item timing (auto-advance after [MediaItem.displayDurationMs]).
 * - Retries on playback errors up to [PlayerConfig.retryCount] times.
 * - Emits a unified [PlaybackState] observable.
 *
 * Concurrency: this class is not thread-safe — call from the main coroutine scope.
 */
class MediaEngineCoordinator(
    private var activeConfig: PlayerConfig,
    private val engine: MediaEngine? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state

    private var timerJob: Job? = null
    private var currentItem: MediaItem? = null
    private var retryCount: Int = 0

    // ── Config ────────────────────────────────────────────────────────────────

    fun updateConfig(config: PlayerConfig) {
        activeConfig = config
    }

    fun currentConfig(): PlayerConfig = activeConfig

    // ── Playback control ──────────────────────────────────────────────────────

    /**
     * Starts playing [item].
     *
     * For [MediaType.IMAGE] and [MediaType.WEB] items, automatically advances
     * to the next item after [MediaItem.displayDurationMs] ms via [onAdvance].
     */
    fun play(item: MediaItem, onAdvance: () -> Unit = {}) {
        timerJob?.cancel()
        currentItem = item
        retryCount = 0

        when (item.type) {
            MediaType.IMAGE, MediaType.WEB, MediaType.YOUTUBE -> {
                _state.value = PlaybackState.Playing(item, 0L)
                scheduleAdvance(item.displayDurationMs, onAdvance)
            }
            else -> {
                // Delegate to MediaEngine for video/stream types
                scope.launch {
                    engine?.play(item)
                }
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        engine?.pause()
        currentItem?.let { _state.value = PlaybackState.Paused(it) }
    }

    fun resume() {
        engine?.resume()
        val item = currentItem ?: return
        _state.value = PlaybackState.Playing(item)
    }

    fun stop() {
        timerJob?.cancel()
        engine?.stop()
        currentItem = null
        _state.value = PlaybackState.Idle
    }

    /**
     * Called by the engine or the timer job to signal the item ended.
     * Retries up to [PlayerConfig.retryCount] on error.
     */
    fun onItemEnded(onAdvance: () -> Unit) {
        _state.value = PlaybackState.Ended
        onAdvance()
    }

    fun onError(message: String, retry: () -> Unit) {
        val item = currentItem ?: return
        if (retryCount < activeConfig.retryCount) {
            retryCount++
            scope.launch {
                delay(1_000L * retryCount)
                retry()
            }
        } else {
            _state.value = PlaybackState.Error(item, message)
        }
    }

    fun release() {
        timerJob?.cancel()
        engine?.release()
        _state.value = PlaybackState.Idle
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun scheduleAdvance(delayMs: Long, onAdvance: () -> Unit) {
        timerJob = scope.launch {
            delay(delayMs)
            _state.value = PlaybackState.Ended
            onAdvance()
        }
    }
}
