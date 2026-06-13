@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.cgens67.avidtune.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.models.NewsItem
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.viewmodels.ViewNewsUiState
import com.cgens67.avidtune.viewmodels.ViewNewsViewModel
import com.cgens67.avidtune.ui.component.IconButton as AppIconButton

@Composable
fun ViewNewsScreen(
    navController: NavController,
    viewModel: ViewNewsViewModel = hiltViewModel(),
) {
    val contentState by viewModel.contentState.collectAsState()
    val newsItem = viewModel.newsItem
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = newsItem?.title ?: "",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    AppIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
            },
            modifier = Modifier.fillMaxSize(),
            label = "viewNewsContent",
        ) { state ->
            when (state) {
                is ViewNewsUiState.Loading -> ViewNewsLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )

                is ViewNewsUiState.Error -> ViewNewsErrorState(
                    message = state.message,
                    onRetry = viewModel::loadContent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )

                is ViewNewsUiState.Success -> ViewNewsArticleContent(
                    newsItem = newsItem,
                    content = state.content,
                    innerPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun ViewNewsArticleContent(
    newsItem: NewsItem?,
    content: String,
    innerPadding: PaddingValues,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            ),
    ) {
        val horizontalPadding = if (maxWidth > 840.dp) (maxWidth - 760.dp) / 2 else 24.dp
        val imageUrls = newsItem?.imageUrls.orEmpty()
        var fullImageUrl by remember { mutableStateOf<String?>(null) }

        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 48.dp,
            ),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (newsItem != null) {
                item(key = "article_meta", contentType = "meta") {
                    ViewNewsMetaRow(
                        item = newsItem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding),
                    )
                }
            }

            if (imageUrls.isNotEmpty()) {
                item(key = "article_carousel", contentType = "carousel") {
                    ViewNewsCarousel(
                        imageUrls = imageUrls,
                        onImageClick = { url -> fullImageUrl = url },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                    )
                }
            }

            item(key = "article_content", contentType = "markdown") {
                AdvancedMarkdownText(
                    markdown = content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                )
            }
        }

        if (fullImageUrl != null) {
            ViewNewsFullImageDialog(
                imageUrl = fullImageUrl!!,
                onDismiss = { fullImageUrl = null },
            )
        }
    }
}

@Composable
private fun ViewNewsCarousel(
    imageUrls: List<String>,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.size == 1) {
        val context = LocalContext.current
        val model = remember(context, imageUrls.first()) {
            ImageRequest.Builder(context)
                .data(imageUrls.first())
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .padding(horizontal = 24.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(role = Role.Image) { onImageClick(imageUrls.first()) },
        )
        return
    }

    val carouselState = rememberCarouselState { imageUrls.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 320.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier,
    ) { index ->
        val context = LocalContext.current
        val imageUrl = imageUrls[index]
        val model = remember(context, imageUrl) {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(role = Role.Image) { onImageClick(imageUrl) },
        )
    }
}

@Composable
private fun ViewNewsMetaRow(
    item: NewsItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.important) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = stringResource(R.string.important),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                border = null,
                shape = MaterialTheme.shapes.large
            )
        }

        val formattedDate = remember(item.timestamp) {
            if (item.timestamp == 0L) ""
            else DateTimeFormatter.ofPattern("d MMM yyyy").format(
                LocalDateTime.ofInstant(Instant.ofEpochSecond(item.timestamp), ZoneId.systemDefault())
            )
        }

        AssistChip(
            onClick = {},
            label = {
                Text(
                    text = "${item.author} • $formattedDate",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            border = null,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun ViewNewsFullImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        ),
    ) {
        val context = LocalContext.current
        val model = remember(context, imageUrl) {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ViewNewsLoadingState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(24.dp),
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge,
            colors = MaterialTheme.colorScheme.surfaceContainerHigh.let {
                androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = it)
            },
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ViewNewsErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(24.dp),
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge,
            colors = MaterialTheme.colorScheme.surfaceContainerHigh.let {
                androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = it)
            },
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(88.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.error),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.could_not_load_article),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ElevatedButton(
                    onClick = onRetry,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_retry),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val cleanedMarkdown = cleanMarkdown(markdown)
    val lines = cleanedMarkdown.lines()

    var inCodeBlock by remember { mutableStateOf(false) }
    var codeBlockContent by remember { mutableStateOf("") }
    var codeBlockLanguage by remember { mutableStateOf("") }
    var inList by remember { mutableStateOf(false) }
    var listItems by remember { mutableStateOf(mutableListOf<String>()) }

    Column(modifier = modifier) {
        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trim()

            when {
                trimmedLine.startsWith("```") -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }

                    if (inCodeBlock) {
                        CodeBlock(
                            code = codeBlockContent.trimEnd(),
                            language = codeBlockLanguage
                        )
                        codeBlockContent = ""
                        codeBlockLanguage = ""
                        inCodeBlock = false
                    } else {
                        codeBlockLanguage = trimmedLine.substring(3).trim()
                        inCodeBlock = true
                    }
                }

                inCodeBlock -> {
                    codeBlockContent += line + "\n"
                }

                trimmedLine.matches(Regex("^#{1,6}\\s+.*")) -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }

                    val level = trimmedLine.takeWhile { it == '#' }.length
                    val text = trimmedLine.substring(level).trim()
                    HeaderText(text = text, level = level)
                }

                trimmedLine.matches(Regex("^[-*+]\\s+.*")) -> {
                    val content = trimmedLine.substring(2).trim()
                    if (!inList) {
                        inList = true
                        listItems.clear()
                    }
                    listItems.add(content)
                }

                trimmedLine.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val content = trimmedLine.substringAfter(". ").trim()
                    if (!inList) {
                        inList = true
                        listItems.clear()
                    }
                    listItems.add(content)
                }

                trimmedLine.startsWith("> ") -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    BlockQuote(trimmedLine.substring(2))
                }

                trimmedLine.matches(Regex("^[-*_]{3,}$")) -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    HorizontalRule()
                }

                trimmedLine.isEmpty() -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                else -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    FormattedText(trimmedLine, style = style, color = color)
                }
            }
        }

        if (inList && listItems.isNotEmpty()) {
            ListContainer(listItems.toList())
        }
    }
}

private fun cleanMarkdown(markdown: String): String {
    var cleaned = markdown

    cleaned = cleaned.replace(Regex("<[^>]+>"), "")
    cleaned = cleaned.replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "")
    cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)")) { matchResult ->
        matchResult.groupValues[1]
    }
    cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\[[^\\]]*\\]")) { matchResult ->
        matchResult.groupValues[1]
    }
    cleaned = cleaned.replace(Regex("^\\[[^\\]]+\\]:.*$", RegexOption.MULTILINE), "")

    val htmlEntities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&nbsp;" to " ",
        "&#39;" to "'",
        "&#x27;" to "'",
        "&hellip;" to "...",
        "&mdash;" to "—",
        "&ndash;" to "–"
    )

    for ((entity, replacement) in htmlEntities) {
        cleaned = cleaned.replace(entity, replacement)
    }

    cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n")
    return cleaned.trim()
}

@Composable
private fun HeaderText(text: String, level: Int) {
    val style = when (level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        4 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }

    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = (12 - level * 2).coerceAtLeast(4).dp)
    )
}

@Composable
private fun ListContainer(items: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                UnorderedListItem(item)
            }
        }
    }
}

@Composable
private fun UnorderedListItem(content: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .size(6.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(3.dp),
            color = MaterialTheme.colorScheme.primary
        ) {}
        Spacer(modifier = Modifier.width(12.dp))
        FormattedText(
            text = content,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BlockQuote(content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            FormattedText(
                text = content,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CodeBlock(code: String, language: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (language.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    Text(
                        text = language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                text = code.trimEnd(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun HorizontalRule() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val annotatedString = buildAnnotatedString {
        parseMarkdownText(text, this)
    }

    Text(
        text = annotatedString,
        style = style,
        color = color,
        modifier = modifier.padding(vertical = 2.dp)
    )
}

private fun parseMarkdownText(text: String, builder: AnnotatedString.Builder) {
    var currentIndex = 0
    val processedText = text.trim()

    val patterns = listOf(
        Triple(
            Regex("`([^`]+)`"),
            { match: MatchResult ->
                builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                ) {
                    append(match.groupValues[1])
                }
            },
            1
        ),
        Triple(
            Regex("\\*\\*([^*]+)\\*\\*"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            },
            2
        ),
        Triple(
            Regex("__([^_]+)__"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            },
            2
        ),
        Triple(
            Regex("(?<!\\*)\\*([^*\\s][^*]*[^*\\s])\\*(?!\\*)"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[1])
                }
            },
            3
        ),
        Triple(
            Regex("(?<!_)_([^_\\s][^_]*[^_\\s])_(?!_)"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[1])
                }
            },
            3
        ),
        Triple(
            Regex("~~([^~]+)~~"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(match.groupValues[1])
                }
            },
            4
        )
    )

    val allMatches = patterns.flatMap { (pattern, handler, priority) ->
        pattern.findAll(processedText).map { match ->
            Triple(match, handler, priority)
        }
    }.sortedWith(compareBy({ it.first.range.first }, { it.third }))

    val processedRanges = mutableListOf<IntRange>()

    for ((match, handler, _) in allMatches) {
        val range = match.range
        val overlaps = processedRanges.any { it.intersect(range).isNotEmpty() }

        if (!overlaps) {
            if (range.first > currentIndex) {
                builder.append(processedText.substring(currentIndex, range.first))
            }
            handler(match)
            currentIndex = range.last + 1
            processedRanges.add(range)
        }
    }

    if (currentIndex < processedText.length) {
        builder.append(processedText.substring(currentIndex))
    }

    if (builder.length == 0) {
        builder.append(processedText)
    }
}
