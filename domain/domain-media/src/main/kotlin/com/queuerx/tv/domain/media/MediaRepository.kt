package com.queuerx.tv.domain.media

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.player.MediaItem
import com.queuerx.tv.core.player.MediaPlaylist

/**
 * Repository interface for media playlist management.
 *
 * Implemented in the data layer (backed by network + local JSON cache).
 */
interface MediaRepository {
    /** Returns the active playlist for this display, or null if not set. */
    suspend fun getActivePlaylist(): MediaPlaylist?

    /** Fetches the latest playlist from the backend and caches it locally. */
    suspend fun fetchPlaylist(playlistId: String): ApiResult<MediaPlaylist>

    /** Saves a playlist to local cache. */
    suspend fun savePlaylist(playlist: MediaPlaylist): ApiResult<Unit>

    /** Returns all locally cached playlists. */
    suspend fun getCachedPlaylists(): List<MediaPlaylist>

    /** Deletes a cached playlist by ID. */
    suspend fun deletePlaylist(playlistId: String)

    /** Sets the active playlist ID for this display. */
    suspend fun setActivePlaylistId(playlistId: String)

    /** Returns the currently active playlist ID. */
    suspend fun getActivePlaylistId(): String?
}
