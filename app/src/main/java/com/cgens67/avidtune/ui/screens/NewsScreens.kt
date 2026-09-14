@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)

package com.cgens67.avidtune.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.NewsLastReadTimestampKey
import com.cgens67.avidtune.ui.component.IconButton as AppIconButton
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.dataStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// MODELS & REPOSITORIES (Untouched logic)
// ==========================================

@Serializable
data class NewsItem(
    @SerialName("id") val id: String = "",
    @SerialName("Title") val title: String,
    @SerialName("Description") val description: String = "",
    @SerialName("ImageURL")
    @Serializable(with = NewsImageUrlsSerializer::class)
    val imageUrls: List<String> = emptyList(),
    @SerialName("Important") val important: Boolean = false,
    @SerialName("Author") val author: String,
    @SerialName("Date") val timestamp: Long = 0L,
) {
    val stableKey: String
        get() = id.ifEmpty { "$timestamp|$author|$title" }
}

object NewsImageUrlsSerializer : KSerializer<List<String>> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> emptyList()
            is JsonArray -> element.mapNotNull { item ->
                (item as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
            }
            is JsonPrimitive -> element.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let(::listOf) ?: emptyList()
            else -> emptyList()
        }
    }
    override fun serialize(encoder: Encoder, value: List<String>) {
        delegate.serialize(encoder, value)
    }
}

@Singleton
class NewsRepository @Inject constructor() {
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 15000
            endpoint { connectTimeout = 15000 }
        }
    }
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    @Volatile private var metadataCache: List<NewsItem>? = null

    suspend fun fetchNews(): List<NewsItem> {
        val response = client.get(METADATA_URL) {
            headers {
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
                append(HttpHeaders.Expires, "0")
            }
        }
        val items = json.decodeFromString<List<NewsItem>>(response.bodyAsText())
        metadataCache = items
        return items
    }

    suspend fun fetchNewsContent(id: String): String {
        return client.get("$CONTENT_BASE_URL$id") {
            headers {
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
                append(HttpHeaders.Expires, "0")
            }
        }.bodyAsText()
    }

    fun getCachedItem(id: String): NewsItem? = metadataCache?.find { it.id == id }

    private companion object {
        const val METADATA_URL = "https://raw.githubusercontent.com/cgens67/avidtune-news/main/metadata.json"
        const val CONTENT_BASE_URL = "https://raw.githubusercontent.com/cgens67/avidtune-news/main/content/"
    }
}

sealed interface NewsUiState {
    data object Loading : NewsUiState
    data class Success(val items: List<NewsItem>) : NewsUiState
    data object Empty : NewsUiState
    data class Error(val message: String) : NewsUiState
}

@dagger.hilt.android.lifecycle.HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {
    private val _rawItems = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _loadState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NewsUiState> = combine(_loadState, searchQuery, _rawItems) { loadState, query, items ->
        when (loadState) {
            is NewsUiState.Loading -> NewsUiState.Loading
            is NewsUiState.Error -> loadState
            is NewsUiState.Empty -> NewsUiState.Empty
            is NewsUiState.Success -> {
                if (query.isBlank()) loadState
                else {
                    val q = query.trim().lowercase()
                    val filtered = items.filter { it.title.lowercase().contains(q) || it.author.lowercase().contains(q) }
                    if (filtered.isEmpty()) NewsUiState.Empty else NewsUiState.Success(filtered)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState.Loading)

    val hasUnreadNews: StateFlow<Boolean> = combine(
        _rawItems,
        context.dataStore.data.map { prefs -> prefs[NewsLastReadTimestampKey] ?: 0L }
    ) { items, lastRead ->
        items.isNotEmpty() && items.maxOf { it.timestamp } > lastRead
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init { fetchNews() }

    fun fetchNews() {
        viewModelScope.launch {
            _loadState.value = NewsUiState.Loading
            runCatching { repository.fetchNews().sortedByDescending { it.timestamp } }
                .onSuccess { items ->
                    _rawItems.value = items
                    _loadState.value = if (items.isEmpty()) NewsUiState.Empty else NewsUiState.Success(items)
                }
                .onFailure { error -> _loadState.value = NewsUiState.Error(error.message ?: "Unknown error") }
        }
    }

    fun markAllRead() {
        val latest = _rawItems.value.maxOfOrNull { it.timestamp } ?: return
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[NewsLastReadTimestampKey] = latest
            }
        }
    }
}

sealed interface ViewNewsUiState {
    data object Loading : ViewNewsUiState
    data class Success(val content: String) : ViewNewsUiState
    data class Error(val message: String) : ViewNewsUiState
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ViewNewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {
    val newsId: String = savedStateHandle.get<String>("newsId") ?: ""
    val newsItem: NewsItem? = repository.getCachedItem(newsId)
    private val _contentState = MutableStateFlow<ViewNewsUiState>(ViewNewsUiState.Loading)
    val contentState: StateFlow<ViewNewsUiState> = _contentState.asStateFlow()

    init { loadContent() }

    fun loadContent() {
        viewModelScope.launch {
            _contentState.value = ViewNewsUiState.Loading
            runCatching { repository.fetchNewsContent(newsId) }
                .onSuccess { _contentState.value = ViewNewsUiState.Success(it) }
                .onFailure { _contentState.value = ViewNewsUiState.Error(it.message ?: "Unknown error") }
        }
    }
}

// ==========================================
// ANIMATED BACKGROUND & SHARED UI
// ==========================================

@Composable
fun AnimatedNewsBackground(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_bg")
    
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot1"
    )
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot2"
    )

    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.25f)
    val color2 = MaterialTheme.colorScheme.tertiary.copy(alpha = if (isDark) 0.12f else 0.2f)

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
            val w = size.width
            val h = size.height
            val r1 = w * 0.8f
            val r2 = w * 0.9f
            
            val cx1 = w/2 + cos(rotation1 * PI / 180).toFloat() * (w * 0.2f)
            val cy1 = h/3 + sin(rotation1 * PI / 180).toFloat() * (h * 0.1f)
            
            val cx2 = w/2 + cos(rotation2 * PI / 180).toFloat() * (w * 0.3f)
            val cy2 = h * 0.7f + sin(rotation2 * PI / 180).toFloat() * (h * 0.15f)

            drawCircle(color = color1, radius = r1, center = Offset(cx1, cy1))
            drawCircle(color = color2, radius = r2, center = Offset(cx2, cy2))
        }
    }
}

// ==========================================
// NEWS LIST SCREEN
// ==========================================

@Composable
fun NewsScreen(
    navController: NavController,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20 } }
    
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState) {
        if (uiState is NewsUiState.Success) viewModel.markAllRead()
    }
    
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedNewsBackground()

            // List Content
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                modifier = Modifier.fillMaxSize(),
                label = "content"
            ) { state ->
                when (state) {
                    is NewsUiState.Loading -> NewsLoadingState(Modifier.fillMaxSize())
                    is NewsUiState.Error -> NewsErrorState(state.message, viewModel::fetchNews, Modifier.fillMaxSize())
                    is NewsUiState.Empty -> NewsEmptyState(searchQuery.isNotBlank(), Modifier.fillMaxSize())
                    is NewsUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding() + 100.dp, // Space for floating header
                                bottom = innerPadding.calculateBottomPadding() + 120.dp,
                                start = 16.dp, end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(state.items, key = { _, i -> i.stableKey }) { index, item ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { delay(index * 80L); visible = true }
                                
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } + fadeIn(tween(400))
                                ) {
                                    if (index == 0 && searchQuery.isBlank()) {
                                        FeaturedNewsCard(item, onNavigate = { navController.navigate("view_news/${Uri.encode(item.id)}") })
                                    } else {
                                        EnhancedNewsCard(item, onNavigate = { navController.navigate("view_news/${Uri.encode(item.id)}") })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Header / Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                val headerAlpha by animateFloatAsState(targetValue = if (isScrolled && !isSearchActive) 0.95f else 1f, label = "bg_alpha")
                val headerElevation by animateDpAsState(targetValue = if (isScrolled) 8.dp else 0.dp, label = "elevation")
                
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = headerAlpha),
                    shadowElevation = headerElevation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSearchActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            AppIconButton(onClick = { isSearchActive = false; viewModel.searchQuery.value = "" }) {
                                Icon(painterResource(R.drawable.arrow_back), null)
                            }
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.searchQuery.value = it },
                                textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(); keyboardController?.hide() }),
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text(stringResource(R.string.search_news_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                AppIconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(painterResource(R.drawable.close), null)
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            AppIconButton(onClick = { navController.navigateUp() }) {
                                Icon(painterResource(R.drawable.arrow_back), null)
                            }
                            Text(
                                text = stringResource(R.string.news),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row {
                                AppIconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, null)
                                }
                                AppIconButton(onClick = { viewModel.fetchNews(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                                    Icon(painterResource(R.drawable.sync), null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FEATURED & STANDARD CARDS
// ==========================================

@Composable
fun FeaturedNewsCard(item: NewsItem, onNavigate: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "scale")
    
    // Ken Burns Effect
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val imgScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "img_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f) // Tall elegant card
            .scale(scale)
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary.copy(0.5f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onNavigate),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(item.imageUrls.first()).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = imgScale; scaleY = imgScale }
                )
            }
            
            // Gradient Overlay
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black),
                        startY = 100f
                    )
                )
            )

            // Content
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (item.important) {
                    Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape, modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(stringResource(R.string.important).uppercase(), color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                        Text(item.author, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedNewsCard(item: NewsItem, onNavigate: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "scale")
    
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns_small")
    val imgScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
        label = "img_scale2"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onNavigate),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column {
            if (item.imageUrls.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(item.imageUrls.first()).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = imgScale; scaleY = imgScale }
                    )
                    if (item.important) {
                        Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                            Text(stringResource(R.string.important).uppercase(), color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(item.author, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    
                    val formattedDate = remember(item.timestamp) {
                        if (item.timestamp == 0L) ""
                        else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDateTime.ofInstant(Instant.ofEpochSecond(item.timestamp), ZoneId.systemDefault()))
                    }
                    Text(formattedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// VIEW NEWS SCREEN (PARALLAX READER)
// ==========================================

@Composable
fun ViewNewsScreen(
    navController: NavController,
    viewModel: ViewNewsViewModel = hiltViewModel(),
) {
    val contentState by viewModel.contentState.collectAsState()
    val newsItem = viewModel.newsItem
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0,0,0,0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedNewsBackground() // Consistent background
            
            AnimatedContent(
                targetState = contentState,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "view_content"
            ) { state ->
                when (state) {
                    is ViewNewsUiState.Loading -> NewsLoadingState(Modifier.fillMaxSize())
                    is ViewNewsUiState.Error -> NewsErrorState(state.message, viewModel::loadContent, Modifier.fillMaxSize())
                    is ViewNewsUiState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = innerPadding.calculateBottomPadding() + 32.dp)
                        ) {
                            // Parallax Header
                            if (newsItem != null && newsItem.imageUrls.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))) {
                                    val parallaxOffset = scrollState.value * 0.5f
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(newsItem.imageUrls.first()).crossfade(true).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().graphicsLayer { translationY = parallaxOffset }
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 100f)))
                                    
                                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                                        if (newsItem.important) {
                                            Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape, modifier = Modifier.padding(bottom = 12.dp)) {
                                                Text(stringResource(R.string.important).uppercase(), color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                            }
                                        }
                                        Text(newsItem.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                                        Spacer(Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                                                Text(newsItem.author, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            val date = remember(newsItem.timestamp) { if (newsItem.timestamp == 0L) "" else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDateTime.ofInstant(Instant.ofEpochSecond(newsItem.timestamp), ZoneId.systemDefault())) }
                                            Text(date, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                // Text only header
                                Column(modifier = Modifier.fillMaxWidth().padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 60.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)) {
                                    Text(newsItem?.title ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                                            Text(newsItem?.author ?: "", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            
                            // Article Body
                            AdvancedMarkdownText(
                                markdown = state.content,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }

            // Glassmorphism Back Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp, start = 16.dp)
                    .size(48.dp)
            ) {
                AppIconButton(onClick = navController::navigateUp, modifier = Modifier.fillMaxSize()) {
                    Icon(painterResource(R.drawable.arrow_back), null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// ==========================================
// SHARED UI HELPERS (Loading, Empty, Error)
// ==========================================

@Composable
private fun NewsLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun NewsEmptyState(isSearching: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    val scale by animateFloatAsState(if (visible) 1f else 0.8f, spring(stiffness = Spring.StiffnessMediumLow), label = "")
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "")

    Column(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(80.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(if (isSearching) R.drawable.search else R.drawable.newspaper), null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(if (isSearching) R.string.no_results_found else R.string.no_news_available), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(if (isSearching) R.string.try_different_keywords else R.string.check_back_later), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    }
}

@Composable
private fun NewsErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(80.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.something_went_wrong), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(32.dp))
        ElevatedButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

// ==========================================
// MARKDOWN PARSER
// ==========================================

@Composable
fun AdvancedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val cleanedMarkdown = cleanMarkdown(markdown)
    val lines = cleanedMarkdown.lines()
    var inList by remember { mutableStateOf(false) }
    val listItems = remember { mutableListOf<String>() }

    Column(modifier = modifier) {
        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                trimmedLine.matches(Regex("^#{1,6}\\s+.*")) -> {
                    if (inList) { ListContainer(listItems.toList()); listItems.clear(); inList = false }
                    val level = trimmedLine.takeWhile { it == '#' }.length
                    val text = trimmedLine.substring(level).trim()
                    HeaderText(text = text, level = level)
                }
                trimmedLine.matches(Regex("^[-*+]\\s+.*")) || trimmedLine.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val content = if (trimmedLine.matches(Regex("^[-*+]\\s+.*"))) trimmedLine.substring(2).trim() else trimmedLine.substringAfter(". ").trim()
                    if (!inList) { inList = true; listItems.clear() }
                    listItems.add(content)
                }
                trimmedLine.startsWith("> ") -> {
                    if (inList) { ListContainer(listItems.toList()); listItems.clear(); inList = false }
                    BlockQuote(trimmedLine.substring(2))
                }
                trimmedLine.isEmpty() -> {
                    if (inList) { ListContainer(listItems.toList()); listItems.clear(); inList = false }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    if (inList) { ListContainer(listItems.toList()); listItems.clear(); inList = false }
                    FormattedText(trimmedLine, style = style, color = color, surfaceVariantColor = surfaceVariant)
                }
            }
        }
        if (inList && listItems.isNotEmpty()) ListContainer(listItems.toList())
    }
}

private fun cleanMarkdown(markdown: String): String {
    var cleaned = markdown.replace(Regex("<[^>]+>"), "").replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "")
    cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)")) { it.groupValues[1] }
    return cleaned.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'").replace(Regex("\n{3,}"), "\n\n").trim()
}

@Composable
private fun HeaderText(text: String, level: Int) {
    val style = when (level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.titleLarge
    }
    Text(text = text, style = style.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
}

@Composable
private fun ListContainer(items: List<String>) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { Row(verticalAlignment = Alignment.Top) { Surface(modifier = Modifier.padding(top = 8.dp).size(6.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {}; Spacer(Modifier.width(12.dp)); FormattedText(text = it, modifier = Modifier.weight(1f), surfaceVariantColor = surfaceVariant) } }
        }
    }
}

@Composable
private fun BlockQuote(content: String) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row {
            Box(modifier = Modifier.width(4.dp).height(40.dp).background(MaterialTheme.colorScheme.primary))
            FormattedText(text = content, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant, surfaceVariantColor = surfaceVariant)
        }
    }
}

@Composable
private fun FormattedText(
    text: String, 
    modifier: Modifier = Modifier, 
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium, 
    color: Color = MaterialTheme.colorScheme.onSurface,
    surfaceVariantColor: Color
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val patterns = listOf(
            Regex("\\*\\*([^*]+)\\*\\*") to { m: MatchResult -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) } },
            Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)") to { m: MatchResult -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[1]) } },
            Regex("`([^`]+)`") to { m: MatchResult -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = surfaceVariantColor)) { append(" ${m.groupValues[1]} ") } }
        )
        val allMatches = patterns.flatMap { (p, h) -> p.findAll(text).map { Triple(it, h, 0) } }.sortedBy { it.first.range.first }
        for ((match, handler) in allMatches) {
            if (match.range.first >= currentIndex) {
                append(text.substring(currentIndex, match.range.first))
                handler(match)
                currentIndex = match.range.last + 1
            }
        }
        if (currentIndex < text.length) append(text.substring(currentIndex))
    }
    Text(text = annotatedString, style = style, color = color, modifier = modifier.padding(vertical = 4.dp))
}
