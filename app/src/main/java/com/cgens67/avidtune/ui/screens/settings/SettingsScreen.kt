@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.innertube.utils.parseCookieString
import com.cgens67.avidtune.BuildConfig
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AccountEmailKey
import com.cgens67.avidtune.constants.AccountNameKey
import com.cgens67.avidtune.constants.InnerTubeCookieKey
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.TopSearch
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.HomeViewModel
import com.cgens67.avidtune.viewmodels.NewsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

val LocalAnimationsDisabled = compositionLocalOf { false }

// --- DIMENSIONS & ANIMATIONS ---
object SettingsDimensions {
    val GroupCardCornerRadius = 24.dp
    val QuickActionCardCornerRadius = 24.dp
    val IntegrationPillCornerRadius = 100.dp // Pill shape
    val BannerCardCornerRadius = 24.dp
    val HeroCardCornerRadius = 32.dp

    val ScreenHorizontalPadding = 16.dp
    val SectionSpacing = 20.dp
}

object SettingsAnimations {
    val PressScale = 0.95f
    val StaggerDelayPerItem = 60

    @Composable
    fun <T> pressSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.7f)

    @Composable
    fun <T> entranceSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.8f)

    @Composable
    fun <T> staggerTween(index: Int): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else tween(durationMillis = 400, delayMillis = index * StaggerDelayPerItem)
}

// --- MODELS ---
data class SettingsQuickAction(val icon: Painter, val label: String, val onClick: () -> Unit, val accentColor: Color)
data class SettingsGroup(val title: String, val items: List<SettingsItem>)
data class SettingsItem(
    val icon: Painter, val title: String, val subtitle: String? = null,
    val badge: String? = null, val showUpdateIndicator: Boolean = false,
    val accentColor: Color = Color.Unspecified, val keywords: List<String> = emptyList(),
    val onClick: () -> Unit
)
data class SettingsIntegrationAction(val icon: Painter, val label: String, val onClick: () -> Unit, val accentColor: Color)

// --- MAIN SCREEN ---
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SettingsScreen(
    latestVersion: Long,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val animationsDisabled = LocalAnimationsDisabled.current
    val listState = rememberLazyListState()
    val viewModel: HomeViewModel = hiltViewModel()
    val newsViewModel: NewsViewModel = hiltViewModel()
    val hasUnreadNews by newsViewModel.hasUnreadNews.collectAsState()

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val isLoading = false

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showBetaUpdateDialog by remember { mutableStateOf(false) }

    var hasUpdate by remember { mutableStateOf(false) }
    var fetchedLatestVersion by remember { mutableStateOf(BuildConfig.VERSION_NAME) }

    var hasBetaUpdate by remember { mutableStateOf(false) }
    var fetchedLatestBetaVersion by remember { mutableStateOf(BuildConfig.VERSION_NAME) }

    var showTogetherScreen by remember { mutableStateOf(false) }

    if (showTogetherScreen) {
        com.cgens67.avidtune.together.MusicTogetherScreen(
            navController = navController,
            scrollBehavior = scrollBehavior,
            onBack = { showTogetherScreen = false }
        )
        return
    }

    val prefs = context.getSharedPreferences("settings_search_history", Context.MODE_PRIVATE)
    var searchHistory by remember { mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()) }

    fun saveSearch(q: String) {
        if (q.isBlank()) return
        val newHistory = (listOf(q) + searchHistory).distinct().take(10)
        searchHistory = newHistory
        prefs.edit().putStringSet("history", newHistory.toSet()).apply()
    }

    fun clearHistory() {
        searchHistory = emptyList()
        prefs.edit().putStringSet("history", emptySet()).apply()
    }

    fun removeHistoryItem(q: String) {
        val newHistory = searchHistory.filter { it != q }
        searchHistory = newHistory
        prefs.edit().putStringSet("history", newHistory.toSet()).apply()
    }

    LaunchedEffect(Unit) {
        val newVersion = checkForUpdates()
        if (newVersion != null && isNewerVersion(newVersion, BuildConfig.VERSION_NAME)) {
            hasUpdate = true
            fetchedLatestVersion = newVersion
        }
        val newBetaVersion = checkForBetaUpdates()
        if (newBetaVersion != null && isNewerVersion(newBetaVersion, BuildConfig.VERSION_NAME)) {
            if (newVersion == null || isNewerVersion(newBetaVersion, newVersion)) {
                hasBetaUpdate = true
                fetchedLatestBetaVersion = newBetaVersion
            }
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null

    var isStorageGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED) }
    var isNotificationGranted by remember { mutableStateOf(notificationPermission == null || ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        isStorageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        if (notificationPermission != null) isNotificationGranted = ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isStorageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
                isNotificationGranted = notificationPermission == null || ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldShowPermissionHint = (notificationPermission != null && !isNotificationGranted) || !isStorageGranted
    val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var hasRequestedPermissions by remember { mutableStateOf(settingsPrefs.getBoolean("has_requested_permissions", false)) }

    val resetSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
    }

    val quickActions = buildQuickActions(navController, resetSearch)
    val integrationActions = buildIntegrationActions(navController, resetSearch) { showTogetherScreen = true }
    val settingsGroups = buildSettingsGroups(navController, resetSearch, { navController.navigate("settings/changelog") }, hasUnreadNews)
    val internalItems = buildInternalItems(navController, resetSearch)

    val queryText = query.text.trim()
    val showSearchBar = isSearching || queryText.isNotBlank()

    val filteredQuickActions = if (queryText.isBlank()) emptyList() else quickActions.filter { it.label.contains(queryText, true) }
    val filteredIntegrations = if (queryText.isBlank()) emptyList() else integrationActions.filter { it.label.contains(queryText, true) }
    val filteredGroups = if (queryText.isBlank()) emptyList() else filterSettingsGroups(settingsGroups, queryText, stringResource(R.string.search_results))
    val filteredInternalItems = if (queryText.isBlank()) emptyList() else internalItems.filter { matchesQuery(it, queryText) }
    
    val hasSearchResults by remember(filteredQuickActions, filteredGroups, filteredIntegrations, filteredInternalItems) {
        derivedStateOf { filteredQuickActions.isNotEmpty() || filteredGroups.isNotEmpty() || filteredIntegrations.isNotEmpty() || filteredInternalItems.isNotEmpty() }
    }

    val internalGroup = if (filteredInternalItems.isNotEmpty()) SettingsGroup(title = stringResource(R.string.internal_settings), items = filteredInternalItems) else null

    val contentState = SettingsContentState(
        profileHeader = SettingsProfileState(isLoading, isLoggedIn, accountName, accountEmail, if (isLoggedIn) accountImageUrl else null),
        quickActions = quickActions, integrations = integrationActions, groups = settingsGroups, internalGroup = null,
        showPermissionBanner = shouldShowPermissionHint, showUpdateBanner = hasUpdate, latestVersion = fetchedLatestVersion,
        showBetaUpdateBanner = hasBetaUpdate, latestBetaVersion = fetchedLatestBetaVersion,
        isSearchActive = false, searchQuery = queryText, searchHistory = searchHistory, hasSearchResults = hasSearchResults,
        onProfileHeaderClick = { navController.navigate("settings/account") },
        onRequestPermission = {
            val toRequest = buildList {
                if (!isStorageGranted) add(storagePermission)
                if (!isNotificationGranted && notificationPermission != null) add(notificationPermission)
            }
            if (toRequest.isNotEmpty()) {
                hasRequestedPermissions = true
                settingsPrefs.edit().putBoolean("has_requested_permissions", true).apply()
                permissionLauncher.launch(toRequest.toTypedArray())
            }
        },
        onUpdateClick = { showUpdateDialog = true },
        onBetaUpdateClick = { showBetaUpdateDialog = true },
        onSearchHistoryItemClick = { q -> query = TextFieldValue(q); focusManager.clearFocus(); saveSearch(q) },
        onRemoveSearchHistoryItem = { q -> removeHistoryItem(q) },
        onClearSearchHistory = { clearHistory() }
    )

    val searchState = contentState.copy(isSearchActive = true, quickActions = filteredQuickActions, integrations = filteredIntegrations, groups = filteredGroups, internalGroup = internalGroup)

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = !showSearchBar, enter = fadeIn(), exit = fadeOut()) {
                LargeTopAppBar(
                    title = { Text(text = stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) { Icon(painterResource(R.drawable.arrow_back), null) } },
                    actions = { IconButton(onClick = { isSearching = true }) { Icon(painterResource(R.drawable.search), null) } },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = !showSearchBar, enter = fadeIn(), exit = fadeOut()) {
                AdaptiveSettingsLayout(state = contentState, listState = listState, topPadding = innerPadding.calculateTopPadding(), modifier = Modifier.fillMaxSize())
            }
            AnimatedVisibility(visible = showSearchBar, enter = fadeIn(), exit = fadeOut()) {
                TopSearch(
                    query = query, onQueryChange = { query = it },
                    onSearch = { focusManager.clearFocus(); saveSearch(query.text.trim()) },
                    active = showSearchBar, onActiveChange = { active -> if (active) isSearching = true else resetSearch() },
                    placeholder = { Text(text = stringResource(R.string.search)) },
                    leadingIcon = { IconButton(onClick = { resetSearch() }, onLongClick = { if (queryText.isBlank()) navController.backToMain() }) { Icon(painterResource(R.drawable.arrow_back), null) } },
                    trailingIcon = { if (query.text.isNotBlank()) IconButton(onClick = { query = TextFieldValue() }) { Icon(painterResource(R.drawable.close), null) } },
                    focusRequester = focusRequester
                ) { AdaptiveSettingsLayout(state = searchState, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }

    if (showUpdateDialog) UpdateDownloadDialog(latestVersion = fetchedLatestVersion, isBeta = false, onDismiss = { showUpdateDialog = false })
    if (showBetaUpdateDialog) UpdateDownloadDialog(latestVersion = fetchedLatestBetaVersion, isBeta = true, onDismiss = { showBetaUpdateDialog = false })
}

// --- LOGIC & BUILDERS ---
fun filterSettingsGroups(groups: List<SettingsGroup>, query: String, searchResultsTitle: String): List<SettingsGroup> {
    if (query.isBlank()) return emptyList()
    val allMatchedItems = groups.flatMap { it.items }.filter { matchesQuery(it, query) }
        .sortedBy { it.title.indexOf(query, ignoreCase = true).let { idx -> if (idx < 0) 1000 else idx } }
    if (allMatchedItems.isEmpty()) return emptyList()
    return listOf(SettingsGroup(title = searchResultsTitle, items = allMatchedItems))
}

fun matchesQuery(item: SettingsItem, query: String): Boolean {
    if (item.title.contains(query, true)) return true
    if (item.subtitle?.contains(query, true) == true) return true
    if (item.badge?.contains(query, true) == true) return true
    return item.keywords.any { it.contains(query, true) || query.contains(it, true) }
}

@Composable
private fun buildQuickActions(navController: NavController, resetSearch: () -> Unit): List<SettingsQuickAction> = listOf(
    SettingsQuickAction(painterResource(R.drawable.palette), stringResource(R.string.appearance), { resetSearch(); navController.navigate("settings/appearance") }, MaterialTheme.colorScheme.primary),
    SettingsQuickAction(painterResource(R.drawable.play), stringResource(R.string.player_and_audio), { resetSearch(); navController.navigate("settings/player") }, MaterialTheme.colorScheme.secondary),
    SettingsQuickAction(painterResource(R.drawable.language), stringResource(R.string.content), { resetSearch(); navController.navigate("settings/content") }, MaterialTheme.colorScheme.tertiary),
    SettingsQuickAction(painterResource(R.drawable.storage), stringResource(R.string.storage), { resetSearch(); navController.navigate("settings/storage") }, MaterialTheme.colorScheme.error)
)

@Composable
private fun buildIntegrationActions(navController: NavController, resetSearch: () -> Unit, onTogetherClick: () -> Unit): List<SettingsIntegrationAction> {
    val uriHandler = LocalUriHandler.current
    return listOf(
        SettingsIntegrationAction(painterResource(R.drawable.person), stringResource(R.string.music_together), { resetSearch(); onTogetherClick() }, Color(0xFF1DB954)),
        SettingsIntegrationAction(painterResource(R.drawable.discord), stringResource(R.string.discord), { resetSearch(); navController.navigate("settings/discord") }, Color(0xFF5865F2)),
        SettingsIntegrationAction(painterResource(R.drawable.github), stringResource(R.string.github), { resetSearch(); uriHandler.openUri("https://github.com/cgens67/AvidTune") }, MaterialTheme.colorScheme.onSurface)
    )
}

@Composable
private fun buildSettingsGroups(navController: NavController, resetSearch: () -> Unit, onChangelogClick: () -> Unit, hasUnreadNews: Boolean): List<SettingsGroup> {
    val uriHandler = LocalUriHandler.current; val context = LocalContext.current
    return listOf(
        SettingsGroup(stringResource(R.string.general_settings), listOf(
            SettingsItem(painterResource(R.drawable.person), stringResource(R.string.account), keywords = listOf("account", "login"), onClick = { resetSearch(); navController.navigate("settings/account") }),
            SettingsItem(painterResource(R.drawable.speed), stringResource(R.string.performance), keywords = listOf("performance", "speed"), onClick = { resetSearch(); navController.navigate("settings/performance") }),
            SettingsItem(painterResource(R.drawable.security), stringResource(R.string.privacy), keywords = listOf("privacy", "security"), onClick = { resetSearch(); navController.navigate("settings/privacy") }),
            SettingsItem(painterResource(R.drawable.restore), stringResource(R.string.backup_restore), keywords = listOf("backup", "restore"), onClick = { resetSearch(); navController.navigate("settings/backup_restore") }),
            SettingsItem(painterResource(R.drawable.link), stringResource(R.string.open_supported_links), keywords = listOf("links", "default"), onClick = { 
                resetSearch()
                try { context.startActivity(Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }) } 
                catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }) }
            }),
            SettingsItem(painterResource(R.drawable.info), stringResource(R.string.about), keywords = listOf("about", "info"), onClick = { resetSearch(); navController.navigate("settings/about") })
        )),
        SettingsGroup(stringResource(R.string.community), listOf(
            SettingsItem(painterResource(R.drawable.newspaper), stringResource(R.string.news), badge = if (hasUnreadNews) stringResource(R.string.new_badge) else null, showUpdateIndicator = hasUnreadNews, keywords = listOf("news"), onClick = { resetSearch(); navController.navigate("news") }),
            SettingsItem(painterResource(R.drawable.schedule), stringResource(R.string.Changelog), keywords = listOf("changelog"), onClick = { resetSearch(); onChangelogClick() }),
            SettingsItem(painterResource(R.drawable.telegram), stringResource(R.string.Telegramchanel), keywords = listOf("telegram"), onClick = { resetSearch(); uriHandler.openUri("https://t.me/avidtuneupdates") })
        ))
    )
}

@Composable
private fun buildInternalItems(navController: NavController, resetSearch: () -> Unit): List<SettingsItem> {
    val ctx = LocalContext.current
    return listOf(
        SettingsItem(painterResource(R.drawable.person), stringResource(R.string.login), keywords = listOf("account", "login", "google", "sign in"), onClick = { resetSearch(); navController.navigate("settings/account") }),
        SettingsItem(painterResource(R.drawable.token), stringResource(R.string.advanced_login), keywords = listOf("advanced", "login", "token", "cookie"), onClick = { resetSearch(); navController.navigate("settings/account") }),
        SettingsItem(painterResource(R.drawable.person), stringResource(R.string.use_login_for_browse), keywords = listOf("use", "login", "browse", "account"), onClick = { resetSearch(); navController.navigate("settings/account") }),
        SettingsItem(painterResource(R.drawable.cached), stringResource(R.string.ytm_sync), keywords = listOf("youtube", "music", "sync", "ytm", "playlists"), onClick = { resetSearch(); navController.navigate("settings/account") }),
        SettingsItem(painterResource(R.drawable.palette), stringResource(R.string.enable_dynamic_theme), keywords = listOf("dynamic", "theme", "color", "material you"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.palette), stringResource(R.string.color_palette), keywords = listOf("color", "palette", "custom theme"), onClick = { resetSearch(); navController.navigate("settings/appearance/palette") }),
        SettingsItem(painterResource(R.drawable.dark_mode), stringResource(R.string.dark_theme), keywords = listOf("dark", "light", "theme", "mode", "amoled"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.contrast), stringResource(R.string.pure_black), keywords = listOf("pitch", "black", "amoled", "oled", "dark"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.text_fields), stringResource(R.string.use_system_font), keywords = listOf("font", "system", "text", "typeface"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.format_align_left), stringResource(R.string.app_text_size), keywords = listOf("text", "size", "large", "small", "font"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.language), stringResource(R.string.app_language), keywords = listOf("app", "language", "locale", "translation"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.gradient), stringResource(R.string.player_background_style), keywords = listOf("player", "background", "style", "blur", "gradient"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.line_curve), stringResource(R.string.shape_and_corners), keywords = listOf("thumbnail", "corner", "radius", "shape", "curve"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.palette), stringResource(R.string.player_buttons_style), keywords = listOf("player", "buttons", "style", "primary", "tertiary"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.sliders), stringResource(R.string.player_slider_style), keywords = listOf("player", "slider", "style", "squiggly", "slim"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.swipe), stringResource(R.string.enable_swipe_thumbnail), keywords = listOf("swipe", "thumbnail", "gesture"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.format_align_center), stringResource(R.string.player_text_alignment), keywords = listOf("player", "text", "alignment", "center", "sided"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.lyrics_text_position), keywords = listOf("lyrics", "text", "position", "alignment"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.lyrics_click_change), keywords = listOf("lyrics", "click", "change", "seek"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.artist), stringResource(R.string.turn_on_artist_canvas), keywords = listOf("artist", "canvas", "video", "background"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.nav_bar), stringResource(R.string.default_open_tab), keywords = listOf("default", "open", "tab", "home", "explore", "library"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.tab), stringResource(R.string.default_lib_chips), keywords = listOf("default", "library", "chips", "filter"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.nav_bar), stringResource(R.string.slim_navbar), keywords = listOf("slim", "navbar", "navigation", "bar"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.grid_view), stringResource(R.string.grid_cell_size), keywords = listOf("grid", "cell", "size", "large", "small"), onClick = { resetSearch(); navController.navigate("settings/appearance") }),
        SettingsItem(painterResource(R.drawable.graphic_eq), stringResource(R.string.audio_quality), keywords = listOf("audio", "quality", "high", "low", "auto"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.fast_forward), stringResource(R.string.double_tap_to_seek), keywords = listOf("double", "tap", "seek", "forward", "rewind"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.fast_forward), stringResource(R.string.skip_silence), keywords = listOf("skip", "silence", "audio"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.skip_next), stringResource(R.string.enable_sponsorblock), keywords = listOf("sponsor", "block", "skip", "sponsorblock"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.graphic_eq), stringResource(R.string.premium_audio_fading), keywords = listOf("premium", "audio", "fading", "fade", "crossfade"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.volume_up), stringResource(R.string.audio_normalization), keywords = listOf("audio", "normalization", "volume", "loudness"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.queue_music), stringResource(R.string.persistent_queue), keywords = listOf("persistent", "queue", "save", "restore"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.skip_next), stringResource(R.string.auto_skip_next_on_error), keywords = listOf("auto", "skip", "error", "next"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.clear_all), stringResource(R.string.stop_music_on_task_clear), keywords = listOf("stop", "music", "task", "clear", "kill"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.info), stringResource(R.string.show_nerd_stats), keywords = listOf("nerd", "stats", "info", "technical"), onClick = { resetSearch(); navController.navigate("settings/player") }),
        SettingsItem(painterResource(R.drawable.play), stringResource(R.string.minimal_player_design), keywords = listOf("minimal", "player", "design", "performance"), onClick = { resetSearch(); navController.navigate("settings/performance") }),
        SettingsItem(painterResource(R.drawable.image), stringResource(R.string.disable_blur_effects), keywords = listOf("disable", "blur", "effects", "performance"), onClick = { resetSearch(); navController.navigate("settings/performance") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.animate_lyrics), keywords = listOf("animate", "lyrics", "smooth", "performance"), onClick = { resetSearch(); navController.navigate("settings/performance") }),
        SettingsItem(painterResource(R.drawable.playlist_add), stringResource(R.string.auto_load_more), keywords = listOf("auto", "load", "more", "queue", "network"), onClick = { resetSearch(); navController.navigate("settings/performance") }),
        SettingsItem(painterResource(R.drawable.similar), stringResource(R.string.enable_similar_content), keywords = listOf("enable", "similar", "content", "recommendations"), onClick = { resetSearch(); navController.navigate("settings/performance") }),
        SettingsItem(painterResource(R.drawable.language), stringResource(R.string.content_language), keywords = listOf("content", "language", "locale"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.location_on), stringResource(R.string.content_country), keywords = listOf("content", "country", "region"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.explicit), stringResource(R.string.hide_explicit), keywords = listOf("hide", "explicit", "content", "nsfw"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.play), stringResource(R.string.hide_music_videos), keywords = listOf("hide", "music", "videos", "omv"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.info), stringResource(R.string.notification), keywords = listOf("notification", "permission", "alert"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.wifi_proxy), stringResource(R.string.enable_proxy), keywords = listOf("proxy", "network", "connection"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.enable_lyrics_plus), keywords = listOf("lyrics", "plus", "provider", "ttml"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.enable_better_lyrics), keywords = listOf("better", "lyrics", "provider", "ttml"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.lyrics), "Enable SimpMusic", keywords = listOf("simpmusic", "lyrics", "provider"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.enable_paxsenix), keywords = listOf("paxsenix", "lyrics", "provider"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.enable_lrclib), keywords = listOf("lrclib", "lyrics", "provider", "synced"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.lyrics), stringResource(R.string.enable_kugou), keywords = listOf("kugou", "lyrics", "provider"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.list), stringResource(R.string.lyrics_provider_priority), keywords = listOf("lyrics", "provider", "priority", "order"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.trending_up), stringResource(R.string.top_length), keywords = listOf("top", "length", "size", "playlist"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.home_outlined), stringResource(R.string.set_quick_picks), keywords = listOf("quick", "picks", "home", "last listened"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.history), stringResource(R.string.history_duration), keywords = listOf("history", "duration", "scrobble", "time"), onClick = { resetSearch(); navController.navigate("settings/content") }),
        SettingsItem(painterResource(R.drawable.download), stringResource(R.string.downloaded_songs), keywords = listOf("downloaded", "songs", "storage", "clear"), onClick = { resetSearch(); navController.navigate("settings/storage") }),
        SettingsItem(painterResource(R.drawable.music_note), stringResource(R.string.song_cache), keywords = listOf("song", "cache", "storage", "clear"), onClick = { resetSearch(); navController.navigate("settings/storage") }),
        SettingsItem(painterResource(R.drawable.image), stringResource(R.string.image_cache), keywords = listOf("image", "cache", "storage", "clear"), onClick = { resetSearch(); navController.navigate("settings/storage") }),
        SettingsItem(painterResource(R.drawable.history), stringResource(R.string.pause_listen_history), keywords = listOf("pause", "listen", "history", "privacy"), onClick = { resetSearch(); navController.navigate("settings/privacy") }),
        SettingsItem(painterResource(R.drawable.delete_history), stringResource(R.string.clear_listen_history), keywords = listOf("clear", "listen", "history", "privacy"), onClick = { resetSearch(); navController.navigate("settings/privacy") }),
        SettingsItem(painterResource(R.drawable.search_off), stringResource(R.string.pause_search_history), keywords = listOf("pause", "search", "history", "privacy"), onClick = { resetSearch(); navController.navigate("settings/privacy") }),
        SettingsItem(painterResource(R.drawable.clear_all), stringResource(R.string.clear_search_history), keywords = listOf("clear", "search", "history", "privacy"), onClick = { resetSearch(); navController.navigate("settings/privacy") }),
        SettingsItem(painterResource(R.drawable.screenshot), stringResource(R.string.disable_screenshot), keywords = listOf("disable", "screenshot", "privacy", "secure"), onClick = { resetSearch(); navController.navigate("settings/privacy") }),
        SettingsItem(painterResource(R.drawable.cloud_lock), stringResource(R.string.cloud_upload_title), keywords = listOf("cloud", "upload", "backup", "sync"), onClick = { resetSearch(); navController.navigate("settings/backup_restore") }),
        SettingsItem(painterResource(R.drawable.backup), stringResource(R.string.backup), keywords = listOf("backup", "export", "data"), onClick = { resetSearch(); navController.navigate("settings/backup_restore") }),
        SettingsItem(painterResource(R.drawable.restore), stringResource(R.string.restore), keywords = listOf("restore", "import", "data"), onClick = { resetSearch(); navController.navigate("settings/backup_restore") }),
        SettingsItem(painterResource(R.drawable.replay), stringResource(R.string.visitor_data_title), keywords = listOf("visitor", "data", "reset", "clear"), onClick = { resetSearch(); navController.navigate("settings/backup_restore") }),
        SettingsItem(painterResource(R.drawable.discord), stringResource(R.string.enable_discord_rpc), keywords = listOf("discord", "rpc", "rich presence", "status"), onClick = { resetSearch(); navController.navigate("settings/discord") }),
        SettingsItem(painterResource(R.drawable.info), stringResource(R.string.discord_use_details), keywords = listOf("discord", "details", "status"), onClick = { resetSearch(); navController.navigate("settings/discord") })
    )
}

// --- STATE CONTAINERS ---
data class SettingsProfileState(val isLoading: Boolean, val isLoggedIn: Boolean, val accountName: String, val accountEmail: String, val accountImageUrl: String?)
data class SettingsContentState(
    val profileHeader: SettingsProfileState, val quickActions: List<SettingsQuickAction>, val integrations: List<SettingsIntegrationAction>,
    val groups: List<SettingsGroup>, val internalGroup: SettingsGroup?, val showPermissionBanner: Boolean, val showUpdateBanner: Boolean, val latestVersion: String,
    val showBetaUpdateBanner: Boolean, val latestBetaVersion: String, val isSearchActive: Boolean, val searchQuery: String,
    val searchHistory: List<String>, val hasSearchResults: Boolean, val onProfileHeaderClick: () -> Unit, val onRequestPermission: () -> Unit,
    val onUpdateClick: () -> Unit, val onBetaUpdateClick: () -> Unit, val onSearchHistoryItemClick: (String) -> Unit,
    val onRemoveSearchHistoryItem: (String) -> Unit, val onClearSearchHistory: () -> Unit
)

// --- LAYOUT ENGINE ---
@Composable
fun AdaptiveSettingsLayout(state: SettingsContentState, modifier: Modifier = Modifier, listState: LazyListState = rememberLazyListState(), topPadding: Dp = 0.dp) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    if (screenWidth >= 600) {
        // Tablet / Landscape Two-pane layout
        Row(
            modifier = modifier.fillMaxSize().padding(horizontal = pad).windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            horizontalArrangement = Arrangement.spacedBy(pad)
        ) {
            LazyColumn(modifier = Modifier.weight(0.4f).fillMaxHeight(), contentPadding = PaddingValues(top = topPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(spacing)) {
                if (!state.isSearchActive) item { SettingsProfileHeader(state.profileHeader, state.onProfileHeaderClick) }
                if (state.showPermissionBanner) item { SettingsPermissionBanner(state.onRequestPermission) }
                if (state.showUpdateBanner) item { SettingsUpdateBanner(state.latestVersion, state.onUpdateClick) }
                if (state.showBetaUpdateBanner) item { SettingsBetaUpdateBanner(state.latestBetaVersion, state.onBetaUpdateClick) }
                if (!state.isSearchActive || state.searchQuery.isNotBlank()) {
                    if (state.quickActions.isNotEmpty()) item { SettingsQuickActionsSection(state.quickActions, 2) }
                    if (state.integrations.isNotEmpty()) item { SettingsIntegrationsSection(state.integrations) }
                }
            }
            LazyColumn(modifier = Modifier.weight(0.6f).fillMaxHeight(), contentPadding = PaddingValues(top = topPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(spacing)) {
                buildRightPaneContent(state)
            }
        }
    } else {
        // Phone layout
        LazyColumn(
            state = listState, modifier = modifier.fillMaxSize().padding(horizontal = pad).windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            if (!state.isSearchActive) item { SettingsProfileHeader(state.profileHeader, state.onProfileHeaderClick) }
            if (!state.isSearchActive && state.showPermissionBanner) item { SettingsPermissionBanner(state.onRequestPermission) }
            if (!state.isSearchActive && state.showUpdateBanner) item { SettingsUpdateBanner(state.latestVersion, state.onUpdateClick) }
            if (!state.isSearchActive && state.showBetaUpdateBanner) item { SettingsBetaUpdateBanner(state.latestBetaVersion, state.onBetaUpdateClick) }
            if (state.isSearchActive && state.searchQuery.isBlank() && state.searchHistory.isNotEmpty()) item { SearchHistorySection(state) }
            else if (state.isSearchActive && !state.hasSearchResults) item { SettingsSearchEmpty() }
            else {
                if (state.quickActions.isNotEmpty()) item { SettingsQuickActionsSection(state.quickActions, 2) }
                if (state.integrations.isNotEmpty()) item { SettingsIntegrationsSection(state.integrations) }
                buildRightPaneContent(state)
            }
        }
    }
}

private fun LazyListScope.buildRightPaneContent(state: SettingsContentState) {
    if (state.isSearchActive && state.searchQuery.isBlank() && state.searchHistory.isNotEmpty()) {
        item { SearchHistorySection(state) }
    } else if (state.isSearchActive && !state.hasSearchResults) {
        item { SettingsSearchEmpty() }
    } else {
        if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
            item { SettingsGroupCard(state.internalGroup) }
        }
        items(state.groups.size, key = { state.groups[it].title }) { index ->
            SettingsGroupCard(state.groups[index])
        }
    }
}

@Composable
private fun SearchHistorySection(state: SettingsContentState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.search_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = state.onClearSearchHistory) { Text(stringResource(R.string.clear_search_history)) }
        }
        state.searchHistory.forEach { item ->
            ListItem(
                headlineContent = { Text(item) },
                leadingContent = { Icon(painterResource(R.drawable.history), null) },
                trailingContent = { IconButton(onClick = { state.onRemoveSearchHistoryItem(item) }) { Icon(painterResource(R.drawable.close), null, modifier = Modifier.size(20.dp)) } },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { state.onSearchHistoryItemClick(item) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    }
}

// --- UI COMPONENTS ---
@Composable
fun SettingsProfileHeader(state: SettingsProfileState, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.PressScale else 1f, animationSpec = SettingsAnimations.pressSpring())
    val title = if (state.isLoading) "Loading..." else if (state.isLoggedIn) state.accountName.ifBlank { stringResource(R.string.account) } else stringResource(R.string.login)
    
    Card(
        modifier = Modifier.fillMaxWidth().scale(scale), shape = RoundedCornerShape(SettingsDimensions.HeroCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().clickable(interactionSource = interactionSource, indication = null, onClick = onClick)) {
            // Background Mesh
            Box(modifier = Modifier.matchParentSize().background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent), radius = 600f, center = Offset(0f, 0f))))
            Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // Avatar
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    if (state.isLoading) CircularWavyProgressIndicator(modifier = Modifier.size(32.dp))
                    else if (state.isLoggedIn && !state.accountImageUrl.isNullOrBlank()) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(state.accountImageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize())
                    else Icon(painterResource(if (state.isLoggedIn) R.drawable.account else R.drawable.login), null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.isLoggedIn && state.accountEmail.isNotBlank()) {
                        Text(state.accountEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(stringResource(R.string.account), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(stringResource(R.string.version_name, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(painterResource(R.drawable.arrow_forward), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun SettingsPermissionBanner(onRequestPermission: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onErrorContainer.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.security), null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(stringResource(R.string.permissions_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(0.8f))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(stringResource(R.string.allow))
            }
        }
    }
}

@Composable
fun SettingsUpdateBanner(latestVersion: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onTertiaryContainer.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.update), null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.new_version_available), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(stringResource(R.string.version_name, latestVersion), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.8f))
            }
            Icon(painterResource(R.drawable.arrow_forward), null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun SettingsBetaUpdateBanner(latestVersion: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.update), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.new_beta_version_available), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(stringResource(R.string.beta_version_name, latestVersion), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.8f))
            }
            Icon(painterResource(R.drawable.arrow_forward), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun SettingsGroupCard(group: SettingsGroup) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Text(text = group.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        Card(shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column {
                group.items.forEachIndexed { index, item ->
                    ListItem(
                        modifier = Modifier.clickable { item.onClick() },
                        headlineContent = { Text(item.title, fontWeight = FontWeight.Medium) },
                        supportingContent = item.subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                        leadingContent = {
                            val iconColor = if (item.accentColor.isSpecified) item.accentColor else MaterialTheme.colorScheme.onSurface
                            Box(modifier = Modifier.size(40.dp).background(iconColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                if (item.showUpdateIndicator) BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error, modifier = Modifier.size(8.dp)) }) { Icon(item.icon, null, tint = iconColor, modifier = Modifier.size(20.dp)) }
                                else Icon(item.icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.badge != null) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(end = 8.dp)) { Text(item.badge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) }
                                }
                                Icon(painterResource(R.drawable.arrow_forward), null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (index < group.items.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                }
            }
        }
    }
}

@Composable
fun SettingsQuickActionsSection(actions: List<SettingsQuickAction>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(columns).forEach { rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowActions.forEach { action -> QuickActionCard(action, Modifier.weight(1f)) }
                repeat(columns - rowActions.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun QuickActionCard(action: SettingsQuickAction, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.PressScale else 1f, animationSpec = SettingsAnimations.pressSpring())

    Surface(
        modifier = modifier.scale(scale).aspectRatio(1.2f), shape = RoundedCornerShape(SettingsDimensions.QuickActionCardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh, onClick = action.onClick, interactionSource = interactionSource
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(action.accentColor.copy(0.08f), Color.Transparent))).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(action.accentColor.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(action.icon, null, tint = action.accentColor, modifier = Modifier.size(24.dp))
            }
            Text(action.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SettingsIntegrationsSection(integrations: List<SettingsIntegrationAction>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(integrations.size, key = { integrations[it].label }) { index ->
            IntegrationPill(integrations[index])
        }
    }
}

@Composable
fun IntegrationPill(action: SettingsIntegrationAction) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.PressScale else 1f, animationSpec = SettingsAnimations.pressSpring())

    Surface(
        modifier = Modifier.scale(scale), shape = RoundedCornerShape(SettingsDimensions.IntegrationPillCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHighest, onClick = action.onClick, interactionSource = interactionSource
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(action.icon, null, tint = action.accentColor, modifier = Modifier.size(20.dp))
            Text(action.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SettingsSearchEmpty() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.search), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        }
        Text(stringResource(R.string.no_results_found), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.try_different_search_term), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- UPDATE LOGIC ---
enum class DownloadStatus { NOT_STARTED, DOWNLOADING, COMPLETED, ERROR }

suspend fun downloadApk(context: Context, version: String, onProgressUpdate: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
    try {
        val apkUrl = "https://github.com/cgens67/AvidTune/releases/download/$version/app-universal-release.apk"
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, "app-universal-release-$version.apk")
        if (apkFile.exists()) apkFile.delete()

        val client = OkHttpClient()
        var request = Request.Builder().url(apkUrl).build()
        var response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val altUrl = "https://github.com/cgens67/AvidTune/releases/download/$version/app-release.apk"
            response = client.newCall(Request.Builder().url(altUrl).build()).execute()
            if (!response.isSuccessful) {
                val altUrl2 = "https://github.com/cgens67/AvidTune/releases/download/$version/AvidTune-$version.apk"
                response = client.newCall(Request.Builder().url(altUrl2).build()).execute()
                if (!response.isSuccessful) return@withContext null
            }
        }

        val body = response.body ?: return@withContext null
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(apkFile)
        val buffer = ByteArray(8 * 1024)
        var totalBytesRead = 0L; var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            if (contentLength > 0) withContext(Dispatchers.Main) { onProgressUpdate(totalBytesRead.toFloat() / contentLength.toFloat()) }
        }
        outputStream.flush(); outputStream.close(); inputStream.close()
        withContext(Dispatchers.Main) { onProgressUpdate(1f) }
        return@withContext FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    } catch (e: Exception) { e.printStackTrace(); return@withContext null }
}

fun installApk(context: Context, apkUri: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData("package:${context.packageName}".toUri()))
            return
        }
    }
    context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(apkUri, "application/vnd.android.package-archive"); flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK })
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try { JSONObject(URL("https://api.github.com/repos/cgens67/AvidTune/releases/latest").openConnection().apply { connect() }.getInputStream().bufferedReader().use { it.readText() }).getString("tag_name") }
    catch (e: Exception) { null }
}

suspend fun checkForBetaUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val arr = org.json.JSONArray(URL("https://api.github.com/repos/cgens67/AvidTune/releases").openConnection().apply { connect() }.getInputStream().bufferedReader().use { it.readText() })
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).getBoolean("prerelease")) return@withContext arr.getJSONObject(i).getString("tag_name")
        return@withContext null
    } catch (e: Exception) { null }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val cleanRemote = remoteVersion.removePrefix("v").substringBefore("-")
    val cleanCurrent = currentVersion.removePrefix("v").substringBefore("-")
    val remote = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
    val current = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0; val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    val rTag = remoteVersion.substringAfter("-", ""); val cTag = currentVersion.substringAfter("-", "")
    if (rTag.isEmpty() && cTag.isNotEmpty()) return true
    if (rTag.isNotEmpty() && cTag.isEmpty()) return false
    if (rTag.isNotEmpty() && cTag.isNotEmpty()) return rTag > cTag
    return false
}

@Composable
fun UpdateDownloadDialog(latestVersion: String, isBeta: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.canRequestPackageInstalls() && uri != null) installApk(context, uri!!) }

    Dialog(onDismissRequest = { if (status != DownloadStatus.DOWNLOADING) onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.update), null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
                Text(text = if (isBeta) stringResource(R.string.update_beta_version, latestVersion) else stringResource(R.string.update_version, latestVersion), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                when (status) {
                    DownloadStatus.NOT_STARTED -> {
                        Text(if (isBeta) stringResource(R.string.download_beta_update_prompt) else stringResource(R.string.download_update_prompt), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(android.R.string.cancel)) }
                            Button(onClick = { status = DownloadStatus.DOWNLOADING; scope.launch { uri = downloadApk(context, latestVersion) { progress = it }; status = if (uri != null) DownloadStatus.COMPLETED else DownloadStatus.ERROR } }) { Text(stringResource(R.string.download)) }
                        }
                    }
                    DownloadStatus.DOWNLOADING -> {
                        Text(stringResource(R.string.downloading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    DownloadStatus.COMPLETED -> {
                        Text(stringResource(R.string.download_completed), color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.close)) }
                            Button(onClick = {
                                if (uri != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) launcher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData("package:${context.packageName}".toUri()))
                                    else installApk(context, uri!!)
                                }
                            }) { Text(stringResource(R.string.install)) }
                        }
                    }
                    DownloadStatus.ERROR -> {
                        Text(stringResource(R.string.download_error), color = MaterialTheme.colorScheme.error)
                        Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}
