@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.cgens67.avidtune.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.component.HideOnScrollFAB
import com.cgens67.avidtune.viewmodels.MoodAndGenresViewModel
import com.cgens67.innertube.pages.MoodAndGenres

@Composable
fun ExploreScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()
    val lazyGridState = rememberLazyGridState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    // 0 = All, 1+ = section index + 1
    var selectedSectionFilter by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(moodAndGenresList?.size) {
        val count = moodAndGenresList?.size ?: 0
        if (selectedSectionFilter > count) {
            selectedSectionFilter = 0
        }
    }

    val displayedSections = remember(moodAndGenresList, selectedSectionFilter) {
        val sections = moodAndGenresList ?: return@remember null
        if (selectedSectionFilter == 0) {
            sections
        } else {
            listOfNotNull(sections.getOrNull(selectedSectionFilter - 1))
        }
    }

    val allExploreItems = remember(moodAndGenresList) {
        moodAndGenresList?.flatMap { it.items }.orEmpty()
    }

    val openRandomMood: () -> Unit = {
        allExploreItems.randomOrNull()?.let { item ->
            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = insetsPadding.calculateStartPadding(layoutDirection) + 20.dp,
                            end = insetsPadding.calculateEndPadding(layoutDirection) + 20.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                top = insetsPadding.calculateTopPadding() + 8.dp,
                                bottom = insetsPadding.calculateBottomPadding() + 24.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExploreShortcutCard(
                            title = stringResource(R.string.trending),
                            subtitle = stringResource(R.string.apple_music_trending),
                            iconRes = R.drawable.trending_up,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shapePolygon = MaterialShapes.Cookie6Sided,
                            onClick = { navController.navigate("apple_music_trending") }
                        )

                        ExploreShortcutCard(
                            title = stringResource(R.string.new_releases),
                            subtitle = stringResource(R.string.new_release_albums),
                            iconRes = R.drawable.album,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shapePolygon = MaterialShapes.Clover4Leaf,
                            onClick = { navController.navigate("new_release") }
                        )

                        moodAndGenresList?.takeIf { it.size > 1 }?.let { sections ->
                            Spacer(Modifier.height(2.dp))
                            ExploreSectionToggleGroup(
                                sections = sections,
                                selectedIndex = selectedSectionFilter,
                                onSelectIndex = { selectedSectionFilter = it }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                    ) {
                        if (displayedSections == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        top = insetsPadding.calculateTopPadding(),
                                        bottom = insetsPadding.calculateBottomPadding()
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        } else {
                            AnimatedContent(
                                targetState = displayedSections,
                                transitionSpec = {
                                    (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                                        slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 16 })
                                        .togetherWith(fadeOut(tween(160)))
                                        .using(SizeTransform(clip = false))
                                },
                                label = "explore_sections_landscape"
                            ) { sections ->
                                LazyVerticalGrid(
                                    state = lazyGridState,
                                    columns = GridCells.Adaptive(minSize = 156.dp),
                                    contentPadding = PaddingValues(
                                        top = insetsPadding.calculateTopPadding() + 8.dp,
                                        bottom = insetsPadding.calculateBottomPadding() + 88.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    sections.forEachIndexed { sectionIdx, moodAndGenres ->
                                        item(
                                            key = "header_${moodAndGenres.title}_$sectionIdx",
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            ExploreSectionHeader(
                                                title = getTranslatedExploreSectionTitle(moodAndGenres.title),
                                                count = moodAndGenres.items.size,
                                                modifier = Modifier.animateItem()
                                            )
                                        }

                                        itemsIndexed(
                                            items = moodAndGenres.items,
                                            key = { idx, item -> "${moodAndGenres.title}_${item.title}_${item.endpoint.browseId}_$idx" }
                                        ) { idx, item ->
                                            MoodAndGenresCard(
                                                title = item.title,
                                                stripeColor = item.stripeColor,
                                                shapeIndex = idx + sectionIdx * 3,
                                                onClick = {
                                                    navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(
                        top = insetsPadding.calculateTopPadding() + 8.dp,
                        bottom = insetsPadding.calculateBottomPadding() + 88.dp,
                        start = insetsPadding.calculateStartPadding(layoutDirection) + 16.dp,
                        end = insetsPadding.calculateEndPadding(layoutDirection) + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(
                        key = "explore_shortcuts",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExploreShortcutCard(
                                title = stringResource(R.string.trending),
                                subtitle = stringResource(R.string.apple_music_trending),
                                iconRes = R.drawable.trending_up,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shapePolygon = MaterialShapes.Cookie6Sided,
                                onClick = { navController.navigate("apple_music_trending") },
                                modifier = Modifier.weight(1f)
                            )

                            ExploreShortcutCard(
                                title = stringResource(R.string.new_releases),
                                subtitle = stringResource(R.string.new_release_albums),
                                iconRes = R.drawable.album,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shapePolygon = MaterialShapes.Clover4Leaf,
                                onClick = { navController.navigate("new_release") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    moodAndGenresList?.takeIf { it.size > 1 }?.let { sections ->
                        item(
                            key = "explore_section_toggle",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            ExploreSectionToggleGroup(
                                sections = sections,
                                selectedIndex = selectedSectionFilter,
                                onSelectIndex = { selectedSectionFilter = it },
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (displayedSections == null) {
                        item(
                            key = "explore_loading",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    } else {
                        displayedSections.forEachIndexed { sectionIdx, moodAndGenres ->
                            item(
                                key = "header_${moodAndGenres.title}_$sectionIdx",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                ExploreSectionHeader(
                                    title = getTranslatedExploreSectionTitle(moodAndGenres.title),
                                    count = moodAndGenres.items.size,
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .animateItem()
                                )
                            }

                            itemsIndexed(
                                items = moodAndGenres.items,
                                key = { idx, item -> "${moodAndGenres.title}_${item.title}_${item.endpoint.browseId}_$idx" }
                            ) { idx, item ->
                                MoodAndGenresCard(
                                    title = item.title,
                                    stripeColor = item.stripeColor,
                                    shapeIndex = idx + sectionIdx * 3,
                                    onClick = {
                                        navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            HideOnScrollFAB(
                visible = allExploreItems.isNotEmpty(),
                lazyListState = lazyGridState,
                icon = R.drawable.shuffle,
                onClick = openRandomMood
            )
        }
    }
}

@Composable
private fun ExploreShortcutCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    shapePolygon: androidx.graphics.shapes.RoundedPolygon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shortcutScale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 28.dp else 22.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "shortcutCorner"
    )
    val badgeShape = shapePolygon.toShape()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(badgeShape)
                    .background(contentColor.copy(alpha = 0.14f))
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ExploreSectionToggleGroup(
    sections: List<MoodAndGenres>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalOptions = 1 + sections.size
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        ToggleButton(
            checked = selectedIndex == 0,
            onCheckedChange = { if (it) onSelectIndex(0) },
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier
                .weight(0.6f)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
        ) {
            Text(
                text = stringResource(R.string.filter_all),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        sections.forEachIndexed { idx, section ->
            val optionIndex = idx + 1
            val label = getTranslatedExploreSectionTitle(section.title)
            val buttonWeight = when {
                sections.size == 2 && idx == 0 -> 1.45f
                sections.size == 2 && idx == 1 -> 0.95f
                else -> 1f
            }

            ToggleButton(
                checked = selectedIndex == optionIndex,
                onCheckedChange = { if (it) onSelectIndex(optionIndex) },
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .weight(buttonWeight)
                    .semantics { role = Role.RadioButton },
                shapes = if (optionIndex == totalOptions - 1) {
                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                } else {
                    ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ExploreSectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MoodAndGenresCard(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapeIndex: Int = 0,
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (stripeColor != 0L) Color(stripeColor) else MaterialTheme.colorScheme.primary
    val surfaceFallback = MaterialTheme.colorScheme.surfaceContainerHigh

    val startColor = baseColor.copy(alpha = if (isDark) 0.62f else 0.88f).compositeOver(surfaceFallback)
    val endColor = baseColor.copy(alpha = if (isDark) 0.24f else 0.46f).compositeOver(surfaceFallback)
    val contentColor = if ((startColor.luminance() + endColor.luminance()) / 2f > 0.56f) {
        Color(0xFF14141A)
    } else {
        Color.White
    }

    val gradient = Brush.linearGradient(
        colors = listOf(startColor, endColor),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 30.dp else 22.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cardCorner"
    )

    val decorativePolygon = remember(shapeIndex) {
        when (shapeIndex % 6) {
            0 -> MaterialShapes.Cookie4Sided
            1 -> MaterialShapes.SoftBurst
            2 -> MaterialShapes.Clover4Leaf
            3 -> MaterialShapes.Sunny
            4 -> MaterialShapes.Cookie6Sided
            else -> MaterialShapes.Slanted
        }
    }
    val decorativeShape = decorativePolygon.toShape()

    val decorativeIcon = remember(shapeIndex) {
        when (shapeIndex % 3) {
            0 -> R.drawable.music_note
            1 -> R.drawable.graphic_eq
            else -> R.drawable.radio
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceFallback)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 18.dp, y = 18.dp)
                    .size(88.dp)
                    .clip(decorativeShape)
                    .background(contentColor.copy(alpha = 0.14f))
            )

            Icon(
                painter = painterResource(decorativeIcon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 6.dp)
                    .size(44.dp)
                    .graphicsLayer {
                        rotationZ = -14f
                        alpha = 0.28f
                    },
                tint = contentColor
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getTranslatedExploreSectionTitle(title: String): String {
    return when (title.lowercase()) {
        "moods & moments" -> stringResource(R.string.moods_and_moments)
        "genres" -> stringResource(R.string.genres)
        else -> title
    }
}