package com.cgens67.avidtune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.cgens67.avidtune.ui.utils.formatFileSize
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Entrance Animation State
    var animateEntrance by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateEntrance = true
    }

    val entranceOffset by animateFloatAsState(
        targetValue = if (animateEntrance) 0f else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "entranceOffset"
    )

    val entranceAlpha by animateFloatAsState(
        targetValue = if (animateEntrance) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "entranceAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer {
                    translationY = entranceOffset
                    alpha = entranceAlpha
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Updated Dashboard Card with dynamic Donut/Pie Chart
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
                            else formatFileSize(it * 1024 * 1024L)
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
                            else formatFileSize(it * 1024 * 1024L)
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.total_managed_space),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            AnimatedDonutChart(
                downloadSize = downloadCacheSize,
                songCacheSize = playerCacheSize,
                imageCacheSize = imageCacheSize,
                total = totalUsedBytes
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    total: Long,
    modifier: Modifier = Modifier
) {
    val dPct = if (total > 0L) downloadSize.toFloat() / total else 0f
    val sPct = if (total > 0L) songCacheSize.toFloat() / total else 0f
    val iPct = if (total > 0L) imageCacheSize.toFloat() / total else 0f

    // Spring animations on each segment target sweep percentage
    val animatedDPct by animateFloatAsState(
        targetValue = dPct,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "downloadPercent"
    )
    val animatedSPct by animateFloatAsState(
        targetValue = sPct,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "songPercent"
    )
    val animatedIPct by animateFloatAsState(
        targetValue = iPct,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "imagePercent"
    )

    // Animated byte count internally
    val animatedTotalBytes by animateFloatAsState(
        targetValue = total.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "totalBytes"
    )

    var isMounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isMounted = true
    }

    // Chart entry transitions (scale and rotation)
    val scale by animateFloatAsState(
        targetValue = if (isMounted) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "chartScale"
    )
    val entryRotation by animateFloatAsState(
        targetValue = if (isMounted) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "chartRotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .size(200.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = entryRotation
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            if (total == 0L) {
                drawCircle(
                    color = outlineColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                var startAngle = -90f

                // Draw Arcs with round caps
                val sweep1 = animatedDPct * 360f
                if (sweep1 > 0f) {
                    drawArc(
                        color = primaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweep1,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweep1
                }

                val sweep2 = animatedSPct * 360f
                if (sweep2 > 0f) {
                    drawArc(
                        color = secondaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweep2,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweep2
                }

                val sweep3 = animatedIPct * 360f
                if (sweep3 > 0f) {
                    drawArc(
                        color = tertiaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweep3,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Inner label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.total_managed_space),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatFileSize(animatedTotalBytes.toLong()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
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
            text = formatFileSize(size),
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
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
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
                            stringResource(R.string.limit_info, formatFileSize(usedSize), formatFileSize(maxSize))
                        } else {
                            formatFileSize(usedSize)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Bouncy scale feedback when clicked or pressed
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "actionButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom scale handling provides the visual feedback
                onClick = onClick
            )
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
                    text = formatFileSize(songInfo.size),
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
