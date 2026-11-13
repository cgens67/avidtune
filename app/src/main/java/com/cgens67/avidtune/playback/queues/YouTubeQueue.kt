package com.cgens67.avidtune.playback.queues

import androidx.media3.common.MediaItem
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
    private val startPosition: Long = 0L,
    val isRadio: Boolean = false,
) : Queue {
    private var continuation: String? = null

    val isSongRadioQueue: Boolean
        get() = endpoint.playlistId == null || endpoint.playlistId?.startsWith("RDAMVM") == true

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult =
            withContext(IO) {
                YouTube.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation

        val songTitle = preloadItem?.title
            ?: nextResult.items.getOrNull(nextResult.currentIndex ?: 0)?.title
            ?: nextResult.items.firstOrNull()?.title

        val resolvedTitle = when {
            isRadio -> nextResult.title ?: "Radio"
            nextResult.title.equals("Radio", ignoreCase = true) || isSongRadioQueue -> {
                if (!songTitle.isNullOrBlank()) "$songTitle Mix" else nextResult.title
            }
            else -> nextResult.title
        }

        return Queue.Status(
            title = resolvedTitle,
            items = nextResult.items.map { it.toMediaItem() },
            mediaItemIndex = nextResult.currentIndex ?: 0,
            position = startPosition,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult =
            withContext(IO) {
                YouTube.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return nextResult.items.map { it.toMediaItem() }
    }

    companion object {
        fun radio(song: MediaMetadata, isRadio: Boolean = false) = YouTubeQueue(
            WatchEndpoint(
                videoId = song.id,
                playlistId = "RDAMVM${song.id}"
            ),
            song,
            isRadio = isRadio,
        )
    }
}