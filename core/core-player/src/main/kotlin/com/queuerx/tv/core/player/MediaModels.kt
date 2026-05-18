package com.queuerx.tv.core.player

import kotlinx.serialization.Serializable

/**
 * All supported media source types for the TV display.
 *
 * The player facade abstraction handles each type uniformly —
 * callers do not need to know which underlying player handles a given type.
 */
@Serializable
enum class MediaType {
    /** MP4 / WebM video file. */
    VIDEO,
    /** Static JPEG/PNG/WebP image with display duration. */
    IMAGE,
    /** HLS (.m3u8) adaptive stream. */
    HLS,
    /** RTSP / RTMP live stream (for IPTV). */
    RTSP,
    /** Web URL displayed in a WebView overlay. */
    WEB,
    /** Direct YouTube embed (handled via WebView). */
    YOUTUBE
}

/**
 * A single playable media item in a playlist.
 *
 * @param mediaId        Unique identifier for cache keying.
 * @param type           What kind of content this is.
 * @param url            Source URL (streaming or static).
 * @param displayDurationMs For [MediaType.IMAGE] and [MediaType.WEB]: how long to show it.
 *                           Ignored for video/stream types.
 * @param muteAudio      Override audio policy for this individual item.
 * @param title          Optional display title (not shown by default).
 */
@Serializable
data class MediaItem(
    val mediaId: String,
    val type: MediaType,
    val url: String,
    val displayDurationMs: Long = 10_000L,
    val muteAudio: Boolean = false,
    val title: String? = null,
    val thumbnailUrl: String? = null
)

/**
 * A named list of [MediaItem]s that plays in sequence or shuffled.
 *
 * Playlists are downloaded from the backend and cached locally.
 * The [version] field is used to detect when a playlist has changed
 * and needs to be refreshed.
 */
@Serializable
data class MediaPlaylist(
    val playlistId: String,
    val name: String,
    val items: List<MediaItem>,
    val shuffle: Boolean = false,
    val version: String = "1",
    val updatedAtEpochMillis: Long = 0L
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val size: Int get() = items.size
}

/**
 * Represents the current playback state of the media engine.
 */
sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Buffering : PlaybackState()
    data class Playing(val item: MediaItem, val positionMs: Long = 0L) : PlaybackState()
    data class Paused(val item: MediaItem) : PlaybackState()
    data class Error(val item: MediaItem, val message: String) : PlaybackState()
    data object Ended : PlaybackState()
}
