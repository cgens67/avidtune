package com.cgens67.avidtune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.imageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.MaxImageCacheSizeKey
import com.cgens67.avidtune.constants.MaxSongCacheSizeKey
import com.cgens67.avidtune.constants.ThumbnailCornerRadius
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.tryOrNull
import com.cgens67.avidtune.ui.component.IconButton as AppIconButton
import com.cgens67.avidtune.ui.component.ListPreference
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.sqrt

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = -1
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = -1
    )

    var imageCacheSize by remember { mutableLongStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableLongStateOf(tryOrNull { playerCache.cacheSpace } ?: 0L) }
    var downloadCacheSize by remember { mutableLongStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0L) }

    var showCachedSongsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(500)
            playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0L
        }
    }
    LaunchedEffect(downloadCache) {
        while (isActive) {
            delay(500)
            downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0L
        }
    }

    val totalUsedBytes = downloadCacheSize + playerCacheSize + imageCacheSize

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New Dashboard Card with fluid animated pie chart
            StorageDashboardCard(
                totalUsedBytes = totalUsedBytes,
                downloadCacheSize = downloadCacheSize,
                playerCacheSize = playerCacheSize,
                imageCacheSize = imageCacheSize
            )

            Text(
                text = stringResource(R.string.manage_storage_categories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            // Downloads Category Card
            ModernStorageCard(
                title = stringResource(R.string.downloaded_songs),
                icon = R.drawable.download,
                usedSize = downloadCacheSize,
                maxSize = null,
                indicatorColor = MaterialTheme.colorScheme.primary,
                onClearClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            downloadCache.keys.toList().forEach { key ->
                                tryOrNull { downloadCache.removeResource(key) }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onManageClick = null
            )

            // Song Cache Category Card
            ModernStorageCard(
                title = stringResource(R.string.song_cache),
                icon = R.drawable.music_note,
                usedSize = playerCacheSize,
                maxSize = if (maxSongCacheSize == -1) -1L else maxSongCacheSize * 1024 * 1024L,
                indicatorColor = MaterialTheme.colorScheme.secondary,
                onClearClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            playerCache.keys.toList().forEach { key ->
                                tryOrNull { playerCache.removeResource(key) }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onManageClick = { showCachedSongsSheet = true },
                limitSelectionContent = {
                    ListPreference(
                        title = { Text(stringResource(R.string.max_cache_size)) },
                        selectedValue = maxSongCacheSize,
                        values = listOf(-1, 128, 256, 512, 1024, 2048, 4096, 8192),
                        valueText = {
                            if (it == -1) stringResource(R.string.unlimited)
                            else formatStorageSize(it * 1024 * 1024L)
                        },
                        onValueSelected = onMaxSongCacheSizeChange,
                    )
                }
            )

            // Image Cache Category Card
            ModernStorageCard(
                title = stringResource(R.string.image_cache),
                icon = R.drawable.image,
                usedSize = imageCacheSize,
                maxSize = if (maxImageCacheSize == -1) -1L else maxImageCacheSize * 1024 * 1024L,
                indicatorColor = MaterialTheme.colorScheme.tertiary,
                onClearClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            imageDiskCache.clear()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onManageClick = null,
                limitSelectionContent = {
                    ListPreference(
                        title = { Text(stringResource(R.string.max_cache_size)) },
                        selectedValue = maxImageCacheSize,
                        values = listOf(-1, 128, 256, 512, 1024, 2048, 4096, 8192),
                        valueText = {
                            if (it == -1) stringResource(R.string.unlimited)
                            else formatStorageSize(it * 1024 * 1024L)
                        },
                        onValueSelected = onMaxImageCacheSizeChange,
                    )
                }
            )
        }

        TopAppBar(
            title = { Text(stringResource(R.string.storage)) },
            navigationIcon = {
                AppIconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )
    }

    if (showCachedSongsSheet) {
        CachedSongsBottomSheet(
            playerCache = playerCache,
            viewModel = viewModel,
            onDismiss = { showCachedSongsSheet = false }
        )
    }
}

@Composable
private fun StorageDashboardCard(
    totalUsedBytes: Long,
    downloadCacheSize: Long,
    playerCacheSize: Long,
    imageCacheSize: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive and smooth Canvas Pie Chart
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedDonutChart(
                        downloadSize = downloadCacheSize,
                        songCacheSize = playerCacheSize,
                        imageCacheSize = imageCacheSize
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.total_managed_space),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatStorageSize(totalUsedBytes),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 28.sp
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendRow(
                    label = stringResource(R.string.downloaded_songs),
                    size = downloadCacheSize,
                    color = MaterialTheme.colorScheme.primary
                )
                LegendRow(
                    label = stringResource(R.string.song_cache),
                    size = playerCacheSize,
                    color = MaterialTheme.colorScheme.secondary
                )
                LegendRow(
                    label = stringResource(R.string.image_cache),
                    size = imageCacheSize,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun AnimatedDonutChart(
    downloadSize: Long,
    songCacheSize: Long,
    imageCacheSize: Long,
    modifier: Modifier = Modifier
) {
    var selectedSegmentIndex by remember { mutableStateOf(-1) }

    val total = downloadSize + songCacheSize + imageCacheSize
    val dTarget = if (total > 0L) downloadSize.toFloat() / total else 0f
    val sTarget = if (total > 0L) songCacheSize.toFloat() / total else 0f
    val iTarget = if (total > 0L) imageCacheSize.toFloat() / total else 0f

    // Morph sizes smoothly without bounce on updates
    val dWeight by animateFloatAsState(
        targetValue = dTarget,
        animationSpec = tween(1200, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
        label = "dWeight"
    )
    val sWeight by animateFloatAsState(
        targetValue = sTarget,
        animationSpec = tween(1200, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
        label = "sWeight"
    )
    val iWeight by animateFloatAsState(
        targetValue = iTarget,
        animationSpec = tween(1200, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
        label = "iWeight"
    )

    // Highlight segments by animating their widths on selection
    val dStroke by animateFloatAsState(
        targetValue = if (selectedSegmentIndex == 0) 22f else 14f,
        animationSpec = tween(350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
        label = "dStroke"
    )
    val sStroke by animateFloatAsState(
        targetValue = if (selectedSegmentIndex == 1) 22f else 14f,
        animationSpec = tween(350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
        label = "sStroke"
    )
    val iStroke by animateFloatAsState(
        targetValue = if (selectedSegmentIndex == 2) 22f else 14f,
        animationSpec = tween(350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
        label = "iStroke"
    )

    // Entry transition states
    var animationPlayed by remember { mutableStateOf(false) }
    val sweepProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f)),
        label = "sweepProgress"
    )
    val entryRotation by animateFloatAsState(
        targetValue = if (animationPlayed) 0f else -180f,
        animationSpec = tween(durationMillis = 1500, easing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f)),
        label = "entryRotation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    // Gentle continuous slow rotation to keep the dashboard dynamic
    val infiniteTransition = rememberInfiniteTransition(label = "pieRotation")
    val idleRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idleRotation"
    )

    val rotationAngle = entryRotation + idleRotationAngle

    // Extract colors outside Canvas scope to prevent @Composable context compilation errors [1]
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val centerText = when (selectedSegmentIndex) {
        0 -> stringResource(R.string.downloaded_songs)
        1 -> stringResource(R.string.song_cache)
        2 -> stringResource(R.string.image_cache)
        else -> "Used"
    }
    val centerValue = when (selectedSegmentIndex) {
        0 -> formatStorageSize(downloadSize)
        1 -> formatStorageSize(songCacheSize)
        2 -> formatStorageSize(imageCacheSize)
        else -> formatStorageSize(total)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rotationAngle, dWeight, sWeight, iWeight) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val x = offset.x
                        val y = offset.y

                        val distance = sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY))
                        val outerRadius = size.width / 2f
                        val innerRadius = outerRadius - 40.dp.toPx()

                        if (distance in innerRadius..outerRadius) {
                            val rawAngle = Math.toDegrees(atan2((y - centerY).toDouble(), (x - centerX).toDouble())).toFloat()
                            val angle = (rawAngle + 360f) % 360f
                            val adjustedAngle = (angle - rotationAngle % 360f + 360f) % 360f

                            val totalW = dWeight + sWeight + iWeight
                            if (totalW > 0f) {
                                val activeSegments = listOf(
                                    Triple(0, dWeight, dStroke),
                                    Triple(1, sWeight, sStroke),
                                    Triple(2, iWeight, iStroke)
                                ).filter { it.second > 0f }

                                val numSegments = activeSegments.size
                                val gapAngle = if (numSegments > 1) 14f else 0f
                                val availableSweep = 360f - (numSegments * gapAngle)

                                var currentAngle = 0f
                                var clickedIndex = -1

                                for (segment in activeSegments) {
                                    val sweep = (segment.second / totalW) * availableSweep
                                    val start = currentAngle + (gapAngle / 2f)
                                    val end = start + sweep
                                    if (adjustedAngle in start..end) {
                                        clickedIndex = segment.first
                                        break
                                    }
                                    currentAngle += sweep + gapAngle
                                }

                                selectedSegmentIndex = if (selectedSegmentIndex == clickedIndex) -1 else clickedIndex
                            }
                        } else {
                            selectedSegmentIndex = -1
                        }
                    }
                }
        ) {
            val totalW = dWeight + sWeight + iWeight

            if (totalW == 0f) {
                drawArc(
                    color = outlineVariantColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                val activeSegments = listOf(
                    Triple(primaryColor, dWeight, dStroke),
                    Triple(secondaryColor, sWeight, sStroke),
                    Triple(tertiaryColor, iWeight, iStroke)
                ).filter { it.second > 0f }

                val numSegments = activeSegments.size
                val gapAngle = if (numSegments > 1) 14f else 0f
                val availableSweep = 360f - (numSegments * gapAngle)

                var currentAngle = rotationAngle

                activeSegments.forEach { (color, weight, strokeWidth) ->
                    val sweepAngle = (weight / totalW) * availableSweep * sweepProgress
                    val drawStartAngle = currentAngle + (gapAngle / 2f)

                    drawArc(
                        color = color,
                        startAngle = drawStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
                    )

                    currentAngle += sweepAngle + gapAngle
                }
            }
        }

        // Center Details Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = centerValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (selectedSegmentIndex) {
                    0 -> primaryColor
                    1 -> secondaryColor
                    2 -> tertiaryColor
                    else -> onSurfaceColor
                },
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SegmentedStorageBar(
    downloadSize: Long,
    songCacheSize: Long,
    imageCacheSize: Long,
    modifier: Modifier = Modifier
) {
    val total = downloadSize + songCacheSize + imageCacheSize
    val dWeight = if (total > 0L) downloadSize.toFloat() / total else 0f
    val sWeight = if (total > 0L) songCacheSize.toFloat() / total else 0f
    val iWeight = if (total > 0L) imageCacheSize.toFloat() / total else 0f

    val dWeightAnimated by animateFloatAsState(targetValue = dWeight, label = "dWeight")
    val sWeightAnimated by animateFloatAsState(targetValue = sWeight, label = "sWeight")
    val iWeightAnimated by animateFloatAsState(targetValue = iWeight, label = "iWeight")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (total == 0L) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        } else {
            if (dWeightAnimated > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(dWeightAnimated)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            if (sWeightAnimated > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(sWeightAnimated)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
            if (iWeightAnimated > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(iWeightAnimated)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

@Composable
private fun LegendRow(
    label: String,
    size: Long,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = formatStorageSize(size),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModernStorageCard(
    title: String,
    icon: Int,
    usedSize: Long,
    maxSize: Long?,
    indicatorColor: Color,
    onClearClick: () -> Unit,
    onManageClick: (() -> Unit)? = null,
    limitSelectionContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(indicatorColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = indicatorColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (maxSize != null && maxSize > 0) {
                            stringResource(R.string.limit_info, formatStorageSize(usedSize), formatStorageSize(maxSize))
                        } else {
                            formatStorageSize(usedSize)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            limitSelectionContent?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                ) {
                    it()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onManageClick != null) {
                    StorageActionButton(
                        text = stringResource(R.string.manage),
                        icon = R.drawable.settings,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onManageClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                StorageActionButton(
                    text = stringResource(R.string.clear),
                    icon = R.drawable.delete,
                    color = MaterialTheme.colorScheme.error,
                    onClick = onClearClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StorageActionButton(
    text: String,
    icon: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CachedSongsBottomSheet(
    playerCache: androidx.media3.datasource.cache.Cache,
    viewModel: HistoryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val events by viewModel.events.collectAsState()

    val cachedSongIds = remember(playerCache) {
        playerCache.keys.map { it.toString() }.toSet()
    }

    val cachedSongs = remember(events, cachedSongIds) {
        events.values.flatten()
            .map { it.song }
            .distinctBy { it.id }
            .filter { it.id in cachedSongIds }
    }

    val cachedSongsWithSize = remember(cachedSongs, playerCache) {
        cachedSongs.mapNotNull { song ->
            val size = tryOrNull {
                playerCache.getCachedBytes(song.id, 0, Long.MAX_VALUE)
            } ?: 0L

            if (size > 0) {
                CachedSongInfo(song = song, size = size)
            } else null
        }.sortedByDescending { it.size }
    }

    var displayedSongs by remember { mutableStateOf(cachedSongsWithSize) }

    LaunchedEffect(cachedSongsWithSize) {
        displayedSongs = cachedSongsWithSize
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset(0f, available.y)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.cached_playlist),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.n_song,
                            displayedSongs.size,
                            displayedSongs.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (displayedSongs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    playerCache.keys.toList().forEach { key ->
                                        tryOrNull { playerCache.removeResource(key) }
                                    }
                                    withContext(Dispatchers.Main) {
                                        displayedSongs = emptyList()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.clear_all_downloads),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedSongs, key = { it.song.id }) { songInfo ->
                    CachedSongItem(
                        songInfo = songInfo,
                        onDeleteClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val keysToRemove = playerCache.keys.filter { key ->
                                        key.contains(songInfo.song.id)
                                    }

                                    keysToRemove.forEach { key ->
                                        tryOrNull { playerCache.removeResource(key) }
                                    }

                                    withContext(Dispatchers.Main) {
                                        displayedSongs = displayedSongs.filter {
                                            it.song.id != songInfo.song.id
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CachedSongItem(
    songInfo: CachedSongInfo,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = songInfo.song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = songInfo.song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = songInfo.song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = formatStorageSize(songInfo.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.clear_song_cache),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class CachedSongInfo(
    val song: Song,
    val size: Long
)

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return if (unitIndex == 0) {
        "$bytes B"
    } else {
        // String.format with Locale.US ensures '.' is universally used as the decimal separator.
        val formattedSize = String.format(java.util.Locale.US, "%.2f", size)
        // Trim trailing zeros and then trailing decimal point if number happens to be whole.
        val cleanSize = formattedSize.trimEnd('0').trimEnd('.')
        "$cleanSize ${units[unitIndex]}"
    }
}
