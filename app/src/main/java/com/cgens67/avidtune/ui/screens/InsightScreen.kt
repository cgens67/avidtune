package com.cgens67.avidtune.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PathEffect
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.utils.ComposeToImage
import com.cgens67.avidtune.viewmodels.InsightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun InsightScreen(
    navController: NavController,
    viewModel: InsightViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val topSongs by viewModel.topSongs.collectAsState()
    val topSongStats by viewModel.topSongStats.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val totalMinutes by viewModel.totalMinutes.collectAsState()

    val playerConnection = LocalPlayerConnection.current

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (topSongs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.insight_no_data),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = { navController.navigateUp() }, modifier = Modifier.padding(top = 64.dp)) {
                Text(stringResource(R.string.insight_close))
            }
        }
        return
    }

    val pageCount = 5
    var currentPage by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Control auto-advance
    var currentProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentPage, isPaused) {
        currentProgress = 0f
        if (!isPaused && currentPage < pageCount - 1) {
            val duration = 5000f
            val startTime = withFrameMillis { it }
            while (currentProgress < 1f) {
                withFrameMillis { time ->
                    currentProgress = (time - startTime) / duration
                }
            }
            currentPage++
        } else if (currentPage == pageCount - 1) {
            currentProgress = 1f // Stay filled on last page
        }
    }

    // Dynamic gradient transitions
    val pageColors = listOf(
        Color(0xFF8E2DE2) to Color(0xFF4A00E0), // Intro: Purple
        Color(0xFF00b09b) to Color(0xFF96c93d), // Time: Green
        Color(0xFFFF512F) to Color(0xFFDD2476), // Top Song: Pink/Red
        Color(0xFFcb2d3e) to Color(0xFFef473a), // Artists: Red
        Color(0xFF141E30) to Color(0xFF243B55)  // Summary: Dark Blue
    )

    val currentColors = pageColors[currentPage.coerceIn(0, pageColors.lastIndex)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(currentColors.first, currentColors.second)))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val isRight = offset.x > size.width / 2
                        coroutineScope.launch {
                            if (isRight) {
                                if (currentPage < pageCount - 1) {
                                    currentPage++
                                }
                            } else {
                                if (currentPage > 0) {
                                    currentPage--
                                }
                            }
                        }
                    }
                )
            }
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> width } + fadeIn(tween(400)) togetherWith
                            slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> -width } + fadeOut(tween(400))
                } else {
                    slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> -width } + fadeIn(tween(400)) togetherWith
                            slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> width } + fadeOut(tween(400))
                }
            },
            label = "InsightPages",
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> IntroPage(topArtists.firstOrNull()?.artist?.name, isActive = true)
                1 -> TotalTimePage(totalMinutes, isActive = true)
                2 -> TopSongPage(topSongs.firstOrNull(), topSongStats?.songCountListened ?: 0, isActive = true)
                3 -> TopArtistsPage(topArtists, isActive = true)
                4 -> SummaryPage(topSongs, totalMinutes, topArtists.firstOrNull()?.artist?.name, isActive = true, playerConnection)
            }
        }

        // Top Progress Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until pageCount) {
                val progress = when {
                    i < currentPage -> 1f
                    i == currentPage -> currentProgress
                    else -> 0f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(Color.White)
                    )
                }
            }
        }

        // Close button top right
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(top = 28.dp, end = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.insight_close),
                tint = Color.White
            )
        }
    }
}

@Composable
fun IntroPage(topArtistName: String?, isActive: Boolean) {
    var phase by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            phase = 0
            kotlinx.coroutines.delay(1500)
            phase = 1
            kotlinx.coroutines.delay(800)
            phase = 2
        } else {
            phase = 0
        }
    }

    // Meteor animations
    val meteorProgress by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "meteor"
    )
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = phase == 0, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.insight_intro_ready), fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.insight_intro_ready_2), fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        if (phase >= 1 && phase < 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = size.width + 200f
                val startY = -200f
                val endX = -200f
                val endY = size.height + 200f
                
                val currentX = lerp(startX, endX, meteorProgress)
                val currentY = lerp(startY, endY, meteorProgress)
                
                drawLine(
                    color = Color.White,
                    start = Offset(currentX + 150f, currentY - 150f),
                    end = Offset(currentX, currentY),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.Yellow,
                    radius = 20f,
                    center = Offset(currentX, currentY)
                )
            }
        }
        
        AnimatedVisibility(
            visible = phase == 2, 
            enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.6f)), 
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.insight_top_artist_reveal), fontSize = 24.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(16.dp))
                Text(topArtistName ?: "N/A", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun TotalTimePage(minutes: Long, isActive: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "scale"
    )

    val trips = maxOf(1L, minutes / 280L)
    val comparison = stringResource(R.string.insight_time_comparison, trips)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isActive, enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.padding(32.dp).scale(scale)
            ) {
                Text(
                    text = stringResource(R.string.insight_minutes_spent),
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = "$minutes",
                    fontSize = 80.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.insight_minutes),
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.insight_minutes_listening),
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = comparison,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TopSongPage(song: com.cgens67.avidtune.db.entities.Song?, playCount: Int, isActive: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isActive, enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = stringResource(R.string.insight_top_song_was),
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(32.dp))
                AsyncImage(
                    model = song?.song?.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(250.dp)
                        .scale(scale)
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = song?.song?.title ?: "",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artists?.joinToString { it.name } ?: "",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "${stringResource(R.string.insight_with)} $playCount ${stringResource(R.string.insight_plays)}",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TopArtistsPage(artists: List<com.cgens67.avidtune.db.entities.Artist>, isActive: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isActive, enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(32.dp)) {
                Text(
                    text = stringResource(R.string.insight_artists_title),
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                artists.take(5).forEachIndexed { index, artist ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            fontSize = 24.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp)
                        )
                        AsyncImage(
                            model = artist.artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = artist.artist.name,
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryPage(topSongs: List<com.cgens67.avidtune.db.entities.Song>, totalMinutes: Long, topArtistName: String?, isActive: Boolean, playerConnection: PlayerConnection?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isActive, enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // "Receipt" Style Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .background(Color(0xFFFDFDFD), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.insight_summary_title),
                            fontSize = 22.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.insight_minutes_spent).uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("$totalMinutes", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.insight_artists_title).replace("Your", "Top").uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(topArtistName ?: "N/A", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp), textAlign = TextAlign.End)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("TOP 5", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(Modifier.height(8.dp))
                        
                        topSongs.take(5).forEachIndexed { index, song ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                                Text(
                                    text = song.song.title,
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Fake barcode
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            val bars = listOf(2, 4, 1, 3, 2, 5, 1, 2, 4, 2, 1, 3, 2, 4)
                            bars.forEach { width ->
                                Box(modifier = Modifier.padding(horizontal = 1.dp).width(width.dp).height(30.dp).background(Color.Black))
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                val topSongText = topSongs.firstOrNull()?.song?.title ?: "N/A"
                                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.insight_share_text, topArtistName ?: "N/A", totalMinutes, topSongText))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.insight_share)))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Icon(painter = painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.insight_share), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                downloadInsightReport(context, topSongs, totalMinutes, topArtistName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Icon(painter = painterResource(R.drawable.download), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.insight_download), fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { 
                        playerConnection?.playQueue(
                            ListQueue(
                                title = "AvidTune Insight",
                                items = topSongs.map { it.toMediaItem() }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(painter = painterResource(R.drawable.play), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.insight_play), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun downloadInsightReport(context: android.content.Context, topSongs: List<com.cgens67.avidtune.db.entities.Song>, totalMinutes: Long, topArtist: String?) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color(0xFF141E30).toArgb(), Color(0xFF243B55).toArgb(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        canvas.drawText("AvidTune Insight 2026", width / 2f, 250f, textPaint)
        
        textPaint.textSize = 45f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = android.graphics.Color.LTGRAY
        canvas.drawText("Total Listening Time", width / 2f, 450f, textPaint)
        
        textPaint.textSize = 100f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.WHITE
        canvas.drawText("$totalMinutes Minutes", width / 2f, 570f, textPaint)
        
        textPaint.textSize = 45f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = android.graphics.Color.LTGRAY
        canvas.drawText("Top Artist", width / 2f, 750f, textPaint)
        
        textPaint.textSize = 90f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.WHITE
        canvas.drawText(topArtist ?: "N/A", width / 2f, 870f, textPaint)
        
        textPaint.textSize = 45f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = android.graphics.Color.LTGRAY
        canvas.drawText("Top Songs", width / 2f, 1050f, textPaint)
        
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 55f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.WHITE
        var y = 1180f
        topSongs.take(5).forEachIndexed { index, song ->
            canvas.drawText("${index + 1}. ${song.song.title}", 150f, y, textPaint)
            y += 100f
        }
        
        ComposeToImage.saveBitmapAsFile(context, bitmap, "AvidTune_Insight_${System.currentTimeMillis()}")
        
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, context.getString(R.string.insight_downloaded_toast), Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}