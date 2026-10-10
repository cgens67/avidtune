package com.cgens67.avidtune.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.cgens67.avidtune.models.toMediaMetadata
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
import timber.log.Timber
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
    val currentPositionProvider by rememberUpdatedState(positionProvider)

    val songId = mediaMetadata?.id

    var lyricsCache by remember { mutableStateOf<Map<String, LyricsEntity>>(emptyMap()) }
    var currentLyricsEntity by remember(songId) {
        mutableStateOf<LyricsEntity?>(lyricsCache[songId])
    }
    var isLoadingLyrics by remember(songId) { mutableStateOf(false) }

    val rawLyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val activeLyricsEntity = rawLyricsEntity?.takeIf { it.id == songId } ?: currentLyricsEntity

    LaunchedEffect(songId) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        val id = songId ?: return@LaunchedEffect

        if (lyricsCache.containsKey(id)) {
            currentLyricsEntity = lyricsCache[id]
            val text = currentLyricsEntity?.lyrics?.trim()
            if (!text.isNullOrBlank() && text != LYRICS_NOT_FOUND && text.startsWith("[provider:")) {
                return@LaunchedEffect
            }
        }

        isLoadingLyrics = true

        withContext(Dispatchers.IO) {
            try {
                val existingLyrics = try {
                    database.getLyrics(id)
                } catch (e: Throwable) {
                    null
                }

                // Fallback to database song metadata if in-memory metadata is missing artist or duration
                val dbSong = try { database.song(id).firstOrNull() } catch (e: Throwable) { null }
                val resolvedDuration = if (metadata.duration > 0) {
                    if (metadata.duration > 10000) metadata.duration / 1000 else metadata.duration
                } else if (dbSong != null && dbSong.song.duration > 0) {
                    dbSong.song.duration
                } else {
                    val pDur = (playerConnection.player.duration / 1000).toInt()
                    if (pDur > 0) pDur else -1
                }

                val resolvedArtists = if (metadata.artists.isNotEmpty() && metadata.artists.any { it.name.isNotBlank() }) {
                    metadata.artists
                } else if (dbSong != null && dbSong.artists.isNotEmpty()) {
                    dbSong.artists.map { MediaMetadata.Artist(it.id, it.name) }
                } else {
                    metadata.artists
                }

                val resolvedMetadata = metadata.copy(
                    artists = resolvedArtists,
                    duration = resolvedDuration
                )

                if (existingLyrics != null && existingLyrics.lyrics != LYRICS_NOT_FOUND && existingLyrics.lyrics.isNotBlank()) {
                    val text = existingLyrics.lyrics.trim()
                    withContext(Dispatchers.Main) {
                        currentLyricsEntity = existingLyrics
                        lyricsCache = lyricsCache + (id to existingLyrics)
                    }

                    // If existing lyrics do not have a provider tag, upgrade them in background
                    if (!text.startsWith("[provider:")) {
                        try {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                LyricsHelperEntryPoint::class.java
                            )
                            val lyricsHelper = entryPoint.lyricsHelper()
                            val fetchedResult = lyricsHelper.getLyrics(resolvedMetadata)

                            val fetchedLyrics = fetchedResult.lyrics
                            val pName = fetchedResult.providerName

                            if (fetchedLyrics.isNotBlank() && fetchedLyrics != LYRICS_NOT_FOUND) {
                                val textToSave = "[provider:$pName]\n$fetchedLyrics"
                                val upgradedEntity = LyricsEntity(id, textToSave)
                                database.query {
                                    upsert(upgradedEntity)
                                }
                                withContext(Dispatchers.Main) {
                                    currentLyricsEntity = upgradedEntity
                                    lyricsCache = lyricsCache + (id to upgradedEntity)
                                }
                            }
                        } catch (e: Throwable) {
                            Timber.e(e, "Error upgrading lyrics in LyricsV2")
                        }
                    }
                } else {
                    // Fetch from LyricsHelper (queries AvidLyrics, LyricsPlus, Paxsenix, etc.)
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            LyricsHelperEntryPoint::class.java
                        )
                        val lyricsHelper = entryPoint.lyricsHelper()
                        val fetchedResult = lyricsHelper.getLyrics(resolvedMetadata)

                        val fetchedLyrics = fetchedResult.lyrics
                        val pName = fetchedResult.providerName

                        val textToSave = if (fetchedLyrics.isNotBlank() && fetchedLyrics != LYRICS_NOT_FOUND) {
                            "[provider:$pName]\n$fetchedLyrics"
                        } else {
                            LYRICS_NOT_FOUND
                        }

                        val entity = LyricsEntity(id, textToSave)
                        database.query {
                            upsert(entity)
                        }

                        withContext(Dispatchers.Main) {
                            currentLyricsEntity = entity
                            lyricsCache = lyricsCache + (id to entity)
                        }
                    } catch (e: Throwable) {
                        Timber.e(e, "Error fetching lyrics in LyricsV2")
                        val errorEntity = LyricsEntity(id, LYRICS_NOT_FOUND)
                        withContext(Dispatchers.Main) {
                            currentLyricsEntity = errorEntity
                            lyricsCache = lyricsCache + (id to errorEntity)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception in LyricsV2 lyrics loader")
                val errorEntity = LyricsEntity(id, LYRICS_NOT_FOUND)
                withContext(Dispatchers.Main) {
                    currentLyricsEntity = errorEntity
                    lyricsCache = lyricsCache + (id to errorEntity)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingLyrics = false
                }
            }
        }
    }

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val animateLyrics by rememberPreference(AnimateLyricsKey, true)
    val currentSkipSegments by playerConnection.currentSkipSegments.collectAsState()
    val sponsorBlockEnabled by playerConnection.sponsorBlockEnabled.collectAsState()

    val rawLyricsText = activeLyricsEntity?.lyrics
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

    val lyricsProviderName = remember(rawLyricsText) {
        val text = rawLyricsText?.trim()
        if (text != null && text.startsWith("[provider:")) {
            text.substringBefore('\n').trim().removePrefix("[provider:").removeSuffix("]")
        } else {
            null
        }
    }

    val isSynced = remember(originalLyrics) {
        !originalLyrics.isNullOrEmpty() && (
            "\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]".toRegex().containsMatchIn(originalLyrics) ||
            "<\\d\\d:\\d\\d\\.\\d{2,3}>".toRegex().containsMatchIn(originalLyrics)
        )
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

    var currentMainLineIndex by remember(songId) { mutableIntStateOf(-1) }
    var previousMainLineIndex by remember(songId) { mutableIntStateOf(-1) }
    val lazyListState = rememberLazyListState()

    // Sync position tracking
    LaunchedEffect(songId, originalLyrics, lyricsOffsetMs, currentSkipSegments, sponsorBlockEnabled) {
        if (originalLyrics.isNullOrEmpty() || !isSynced) {
            currentMainLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(50)
            val basePosition = currentPositionProvider() ?: playerConnection.player.currentPosition
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

    suspend fun scrollToCenter(targetIndex: Int, animate: Boolean = true) {
        if (targetIndex !in lines.indices) return
        val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
        val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
        var itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }

        if (itemInfo == null) {
            val centerOffset = if (viewportHeight > 0) -(viewportHeight / 2) else 0
            lazyListState.scrollToItem(targetIndex, centerOffset)
            delay(20)
            itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        }

        if (itemInfo != null) {
            val itemCenter = itemInfo.offset + itemInfo.size / 2
            val offset = itemCenter - center
            if (abs(offset) > 10) {
                lazyListState.animateScrollBy(
                    value = offset.toFloat(),
                    animationSpec = if (animate && animateLyrics) tween(800, easing = FastOutSlowInEasing) else tween(1)
                )
            }
        }
    }

    // Auto-scroll to current lyric line
    LaunchedEffect(currentMainLineIndex) {
        if (!isSynced || !scrollLyrics || currentMainLineIndex == -1) return@LaunchedEffect
        if (currentMainLineIndex != previousMainLineIndex) {
            previousMainLineIndex = currentMainLineIndex

            if (lazyListState.layoutInfo.viewportEndOffset <= 0) {
                delay(50)
            }

            scrollToCenter(currentMainLineIndex)
        }
    }

    LaunchedEffect(songId) {
        currentMainLineIndex = -1
        previousMainLineIndex = -1
        if (lines.isNotEmpty()) {
            lazyListState.scrollToItem(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .fadingEdge(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lines.isEmpty()) {
            if (isLoadingLyrics || activeLyricsEntity == null) {
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
                    val isAssociatedBg = item.isBackground &&
                            currentMainLineIndex >= 0 &&
                            index > currentMainLineIndex &&
                            lines.subList(currentMainLineIndex + 1, index + 1).all { it.isBackground }
                    val isActiveLine = (index == currentMainLineIndex || isAssociatedBg) && isSynced
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
                                var targetTime = item.time - lyricsOffsetMs
                                if (sponsorBlockEnabled) {
                                    for (segment in currentSkipSegments) {
                                        if (targetTime >= segment.first) {
                                            targetTime += (segment.second - segment.first)
                                        }
                                    }
                                }
                                playerConnection.player.seekTo(targetTime.coerceAtLeast(0L))
                                coroutineScope.launch {
                                    scrollToCenter(index)
                                }
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

                if (!lyricsProviderName.isNullOrBlank() && originalLyrics != LYRICS_NOT_FOUND) {
                    item(key = "provider_credit") {
                        Text(
                            text = stringResource(R.string.lyrics_provided_by, lyricsProviderName),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
