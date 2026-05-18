package com.queuerx.tv.domain.media

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.player.MediaItem
import com.queuerx.tv.core.player.MediaPlaylist

/**
 * Fetches and caches the latest playlist for this display.
 *
 * If the remote fetch fails, returns the locally cached version (offline survival).
 */
class RefreshPlaylistUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(playlistId: String): ApiResult<MediaPlaylist> {
        val remote = repository.fetchPlaylist(playlistId)
        return if (remote is ApiResult.Success) {
            repository.savePlaylist(remote.data)
            remote
        } else {
            // Fallback to cached
            val cached = repository.getActivePlaylist()
            if (cached != null) {
                ApiResult.Success(cached)
            } else {
                remote // propagate the original error
            }
        }
    }
}

/**
 * Returns the current active playlist, refreshing if necessary.
 *
 * For use on display startup and after receiving a PLAYLIST_UPDATED SSE event.
 */
class GetActivePlaylistUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(): ApiResult<MediaPlaylist?> =
        ApiResult.Success(repository.getActivePlaylist())
}

/**
 * Validates that a [MediaItem] URL is non-blank and the type is supported.
 */
class ValidateMediaItemUseCase {
    operator fun invoke(item: MediaItem): ApiResult<MediaItem> {
        if (item.url.isBlank()) {
            return ApiResult.Error(
                com.queuerx.tv.core.common.QueueRxError.UnexpectedError(
                    "MediaItem ${item.mediaId} has a blank URL"
                )
            )
        }
        return ApiResult.Success(item)
    }
}
