package com.cgens67.avidtune.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.db.entities.LyricsEntity
import com.cgens67.avidtune.extensions.togglePlayPause
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLyricsScreen(navController: NavController) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val songId = navController.currentBackStackEntry?.arguments?.getString("songId") ?: return
    val song by database.song(songId).collectAsState(initial = null)
    val lyricsEntity by database.lyrics(songId).collectAsState(initial = null)
    
    val rawText = lyricsEntity?.lyrics.orEmpty()
    val cleanText = if (rawText.startsWith("[provider:")) rawText.substringAfter('\n') else rawText
    
    val plainLines = remember(cleanText) {
        cleanText.lines().map { it.replace(Regex("\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]"), "").trim() }.filter { it.isNotEmpty() }
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
    LaunchedEffect(currentIndex) {
        if (currentIndex in plainLines.indices) {
            listState.animateScrollToItem(maxOf(0, currentIndex - 2))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Sync Lyrics", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.close), contentDescription = "Close", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val providerLine = if (rawText.startsWith("[provider:")) {
                            rawText.substringBefore('\n') + "\n"
                        } else ""

                        val syncedText = plainLines.mapIndexed { index, line ->
                            val time = timestamps[index]
                            if (time != null) {
                                val min = time / 60000
                                val sec = (time % 60000) / 1000
                                val ms = (time % 1000) / 10
                                String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s", min, sec, ms, line)
                            } else {
                                line
                            }
                        }.joinToString("\n")

                        val finalLyrics = providerLine + syncedText.trimStart('\n')
                        database.query {
                            upsert(LyricsEntity(songId, finalLyrics))
                        }
                        navController.popBackStack()
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = com.cgens67.avidtune.utils.makeTimeString(currentPosition),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    currentIndex--
                                    timestamps.remove(currentIndex)
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(painterResource(R.drawable.skip_previous), contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        IconButton(
                            onClick = { playerConnection.player.seekTo(max(0, currentPosition - 5000)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(painterResource(R.drawable.replay_5), contentDescription = "-5s", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { playerConnection.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerConnection.player.seekTo(currentPosition + 5000) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(painterResource(R.drawable.forward_5), contentDescription = "+5s", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Spacer(Modifier.width(48.dp)) // Visual balance
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (currentIndex < plainLines.size) {
                                timestamps[currentIndex] = currentPosition
                                currentIndex++
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        enabled = currentIndex < plainLines.size
                    ) {
                        Text(
                            text = if (currentIndex < plainLines.size) "Sync Next Line" else "Finished", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Blurred Background
            AsyncImage(
                model = song?.song?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
            ) {
                itemsIndexed(plainLines) { index, line ->
                    val isCurrent = index == currentIndex
                    val isSynced = timestamps.containsKey(index)
                    val time = timestamps[index]
                    
                    val scale by animateFloatAsState(targetValue = if (isCurrent) 1.05f else 1f, animationSpec = spring())
                    val textColor by animateColorAsState(
                        targetValue = if (isCurrent) MaterialTheme.colorScheme.primary 
                        else if (isSynced) Color.White 
                        else Color.White.copy(alpha = 0.5f),
                        animationSpec = spring()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isSynced) {
                                    playerConnection.player.seekTo(time!!)
                                } else {
                                    currentIndex = index
                                }
                            }
                            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSynced) {
                                val min = time!! / 60000
                                val sec = (time % 60000) / 1000
                                val ms = (time % 1000) / 10
                                String.format(java.util.Locale.US, "[%02d:%02d.%02d]", min, sec, ms)
                            } else "[--:--.--]",
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(90.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = line.ifBlank { "..." },
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                            color = textColor,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
