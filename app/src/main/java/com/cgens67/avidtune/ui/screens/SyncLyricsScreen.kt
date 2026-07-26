package com.cgens67.avidtune.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cgens67.avidtune.R
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.ui.utils.fadingEdge
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLyricsScreen(
    lyricsText: String,
    playerConnection: PlayerConnection,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val plainLines = remember(lyricsText) {
        val clean = if (lyricsText.startsWith("[provider:")) {
            lyricsText.substringAfter('\n')
        } else {
            lyricsText
        }
        clean.lines().map { it.replace(Regex("\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]"), "").trim() }.filter { it.isNotEmpty() }
    }

    val timestamps = remember { mutableStateMapOf<Int, Long>() }
    var currentIndex by remember { mutableIntStateOf(0) }

    var currentPosition by remember { mutableLongStateOf(playerConnection.player.currentPosition) }
    val isPlaying by playerConnection.isPlaying.collectAsState()

    LaunchedEffect(playerConnection.playbackState.collectAsState().value, isPlaying) {
        while (isActive) {
            currentPosition = playerConnection.player.currentPosition
            delay(50)
        }
    }

    val listState = rememberLazyListState()

    // Smoothly auto-scroll to keep the current item in view (slightly above center)
    LaunchedEffect(currentIndex) {
        if (currentIndex in plainLines.indices) {
            coroutineScope.launch {
                listState.animateScrollToItem(max(0, currentIndex - 2))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Background Layer: Heavily blurred album art
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Gradient Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Bar
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Sync Lyrics",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(R.drawable.close),
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val providerLine = if (lyricsText.startsWith("[provider:")) {
                                    lyricsText.substringBefore('\n') + "\n"
                                } else ""

                                val syncedText = plainLines.mapIndexed { index, line ->
                                    val time = timestamps[index]
                                    if (time != null) {
                                        val min = time / 60000
                                        val sec = (time % 60000) / 1000
                                        val ms = (time % 1000) / 10
                                        String.format(Locale.US, "[%02d:%02d.%02d]%s", min, sec, ms, line)
                                    } else {
                                        line
                                    }
                                }.joinToString("\n")

                                onSave(providerLine + syncedText.trimStart('\n'))
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                // Lyrics List with smooth top/bottom edge fade
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fadingEdge(vertical = 48.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(plainLines) { index, line ->
                        val isCurrent = index == currentIndex
                        val isSynced = timestamps.containsKey(index)
                        val time = timestamps[index]

                        val scale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.05f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "scale"
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else if (isSynced) 0.5f else 0.3f,
                            animationSpec = tween(300),
                            label = "alpha"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isCurrent) Color.White.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .clickable {
                                    if (isSynced) {
                                        playerConnection.player.seekTo(time!!)
                                    } else {
                                        currentIndex = index
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(
                                visible = isSynced,
                                enter = fadeIn(tween(300)) + expandHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                                exit = fadeOut(tween(300)) + shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium))
                            ) {
                                Text(
                                    text = if (isSynced) {
                                        val min = time!! / 60000
                                        val sec = (time % 60000) / 1000
                                        val ms = (time % 1000) / 10
                                        String.format(Locale.US, "[%02d:%02d.%02d]", min, sec, ms)
                                    } else "",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .width(85.dp) // Fixed width to prevent wrapping issues
                                )
                            }
                            
                            Text(
                                text = line.ifBlank { "..." },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bottom Panel Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    // Current time
                    Text(
                        text = com.cgens67.avidtune.utils.makeTimeString(currentPosition),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(24.dp))

                    // Playback Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // -2s Seek Back
                        val seekBackInteractionSource = remember { MutableInteractionSource() }
                        val seekBackIsPressed by seekBackInteractionSource.collectIsPressedAsState()
                        val seekBackScale by animateFloatAsState(if (seekBackIsPressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessMedium))
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(seekBackScale)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable(
                                    interactionSource = seekBackInteractionSource,
                                    indication = androidx.compose.material3.ripple(bounded = false)
                                ) {
                                    playerConnection.player.seekTo(maxOf(0L, currentPosition - 2000L))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-2s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        // Play/Pause
                        val playPauseInteractionSource = remember { MutableInteractionSource() }
                        val playPauseIsPressed by playPauseInteractionSource.collectIsPressedAsState()
                        val playPauseScale by animateFloatAsState(if (playPauseIsPressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessMedium))

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(playPauseScale)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(
                                    interactionSource = playPauseInteractionSource,
                                    indication = androidx.compose.material3.ripple(bounded = false)
                                ) {
                                    playerConnection.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Undo
                        val undoInteractionSource = remember { MutableInteractionSource() }
                        val undoIsPressed by undoInteractionSource.collectIsPressedAsState()
                        val undoScale by animateFloatAsState(if (undoIsPressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessMedium))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(undoScale)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable(
                                    interactionSource = undoInteractionSource,
                                    indication = androidx.compose.material3.ripple(bounded = false),
                                    enabled = currentIndex > 0
                                ) {
                                    if (currentIndex > 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentIndex--
                                        timestamps.remove(currentIndex)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = "Undo",
                                tint = if (currentIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Big Sync Button
                    val syncInteractionSource = remember { MutableInteractionSource() }
                    val syncIsPressed by syncInteractionSource.collectIsPressedAsState()
                    val syncScale by animateFloatAsState(if (syncIsPressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium))
                    val isSyncEnabled = currentIndex < plainLines.size

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .scale(syncScale)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSyncEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = syncInteractionSource,
                                indication = androidx.compose.material3.ripple(),
                                enabled = isSyncEnabled
                            ) {
                                if (isSyncEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    timestamps[currentIndex] = currentPosition
                                    currentIndex++
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SYNC NEXT LINE",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = if (isSyncEnabled) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
