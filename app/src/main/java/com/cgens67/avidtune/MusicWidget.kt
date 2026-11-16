package com.cgens67.avidtune

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
import android.os.Bundle
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.extensions.currentMetadata
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.extensions.toggleRepeatMode
import com.cgens67.avidtune.playback.MusicService
import com.cgens67.avidtune.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MusicWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in CUSTOM_ACTIONS) {
            super.onReceive(context, intent)
            return
        }

        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                var optimisticLikeOverride: Boolean? = null

                withContext(Dispatchers.Main) {
                    val conn = PlayerConnection.instance
                    val service = MusicService.instance ?: conn?.service
                    val player = conn?.player ?: service?.player

                    when (action) {
                        ACTION_PLAY_PAUSE -> {
                            when {
                                conn != null -> conn.togglePlayPause()
                                player != null -> player.togglePlayPause()
                                context.filesDir.resolve(MusicService.PERSISTENT_QUEUE_FILE).exists() -> {
                                    val svcIntent = Intent(context, MusicService::class.java).apply {
                                        this.action = MusicService.ACTION_WIDGET_PLAY
                                    }
                                    ContextCompat.startForegroundService(context, svcIntent)
                                }
                                else -> openApp(context)
                            }
                        }

                        ACTION_PREV -> {
                            when {
                                conn != null -> conn.seekToPrevious()
                                player != null -> if (player.hasPreviousMediaItem() || player.currentPosition > 3000) {
                                    player.seekToPrevious()
                                }
                            }
                        }

                        ACTION_NEXT -> {
                            when {
                                conn != null -> conn.seekToNext()
                                player != null -> if (player.hasNextMediaItem()) {
                                    player.seekToNext()
                                }
                            }
                        }

                        ACTION_SHUFFLE -> {
                            when {
                                conn != null -> conn.toggleShuffle()
                                player != null -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                            }
                        }

                        ACTION_LIKE -> {
                            val currentlyLiked = conn?.isCurrentSongLiked()
                                ?: service?.isCurrentSongLiked
                                ?: false
                            when {
                                conn != null -> conn.toggleLike()
                                service != null -> {
                                    service.toggleLike()
                                    optimisticLikeOverride = !currentlyLiked
                                }
                            }
                        }

                        ACTION_REPLAY -> {
                            when {
                                conn != null -> conn.toggleReplayMode()
                                player != null -> player.toggleRepeatMode()
                            }
                        }

                        ACTION_OPEN_APP -> openApp(context)
                    }
                }

                if (action == ACTION_UPDATE_PROGRESS) {
                    updateProgressOnly(context)
                } else if (action != ACTION_OPEN_APP) {
                    updateAllWidgets(
                        context = context,
                        forceFull = false,
                        likedOverride = optimisticLikeOverride,
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed handling widget action: $action")
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                updateAllWidgets(context, forceFull = true, specificIds = appWidgetIds)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed in onUpdate")
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                updateAllWidgets(context, forceFull = true, specificIds = intArrayOf(appWidgetId))
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed in onAppWidgetOptionsChanged")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun openApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, MainActivity::class.java)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed opening app from widget")
        }
    }

    private data class PlayerSnapshot(
        val hasQueue: Boolean,
        val trackKey: String,
        val title: String,
        val artist: String,
        val artworkUrl: String?,
        val isPlaying: Boolean,
        val isShuffled: Boolean,
        val isLiked: Boolean,
        val currentPos: Long,
        val duration: Long,
    )

    companion object {
        private const val TAG = "MusicWidget"

        const val ACTION_PLAY_PAUSE = "com.cgens67.avidtune.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.cgens67.avidtune.ACTION_PREV"
        const val ACTION_NEXT = "com.cgens67.avidtune.ACTION_NEXT"
        const val ACTION_SHUFFLE = "com.cgens67.avidtune.ACTION_SHUFFLE"
        const val ACTION_LIKE = "com.cgens67.avidtune.ACTION_LIKE"
        const val ACTION_REPLAY = "com.cgens67.avidtune.ACTION_REPLAY"
        const val ACTION_OPEN_APP = "com.cgens67.avidtune.ACTION_OPEN_APP"
        const val ACTION_STATE_CHANGED = "com.cgens67.avidtune.ACTION_STATE_CHANGED"
        const val ACTION_UPDATE_PROGRESS = "com.cgens67.avidtune.ACTION_UPDATE_PROGRESS"

        private val CUSTOM_ACTIONS = setOf(
            ACTION_PLAY_PAUSE,
            ACTION_PREV,
            ACTION_NEXT,
            ACTION_SHUFFLE,
            ACTION_LIKE,
            ACTION_REPLAY,
            ACTION_OPEN_APP,
            ACTION_STATE_CHANGED,
            ACTION_UPDATE_PROGRESS,
        )

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val updateMutex = Mutex()

        @Volatile
        private var hasFullRenderedInProcess = false
        private var lastRenderedTrackKey: String? = null
        private var lastRenderedArtUrl: String? = null
        private var lastRenderedHasArt: Boolean = false
        private var lastRenderedPlaying: Boolean = false
        private var lastRenderedLiked: Boolean = false
        private var lastRenderedShuffle: Boolean = false

        private val bitmapCache = LruCache<String, Pair<Bitmap, Bitmap>>(4)

        @Volatile
        private var cachedPlaceholder: Bitmap? = null

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            widgetScope.launch {
                try {
                    updateAllWidgets(appContext, forceFull = false, specificIds = null, likedOverride = null)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed in updateAllWidgets")
                }
            }
        }

        private suspend fun readPlayerSnapshot(
            context: Context,
            likedOverride: Boolean? = null,
        ): PlayerSnapshot = withContext(Dispatchers.Main) {
            val conn = PlayerConnection.instance
            val service = MusicService.instance ?: conn?.service
            val player = conn?.player ?: service?.player

            if (player == null || player.mediaItemCount == 0) {
                PlayerSnapshot(
                    hasQueue = false,
                    trackKey = "idle",
                    title = context.getString(R.string.app_name),
                    artist = context.getString(R.string.tap_to_open),
                    artworkUrl = null,
                    isPlaying = false,
                    isShuffled = false,
                    isLiked = false,
                    currentPos = 0L,
                    duration = 0L,
                )
            } else {
                val customMeta = conn?.mediaMetadata?.value
                    ?: service?.currentMediaMetadata?.value
                    ?: player.currentMetadata
                    ?: player.currentMediaItem?.metadata
                val itemMeta = player.currentMediaItem?.mediaMetadata
                val playerMeta = player.mediaMetadata

                val title = customMeta?.title?.takeIf { it.isNotBlank() }
                    ?: itemMeta?.title?.toString()?.takeIf { it.isNotBlank() }
                    ?: playerMeta.title?.toString()?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.song_title)

                val artist = customMeta?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() }
                    ?: itemMeta?.artist?.toString()?.takeIf { it.isNotBlank() }
                    ?: itemMeta?.subtitle?.toString()?.takeIf { it.isNotBlank() }
                    ?: playerMeta.artist?.toString()?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.artist_name)

                val artUrl = customMeta?.thumbnailUrl?.takeIf { it.isNotBlank() }
                    ?: itemMeta?.artworkUri?.toString()?.takeIf { it.isNotBlank() }
                    ?: playerMeta.artworkUri?.toString()?.takeIf { it.isNotBlank() }

                val mediaId = customMeta?.id ?: player.currentMediaItem?.mediaId ?: "$title|$artist"
                val isPlaying = player.playWhenReady &&
                    player.playbackState != Player.STATE_IDLE &&
                    player.playbackState != Player.STATE_ENDED

                val isLiked = likedOverride
                    ?: conn?.isCurrentSongLiked()
                    ?: service?.isCurrentSongLiked
                    ?: false

                PlayerSnapshot(
                    hasQueue = true,
                    trackKey = "$mediaId|$title|$artist",
                    title = title,
                    artist = artist,
                    artworkUrl = artUrl,
                    isPlaying = isPlaying,
                    isShuffled = player.shuffleModeEnabled,
                    isLiked = isLiked,
                    currentPos = player.currentPosition.coerceAtLeast(0L),
                    duration = player.duration.takeIf { it > 0 && it != Long.MAX_VALUE } ?: 0L,
                )
            }
        }

        private suspend fun updateProgressOnly(context: Context) {
            if (!updateMutex.tryLock()) return
            val needsFullUpdate = try {
                if (!hasFullRenderedInProcess) {
                    true
                } else {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val widgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, MusicWidget::class.java)
                    )
                    if (widgetIds.isNotEmpty()) {
                        val snapshot = readPlayerSnapshot(context)
                        val views = RemoteViews(context.packageName, R.layout.widget_music)
                        bindProgress(views, snapshot)
                        appWidgetManager.partiallyUpdateAppWidget(widgetIds, views)
                    }
                    false
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Partial progress update failed, falling back to full update")
                true
            } finally {
                updateMutex.unlock()
            }

            if (needsFullUpdate) {
                updateAllWidgets(context, forceFull = true)
            }
        }

        private suspend fun updateAllWidgets(
            context: Context,
            forceFull: Boolean = false,
            specificIds: IntArray? = null,
            likedOverride: Boolean? = null,
        ) = updateMutex.withLock {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = specificIds ?: appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidget::class.java)
            )
            if (widgetIds.isEmpty()) return

            val snapshot = readPlayerSnapshot(context, likedOverride)
            val isFullUpdate = !hasFullRenderedInProcess || forceFull

            if (isFullUpdate) {
                val artPair = snapshot.artworkUrl?.let { loadArtAndBackdrop(context, it) }
                val views = RemoteViews(context.packageName, R.layout.widget_music)

                setPendingIntents(context, views)
                bindTrack(context, views, snapshot, artPair)
                bindPlayState(views, snapshot.isPlaying)
                bindLikeState(views, snapshot.isLiked)
                bindShuffleState(views, snapshot.isShuffled)
                bindProgress(views, snapshot)

                appWidgetManager.updateAppWidget(widgetIds, views)

                hasFullRenderedInProcess = true
                lastRenderedTrackKey = snapshot.trackKey
                lastRenderedArtUrl = snapshot.artworkUrl
                lastRenderedHasArt = artPair != null
                lastRenderedPlaying = snapshot.isPlaying
                lastRenderedLiked = snapshot.isLiked
                lastRenderedShuffle = snapshot.isShuffled
                return
            }

            val trackChanged = lastRenderedTrackKey != snapshot.trackKey
            val artNeedsUpdate = lastRenderedArtUrl != snapshot.artworkUrl ||
                (snapshot.artworkUrl != null && !lastRenderedHasArt)
            val playChanged = lastRenderedPlaying != snapshot.isPlaying
            val likeChanged = lastRenderedLiked != snapshot.isLiked
            val shuffleChanged = lastRenderedShuffle != snapshot.isShuffled

            val views = RemoteViews(context.packageName, R.layout.widget_music)

            var artPair: Pair<Bitmap, Bitmap>? = null
            if (trackChanged || artNeedsUpdate) {
                artPair = snapshot.artworkUrl?.let { loadArtAndBackdrop(context, it) }
                bindTrack(context, views, snapshot, artPair)
            }

            if (playChanged) {
                bindPlayState(views, snapshot.isPlaying)
            }

            if (likeChanged) {
                bindLikeState(views, snapshot.isLiked)
            }

            if (shuffleChanged) {
                bindShuffleState(views, snapshot.isShuffled)
            }

            bindProgress(views, snapshot)

            try {
                appWidgetManager.partiallyUpdateAppWidget(widgetIds, views)
                lastRenderedTrackKey = snapshot.trackKey
                if (trackChanged || artNeedsUpdate) {
                    lastRenderedArtUrl = snapshot.artworkUrl
                    lastRenderedHasArt = artPair != null
                }
                lastRenderedPlaying = snapshot.isPlaying
                lastRenderedLiked = snapshot.isLiked
                lastRenderedShuffle = snapshot.isShuffled
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Partial update failed, performing full update")
                hasFullRenderedInProcess = false
            }
        }

        private fun bindTrack(
            context: Context,
            views: RemoteViews,
            snapshot: PlayerSnapshot,
            artPair: Pair<Bitmap, Bitmap>?,
        ) {
            views.setTextViewText(R.id.widget_title_0, snapshot.title)
            views.setTextViewText(R.id.widget_artist_0, snapshot.artist)

            if (artPair != null && !artPair.first.isRecycled && !artPair.second.isRecycled) {
                views.setImageViewBitmap(R.id.widget_art_0, artPair.first)
                views.setImageViewBitmap(R.id.widget_bg_0, artPair.second)
            } else {
                views.setImageViewBitmap(R.id.widget_art_0, getOrCreateFallbackArtwork(context))
                views.setImageViewResource(R.id.widget_bg_0, 0)
            }
        }

        private fun bindPlayState(views: RemoteViews, isPlaying: Boolean) {
            views.setViewVisibility(R.id.widget_play_btn, if (isPlaying) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_pause_btn, if (isPlaying) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_eq_off, if (isPlaying) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_eq_on, if (isPlaying) View.VISIBLE else View.GONE)
        }

        private fun bindLikeState(views: RemoteViews, isLiked: Boolean) {
            views.setViewVisibility(R.id.widget_like_off, if (isLiked) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_like_on, if (isLiked) View.VISIBLE else View.GONE)
        }

        private fun bindShuffleState(views: RemoteViews, isShuffled: Boolean) {
            views.setViewVisibility(R.id.widget_shuffle_off, if (isShuffled) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_shuffle_on, if (isShuffled) View.VISIBLE else View.GONE)
        }

        private fun bindProgress(views: RemoteViews, snapshot: PlayerSnapshot) {
            val hasValidDuration = snapshot.hasQueue && snapshot.duration > 0L
            if (hasValidDuration) {
                val progress = ((snapshot.currentPos * 1000L) / snapshot.duration)
                    .toInt()
                    .coerceIn(0, 1000)
                views.setProgressBar(R.id.widget_progress_bar, 1000, progress, false)
                views.setTextViewText(R.id.widget_current_time, formatTime(snapshot.currentPos))
                views.setTextViewText(R.id.widget_duration, formatTime(snapshot.duration))
                views.setViewVisibility(R.id.widget_progress_row, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_progress_row, View.GONE)
            }
        }

        private fun getOrCreateFallbackArtwork(context: Context): Bitmap {
            cachedPlaceholder?.takeIf { !it.isRecycled }?.let { return it }

            val size = 180
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    size.toFloat(),
                    size.toFloat(),
                    intArrayOf(Color.rgb(44, 44, 56), Color.rgb(24, 24, 32)),
                    null,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

            ContextCompat.getDrawable(context, R.drawable.music_note)?.mutate()?.let { icon ->
                val pad = size / 4
                icon.setBounds(pad, pad, size - pad, size - pad)
                icon.colorFilter = PorterDuffColorFilter(
                    Color.argb(215, 255, 255, 255),
                    PorterDuff.Mode.SRC_IN,
                )
                icon.draw(canvas)
            }

            cachedPlaceholder = bmp
            return bmp
        }

        private suspend fun loadArtAndBackdrop(
            context: Context,
            url: String,
        ): Pair<Bitmap, Bitmap>? {
            bitmapCache.get(url)
                ?.takeIf { !it.first.isRecycled && !it.second.isRecycled }
                ?.let { return it }

            return withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(180, 180)
                        .allowHardware(false)
                        .build()
                    val drawable = context.imageLoader.execute(request).drawable ?: return@withContext null
                    val rawCover = drawable.toBitmap(180, 180, Bitmap.Config.ARGB_8888)
                    val backdrop = createBlurredBackdrop(rawCover)
                    val result = rawCover to backdrop
                    bitmapCache.put(url, result)
                    result
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Failed loading widget artwork")
                    null
                }
            }
        }

        private fun createBlurredBackdrop(source: Bitmap): Bitmap {
            val w = 96
            val h = 54
            val small = Bitmap.createScaledBitmap(source, w, h, true)
            val blurred = fastBoxBlur(small, radius = 6)
            if (small !== blurred) small.recycle()

            val palette = Palette.from(source).maximumColorCount(6).generate()
            val accent = palette.getDarkVibrantColor(
                palette.getVibrantColor(
                    palette.getMutedColor(Color.rgb(24, 24, 32))
                )
            )

            val canvas = Canvas(blurred)
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    w.toFloat(),
                    h.toFloat(),
                    intArrayOf(
                        Color.argb(145, Color.red(accent), Color.green(accent), Color.blue(accent)),
                        Color.argb(190, 12, 12, 18),
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), tintPaint)
            return blurred
        }

        private fun fastBoxBlur(bitmap: Bitmap, radius: Int): Bitmap {
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val temp = IntArray(w * h)

            repeat(2) {
                for (y in 0 until h) {
                    val rowOffset = y * w
                    for (x in 0 until w) {
                        var r = 0
                        var g = 0
                        var b = 0
                        var count = 0
                        for (dx in -radius..radius) {
                            val nx = (x + dx).coerceIn(0, w - 1)
                            val c = pixels[rowOffset + nx]
                            r += (c shr 16) and 0xFF
                            g += (c shr 8) and 0xFF
                            b += c and 0xFF
                            count++
                        }
                        temp[rowOffset + x] = Color.rgb(r / count, g / count, b / count)
                    }
                }
                for (x in 0 until w) {
                    for (y in 0 until h) {
                        var r = 0
                        var g = 0
                        var b = 0
                        var count = 0
                        for (dy in -radius..radius) {
                            val ny = (y + dy).coerceIn(0, h - 1)
                            val c = temp[ny * w + x]
                            r += (c shr 16) and 0xFF
                            g += (c shr 8) and 0xFF
                            b += c and 0xFF
                            count++
                        }
                        pixels[y * w + x] = Color.rgb(r / count, g / count, b / count)
                    }
                }
            }

            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            output.setPixels(pixels, 0, w, 0, 0, w, h)
            return output
        }

        private fun setPendingIntents(context: Context, views: RemoteViews) {
            val playPauseIntent = getBroadcastPendingIntent(context, ACTION_PLAY_PAUSE)
            val prevIntent = getBroadcastPendingIntent(context, ACTION_PREV)
            val nextIntent = getBroadcastPendingIntent(context, ACTION_NEXT)
            val shuffleIntent = getBroadcastPendingIntent(context, ACTION_SHUFFLE)
            val likeIntent = getBroadcastPendingIntent(context, ACTION_LIKE)
            val openAppIntent = getOpenAppPendingIntent(context)

            views.setOnClickPendingIntent(R.id.widget_play_btn, playPauseIntent)
            views.setOnClickPendingIntent(R.id.widget_pause_btn, playPauseIntent)
            views.setOnClickPendingIntent(R.id.widget_prev, prevIntent)
            views.setOnClickPendingIntent(R.id.widget_next, nextIntent)
            views.setOnClickPendingIntent(R.id.widget_shuffle_off, shuffleIntent)
            views.setOnClickPendingIntent(R.id.widget_shuffle_on, shuffleIntent)
            views.setOnClickPendingIntent(R.id.widget_like_off, likeIntent)
            views.setOnClickPendingIntent(R.id.widget_like_on, likeIntent)

            views.setOnClickPendingIntent(R.id.widget_top_row, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_art_0, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_info_0, openAppIntent)
        }

        private fun getOpenAppPendingIntent(context: Context): PendingIntent {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, MainActivity::class.java)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            return PendingIntent.getActivity(
                context,
                ACTION_OPEN_APP.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun getBroadcastPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicWidget::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        @SuppressLint("DefaultLocale")
        private fun formatTime(millis: Long): String {
            return if (millis <= 0L || millis == Long.MAX_VALUE) {
                "0:00"
            } else {
                String.format(
                    "%d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                )
            }
        }
    }
}