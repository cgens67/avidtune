@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cgens67.avidtune.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.cgens67.avidtune.BuildConfig
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

// --- Data Models ---

data class CommitData(
    val sha: String,
    val message: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val authorLogin: String?,
    val date: String,
    val htmlUrl: String
)

data class ChangelogSection(val title: String, val items: List<String>)
data class ReleaseMetadata(
    val tagName: String, 
    val name: String, 
    val date: String, 
    val changelogUrl: String?,
    val isPrerelease: Boolean,
    val rawDate: String
)
data class CachedChangelogData(val sections: List<ChangelogSection>, val image: String?, val description: String?, val warning: String?)

// --- Utils ---

fun getTimeAgo(context: Context, dateString: String?): String {
    if (dateString.isNullOrBlank()) return context.getString(R.string.unknown)
    return try {
        val zdt = ZonedDateTime.parse(dateString)
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val duration = Duration.between(zdt, now)
        
        val seconds = duration.seconds
        when {
            seconds < 0 -> context.getString(R.string.just_now)
            seconds < 60 -> context.getString(R.string.seconds_ago, seconds)
            seconds < 120 -> context.getString(R.string.one_minute_ago)
            seconds < 3600 -> context.getString(R.string.minutes_ago, seconds / 60)
            seconds < 7200 -> context.getString(R.string.one_hour_ago)
            seconds < 86400 -> context.getString(R.string.hours_ago, seconds / 3600)
            seconds < 172800 -> context.getString(R.string.one_day_ago)
            seconds < 2592000 -> context.getString(R.string.days_ago, seconds / 86400)
            seconds < 5184000 -> context.getString(R.string.one_month_ago)
            seconds < 31536000 -> context.getString(R.string.months_ago, seconds / 2592000)
            seconds < 63072000 -> context.getString(R.string.one_year_ago)
            else -> context.getString(R.string.years_ago, seconds / 31536000)
        }
    } catch (e: Exception) {
        dateString ?: ""
    }
}

fun getBetaTarget(tagName: String): String? {
    val base = tagName.removePrefix("v")
    if (base.contains("-")) {
        return base.substringBefore("-")
    }
    return null
}

fun normalizeVersionString(version: String): String {
    return version.trim()
        .lowercase()
        .replace(" ", "")
        .removePrefix("v.")
        .removePrefix("v")
        .replace("..", ".")
}

fun matchesVersion(tagName: String, query: String): Boolean {
    val cleanQuery = query.trim().lowercase().replace(" ", "")
    if (cleanQuery.isBlank()) return true
    if (cleanQuery == "v" || cleanQuery == "v.") return true

    if (tagName.lowercase().replace(" ", "").contains(cleanQuery)) return true

    val normTag = normalizeVersionString(tagName)
    val normQuery = normalizeVersionString(cleanQuery)

    if (normQuery.isNotEmpty() && normTag.contains(normQuery)) return true

    return false
}

// --- Empty Search Results Component ---

@Composable
fun EmptySearchResultsView(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.no_results_found),
    subtitle: String = stringResource(R.string.try_another_term)
) {
    var visible by remember(title, subtitle) { mutableStateOf(false) }
    LaunchedEffect(title, subtitle) {
        visible = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "emptyIconScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "emptyContentAlpha"
    )

    val contentOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "emptyContentOffsetY"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = contentAlpha
                translationY = contentOffsetY.toPx()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(iconScale)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.search),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// --- Highlighting Helper ---

@Composable
fun getHighlightedString(
    text: String,
    query: String,
    baseColor: Color = Color.Unspecified
): AnnotatedString {
    if (query.isBlank()) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) { append(text) }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "highlight_anim")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "highlight_alpha"
    )

    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha)
    val highlightTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    return buildAnnotatedString {
        val matches = Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text)
        var lastIndex = 0
        for (match in matches) {
            withStyle(SpanStyle(color = baseColor)) {
                append(text.substring(lastIndex, match.range.first))
            }
            withStyle(SpanStyle(background = highlightColor, color = highlightTextColor, fontWeight = FontWeight.ExtraBold)) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        withStyle(SpanStyle(color = baseColor)) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun getChangelogItemString(
    text: String,
    query: String,
    baseColor: Color = Color.Unspecified
): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary

    if (query.isBlank()) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) { append(text) }
            val urls = text.extractUrls()
            urls.forEach { (range, url) ->
                addStringAnnotation("URL", url, range.first, range.last + 1)
                addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), range.first, range.last + 1)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "highlight_anim")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "highlight_alpha"
    )

    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha)
    val highlightTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    return buildAnnotatedString {
        val matches = Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text)
        var lastIndex = 0
        for (match in matches) {
            withStyle(SpanStyle(color = baseColor)) {
                append(text.substring(lastIndex, match.range.first))
            }
            withStyle(SpanStyle(background = highlightColor, color = highlightTextColor, fontWeight = FontWeight.ExtraBold)) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        withStyle(SpanStyle(color = baseColor)) {
            append(text.substring(lastIndex))
        }
        
        val urls = text.extractUrls()
        urls.forEach { (range, url) ->
            addStringAnnotation("URL", url, range.first, range.last + 1)
            addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), range.first, range.last + 1)
        }
    }
}

// --- Main Screen ---

@Composable
fun ChangelogScreen(
    onDismiss: () -> Unit = {},
    versionTag: String = BuildConfig.VERSION_NAME
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "changelogTopBar"
            ) { searching ->
                if (searching) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearch = { isSearchActive = false },
                                expanded = false,
                                onExpandedChange = {},
                                placeholder = {
                                    Text(text = stringResource(R.string.search))
                                },
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            isSearchActive = false
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_back),
                                            contentDescription = stringResource(R.string.back)
                                        )
                                    }
                                },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = stringResource(R.string.clear)
                                            )
                                        }
                                    }
                                } else null
                            )
                        },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {}
                } else {
                    LargeTopAppBar(
                        title = { 
                            Text(
                                text = stringResource(R.string.Changelog),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(painterResource(R.drawable.arrow_back), null)
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                            IconButton(onClick = { refreshTrigger++ }) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = stringResource(R.string.action_retry)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_releases), fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.beta_releases), fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_commits), fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0 || selectedTab == 1) {
                ReleasesContent(
                    versionTag = versionTag,
                    refreshTrigger = refreshTrigger,
                    isBetaTab = selectedTab == 1,
                    searchQuery = searchQuery
                )
            } else {
                CommitsContent(
                    refreshTrigger = refreshTrigger,
                    searchQuery = searchQuery
                )
            }
        }
    }
}

// --- Releases (News-style) Content ---

@Composable
fun ReleasesContent(
    versionTag: String, 
    refreshTrigger: Int, 
    isBetaTab: Boolean,
    searchQuery: String = ""
) {
    val context = LocalContext.current
    var changelogSections by remember { mutableStateOf<List<ChangelogSection>>(emptyList()) }
    var updateImage by remember { mutableStateOf<String?>(null) }
    var updateDescription by remember { mutableStateOf<String?>(null) }
    var updateWarning by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var detailedError by remember { mutableStateOf<String?>(null) }
    var showingCached by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var currentVersionTag by remember { mutableStateOf("") }
    var availableReleases by remember { mutableStateOf<List<ReleaseMetadata>>(emptyList()) }
    var isFetchingOldReleases by remember { mutableStateOf(true) }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoading || isFetchingOldReleases

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
    }

    val httpClient = remember(context) {
        val cacheSize = 10L * 1024 * 1024 // 10 MiB Cache size
        val cache = Cache(File(context.cacheDir, "github_api_cache"), cacheSize)
        
        OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val filteredReleases = availableReleases.filter { it.isPrerelease == isBetaTab }

    val searchFilteredReleases = remember(filteredReleases, searchQuery, changelogSections, updateDescription, currentVersionTag) {
        if (searchQuery.isBlank()) {
            filteredReleases
        } else {
            val matchesCurrentChangelog = changelogSections.any { section ->
                section.title.contains(searchQuery, ignoreCase = true) ||
                section.items.any { it.contains(searchQuery, ignoreCase = true) }
            } || (updateDescription?.contains(searchQuery, ignoreCase = true) == true)

            filteredReleases.filter { release ->
                matchesVersion(release.tagName, searchQuery) ||
                release.name.contains(searchQuery, ignoreCase = true) ||
                (release.tagName == currentVersionTag && matchesCurrentChangelog)
            }
        }
    }

    val filteredChangelogSections = remember(changelogSections, searchQuery) {
        if (searchQuery.isBlank()) {
            changelogSections
        } else {
            changelogSections.mapNotNull { section ->
                val titleMatches = section.title.contains(searchQuery, ignoreCase = true)
                val filteredItems = section.items.filter { 
                    it.contains(searchQuery, ignoreCase = true) 
                }
                
                if (titleMatches) {
                    section
                } else if (filteredItems.isNotEmpty()) {
                    section.copy(items = filteredItems)
                } else {
                    null
                }
            }
        }
    }

    val showDescription = updateDescription?.let { desc ->
        searchQuery.isBlank() || desc.contains(searchQuery, ignoreCase = true)
    } ?: false

    val showImage = updateImage != null && (searchQuery.isBlank() || showDescription || filteredChangelogSections.isNotEmpty())

    fun fetchChangelog(tag: String, bypassCache: Boolean = false) {
        if (tag.isBlank()) return
        isLoading = true
        hasError = false
        detailedError = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val cachedData = if (!bypassCache) loadChangelogFromCache(context, tag) else null
                if (cachedData != null) {
                    withContext(Dispatchers.Main) {
                        changelogSections = cachedData.sections
                        updateImage = cachedData.image
                        updateDescription = cachedData.description
                        updateWarning = cachedData.warning
                        isLoading = false
                        showingCached = true
                    }
                } else {
                    val releaseMeta = availableReleases.find { it.tagName == tag }
                    val urlToFetch = releaseMeta?.changelogUrl 
                        ?: "https://github.com/cgens67/AvidTune/releases/download/$tag/changelog.json"

                    val request = Request.Builder()
                        .url(urlToFetch)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()

                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val changelogJson = response.body?.string() ?: "{}"
                        try {
                            val changelogData = JSONObject(changelogJson)

                            val desc = changelogData.optString("description", "").takeIf { it.isNotBlank() }
                            val imageUrl = changelogData.optString("image", "").takeIf { it.isNotBlank() }
                            val warning = changelogData.optString("warning", "").takeIf { it.isNotBlank() }
                            val changelogArray = changelogData.optJSONArray("changelog")

                            val sections = mutableListOf<ChangelogSection>()
                            if (changelogArray != null) {
                                for (i in 0 until changelogArray.length()) {
                                    val sectionObj = changelogArray.optJSONObject(i)
                                    if (sectionObj != null) {
                                        val title = sectionObj.optString("title", "")
                                        val itemsArray = sectionObj.optJSONArray("items")
                                        val items = mutableListOf<String>()
                                        if (itemsArray != null) {
                                            for (j in 0 until itemsArray.length()) {
                                                items.add(itemsArray.getString(j))
                                            }
                                        }
                                        if (title.isNotBlank() || items.isNotEmpty()) {
                                            sections.add(ChangelogSection(title, items))
                                        }
                                    } else {
                                        val item = changelogArray.optString(i, "")
                                        if (item.isNotBlank()) {
                                            if (sections.isEmpty() || sections[0].title.isNotBlank()) {
                                                sections.add(0, ChangelogSection("", mutableListOf()))
                                            }
                                            (sections[0].items as MutableList<String>).add(item)
                                        }
                                    }
                                }
                            }

                            saveChangelogToCache(context, tag, sections, imageUrl, desc, warning)
                            withContext(Dispatchers.Main) {
                                changelogSections = sections
                                updateImage = imageUrl
                                updateDescription = desc
                                updateWarning = warning
                                isLoading = false
                                hasError = false
                                showingCached = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { 
                                hasError = true
                                detailedError = "JSON Parse Error: ${e.message}"
                                isLoading = false 
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { 
                            hasError = true
                            detailedError = if (response.code == 403) context.getString(R.string.github_api_rate_limit_exceeded) else "HTTP ${response.code}: ${response.message}\nURL: $urlToFetch"
                            isLoading = false 
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hasError = true
                    detailedError = "Network Error: ${e.javaClass.simpleName} - ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    fun fetchOldReleases(bypassCache: Boolean = false) {
        isFetchingOldReleases = true
        hasError = false
        detailedError = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val cachedJson = if (!bypassCache) loadReleasesFromCache(context) else null
                val json = if (cachedJson != null) {
                    cachedJson
                } else {
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/cgens67/AvidTune/releases?per_page=50")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()

                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: "[]"
                        saveReleasesToCache(context, bodyString)
                        bodyString
                    } else {
                        withContext(Dispatchers.Main) {
                            isFetchingOldReleases = false
                            hasError = true
                            detailedError = if (response.code == 403) context.getString(R.string.github_api_rate_limit_exceeded) else "HTTP ${response.code}: ${response.message}"
                        }
                        return@launch
                    }
                }

                val array = JSONArray(json)
                val list = mutableListOf<ReleaseMetadata>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tagName = obj.getString("tag_name")
                    val name = obj.optString("name", tagName)
                    val publishedAt = obj.getString("published_at")
                    val isPrerelease = obj.getBoolean("prerelease")
                    val formattedDate = getTimeAgo(context, publishedAt)

                    val assets = obj.getJSONArray("assets")
                    var changelogUrl: String? = null
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        if (asset.getString("name").equals("changelog.json", ignoreCase = true)) {
                            changelogUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    list.add(ReleaseMetadata(tagName, name, formattedDate, changelogUrl, isPrerelease, publishedAt))
                }
                withContext(Dispatchers.Main) {
                    availableReleases = list
                    isFetchingOldReleases = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isFetchingOldReleases = false
                    hasError = true
                    detailedError = "Network Error: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchOldReleases(false)
    }

    LaunchedEffect(isBetaTab, availableReleases) {
        if (filteredReleases.isNotEmpty()) {
            if (currentVersionTag.isBlank() || filteredReleases.none { it.tagName == currentVersionTag }) {
                val appVerMatchesTab = (versionTag.contains("-") == isBetaTab)
                currentVersionTag = if (appVerMatchesTab && filteredReleases.any { it.tagName == versionTag }) {
                    versionTag
                } else {
                    filteredReleases.first().tagName
                }
            }
        }
    }

    LaunchedEffect(searchFilteredReleases) {
        if (searchFilteredReleases.isNotEmpty() && searchFilteredReleases.none { it.tagName == currentVersionTag }) {
            currentVersionTag = searchFilteredReleases.first().tagName
        }
    }

    LaunchedEffect(currentVersionTag) {
        if (currentVersionTag.isNotBlank()) {
            cleanupOldChangelogCache(context, currentVersionTag)
            fetchChangelog(currentVersionTag)
        }
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            isFetchingOldReleases = true
            fetchOldReleases(bypassCache = true)
            if (currentVersionTag.isNotBlank()) {
                fetchChangelog(currentVersionTag, bypassCache = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { 
                    isFetchingOldReleases = true
                    fetchOldReleases(bypassCache = true)
                    if (currentVersionTag.isNotBlank()) fetchChangelog(currentVersionTag, bypassCache = true) 
                }
            )
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (searchFilteredReleases.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        searchFilteredReleases.forEachIndexed { index, release ->
                            ToggleButton(
                                checked = currentVersionTag == release.tagName,
                                onCheckedChange = {
                                    if (currentVersionTag != release.tagName) {
                                        currentVersionTag = release.tagName
                                    }
                                },
                                shapes = when {
                                    searchFilteredReleases.size == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    index == searchFilteredReleases.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Text(
                                    text = release.tagName,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    if (isFetchingOldReleases) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val currentRelease = searchFilteredReleases.find { it.tagName == currentVersionTag }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentVersionTag,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (currentRelease != null) {
                            Text(
                                text = getTimeAgo(context, currentRelease.rawDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentRelease.isPrerelease) {
                                val target = getBetaTarget(currentRelease.tagName)
                                if (target != null) {
                                    Text(
                                        text = stringResource(R.string.pre_release_for, target),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    if (showingCached) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(stringResource(R.string.cached), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            } else if (!isFetchingOldReleases && !isLoading && !hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptySearchResultsView(
                        title = stringResource(R.string.no_results_found),
                        subtitle = stringResource(R.string.try_another_term)
                    )
                }
            }

            if (hasError && !isLoading && !isFetchingOldReleases) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.error_loading_changelog), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        
                        detailedError?.let { detail ->
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = detail, 
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { 
                            isFetchingOldReleases = true
                            fetchOldReleases(bypassCache = true)
                            if (currentVersionTag.isNotBlank()) fetchChangelog(currentVersionTag, bypassCache = true) 
                        }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            } else if (searchFilteredReleases.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (showImage) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ElevatedCard(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = updateImage,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                            if (showDescription && updateDescription != null) {
                                Text(
                                    text = getHighlightedString(updateDescription!!, searchQuery, MaterialTheme.colorScheme.onSurfaceVariant),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else if (showDescription && updateDescription != null) {
                        Spacer(Modifier.height(8.dp))
                        ElevatedCard(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = getHighlightedString(updateDescription!!, searchQuery, MaterialTheme.colorScheme.onSurfaceVariant),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    if (filteredChangelogSections.isNotEmpty()) {
                        filteredChangelogSections.forEach { section ->
                            Spacer(Modifier.height(16.dp))
                            ElevatedCard(
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    if (section.title.isNotBlank()) {
                                        Text(
                                            text = getHighlightedString(section.title, searchQuery, MaterialTheme.colorScheme.onSurface),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(12.dp))
                                    }

                                    section.items.forEach { item ->
                                        val annotatedText = getChangelogItemString(item.trim(), searchQuery, MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp), 
                                            verticalAlignment = Alignment.Top, 
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(modifier = Modifier
                                                .padding(top = 8.dp)
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape))
                                            ClickableText(
                                                text = annotatedText,
                                                onClick = { offset ->
                                                    annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                                        ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!showDescription && searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            EmptySearchResultsView(
                                title = stringResource(R.string.no_results_found),
                                subtitle = stringResource(R.string.try_another_term)
                            )
                        }
                    }

                    updateWarning?.let { warning ->
                        Spacer(Modifier.height(24.dp))
                        Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Text(
                                    text = getHighlightedString(warning, searchQuery, MaterialTheme.colorScheme.onErrorContainer), 
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                }
        ) {
            PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = isRefreshing)
        }
    }
}

// --- Commits Content ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitsContent(
    refreshTrigger: Int,
    searchQuery: String = ""
) {
    val context = LocalContext.current
    var commits by remember { mutableStateOf<List<CommitData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var detailedError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val pullToRefreshState = rememberPullToRefreshState()

    val scaleFraction = {
        if (isLoading) 1f
        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
    }

    val httpClient = remember(context) {
        val cacheSize = 10L * 1024 * 1024 // 10 MiB Cache size
        val cache = Cache(File(context.cacheDir, "github_api_cache"), cacheSize)
        
        OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun fetchCommits(bypassCache: Boolean = false) {
        isLoading = true
        hasError = false
        detailedError = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val cachedJson = if (!bypassCache) loadCommitsFromCache(context) else null
                val json = if (cachedJson != null) {
                    cachedJson
                } else {
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/cgens67/AvidTune/commits?branch=main&per_page=50")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()

                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: "[]"
                        saveCommitsToCache(context, bodyString)
                        bodyString
                    } else {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            hasError = true
                            detailedError = if (response.code == 403) context.getString(R.string.github_api_rate_limit_exceeded) else "HTTP ${response.code}: ${response.message}"
                        }
                        return@launch
                    }
                }

                val array = JSONArray(json)

                val list = mutableListOf<CommitData>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val sha = obj.getString("sha")
                    val htmlUrl = obj.getString("html_url")

                    val commitObj = obj.getJSONObject("commit")
                    val fullMessage = commitObj.getString("message")
                    val message = fullMessage.lines().firstOrNull { it.isNotBlank() } ?: fullMessage

                    val authorObj = commitObj.getJSONObject("author")
                    val authorName = authorObj.optString("name").takeIf { it.isNotEmpty() } ?: context.getString(R.string.unknown)
                    val rawDate = authorObj.optString("date", "")
                    val formattedDate = getTimeAgo(context, rawDate)

                    val authorLogin = if (!obj.isNull("author")) {
                        obj.getJSONObject("author").optString("login", null)
                    } else null
                    val authorAvatarUrl = if (!obj.isNull("author")) {
                        obj.getJSONObject("author").optString("avatar_url", null)
                    } else null

                    list.add(CommitData(sha, message, authorName, authorAvatarUrl, authorLogin, formattedDate, htmlUrl))
                }

                withContext(Dispatchers.Main) {
                    commits = list
                    isLoading = false
                    hasError = false
                }
            } catch (e: Exception) {
                Log.e("CommitScreen", "Error fetching commits: ${e.message}")
                withContext(Dispatchers.Main) {
                    hasError = true
                    detailedError = "Network Error: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    val filteredCommits = remember(commits, searchQuery) {
        if (searchQuery.isBlank()) {
            commits
        } else {
            commits.filter { commit ->
                commit.message.contains(searchQuery, ignoreCase = true) ||
                commit.authorName.contains(searchQuery, ignoreCase = true) ||
                commit.sha.contains(searchQuery, ignoreCase = true) ||
                (commit.authorLogin?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchCommits(false)
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            fetchCommits(bypassCache = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isLoading,
                onRefresh = { fetchCommits(bypassCache = true) }
            )
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
    ) {
        when {
            hasError && !isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.error_loading_commits),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        
                        detailedError?.let { detail ->
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = detail, 
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { fetchCommits(bypassCache = true) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }

            filteredCommits.isNotEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredCommits.forEachIndexed { index, commit ->
                        CommitItem(
                            commit = commit,
                            searchQuery = searchQuery,
                            onClick = {
                                ContextCompat.startActivity(
                                    context,
                                    Intent(Intent.ACTION_VIEW, Uri.parse(commit.htmlUrl)),
                                    null
                                )
                            }
                        )
                        if (index < filteredCommits.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            !isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 48.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptySearchResultsView(
                        title = stringResource(R.string.no_results_found),
                        subtitle = stringResource(R.string.try_another_term)
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                }
        ) {
            PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = isLoading)
        }
    }
}

@Composable
fun CommitItem(
    commit: CommitData,
    searchQuery: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.history), 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = getHighlightedString(commit.message, searchQuery, MaterialTheme.colorScheme.onSurface),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = getHighlightedString(commit.authorName, searchQuery, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = commit.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = getHighlightedString(commit.sha.take(7), searchQuery, MaterialTheme.colorScheme.onSecondaryContainer),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        if (commit.authorAvatarUrl != null) {
            AsyncImage(
                model = commit.authorAvatarUrl,
                contentDescription = commit.authorName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = commit.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// --- Cache Utils ---

private fun saveReleasesToCache(context: Context, json: String) {
    try {
        context.openFileOutput("releases_cache.json", Context.MODE_PRIVATE).use { it.write(json.toByteArray()) }
        context.getSharedPreferences("changelog_prefs", Context.MODE_PRIVATE).edit().putLong("releases_cache_time", System.currentTimeMillis()).apply()
    } catch (e: Exception) { Log.e("ChangelogCache", "Error saving releases cache", e) }
}

private fun loadReleasesFromCache(context: Context): String? {
    val prefs = context.getSharedPreferences("changelog_prefs", Context.MODE_PRIVATE)
    val time = prefs.getLong("releases_cache_time", 0)
    if (System.currentTimeMillis() - time > 3600_000) return null // 1 hour expiration
    return try {
        val file = File(context.filesDir, "releases_cache.json")
        if (!file.exists()) return null
        context.openFileInput("releases_cache.json").use { it.bufferedReader().readText() }
    } catch (e: Exception) { null }
}

private fun saveCommitsToCache(context: Context, json: String) {
    try {
        context.openFileOutput("commits_cache.json", Context.MODE_PRIVATE).use { it.write(json.toByteArray()) }
        context.getSharedPreferences("changelog_prefs", Context.MODE_PRIVATE).edit().putLong("commits_cache_time", System.currentTimeMillis()).apply()
    } catch (e: Exception) { Log.e("ChangelogCache", "Error saving commits cache", e) }
}

private fun loadCommitsFromCache(context: Context): String? {
    val prefs = context.getSharedPreferences("changelog_prefs", Context.MODE_PRIVATE)
    val time = prefs.getLong("commits_cache_time", 0)
    if (System.currentTimeMillis() - time > 3600_000) return null // 1 hour expiration
    return try {
        val file = File(context.filesDir, "commits_cache.json")
        if (!file.exists()) return null
        context.openFileInput("commits_cache.json").use { it.bufferedReader().readText() }
    } catch (e: Exception) { null }
}

private fun cleanupOldChangelogCache(context: Context, currentVersionTag: String) {
    try {
        context.filesDir.listFiles { file -> file.name.startsWith("changelog_cache_") && file.name.endsWith(".json") }?.forEach { file ->
            if (file.name != "changelog_cache_$currentVersionTag.json") file.delete()
        }
    } catch (e: Exception) { Log.e("ChangelogCache", "Error cleaning up cache", e) }
}

private fun saveChangelogToCache(context: Context, versionTag: String, sections: List<ChangelogSection>, image: String?, description: String?, warning: String?) {
    try {
        val cacheData = JSONObject().apply {
            val sectionsArray = JSONArray()
            sections.forEach { section ->
                val sectionObj = JSONObject().apply {
                    put("title", section.title)
                    val itemsArray = JSONArray()
                    section.items.forEach { itemsArray.put(it) }
                    put("items", itemsArray)
                }
                sectionsArray.put(sectionObj)
            }
            put("sections", sectionsArray)
            put("image", image ?: "")
            put("description", description ?: "")
            put("warning", warning ?: "")
        }
        context.openFileOutput("changelog_cache_$versionTag.json", Context.MODE_PRIVATE).use { it.write(cacheData.toString().toByteArray()) }
    } catch (e: Exception) { Log.e("ChangelogCache", "Error saving cache", e) }
}

private fun loadChangelogFromCache(context: Context, versionTag: String): CachedChangelogData? {
    return try {
        val cacheFile = File(context.filesDir, "changelog_cache_$versionTag.json")
        if (!cacheFile.exists()) return null
        val cacheData = JSONObject(context.openFileInput("changelog_cache_$versionTag.json").use { it.bufferedReader().readText() })

        val sectionsArray = cacheData.optJSONArray("sections")
        val sections = mutableListOf<ChangelogSection>()
        if (sectionsArray != null) {
            for (i in 0 until sectionsArray.length()) {
                val sectionObj = sectionsArray.getJSONObject(i)
                val title = sectionObj.getString("title")
                val itemsArray = sectionObj.getJSONArray("items")
                val items = mutableListOf<String>()
                for (j in 0 until itemsArray.length()) {
                    items.add(itemsArray.getString(j))
                }
                sections.add(ChangelogSection(title, items))
            }
        }

        CachedChangelogData(
            sections = sections,
            image = cacheData.optString("image", null).takeIf { !it.isNullOrBlank() },
            description = cacheData.optString("description", null).takeIf { !it.isNullOrBlank() },
            warning = cacheData.optString("warning", null).takeIf { !it.isNullOrBlank() }
        )
    } catch (e: Exception) { null }
}

fun String.extractUrls(): List<Pair<IntRange, String>> {
    val urlRegex = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))".toRegex()
    return urlRegex.findAll(this).map { it.range to it.value }.toList()
}
