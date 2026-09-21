package com.cgens67.avidtune.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlin.math.max

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

    // Auto-scroll to current lyric line
    LaunchedEffect(currentMainLineIndex) {
        if (!isSynced || !scrollLyrics || currentMainLineIndex == -1) return@LaunchedEffect
        if (currentMainLineIndex != previousMainLineIndex) {
            previousMainLineIndex = currentMainLineIndex
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentMainLineIndex }
            if (itemInfo != null) {
                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val offset = itemCenter - center
                if (abs(offset) > 10) {
                    lazyListState.animateScrollBy(
                        value = offset.toFloat(),
                        animationSpec = if (animateLyrics) tween(800) else tween(1)
                    )
                }
            } else {
                lazyListState.scrollToItem(max(0, currentMainLineIndex - 2))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .fadingEdge(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
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
                contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp, start = 8.dp, end = 8.dp),
                modifier = Modifier.fillMaxSize()
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
                        textSize = 28f,
                        lineSpacing = 6f,
                        onClick = {
                            if (isSynced && changeLyrics) {
                                val targetTime = item.time - lyricsOffsetMs
                                playerConnection.player.seekTo(targetTime.coerceAtLeast(0L))
                            }
                        },
                        onLongClick = {},
                        isSelected = false,
                        isSelectionModeActive = false,
                        isAutoScrollActive = true,
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
