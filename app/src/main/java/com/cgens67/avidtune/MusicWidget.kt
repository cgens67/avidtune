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
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                withContext(Dispatchers.Main) {
                    when (action) {
                        ACTION_PLAY_PAUSE -> PlayerConnection.instance?.togglePlayPause()
                        ACTION_PREV -> PlayerConnection.instance?.seekToPrevious()
                        ACTION_NEXT -> PlayerConnection.instance?.seekToNext()
                        ACTION_SHUFFLE -> PlayerConnection.instance?.toggleShuffle()
                        ACTION_LIKE -> PlayerConnection.instance?.toggleLike()
                        ACTION_REPLAY -> PlayerConnection.instance?.toggleReplayMode()
                        ACTION_OPEN_APP -> openApp(context)
                    }
                }

                if (action == ACTION_UPDATE_PROGRESS) {
                    updateProgressOnly(context)
                } else if (action != ACTION_OPEN_APP) {
                    updateAllWidgets(context, forceFull = false)
                }
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
            e.printStackTrace()
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

        private const val PREFS_NAME = "avidtune_widget_state"
        private const val KEY_TRACK = "last_track_key"
        private const val KEY_SLOT = "active_slot"
        private const val KEY_PLAYING = "last_playing"
        private const val KEY_LIKED = "last_liked"
        private const val KEY_SHUFFLE = "last_shuffle"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile
        private var hasFullRenderedInProcess = false

        private val bitmapCache = LruCache<String, Pair<Bitmap, Bitmap>>(4)

        @Volatile
        private var cachedPlaceholder: Bitmap? = null

        fun updateAllWidgets(context: Context) {
            widgetScope.launch {
                updateAllWidgets(context, forceFull = false)
            }
        }

        private suspend fun readPlayerSnapshot(context: Context): PlayerSnapshot =
            withContext(Dispatchers.Main) {
                val connection = PlayerConnection.instance
                val player = connection?.player
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
                    val title = player.mediaMetadata.title?.toString()
                        ?: context.getString(R.string.song_title)
                    val artist = player.mediaMetadata.artist?.toString()
                        ?: context.getString(R.string.artist_name)
                    val artUrl = player.mediaMetadata.artworkUri?.toString()
                    val mediaId = player.currentMediaItem?.mediaId ?: "$title|$artist"

                    PlayerSnapshot(
                        hasQueue = true,
                        trackKey = "$mediaId|$title|$artist",
                        title = title,
                        artist = artist,
                        artworkUrl = artUrl,
                        isPlaying = player.isPlaying,
                        isShuffled = player.shuffleModeEnabled,
                        isLiked = connection.isCurrentSongLiked(),
                        currentPos = player.currentPosition.coerceAtLeast(0L),
                        duration = player.duration.takeIf { it > 0 && it != Long.MAX_VALUE } ?: 0L,
                    )
                }
            }

        private suspend fun updateProgressOnly(context: Context) {
            if (!hasFullRenderedInProcess) {
                updateAllWidgets(context, forceFull = true)
                return
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidget::class.java)
            )
            if (widgetIds.isEmpty()) return

            val snapshot = readPlayerSnapshot(context)
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            bindProgress(views, snapshot)
            appWidgetManager.partiallyUpdateAppWidget(widgetIds, views)
        }

        private suspend fun updateAllWidgets(
            context: Context,
            forceFull: Boolean,
            specificIds: IntArray? = null,
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = specificIds ?: appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidget::class.java)
            )
            if (widgetIds.isEmpty()) return

            val snapshot = readPlayerSnapshot(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val prevTrackKey = prefs.getString(KEY_TRACK, null)
            val prevSlot = prefs.getInt(KEY_SLOT, 0)
            val prevPlaying = prefs.getBoolean(KEY_PLAYING, false)
            val prevLiked = prefs.getBoolean(KEY_LIKED, false)
            val prevShuffle = prefs.getBoolean(KEY_SHUFFLE, false)

            val isFirstRender = !hasFullRenderedInProcess || forceFull
            val trackChanged = prevTrackKey != null && prevTrackKey != snapshot.trackKey
            val activeSlot = if (trackChanged) 1 - prevSlot else prevSlot

            val artPair = snapshot.artworkUrl?.let { loadArtAndBackdrop(context, it) }

            val views = RemoteViews(context.packageName, R.layout.widget_music)

            if (isFirstRender) {
                setPendingIntents(context, views)
            }

            if (isFirstRender || trackChanged) {
                bindTrackSlot(context, views, activeSlot, snapshot, artPair)
                views.setDisplayedChild(R.id.widget_bg_flipper, activeSlot)
                views.setDisplayedChild(R.id.widget_art_flipper, activeSlot)
                views.setDisplayedChild(R.id.widget_info_flipper, activeSlot)
            }

            if (isFirstRender || prevPlaying != snapshot.isPlaying) {
                val playChild = if (snapshot.isPlaying) 1 else 0
                views.setDisplayedChild(R.id.widget_play_pause_flipper, playChild)
                views.setDisplayedChild(R.id.widget_eq_flipper, playChild)
            }

            if (isFirstRender || prevLiked != snapshot.isLiked) {
                views.setDisplayedChild(R.id.widget_like_flipper, if (snapshot.isLiked) 1 else 0)
            }

            if (isFirstRender || prevShuffle != snapshot.isShuffled) {
                views.setDisplayedChild(R.id.widget_shuffle_flipper, if (snapshot.isShuffled) 1 else 0)
            }

            bindProgress(views, snapshot)

            if (isFirstRender) {
                appWidgetManager.updateAppWidget(widgetIds, views)
                hasFullRenderedInProcess = true
            } else {
                appWidgetManager.partiallyUpdateAppWidget(widgetIds, views)
            }

            prefs.edit()
                .putString(KEY_TRACK, snapshot.trackKey)
                .putInt(KEY_SLOT, activeSlot)
                .putBoolean(KEY_PLAYING, snapshot.isPlaying)
                .putBoolean(KEY_LIKED, snapshot.isLiked)
                .putBoolean(KEY_SHUFFLE, snapshot.isShuffled)
                .apply()
        }

        private fun bindTrackSlot(
            context: Context,
            views: RemoteViews,
            slot: Int,
            snapshot: PlayerSnapshot,
            artPair: Pair<Bitmap, Bitmap>?,
        ) {
            val titleId = if (slot == 0) R.id.widget_title_0 else R.id.widget_title_1
            val artistId = if (slot == 0) R.id.widget_artist_0 else R.id.widget_artist_1
            val artId = if (slot == 0) R.id.widget_art_0 else R.id.widget_art_1
            val bgId = if (slot == 0) R.id.widget_bg_0 else R.id.widget_bg_1

            views.setTextViewText(titleId, snapshot.title)
            views.setTextViewText(artistId, snapshot.artist)

            if (artPair != null) {
                views.setImageViewBitmap(artId, artPair.first)
                views.setImageViewBitmap(bgId, artPair.second)
            } else {
                views.setImageViewBitmap(artId, getOrCreateFallbackArtwork(context))
                views.setImageViewBitmap(bgId, null)
            }
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
            cachedPlaceholder?.let { return it }

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
                icon.colorFilter = PorterDuffColorFilter(Color.argb(215, 255, 255, 255), PorterDuff.Mode.SRC_IN)
                icon.draw(canvas)
            }

            cachedPlaceholder = bmp
            return bmp
        }

        private suspend fun loadArtAndBackdrop(
            context: Context,
            url: String,
        ): Pair<Bitmap, Bitmap>? {
            bitmapCache.get(url)?.let { return it }

            return withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(180, 180)
                        .allowHardware(false)
                        .build()
                    val drawable = ImageLoader(context).execute(request).drawable ?: return@withContext null
                    val rawCover = drawable.toBitmap(180, 180, Bitmap.Config.ARGB_8888)
                    val backdrop = createBlurredBackdrop(rawCover)
                    val result = rawCover to backdrop
                    bitmapCache.put(url, result)
                    result
                } catch (_: Exception) {
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

            return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
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

            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_art_0, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_art_1, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_info_0, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_info_1, openAppIntent)
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