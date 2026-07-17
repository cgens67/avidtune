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
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
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

// --- DIMENSIONS & ANIMATIONS (EXPRESSIVE) ---

object SettingsDimensions {
    val GroupCardCornerRadius = 32.dp
    val QuickActionCardCornerRadius = 32.dp
    val IntegrationPillCornerRadius = 100.dp
    val BannerCardCornerRadius = 36.dp
    val HeroCardCornerRadius = 40.dp
    val RowIconCornerRadius = 24.dp

    val ScreenHorizontalPadding = 20.dp
    val SectionSpacing = 24.dp
    val RowVerticalPadding = 18.dp
    val RowHorizontalPadding = 20.dp

    val RowIconSize = 48.dp
    val RowIconInnerSize = 24.dp
    val QuickActionIconSize = 64.dp
    val QuickActionIconInnerSize = 32.dp
    val HeroIconSize = 80.dp
    val HeroIconInnerSize = 40.dp
    val IntegrationIconSize = 40.dp
    val IntegrationIconInnerSize = 20.dp
    val ChevronSize = 20.dp

    val SectionHeaderBottomPadding = 12.dp
    val SectionHeaderHorizontalPadding = 24.dp

    val QuickActionTileAspectRatio = 1.0f

    val CompactColumns = 2
    val MediumColumns = 4
    val ExpandedColumns = 4

    val MediumPaneLeftWeight = 0.45f
    val MediumPaneRightWeight = 0.55f
    val ExpandedListPaneWidth = 420.dp
}

object SettingsAnimations {
    val PressScale = 0.94f
    val TilePressScale = 0.90f
    val PillPressScale = 0.92f
    val IconPressRotation = 12f
    val PillPressLift = (-4).dp

    val EntranceSlideDuration = 450
    val StaggerDelayPerItem = 60
    val ExitFadeDuration = 250

    @Composable
    fun <T> pressSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

    @Composable
    fun <T> entranceSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)

    @Composable
    fun <T> exitTween(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else tween(durationMillis = ExitFadeDuration)

    @Composable
    fun <T> fadeTween(durationMillis: Int): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else tween(durationMillis = durationMillis)

    @Composable
    fun <T> staggerTween(index: Int): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else tween(durationMillis = EntranceSlideDuration, delayMillis = index * StaggerDelayPerItem)
}

// --- MODELS ---

data class SettingsQuickAction(
    val icon: Painter,
    val label: String,
    val onClick: () -> Unit,
    val accentColor: Color,
)

data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>,
)

data class SettingsItem(
    val icon: Painter,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val showUpdateIndicator: Boolean = false,
    val accentColor: Color = Color.Unspecified,
    val keywords: List<String> = emptyList(),
    val onClick: () -> Unit,
)

data class SettingsIntegrationAction(
    val icon: Painter,
    val label: String,
    val onClick: () -> Unit,
    val accentColor: Color,
)

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
    var searchHistory by remember {
        mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList())
    }

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
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    var isStorageGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            notificationPermission == null ||
                ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        isStorageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        if (notificationPermission != null) {
            isNotificationGranted = ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isStorageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
                isNotificationGranted = notificationPermission == null ||
                    ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldShowPermissionHint = if (notificationPermission != null) {
        !isNotificationGranted
    } else {
        !isStorageGranted
    }

    val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var hasRequestedPermissions by remember {
        mutableStateOf(settingsPrefs.getBoolean("has_requested_permissions", false))
    }

    val resetSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
    }

    val quickActions = buildQuickActions(navController, resetSearch)
    val integrationActions = buildIntegrationActions(navController, resetSearch) { showTogetherScreen = true }
    val settingsGroups = buildSettingsGroups(navController, resetSearch, onChangelogClick = { navController.navigate("settings/changelog") }, hasUnreadNews)
    val internalItems = buildInternalItems(navController, resetSearch)

    val queryText = query.text.trim()
    val showSearchBar = isSearching || queryText.isNotBlank()

    val searchResultsTitle = stringResource(R.string.search_results)

    val filteredQuickActions = if (queryText.isBlank()) emptyList() else filterQuickActions(quickActions, queryText)
    val filteredIntegrations = if (queryText.isBlank()) emptyList() else filterIntegrations(integrationActions, queryText)
    val filteredGroups = if (queryText.isBlank()) emptyList() else filterSettingsGroups(settingsGroups, queryText, searchResultsTitle)
    val filteredInternalItems = if (queryText.isBlank()) emptyList() else filterInternalItems(internalItems, queryText)

    val hasSearchResults by remember(
        filteredQuickActions,
        filteredGroups,
        filteredIntegrations,
        filteredInternalItems,
    ) {
        derivedStateOf {
            filteredQuickActions.isNotEmpty() ||
                filteredGroups.isNotEmpty() ||
                filteredIntegrations.isNotEmpty() ||
                filteredInternalItems.isNotEmpty()
        }
    }

    val internalGroup = if (filteredInternalItems.isNotEmpty()) {
        SettingsGroup(
            title = stringResource(R.string.internal_settings),
            items = filteredInternalItems,
        )
    } else null

    val contentState = SettingsContentState(
        profileHeader = SettingsProfileState(
            isLoading = isLoading,
            isLoggedIn = isLoggedIn,
            accountName = accountName,
            accountEmail = accountEmail,
            accountImageUrl = if (isLoggedIn) accountImageUrl else null,
        ),
        quickActions = quickActions,
        integrations = integrationActions,
        groups = settingsGroups,
        internalGroup = null,
        showPermissionBanner = shouldShowPermissionHint,
        showUpdateBanner = hasUpdate,
        latestVersion = fetchedLatestVersion,
        showBetaUpdateBanner = hasBetaUpdate,
        latestBetaVersion = fetchedLatestBetaVersion,
        isSearchActive = false,
        searchQuery = queryText,
        searchHistory = searchHistory,
        hasSearchResults = hasSearchResults,
        onProfileHeaderClick = { navController.navigate("settings/account") },
        onRequestPermission = {
            val toRequest = buildList {
                if (!isStorageGranted) add(storagePermission)
                if (!isNotificationGranted && notificationPermission != null) add(notificationPermission)
            }
            if (toRequest.isNotEmpty()) {
                var currentContext = context
                var activity: android.app.Activity? = null
                while (currentContext is android.content.ContextWrapper) {
                    if (currentContext is android.app.Activity) {
                        activity = currentContext
                        break
                    }
                    currentContext = currentContext.baseContext
                }

                val shouldShowRationale = activity != null && toRequest.any {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }

                if (hasRequestedPermissions && !shouldShowRationale) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } else {
                    hasRequestedPermissions = true
                    settingsPrefs.edit().putBoolean("has_requested_permissions", true).apply()
                    permissionLauncher.launch(toRequest.toTypedArray())
                }
            }
        },
        onUpdateClick = { showUpdateDialog = true },
        onBetaUpdateClick = { showBetaUpdateDialog = true },
        onSearchHistoryItemClick = { clickedQuery ->
            query = TextFieldValue(clickedQuery)
            focusManager.clearFocus()
            saveSearch(clickedQuery)
        },
        onRemoveSearchHistoryItem = { q -> removeHistoryItem(q) },
        onClearSearchHistory = { clearHistory() },
    )

    val searchState = contentState.copy(
        isSearchActive = true,
        quickActions = filteredQuickActions,
        integrations = filteredIntegrations,
        groups = filteredGroups,
        internalGroup = internalGroup,
    )

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !showSearchBar,
                enter = fadeIn(SettingsAnimations.fadeTween(if (animationsDisabled) 0 else 220)),
                exit = fadeOut(SettingsAnimations.fadeTween(if (animationsDisabled) 0 else 160)),
            ) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain,
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearching = true },
                            onLongClick = {},
                        ) {
                            Icon(
                                painterResource(R.drawable.search),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !showSearchBar,
                enter = fadeIn(SettingsAnimations.fadeTween(if (animationsDisabled) 0 else 220)),
                exit = fadeOut(SettingsAnimations.fadeTween(if (animationsDisabled) 0 else 160)),
            ) {
                AdaptiveSettingsLayout(
                    state = contentState,
                    listState = listState,
                    topPadding = innerPadding.calculateTopPadding(),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedVisibility(
                visible = showSearchBar,
                enter = fadeIn(SettingsAnimations.fadeTween(if (animationsDisabled) 0 else 220)),
                exit = fadeOut(SettingsAnimations.fadeTween(if (animationsDisabled) 0 else 160)),
            ) {
                TopSearch(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { 
                        focusManager.clearFocus() 
                        saveSearch(query.text.trim())
                    },
                    active = showSearchBar,
                    onActiveChange = { active ->
                        if (active) isSearching = true else resetSearch()
                    },
                    placeholder = { 
                        Text(text = stringResource(R.string.search), style = MaterialTheme.typography.titleMedium) 
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = { resetSearch() },
                            onLongClick = { if (queryText.isBlank()) navController.backToMain() },
                        ) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                        }
                    },
                    trailingIcon = {
                        if (query.text.isNotBlank()) {
                            IconButton(onClick = { query = TextFieldValue() }) {
                                Icon(painterResource(R.drawable.close), contentDescription = null)
                            }
                        }
                    },
                    focusRequester = focusRequester,
                ) {
                    AdaptiveSettingsLayout(
                        state = searchState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showUpdateDialog) {
        UpdateDownloadDialog(
            latestVersion = fetchedLatestVersion,
            isBeta = false,
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showBetaUpdateDialog) {
        UpdateDownloadDialog(
            latestVersion = fetchedLatestBetaVersion,
            isBeta = true,
            onDismiss = { showBetaUpdateDialog = false }
        )
    }
}

// --- SEARCH & FILTER LOGIC ---

fun filterQuickActions(actions: List<SettingsQuickAction>, query: String): List<SettingsQuickAction> {
    if (query.isBlank()) return emptyList()
    return actions.filter { it.label.contains(query, ignoreCase = true) }
}

fun filterSettingsGroups(groups: List<SettingsGroup>, query: String, searchResultsTitle: String): List<SettingsGroup> {
    if (query.isBlank()) return emptyList()
    val allMatchedItems = groups.flatMap { it.items }.filter { matchesQuery(it, query) }
        .sortedBy { it.title.indexOf(query, ignoreCase = true).let { idx -> if (idx < 0) 1000 else idx } }
    if (allMatchedItems.isEmpty()) return emptyList()
    return listOf(SettingsGroup(title = searchResultsTitle, items = allMatchedItems))
}

fun matchesQuery(item: SettingsItem, query: String): Boolean {
    if (item.title.contains(query, ignoreCase = true)) return true
    if (item.subtitle?.contains(query, ignoreCase = true) == true) return true
    if (item.badge?.contains(query, ignoreCase = true) == true) return true
    return item.keywords.any { it.contains(query, ignoreCase = true) || query.contains(it, ignoreCase = true) }
}

fun filterInternalItems(items: List<SettingsItem>, query: String): List<SettingsItem> {
    if (query.isBlank()) return emptyList()
    return items.filter { matchesQuery(it, query) }
}

fun filterIntegrations(integrations: List<SettingsIntegrationAction>, query: String): List<SettingsIntegrationAction> {
    if (query.isBlank()) return emptyList()
    return integrations.filter { it.label.contains(query, ignoreCase = true) }
}

// --- BUILDER FUNCTIONS ---

@Composable
private fun buildQuickActions(navController: NavController, resetSearch: () -> Unit): List<SettingsQuickAction> {
    return listOf(
        SettingsQuickAction(
            icon = painterResource(R.drawable.palette),
            label = stringResource(R.string.appearance),
            onClick = { resetSearch(); navController.navigate("settings/appearance") },
            accentColor = MaterialTheme.colorScheme.primary
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.play),
            label = stringResource(R.string.player_and_audio),
            onClick = { resetSearch(); navController.navigate("settings/player") },
            accentColor = MaterialTheme.colorScheme.secondary
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.language),
            label = stringResource(R.string.content),
            onClick = { resetSearch(); navController.navigate("settings/content") },
            accentColor = MaterialTheme.colorScheme.tertiary
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.storage),
            label = stringResource(R.string.storage),
            onClick = { resetSearch(); navController.navigate("settings/storage") },
            accentColor = MaterialTheme.colorScheme.error
        )
    )
}

@Composable
private fun buildIntegrationActions(
    navController: NavController, 
    resetSearch: () -> Unit,
    onTogetherClick: () -> Unit
): List<SettingsIntegrationAction> {
    val uriHandler = LocalUriHandler.current
    return listOf(
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.person),
            label = stringResource(R.string.music_together),
            onClick = { resetSearch(); onTogetherClick() },
            accentColor = Color(0xFF1ED760) // Brighter Spotify-esque Expressive Green
        ),
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.discord),
            label = stringResource(R.string.discord),
            onClick = { resetSearch(); navController.navigate("settings/discord") },
            accentColor = Color(0xFF5865F2)
        ),
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.github),
            label = stringResource(R.string.github),
            onClick = { resetSearch(); uriHandler.openUri("https://github.com/cgens67/AvidTune") },
            accentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun buildSettingsGroups(
    navController: NavController,
    resetSearch: () -> Unit,
    onChangelogClick: () -> Unit,
    hasUnreadNews: Boolean
): List<SettingsGroup> {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    return listOf(
        SettingsGroup(
            title = stringResource(R.string.general_settings),
            items = listOf(
                SettingsItem(
                    icon = painterResource(R.drawable.person),
                    title = stringResource(R.string.account),
                    keywords = listOf("account", "login", "profile"),
                    onClick = { resetSearch(); navController.navigate("settings/account") }
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.speed),
                    title = stringResource(R.string.performance),
                    keywords = listOf("performance", "speed", "blur", "minimal"),
                    onClick = { resetSearch(); navController.navigate("settings/performance") }
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.security),
                    title = stringResource(R.string.privacy),
                    keywords = listOf("privacy", "history", "security"),
                    onClick = { resetSearch(); navController.navigate("settings/privacy") }
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.restore),
                    title = stringResource(R.string.backup_restore),
                    keywords = listOf("backup", "restore", "data"),
                    onClick = { resetSearch(); navController.navigate("settings/backup_restore") }
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.link),
                    title = stringResource(R.string.open_supported_links),
                    keywords = listOf("open", "supported", "links", "default"),
                    onClick = { 
                        resetSearch()
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }
                        }
                        try { context.startActivity(intent) } catch (e: Exception) { e.printStackTrace() }
                    }
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = stringResource(R.string.about),
                    keywords = listOf("about", "info", "version"),
                    onClick = { resetSearch(); navController.navigate("settings/about") }
                )
            )
        ),
        SettingsGroup(
            title = stringResource(R.string.community),
            items = listOf(
                SettingsItem(
                    icon = painterResource(R.drawable.newspaper),
                    title = stringResource(R.string.news),
                    badge = if (hasUnreadNews) stringResource(R.string.new_badge) else null,
                    showUpdateIndicator = hasUnreadNews,
                    keywords = listOf("news", "updates", "announcements"),
                    onClick = { resetSearch(); navController.navigate("news") }
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.schedule),
                    title = stringResource(R.string.Changelog),
                    keywords = listOf("changelog", "updates", "features"),
                    onClick = { resetSearch(); onChangelogClick() }
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.telegram),
                    title = stringResource(R.string.Telegramchanel),
                    keywords = listOf("telegram", "community", "channel"),
                    onClick = { resetSearch(); uriHandler.openUri("https://t.me/avidtuneupdates") }
                )
            )
        )
    )
}

@Composable
private fun buildInternalItems(navController: NavController, resetSearch: () -> Unit): List<SettingsItem> {
    val context = LocalContext.current
    return listOf(
        SettingsItem(
            icon = painterResource(R.drawable.person),
            title = stringResource(R.string.login),
            keywords = listOf("account", "login", "google", "sign in"),
            onClick = { resetSearch(); navController.navigate("settings/account") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.enable_dynamic_theme),
            keywords = listOf("dynamic", "theme", "color", "material you"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.graphic_eq),
            title = stringResource(R.string.audio_quality),
            keywords = listOf("audio", "quality", "high", "low", "auto"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.play),
            title = stringResource(R.string.minimal_player_design),
            keywords = listOf("minimal", "player", "design", "performance"),
            onClick = { resetSearch(); navController.navigate("settings/performance") }
        )
    )
}

// --- LAYOUT ENGINE ---

enum class SettingsLayoutMode { COMPACT, MEDIUM, EXPANDED }

@Composable
fun resolveLayoutMode(): SettingsLayoutMode {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidth >= 840 -> SettingsLayoutMode.EXPANDED
        screenWidth >= 600 -> SettingsLayoutMode.MEDIUM
        else -> SettingsLayoutMode.COMPACT
    }
}

data class SettingsProfileState(
    val isLoading: Boolean,
    val isLoggedIn: Boolean,
    val accountName: String,
    val accountEmail: String,
    val accountImageUrl: String?,
)

data class SettingsContentState(
    val profileHeader: SettingsProfileState,
    val quickActions: List<SettingsQuickAction>,
    val integrations: List<SettingsIntegrationAction>,
    val groups: List<SettingsGroup>,
    val internalGroup: SettingsGroup?,
    val showPermissionBanner: Boolean,
    val showUpdateBanner: Boolean,
    val latestVersion: String,
    val showBetaUpdateBanner: Boolean,
    val latestBetaVersion: String,
    val isSearchActive: Boolean,
    val searchQuery: String,
    val searchHistory: List<String>,
    val hasSearchResults: Boolean,
    val onProfileHeaderClick: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onUpdateClick: () -> Unit,
    val onBetaUpdateClick: () -> Unit,
    val onSearchHistoryItemClick: (String) -> Unit,
    val onRemoveSearchHistoryItem: (String) -> Unit,
    val onClearSearchHistory: () -> Unit,
)

private fun LazyListScope.SearchHistorySection(state: SettingsContentState, pad: Dp) {
    if (state.searchHistory.isNotEmpty()) {
        item(key = "search_history_header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = pad, vertical = 12.dp).animateItem(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_history),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = state.onClearSearchHistory) {
                    Text(stringResource(R.string.clear_search_history), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        items(state.searchHistory, key = { "history_$it" }) { historyItem ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 6.dp)
                    .animateItem()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { state.onSearchHistoryItemClick(historyItem) },
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = historyItem,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { state.onRemoveSearchHistoryItem(historyItem) }, modifier = Modifier.size(36.dp)) {
                        Icon(painter = painterResource(R.drawable.close), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "search_history_spacer") { Spacer(modifier = Modifier.height(24.dp).animateItem()) }
    }
}

@Composable
fun AdaptiveSettingsLayout(
    state: SettingsContentState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
) {
    val layoutMode = resolveLayoutMode()
    val animationsDisabled = LocalAnimationsDisabled.current

    var heroVisible by remember { mutableStateOf(false) }
    var bannerVisible by remember { mutableStateOf(false) }
    var quickActionsVisible by remember { mutableStateOf(false) }
    var integrationsVisible by remember { mutableStateOf(false) }
    var categoriesVisible by remember { mutableStateOf(false) }

    LaunchedEffect(animationsDisabled) {
        if (animationsDisabled) {
            heroVisible = true; bannerVisible = true; quickActionsVisible = true; integrationsVisible = true; categoriesVisible = true
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(50)); heroVisible = true
        anim.animateTo(1f, tween(60)); bannerVisible = true
        anim.animateTo(1f, tween(60)); quickActionsVisible = true
        anim.animateTo(1f, tween(70)); integrationsVisible = true
        anim.animateTo(1f, tween(70)); categoriesVisible = true
    }

    val quickActionColumns = when (layoutMode) {
        SettingsLayoutMode.COMPACT -> SettingsDimensions.CompactColumns
        SettingsLayoutMode.MEDIUM -> SettingsDimensions.MediumColumns
        SettingsLayoutMode.EXPANDED -> SettingsDimensions.ExpandedColumns
    }

    when (layoutMode) {
        SettingsLayoutMode.COMPACT -> CompactSettingsLayout(state, listState, quickActionColumns, heroVisible, bannerVisible, quickActionsVisible, integrationsVisible, categoriesVisible, topPadding, modifier)
        SettingsLayoutMode.MEDIUM -> MediumSettingsLayout(state, quickActionColumns, heroVisible, bannerVisible, quickActionsVisible, integrationsVisible, categoriesVisible, topPadding, modifier)
        SettingsLayoutMode.EXPANDED -> ExpandedSettingsLayout(state, quickActionColumns, heroVisible, bannerVisible, quickActionsVisible, integrationsVisible, categoriesVisible, topPadding, modifier)
    }
}

@Composable
private fun CompactSettingsLayout(
    state: SettingsContentState, listState: LazyListState, quickActionColumns: Int,
    heroVisible: Boolean, bannerVisible: Boolean, quickActionsVisible: Boolean, integrationsVisible: Boolean, categoriesVisible: Boolean,
    topPadding: Dp, modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
        contentPadding = PaddingValues(top = topPadding, bottom = 48.dp),
    ) {
        if (!state.isSearchActive) {
            item(key = "hero") {
                AnimatedVisibility(
                    visible = heroVisible,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) + slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = SettingsAnimations.entranceSpring()),
                ) {
                    SettingsProfileHeader(state = state.profileHeader, onClick = state.onProfileHeaderClick, modifier = Modifier.padding(horizontal = pad).padding(top = 8.dp, bottom = spacing))
                }
            }
            item(key = "permission") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showPermissionBanner,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()),
                    exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                ) {
                    SettingsPermissionBanner(onRequestPermission = state.onRequestPermission, modifier = Modifier.padding(horizontal = pad).padding(bottom = spacing))
                }
            }
            item(key = "update") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showUpdateBanner,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()),
                    exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                ) {
                    SettingsUpdateBanner(latestVersion = state.latestVersion, onClick = state.onUpdateClick, modifier = Modifier.padding(horizontal = pad).padding(bottom = spacing))
                }
            }
            item(key = "beta_update") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showBetaUpdateBanner,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()),
                    exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                ) {
                    SettingsBetaUpdateBanner(latestVersion = state.latestBetaVersion, onClick = state.onBetaUpdateClick, modifier = Modifier.padding(horizontal = pad).padding(bottom = spacing))
                }
            }
        }

        if (state.isSearchActive && state.searchQuery.isBlank()) {
            SearchHistorySection(state, pad)
        } else if (state.isSearchActive && !state.hasSearchResults) {
            item(key = "empty") {
                Spacer(modifier = Modifier.height(32.dp).animateItem())
                SettingsSearchEmpty(modifier = Modifier.padding(horizontal = pad).animateItem())
            }
        } else {
            if (state.quickActions.isNotEmpty()) {
                item(key = "quickActions") {
                    AnimatedVisibility(
                        modifier = Modifier.animateItem(),
                        visible = quickActionsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsQuickActionsSection(actions = state.quickActions, columns = quickActionColumns, modifier = Modifier.padding(horizontal = pad).padding(bottom = spacing))
                    }
                }
            }
            if (state.integrations.isNotEmpty()) {
                item(key = "integrations") {
                    AnimatedVisibility(
                        modifier = Modifier.animateItem(),
                        visible = integrationsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsIntegrationsSection(integrations = state.integrations, modifier = Modifier.padding(horizontal = pad).padding(bottom = spacing))
                    }
                }
            }
            if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                item(key = "internalSearchResults") {
                    SettingsGroupCard(group = state.internalGroup, modifier = Modifier.padding(horizontal = pad).padding(bottom = spacing).animateItem())
                }
            }
            items(count = state.groups.size, key = { state.groups[it].title }) { index ->
                AnimatedVisibility(
                    modifier = Modifier.animateItem(),
                    visible = categoriesVisible,
                    enter = fadeIn(SettingsAnimations.staggerTween(index)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = SettingsAnimations.staggerTween(index)),
                ) {
                    SettingsGroupCard(group = state.groups[index], modifier = Modifier.padding(horizontal = pad).padding(bottom = spacing))
                }
            }
        }
    }
}

@Composable
private fun MediumSettingsLayout(
    state: SettingsContentState, quickActionColumns: Int, heroVisible: Boolean, bannerVisible: Boolean, quickActionsVisible: Boolean, integrationsVisible: Boolean, categoriesVisible: Boolean,
    topPadding: Dp, modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    Row(
        modifier = modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(horizontal = pad),
        horizontalArrangement = Arrangement.spacedBy(pad),
    ) {
        LazyColumn(modifier = Modifier.weight(SettingsDimensions.MediumPaneLeftWeight).fillMaxHeight(), contentPadding = PaddingValues(top = topPadding, bottom = 48.dp)) {
            if (!state.isSearchActive) {
                item(key = "hero") { AnimatedVisibility(visible = heroVisible, enter = fadeIn(SettingsAnimations.entranceSpring())) { SettingsProfileHeader(state = state.profileHeader, onClick = state.onProfileHeaderClick, modifier = Modifier.padding(top = 8.dp, bottom = spacing)) } }
                item(key = "permission") { AnimatedVisibility(visible = bannerVisible && state.showPermissionBanner, enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()), exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween())) { SettingsPermissionBanner(onRequestPermission = state.onRequestPermission, modifier = Modifier.padding(bottom = spacing)) } }
                item(key = "update") { AnimatedVisibility(visible = bannerVisible && state.showUpdateBanner, enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()), exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween())) { SettingsUpdateBanner(latestVersion = state.latestVersion, onClick = state.onUpdateClick, modifier = Modifier.padding(bottom = spacing)) } }
                item(key = "beta_update") { AnimatedVisibility(visible = bannerVisible && state.showBetaUpdateBanner, enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()), exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween())) { SettingsBetaUpdateBanner(latestVersion = state.latestBetaVersion, onClick = state.onBetaUpdateClick, modifier = Modifier.padding(bottom = spacing)) } }
            }
            if (!state.isSearchActive || state.searchQuery.isNotBlank()) {
                if (state.quickActions.isNotEmpty()) { item(key = "quickActions") { AnimatedVisibility(modifier = Modifier.animateItem(), visible = quickActionsVisible, enter = fadeIn(SettingsAnimations.entranceSpring())) { SettingsQuickActionsSection(actions = state.quickActions, columns = 2, modifier = Modifier.padding(bottom = spacing)) } } }
                if (state.integrations.isNotEmpty()) { item(key = "integrations") { AnimatedVisibility(modifier = Modifier.animateItem(), visible = integrationsVisible, enter = fadeIn(SettingsAnimations.entranceSpring())) { SettingsIntegrationsSection(integrations = state.integrations, modifier = Modifier.padding(bottom = spacing)) } } }
            }
        }
        LazyColumn(modifier = Modifier.weight(SettingsDimensions.MediumPaneRightWeight).fillMaxHeight(), contentPadding = PaddingValues(top = topPadding, bottom = 48.dp)) {
            if (state.isSearchActive && state.searchQuery.isBlank()) { SearchHistorySection(state, 0.dp) }
            else if (state.isSearchActive && !state.hasSearchResults) { item(key = "empty") { Spacer(modifier = Modifier.height(32.dp).animateItem()); SettingsSearchEmpty(modifier = Modifier.animateItem()) } }
            else {
                if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) { item(key = "internalSearchResults") { SettingsGroupCard(group = state.internalGroup, modifier = Modifier.padding(bottom = spacing).animateItem()) } }
                items(count = state.groups.size, key = { state.groups[it].title }) { index -> AnimatedVisibility(modifier = Modifier.animateItem(), visible = categoriesVisible, enter = fadeIn(SettingsAnimations.staggerTween(index)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = SettingsAnimations.staggerTween(index))) { SettingsGroupCard(group = state.groups[index], modifier = Modifier.padding(bottom = spacing)) } }
            }
        }
    }
}

@Composable
private fun ExpandedSettingsLayout(
    state: SettingsContentState, quickActionColumns: Int, heroVisible: Boolean, bannerVisible: Boolean, quickActionsVisible: Boolean, integrationsVisible: Boolean, categoriesVisible: Boolean,
    topPadding: Dp, modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    Row(
        modifier = modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(horizontal = pad),
        horizontalArrangement = Arrangement.spacedBy(pad),
    ) {
        LazyColumn(modifier = Modifier.width(SettingsDimensions.ExpandedListPaneWidth).fillMaxHeight(), contentPadding = PaddingValues(top = topPadding, bottom = 48.dp)) {
            if (!state.isSearchActive) {
                item(key = "hero") { AnimatedVisibility(visible = heroVisible, enter = fadeIn(SettingsAnimations.entranceSpring())) { SettingsProfileHeader(state = state.profileHeader, onClick = state.onProfileHeaderClick, modifier = Modifier.padding(top = 8.dp, bottom = spacing)) } }
                item(key = "permission") { AnimatedVisibility(visible = bannerVisible && state.showPermissionBanner, enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()), exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween())) { SettingsPermissionBanner(onRequestPermission = state.onRequestPermission, modifier = Modifier.padding(bottom = spacing)) } }
                item(key = "update") { AnimatedVisibility(visible = bannerVisible && state.showUpdateBanner, enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()), exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween())) { SettingsUpdateBanner(latestVersion = state.latestVersion, onClick = state.onUpdateClick, modifier = Modifier.padding(bottom = spacing)) } }
                item(key = "beta_update") { AnimatedVisibility(visible = bannerVisible && state.showBetaUpdateBanner, enter = fadeIn(SettingsAnimations.entranceSpring()) + expandVertically(SettingsAnimations.entranceSpring()), exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween())) { SettingsBetaUpdateBanner(latestVersion = state.latestBetaVersion, onClick = state.onBetaUpdateClick, modifier = Modifier.padding(bottom = spacing)) } }
            }
            if (!state.isSearchActive || state.searchQuery.isNotBlank()) {
                if (state.quickActions.isNotEmpty()) { item(key = "quickActions") { AnimatedVisibility(modifier = Modifier.animateItem(), visible = quickActionsVisible, enter = fadeIn(SettingsAnimations.entranceSpring())) { SettingsQuickActionsSection(actions = state.quickActions, columns = 2, modifier = Modifier.padding(bottom = spacing)) } } }
                if (state.integrations.isNotEmpty()) { item(key = "integrations") { AnimatedVisibility(modifier = Modifier.animateItem(), visible = integrationsVisible, enter = fadeIn(SettingsAnimations.entranceSpring())) { SettingsIntegrationsSection(integrations = state.integrations, modifier = Modifier.padding(bottom = spacing)) } } }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(top = topPadding, bottom = 48.dp)) {
            if (state.isSearchActive && state.searchQuery.isBlank()) { SearchHistorySection(state, 0.dp) }
            else if (state.isSearchActive && !state.hasSearchResults) { item(key = "empty") { Spacer(modifier = Modifier.height(32.dp).animateItem()); SettingsSearchEmpty(modifier = Modifier.animateItem()) } }
            else {
                if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) { item(key = "internalSearchResults") { SettingsGroupCard(group = state.internalGroup, modifier = Modifier.padding(bottom = spacing).animateItem()) } }
                items(count = state.groups.size, key = { state.groups[it].title }) { index -> AnimatedVisibility(modifier = Modifier.animateItem(), visible = categoriesVisible, enter = fadeIn(SettingsAnimations.staggerTween(index)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = SettingsAnimations.staggerTween(index))) { SettingsGroupCard(group = state.groups[index], modifier = Modifier.padding(bottom = spacing)) } }
            }
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun SettingsProfileHeader(
    state: SettingsProfileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.PressScale else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "profileHeaderScale",
    )
    val title = if (state.isLoading) "Loading..." else if (state.isLoggedIn) state.accountName.ifBlank { stringResource(R.string.account) } else stringResource(R.string.login)

    Card(
        modifier = modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(SettingsDimensions.HeroCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    modifier = Modifier.size(SettingsDimensions.HeroIconSize).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoading) {
                        CircularWavyProgressIndicator(modifier = Modifier.size(40.dp), color = MaterialTheme.colorScheme.primary)
                    } else if (state.isLoggedIn && !state.accountImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(state.accountImageUrl).crossfade(true).build(),
                            contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Icon(painter = painterResource(if (state.isLoggedIn) R.drawable.account else R.drawable.login), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(SettingsDimensions.HeroIconInnerSize))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = stringResource(R.string.account), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)) {
                        Text(text = stringResource(R.string.version_name, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.arrow_forward), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(R.drawable.security), contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.permissions_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(text = stringResource(R.string.permissions_desc), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiaryContainer, contentColor = MaterialTheme.colorScheme.tertiaryContainer),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(text = stringResource(R.string.allow), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun SettingsUpdateBanner(latestVersion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.PressScale else 1f, animationSpec = SettingsAnimations.pressSpring(), label = "updateScale")

    Card(
        modifier = modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(R.drawable.update), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.new_version_available), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(text = stringResource(R.string.version_name, latestVersion), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            }
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.arrow_forward), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun SettingsBetaUpdateBanner(latestVersion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.PressScale else 1f, animationSpec = SettingsAnimations.pressSpring(), label = "betaUpdateScale")

    Card(
        modifier = modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(R.drawable.update), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.new_beta_version_available), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(text = stringResource(R.string.beta_version_name, latestVersion), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
            }
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.arrow_forward), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun SettingsSearchEmpty(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.search), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            }
            Text(text = stringResource(R.string.no_results_found), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(R.string.try_different_search_term), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SettingsGroupCard(group: SettingsGroup, modifier: Modifier = Modifier) {
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = SettingsDimensions.SectionHeaderHorizontalPadding, vertical = SettingsDimensions.SectionHeaderBottomPadding),
        )
        Card(
            shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.animateContentSize()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                group.items.forEach { item -> SettingsRow(item = item) }
            }
        }
    }
}

@Composable
fun SettingsRow(item: SettingsItem, modifier: Modifier = Modifier) {
    val effectiveAccent = if (item.accentColor.isSpecified) item.accentColor else MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = SettingsAnimations.pressSpring(), label = "rowScale")
    val bgAlpha by animateFloatAsState(targetValue = if (isPressed) 1f else 0f, animationSpec = SettingsAnimations.pressSpring(), label = "rowBgAlpha")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(SettingsDimensions.RowIconCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = item.onClick)
            .padding(horizontal = SettingsDimensions.RowHorizontalPadding - 8.dp, vertical = SettingsDimensions.RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(SettingsDimensions.RowIconSize).clip(RoundedCornerShape(16.dp)).background(effectiveAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.showUpdateIndicator) {
                BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp)) }) {
                    Icon(painter = item.icon, contentDescription = null, tint = effectiveAccent, modifier = Modifier.size(SettingsDimensions.RowIconInnerSize))
                }
            } else {
                Icon(painter = item.icon, contentDescription = null, tint = effectiveAccent, modifier = Modifier.size(SettingsDimensions.RowIconInnerSize))
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            item.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = if (item.showUpdateIndicator) effectiveAccent else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item.badge?.let { badge ->
            Spacer(modifier = Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(text = badge, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(painter = painterResource(R.drawable.arrow_forward), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(SettingsDimensions.ChevronSize))
    }
}

@Composable
fun SettingsQuickActionsSection(actions: List<SettingsQuickAction>, columns: Int = SettingsDimensions.CompactColumns, modifier: Modifier = Modifier) {
    if (actions.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val rows = actions.chunked(columns)
        rows.forEach { rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                rowActions.forEach { action -> QuickActionCard(action = action, modifier = Modifier.weight(1f)) }
                repeat(columns - rowActions.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun QuickActionCard(action: SettingsQuickAction, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.TilePressScale else 1f, animationSpec = SettingsAnimations.pressSpring(), label = "tileScale")
    val iconRotation by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.IconPressRotation else 0f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "iconRotation")

    Surface(
        modifier = modifier.scale(scale).aspectRatio(SettingsDimensions.QuickActionTileAspectRatio),
        shape = RoundedCornerShape(SettingsDimensions.QuickActionCardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = action.onClick,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(SettingsDimensions.QuickActionIconSize).clip(CircleShape).background(action.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = action.icon, contentDescription = action.label, tint = action.accentColor, modifier = Modifier.size(SettingsDimensions.QuickActionIconInnerSize).graphicsLayer { rotationZ = iconRotation })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = action.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SettingsIntegrationsSection(integrations: List<SettingsIntegrationAction>, modifier: Modifier = Modifier) {
    if (integrations.isEmpty()) return
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(count = integrations.size, key = { integrations[it].label }) { index -> IntegrationPill(action = integrations[index]) }
    }
}

@Composable
fun IntegrationPill(action: SettingsIntegrationAction, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.PillPressScale else 1f, animationSpec = SettingsAnimations.pressSpring(), label = "pillScale")
    val lift by animateFloatAsState(targetValue = if (isPressed) SettingsAnimations.PillPressLift.value else 0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "pillLift")

    Surface(
        modifier = modifier.scale(scale).graphicsLayer { translationY = lift },
        shape = RoundedCornerShape(SettingsDimensions.IntegrationPillCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = action.onClick,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(SettingsDimensions.IntegrationIconSize).clip(CircleShape).background(action.accentColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(painter = action.icon, contentDescription = null, tint = action.accentColor, modifier = Modifier.size(SettingsDimensions.IntegrationIconInnerSize))
            }
            Text(text = action.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// --- UPDATE DIALOG (EXPRESSIVE) ---

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
            request = Request.Builder().url(altUrl).build()
            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val altUrl2 = "https://github.com/cgens67/AvidTune/releases/download/$version/AvidTune-$version.apk"
                request = Request.Builder().url(altUrl2).build()
                response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
            }
        }

        val body = response.body ?: return@withContext null
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(apkFile)
        val buffer = ByteArray(8 * 1024)
        var totalBytesRead = 0L
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            if (contentLength > 0) {
                val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                withContext(Dispatchers.Main) { onProgressUpdate(progress) }
            }
        }
        outputStream.flush(); outputStream.close(); inputStream.close()
        withContext(Dispatchers.Main) { onProgressUpdate(1f) }
        return@withContext FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    } catch (e: Exception) { e.printStackTrace(); return@withContext null }
}

fun installApk(context: Context, apkUri: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData("package:${context.packageName}".toUri())
            context.startActivity(intent)
            return
        }
    }
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(installIntent)
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/cgens67/AvidTune/releases/latest")
        val connection = url.openConnection(); connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        JSONObject(json).getString("tag_name")
    } catch (e: Exception) { e.printStackTrace(); null }
}

suspend fun checkForBetaUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/cgens67/AvidTune/releases")
        val connection = url.openConnection(); connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonArray = org.json.JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val release = jsonArray.getJSONObject(i)
            if (release.getBoolean("prerelease")) return@withContext release.getString("tag_name")
        }
        null
    } catch (e: Exception) { e.printStackTrace(); null }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val cleanRemote = remoteVersion.removePrefix("v").substringBefore("-")
    val cleanCurrent = currentVersion.removePrefix("v").substringBefore("-")
    val remote = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
    val current = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    val remoteTag = remoteVersion.substringAfter("-", "")
    val currentTag = currentVersion.substringAfter("-", "")
    if (remoteTag.isEmpty() && currentTag.isNotEmpty()) return true
    if (remoteTag.isNotEmpty() && currentTag.isEmpty()) return false
    if (remoteTag.isNotEmpty() && currentTag.isNotEmpty()) return remoteTag > currentTag
    return false
}

@Composable
fun UpdateDownloadDialog(latestVersion: String, isBeta: Boolean = false, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }
    val downloadScope = rememberCoroutineScope()

    val installPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls() && downloadedApkUri != null) installApk(context, downloadedApkUri!!)
        }
    }

    Dialog(onDismissRequest = { if (downloadStatus != DownloadStatus.DOWNLOADING) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(if (isBeta) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.update), contentDescription = null, tint = if (isBeta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = if (isBeta) stringResource(R.string.update_beta_version, latestVersion) else stringResource(R.string.update_version, latestVersion), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                when (downloadStatus) {
                    DownloadStatus.NOT_STARTED -> {
                        Text(text = if (isBeta) stringResource(R.string.download_beta_update_prompt) else stringResource(R.string.download_update_prompt), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(android.R.string.cancel), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                            Button(onClick = {
                                downloadStatus = DownloadStatus.DOWNLOADING
                                downloadScope.launch {
                                    downloadedApkUri = downloadApk(context, latestVersion) { progress -> downloadProgress = progress }
                                    if (downloadedApkUri != null) { downloadStatus = DownloadStatus.COMPLETED; downloadProgress = 1f } else { downloadStatus = DownloadStatus.ERROR }
                                }
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                Text(stringResource(R.string.download), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    DownloadStatus.DOWNLOADING -> {
                        Text(stringResource(R.string.downloading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), strokeCap = StrokeCap.Round)
                        Text(text = "${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
                    }
                    DownloadStatus.COMPLETED -> {
                        Text(stringResource(R.string.download_completed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.close), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                            Button(onClick = {
                                if (downloadedApkUri != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                        installPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData("package:${context.packageName}".toUri()))
                                    } else installApk(context, downloadedApkUri!!)
                                }
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                Text(stringResource(R.string.install), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    DownloadStatus.ERROR -> {
                        Text(stringResource(R.string.download_error), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
