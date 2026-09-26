package com.cgens67.avidtune.ui.component

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AnimateLyricsKey
import com.cgens67.avidtune.constants.LyricsClickKey
import com.cgens67.avidtune.constants.LyricsScrollKey
import com.cgens67.avidtune.constants.LyricsTextPositionKey
import com.cgens67.avidtune.db.entities.LyricsEntity
import com.cgens67.avidtune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.cgens67.avidtune.di.LyricsHelperEntryPoint
import com.cgens67.avidtune.lyrics.LyricsEntry
import com.cgens67.avidtune.lyrics.LyricsUtils.findCurrentLineIndex
import com.cgens67.avidtune.lyrics.LyricsUtils.parseLyrics
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.ui.screens.settings.LyricsPosition
import com.cgens67.avidtune.ui.utils.fadingEdge
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun LyricsV2(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean = true,
    positionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    var isLoadingLyrics by remember(mediaMetadata?.id) { mutableStateOf(false) }

    val songId = mediaMetadata?.id
    val matchingLyrics = remember(currentLyrics, songId) {
        if (currentLyrics?.id == songId) currentLyrics else null
    }

    LaunchedEffect(mediaMetadata?.id) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        val id = metadata.id

        val existing = withContext(Dispatchers.IO) {
            database.lyrics(id).firstOrNull()
        }

        if (existing == null) {
            isLoadingLyrics = true
            withContext(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val result = lyricsHelper.getLyrics(metadata)

                    val textToSave = if (result.lyrics.isNotBlank() && result.lyrics != LYRICS_NOT_FOUND) {
                        "[provider:${result.providerName}]\n${result.lyrics}"
                    } else {
                        LYRICS_NOT_FOUND
                    }

                    database.query {
                        upsert(LyricsEntity(id, textToSave))
                    }
                } catch (e: Throwable) {
                    database.query {
                        upsert(LyricsEntity(id, LYRICS_NOT_FOUND))
                    }
                } finally {
                    isLoadingLyrics = false
                }
            }
        } else {
            isLoadingLyrics = false
        }
    }

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val animateLyrics by rememberPreference(AnimateLyricsKey, true)
    val currentSkipSegments by playerConnection.currentSkipSegments.collectAsState()
    val sponsorBlockEnabled by playerConnection.sponsorBlockEnabled.collectAsState()

    val rawLyricsText = matchingLyrics?.lyrics
    val isNotFound = remember(rawLyricsText) {
        rawLyricsText != null && (
            rawLyricsText == LYRICS_NOT_FOUND ||
            rawLyricsText.isBlank()
        )
    }

    val originalLyrics = remember(rawLyricsText) {
        var text = rawLyricsText?.trim()
        if (text != null && text.startsWith("[provider:")) {
            text = text.substringAfter('\n').trim()
        }
        text
    }

    val lyricsOffsetMs = remember(rawLyricsText) {
        val raw = rawLyricsText.orEmpty()
        Regex("\\[offset:(-?\\d+)\\]").find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    val isSynced = remember(originalLyrics) {
        !originalLyrics.isNullOrEmpty() && "\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]".toRegex().containsMatchIn(originalLyrics)
    }

    val lines = remember(originalLyrics, isNotFound) {
        if (originalLyrics.isNullOrEmpty() || isNotFound) {
            emptyList()
        } else if (isSynced) {
            listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parseLyrics(originalLyrics)
        } else {
            originalLyrics.lines().mapIndexed { index, line ->
                LyricsEntry(index * 100L, line)
            }
        }
    }

    var currentMainLineIndex by remember(mediaMetadata?.id) { mutableIntStateOf(-1) }
    var previousMainLineIndex by remember(mediaMetadata?.id) { mutableIntStateOf(-1) }
    val lazyListState = rememberLazyListState()

    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    var userScrolledTime by remember { mutableLongStateOf(0L) }
    var isAnimatingScroll by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    isAutoScrollEnabled = false
                    userScrolledTime = System.currentTimeMillis()
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    LaunchedEffect(userScrolledTime) {
        if (userScrolledTime != 0L) {
            delay(3500)
            isAutoScrollEnabled = true
            userScrolledTime = 0L
        }
    }

    // Sync position tracking
    LaunchedEffect(originalLyrics, lyricsOffsetMs, currentSkipSegments, sponsorBlockEnabled) {
        if (originalLyrics.isNullOrEmpty() || !isSynced) {
            currentMainLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(50)
            val basePosition = positionProvider() ?: playerConnection.player.currentPosition
            var sponsorBlockOffset = 0L
            if (sponsorBlockEnabled) {
                for (segment in currentSkipSegments) {
                    if (basePosition >= segment.second) {
                        sponsorBlockOffset += (segment.second - segment.first)
                    } else if (basePosition > segment.first) {
                        sponsorBlockOffset += (basePosition - segment.first)
                    }
                }
            }

            val rawIndex = findCurrentLineIndex(
                lines,
                basePosition - sponsorBlockOffset + lyricsOffsetMs
            )
            var mainIdx = rawIndex
            while (mainIdx >= 0 && lines.getOrNull(mainIdx)?.isBackground == true) {
                mainIdx--
            }
            currentMainLineIndex = mainIdx
        }
    }

    suspend fun scrollToCurrentLine(targetIndex: Int) {
        if (targetIndex < 0 || targetIndex >= lines.size || isAnimatingScroll) return
        isAnimatingScroll = true
        try {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height
            if (viewportHeight > 0) {
                if (itemInfo != null) {
                    val center = viewportHeight / 2
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val offset = itemCenter - center
                    if (abs(offset) > 8) {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = tween(
                                durationMillis = if (animateLyrics) 600 else 1,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                } else {
                    val targetOffset = -(viewportHeight / 2)
                    lazyListState.animateScrollToItem(targetIndex, targetOffset)
                }
            }
        } catch (_: Exception) {
        } finally {
            isAnimatingScroll = false
        }
    }

    // Auto-scroll to current lyric line
    LaunchedEffect(currentMainLineIndex, isAutoScrollEnabled) {
        if (!isSynced || !scrollLyrics || currentMainLineIndex == -1 || !isAutoScrollEnabled) return@LaunchedEffect
        if (currentMainLineIndex != previousMainLineIndex) {
            previousMainLineIndex = currentMainLineIndex
            scrollToCurrentLine(currentMainLineIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .fadingEdge(vertical = if (isLandscape) 20.dp else 40.dp),
        contentAlignment = Alignment.Center
    ) {
        val halfHeight = maxHeight / 2
        val topPadding = if (isLandscape) (maxHeight / 3) else (halfHeight - 24.dp).coerceAtLeast(40.dp)
        val bottomPadding = if (isLandscape) (maxHeight / 3) else halfHeight.coerceAtLeast(40.dp)

        if (lines.isEmpty()) {
            if (isLoadingLyrics || matchingLyrics == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = textColor.copy(alpha = 0.8f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.loading_lyrics),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding, start = 8.dp, end = 8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                itemsIndexed(lines, key = { idx, item -> "$idx-${item.time}" }) { index, item ->
                    val isActiveLine = (index == currentMainLineIndex) && isSynced
                    val distance = if (isActiveLine) 0 else abs(index - currentMainLineIndex)

                    LyricsLine(
                        entry = item,
                        romanizedText = null,
                        isSynced = isSynced,
                        isActive = isActiveLine,
                        distanceFromCurrent = distance,
                        lyricsTextPosition = lyricsTextPosition,
                        textColor = textColor,
                        textSize = if (isLandscape) 22f else 28f,
                        lineSpacing = if (isLandscape) 4f else 6f,
                        onClick = {
                            if (isSynced && changeLyrics) {
                                val targetTime = item.time - lyricsOffsetMs
                                playerConnection.player.seekTo(targetTime.coerceAtLeast(0L))
                                isAutoScrollEnabled = true
                                coroutineScope.launch {
                                    scrollToCurrentLine(index)
                                }
                            }
                        },
                        onLongClick = {},
                        isSelected = false,
                        isSelectionModeActive = false,
                        isAutoScrollActive = isAutoScrollEnabled,
                        animateLyrics = animateLyrics,
                        lyricsOffset = lyricsOffsetMs,
                        currentSkipSegments = currentSkipSegments,
                        sponsorBlockEnabled = sponsorBlockEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
