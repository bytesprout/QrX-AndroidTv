package com.queuerx.tv.domain.media

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.player.MediaItem
import com.queuerx.tv.core.player.MediaPlaylist
import com.queuerx.tv.core.player.MediaType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class PlaylistSchedulerTest {

    private fun item(id: String) = MediaItem(
        mediaId = id,
        type = MediaType.IMAGE,
        url = "https://example.com/$id.jpg",
        displayDurationMs = 5_000L
    )

    private fun playlist(vararg ids: String, shuffle: Boolean = false) = MediaPlaylist(
        playlistId = "pl-1",
        name = "Test Playlist",
        items = ids.map { item(it) },
        shuffle = shuffle
    )

    @Nested
    inner class Sequential {
        @Test
        fun `next loops through all items in order`() {
            val scheduler = PlaylistScheduler(playlist("A", "B", "C"))
            assertThat(scheduler.next()?.mediaId).isEqualTo("A")
            assertThat(scheduler.next()?.mediaId).isEqualTo("B")
            assertThat(scheduler.next()?.mediaId).isEqualTo("C")
            assertThat(scheduler.next()?.mediaId).isEqualTo("A") // wraps
        }

        @Test
        fun `next returns null for empty playlist`() {
            val scheduler = PlaylistScheduler(playlist())
            assertThat(scheduler.next()).isNull()
        }

        @Test
        fun `current does not advance position`() {
            val scheduler = PlaylistScheduler(playlist("X", "Y"))
            val first = scheduler.current()
            val second = scheduler.current()
            assertThat(first?.mediaId).isEqualTo(second?.mediaId)
        }

        @Test
        fun `seekTo jumps to item`() {
            val scheduler = PlaylistScheduler(playlist("A", "B", "C"))
            val found = scheduler.seekTo("B")
            assertThat(found).isTrue()
            assertThat(scheduler.next()?.mediaId).isEqualTo("B")
        }

        @Test
        fun `seekTo returns false for unknown mediaId`() {
            val scheduler = PlaylistScheduler(playlist("A", "B"))
            assertThat(scheduler.seekTo("UNKNOWN")).isFalse()
        }
    }

    @Nested
    inner class Shuffle {
        @RepeatedTest(5)
        fun `shuffle mode returns all items before repeating`() {
            val scheduler = PlaylistScheduler(playlist("A", "B", "C", "D", shuffle = true))
            val seen = (1..4).map { scheduler.next()?.mediaId }.toSet()
            assertThat(seen).containsExactly("A", "B", "C", "D")
        }
    }

    @Nested
    inner class UpdatePlaylist {
        @Test
        fun `updatePlaylist resets position when playlist ID changes`() {
            val scheduler = PlaylistScheduler(playlist("A", "B", "C"))
            scheduler.next() // A
            scheduler.next() // B
            val newPlaylist = playlist("X", "Y").copy(playlistId = "pl-2")
            scheduler.updatePlaylist(newPlaylist)
            assertThat(scheduler.next()?.mediaId).isEqualTo("X")
        }

        @Test
        fun `updatePlaylist preserves position for same playlist ID`() {
            val original = playlist("A", "B", "C")
            val scheduler = PlaylistScheduler(original)
            scheduler.next() // A → position now at 1
            val updated = original.copy(version = "2") // same ID, new version
            scheduler.updatePlaylist(updated)
            // Position preserved → returns B
            assertThat(scheduler.next()?.mediaId).isEqualTo("B")
        }

        @Test
        fun `hasChangedSince detects version difference`() {
            val v1 = playlist("A", "B").copy(version = "1")
            val v2 = playlist("A", "B").copy(version = "2")
            assertThat(v2.hasChangedSince(v1)).isTrue()
            assertThat(v1.hasChangedSince(v1)).isFalse()
        }
    }
}
