package com.queuerx.tv.domain.media

import com.queuerx.tv.core.player.MediaItem
import com.queuerx.tv.core.player.MediaPlaylist
import com.queuerx.tv.core.player.MediaType

/**
 * Manages the playback schedule for a [MediaPlaylist].
 *
 * Supports sequential and shuffle modes. Provides the next item to play
 * and tracks the current position. Thread-safe for single-threaded UI use.
 */
class PlaylistScheduler(
    private var playlist: MediaPlaylist,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var currentIndex: Int = 0
    private val shuffledOrder: MutableList<Int> = mutableListOf()
    private var shuffleEnabled: Boolean = playlist.shuffle

    init {
        if (shuffleEnabled) buildShuffleOrder()
    }

    /**
     * Returns the next [MediaItem] to play and advances the internal position.
     *
     * Returns null if the playlist is empty.
     * When the playlist ends, wraps around to the beginning (continuous loop).
     */
    fun next(): MediaItem? {
        if (playlist.isEmpty) return null

        val item = if (shuffleEnabled) {
            val idx = shuffledOrder[currentIndex % shuffledOrder.size]
            playlist.items[idx]
        } else {
            playlist.items[currentIndex % playlist.size]
        }

        currentIndex = (currentIndex + 1) % playlist.size
        if (shuffleEnabled && currentIndex == 0) buildShuffleOrder()

        return item
    }

    /** Returns the current item without advancing. */
    fun current(): MediaItem? {
        if (playlist.isEmpty) return null
        return if (shuffleEnabled) {
            val idx = shuffledOrder[currentIndex % shuffledOrder.size]
            playlist.items[idx]
        } else {
            playlist.items[currentIndex % playlist.size]
        }
    }

    /** Replaces the active playlist. Resets position if the playlist ID changed. */
    fun updatePlaylist(newPlaylist: MediaPlaylist) {
        if (newPlaylist.playlistId != playlist.playlistId) {
            currentIndex = 0
        }
        playlist = newPlaylist
        shuffleEnabled = newPlaylist.shuffle
        if (shuffleEnabled) buildShuffleOrder()
    }

    /** Jumps to a specific item by its [mediaId]. Returns false if not found. */
    fun seekTo(mediaId: String): Boolean {
        val idx = playlist.items.indexOfFirst { it.mediaId == mediaId }
        if (idx == -1) return false
        currentIndex = if (shuffleEnabled) {
            shuffledOrder.indexOf(idx).coerceAtLeast(0)
        } else {
            idx
        }
        return true
    }

    val isEmpty: Boolean get() = playlist.isEmpty
    val currentPlaylist: MediaPlaylist get() = playlist

    private fun buildShuffleOrder() {
        shuffledOrder.clear()
        shuffledOrder.addAll((0 until playlist.size).toMutableList().also { it.shuffle() })
    }
}

/**
 * Returns true if [playlist] has changed (newer version or different items).
 */
fun MediaPlaylist.hasChangedSince(other: MediaPlaylist): Boolean =
    this.version != other.version || this.items.size != other.items.size
