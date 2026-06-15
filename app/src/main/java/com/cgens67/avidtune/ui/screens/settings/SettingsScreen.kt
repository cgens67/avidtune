@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.focus.focusRequester
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
import kotlinx.coroutines.delay
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
    val GroupCardCornerRadius = 16.dp
    val QuickActionCardCornerRadius = 20.dp
    val IntegrationPillCornerRadius = 14.dp
    val BannerCardCornerRadius = 20.dp
    val HeroCardCornerRadius = 24.dp
    val RowIconCornerRadius = 12.dp

    val ScreenHorizontalPadding = 16.dp
    val SectionSpacing = 14.dp
    val RowVerticalPadding = 14.dp
    val RowHorizontalPadding = 16.dp

    val RowIconSize = 36.dp
    val RowIconInnerSize = 20.dp
    val QuickActionIconSize = 40.dp
    val QuickActionIconInnerSize = 22.dp
    val HeroIconSize = 56.dp
    val HeroIconInnerSize = 30.dp
    val IntegrationIconSize = 28.dp
    val IntegrationIconInnerSize = 16.dp
    val BannerIconSize = 44.dp
    val BannerIconInnerSize = 22.dp
    val ChevronSize = 18.dp

    val DividerThickness = 0.5.dp
    val DividerStartIndent = 60.dp

    val SectionHeaderBottomPadding = 6.dp
    val SectionHeaderHorizontalPadding = 20.dp

    val QuickActionTileAspectRatio = 1.4f

    val CompactColumns = 2
    val MediumColumns = 4
    val ExpandedColumns = 4

    val MediumPaneLeftWeight = 0.42f
    val MediumPaneRightWeight = 0.58f
    val ExpandedListPaneWidth = 380.dp
}

object SettingsAnimations {
    val PressScale = 0.97f
    val TilePressScale = 0.94f
    val PillPressScale = 0.95f
    val IconPressRotation = 5f
    val PillPressLift = (-2).dp

    val EntranceSlideDuration = 350
    val StaggerDelayPerItem = 80
    val ExitFadeDuration = 200

    @Composable
    fun <T> pressSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else spring(stiffness = Spring.StiffnessHigh)

    @Composable
    fun <T> entranceSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) snap()
        else spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f)

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    latestVersion: Long,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val animationsDisabled = LocalAnimationsDisabled.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
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

    var hasUpdate by remember { mutableStateOf(false) }
    var fetchedLatestVersion by remember { mutableStateOf(BuildConfig.VERSION_NAME) }

    var showTogetherScreen by remember { mutableStateOf(false) }

    if (showTogetherScreen) {
        com.cgens67.avidtune.together.MusicTogetherScreen(
            navController = navController,
            scrollBehavior = scrollBehavior,
            onBack = { showTogetherScreen = false }
        )
        return // Stop rendering SettingsScreen beneath it
    }

    // Search History State
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

    // Observe lifecycle events to detect when returning from app settings
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

    // Determine banner visibility. Hide immediately when notifications are allowed (Tiramisu+)
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
        isSearchActive = false,
        searchQuery = queryText,
        searchHistory = searchHistory,
        hasSearchResults = hasSearchResults,
        onProfileHeaderClick = { navController.navigate("settings/account") },
        onRequestPermission = {
            val toRequest = buildList {
                if (!isStorageGranted) add(storagePermission)
                if (!isNotificationGranted && notificationPermission != null) {
                    add(notificationPermission)
                }
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

                // Go directly to settings if previously denied, bypassing silent failures
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
                            fontWeight = FontWeight.Bold,
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
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
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
                        if (active) {
                            isSearching = true
                        } else {
                            resetSearch()
                        }
                    },
                    placeholder = { Text(text = stringResource(R.string.search)) },
                    leadingIcon = {
                        IconButton(
                            onClick = { resetSearch() },
                            onLongClick = {
                                if (queryText.isBlank()) {
                                    navController.backToMain()
                                }
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                            )
                        }
                    },
                    trailingIcon = {
                        Row {
                            if (query.text.isNotBlank()) {
                                IconButton(
                                    onClick = { query = TextFieldValue() },
                                    onLongClick = {},
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = null,
                                    )
                                }
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
            onDismiss = { showUpdateDialog = false }
        )
    }
}

// --- SEARCH & FILTER LOGIC ---

fun filterQuickActions(
    actions: List<SettingsQuickAction>,
    query: String,
): List<SettingsQuickAction> {
    if (query.isBlank()) return emptyList()
    return actions.filter { it.label.contains(query, ignoreCase = true) }
}

fun filterSettingsGroups(
    groups: List<SettingsGroup>,
    query: String,
    searchResultsTitle: String
): List<SettingsGroup> {
    if (query.isBlank()) return emptyList()
    val allMatchedItems = groups.flatMap { it.items }.filter { matchesQuery(it, query) }
        .sortedBy { it.title.indexOf(query, ignoreCase = true).let { idx -> if (idx < 0) 1000 else idx } }
    
    if (allMatchedItems.isEmpty()) return emptyList()
    
    return listOf(SettingsGroup(title = searchResultsTitle, items = allMatchedItems))
}

fun matchesQuery(
    item: SettingsItem,
    query: String,
): Boolean {
    if (item.title.contains(query, ignoreCase = true)) return true
    if (item.subtitle?.contains(query, ignoreCase = true) == true) return true
    if (item.badge?.contains(query, ignoreCase = true) == true) return true
    return item.keywords.any { keyword ->
        keyword.contains(query, ignoreCase = true) ||
            query.contains(keyword, ignoreCase = true)
    }
}

fun filterInternalItems(
    items: List<SettingsItem>,
    query: String,
): List<SettingsItem> {
    if (query.isBlank()) return emptyList()
    return items.filter { matchesQuery(it, query) }
}

fun filterIntegrations(
    integrations: List<SettingsIntegrationAction>,
    query: String,
): List<SettingsIntegrationAction> {
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
            accentColor = MaterialTheme.colorScheme.onSurface
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
            accentColor = Color(0xFF1DB954)
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
    return listOf(
        // Appearance
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.enable_dynamic_theme),
            keywords = listOf("dynamic", "theme", "color", "material you"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.color_palette),
            keywords = listOf("color", "palette", "custom theme"),
            onClick = { resetSearch(); navController.navigate("settings/appearance/palette") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.dark_mode),
            title = stringResource(R.string.dark_theme),
            keywords = listOf("dark", "light", "theme", "mode", "amoled"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.contrast),
            title = stringResource(R.string.pure_black),
            keywords = listOf("pitch", "black", "amoled", "oled", "dark"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.format_align_left),
            title = stringResource(R.string.use_system_font),
            keywords = listOf("font", "system", "text", "typeface"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.format_align_left),
            title = stringResource(R.string.app_text_size),
            keywords = listOf("text", "size", "large", "small", "font"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.gradient),
            title = stringResource(R.string.player_background_style),
            keywords = listOf("player", "background", "style", "blur", "gradient"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.player_buttons_style),
            keywords = listOf("player", "buttons", "style", "primary", "tertiary"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.sliders),
            title = stringResource(R.string.player_slider_style),
            keywords = listOf("player", "slider", "style", "squiggly", "slim"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.swipe),
            title = stringResource(R.string.enable_swipe_thumbnail),
            keywords = listOf("swipe", "thumbnail", "gesture"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.format_align_center),
            title = stringResource(R.string.player_text_alignment),
            keywords = listOf("player", "text", "alignment", "center", "sided"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.lyrics),
            title = stringResource(R.string.lyrics_text_position),
            keywords = listOf("lyrics", "text", "position", "alignment"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.lyrics),
            title = stringResource(R.string.lyrics_click_change),
            keywords = listOf("lyrics", "click", "change", "seek"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.nav_bar),
            title = stringResource(R.string.default_open_tab),
            keywords = listOf("default", "open", "tab", "home", "explore", "library"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.tab),
            title = stringResource(R.string.default_lib_chips),
            keywords = listOf("default", "library", "chips", "filter"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.nav_bar),
            title = stringResource(R.string.slim_navbar),
            keywords = listOf("slim", "navbar", "navigation", "bar"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.grid_view),
            title = stringResource(R.string.grid_cell_size),
            keywords = listOf("grid", "cell", "size", "large", "small"),
            onClick = { resetSearch(); navController.navigate("settings/appearance") }
        ),
        
        // Player
        SettingsItem(
            icon = painterResource(R.drawable.graphic_eq),
            title = stringResource(R.string.audio_quality),
            keywords = listOf("audio", "quality", "high", "low", "auto"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.fast_forward),
            title = stringResource(R.string.skip_silence),
            keywords = listOf("skip", "silence", "audio"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.skip_next),
            title = stringResource(R.string.enable_sponsorblock),
            keywords = listOf("sponsor", "block", "skip", "sponsorblock"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.volume_up),
            title = stringResource(R.string.audio_normalization),
            keywords = listOf("audio", "normalization", "volume", "loudness"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.queue_music),
            title = stringResource(R.string.persistent_queue),
            keywords = listOf("persistent", "queue", "save", "restore"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.skip_next),
            title = stringResource(R.string.auto_skip_next_on_error),
            keywords = listOf("auto", "skip", "error", "next"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.clear_all),
            title = stringResource(R.string.stop_music_on_task_clear),
            keywords = listOf("stop", "music", "task", "clear", "kill"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.info),
            title = stringResource(R.string.show_nerd_stats),
            keywords = listOf("nerd", "stats", "info", "technical"),
            onClick = { resetSearch(); navController.navigate("settings/player") }
        ),

        // Performance
        SettingsItem(
            icon = painterResource(R.drawable.play),
            title = stringResource(R.string.minimal_player_design),
            keywords = listOf("minimal", "player", "design", "performance"),
            onClick = { resetSearch(); navController.navigate("settings/performance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.image),
            title = stringResource(R.string.disable_blur_effects),
            keywords = listOf("disable", "blur", "effects", "performance"),
            onClick = { resetSearch(); navController.navigate("settings/performance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.lyrics),
            title = stringResource(R.string.animate_lyrics),
            keywords = listOf("animate", "lyrics", "smooth", "performance"),
            onClick = { resetSearch(); navController.navigate("settings/performance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.playlist_add),
            title = stringResource(R.string.auto_load_more),
            keywords = listOf("auto", "load", "more", "queue", "network"),
            onClick = { resetSearch(); navController.navigate("settings/performance") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.similar),
            title = stringResource(R.string.enable_similar_content),
            keywords = listOf("enable", "similar", "content", "recommendations"),
            onClick = { resetSearch(); navController.navigate("settings/performance") }
        ),

        // Content
        SettingsItem(
            icon = painterResource(R.drawable.language),
            title = stringResource(R.string.content_language),
            keywords = listOf("content", "language", "locale"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.location_on),
            title = stringResource(R.string.content_country),
            keywords = listOf("content", "country", "region"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.explicit),
            title = stringResource(R.string.hide_explicit),
            keywords = listOf("hide", "explicit", "content", "nsfw"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.wifi_proxy),
            title = stringResource(R.string.enable_proxy),
            keywords = listOf("proxy", "network", "connection"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.list),
            title = stringResource(R.string.lyrics_provider_priority),
            keywords = listOf("lyrics", "provider", "priority", "order"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.trending_up),
            title = stringResource(R.string.top_length),
            keywords = listOf("top", "length", "size", "playlist"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.home_outlined),
            title = stringResource(R.string.set_quick_picks),
            keywords = listOf("quick", "picks", "home", "last listened"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.history),
            title = stringResource(R.string.history_duration),
            keywords = listOf("history", "duration", "scrobble", "time"),
            onClick = { resetSearch(); navController.navigate("settings/content") }
        ),

        // Storage
        SettingsItem(
            icon = painterResource(R.drawable.download),
            title = stringResource(R.string.downloaded_songs),
            keywords = listOf("downloaded", "songs", "storage", "clear"),
            onClick = { resetSearch(); navController.navigate("settings/storage") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.music_note),
            title = stringResource(R.string.song_cache),
            keywords = listOf("song", "cache", "storage", "clear"),
            onClick = { resetSearch(); navController.navigate("settings/storage") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.image),
            title = stringResource(R.string.image_cache),
            keywords = listOf("image", "cache", "storage", "clear"),
            onClick = { resetSearch(); navController.navigate("settings/storage") }
        ),

        // Privacy
        SettingsItem(
            icon = painterResource(R.drawable.history),
            title = stringResource(R.string.pause_listen_history),
            keywords = listOf("pause", "listen", "history", "privacy"),
            onClick = { resetSearch(); navController.navigate("settings/privacy") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.delete_history),
            title = stringResource(R.string.clear_listen_history),
            keywords = listOf("clear", "listen", "history", "privacy"),
            onClick = { resetSearch(); navController.navigate("settings/privacy") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.search_off),
            title = stringResource(R.string.pause_search_history),
            keywords = listOf("pause", "search", "history", "privacy"),
            onClick = { resetSearch(); navController.navigate("settings/privacy") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.clear_all),
            title = stringResource(R.string.clear_search_history),
            keywords = listOf("clear", "search", "history", "privacy"),
            onClick = { resetSearch(); navController.navigate("settings/privacy") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.screenshot),
            title = stringResource(R.string.disable_screenshot),
            keywords = listOf("disable", "screenshot", "privacy", "secure"),
            onClick = { resetSearch(); navController.navigate("settings/privacy") }
        ),

        // Backup & Restore
        SettingsItem(
            icon = painterResource(R.drawable.cloud_lock),
            title = stringResource(R.string.cloud_upload_title),
            keywords = listOf("cloud", "upload", "backup", "sync"),
            onClick = { resetSearch(); navController.navigate("settings/backup_restore") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.backup),
            title = stringResource(R.string.backup),
            keywords = listOf("backup", "export", "data"),
            onClick = { resetSearch(); navController.navigate("settings/backup_restore") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.restore),
            title = stringResource(R.string.restore),
            keywords = listOf("restore", "import", "data"),
            onClick = { resetSearch(); navController.navigate("settings/backup_restore") }
        ),
        SettingsItem(
            icon = painterResource(R.drawable.replay),
            title = stringResource(R.string.visitor_data_title),
            keywords = listOf("visitor", "data", "reset", "clear"),
            onClick = { resetSearch(); navController.navigate("settings/backup_restore") }
        )
    )
}

// --- LAYOUT ENGINE ---

enum class SettingsLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Composable
fun resolveLayoutMode(): SettingsLayoutMode {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
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
    val isSearchActive: Boolean,
    val searchQuery: String,
    val searchHistory: List<String>,
    val hasSearchResults: Boolean,
    val onProfileHeaderClick: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onUpdateClick: () -> Unit,
    val onSearchHistoryItemClick: (String) -> Unit,
    val onRemoveSearchHistoryItem: (String) -> Unit,
    val onClearSearchHistory: () -> Unit,
)

private fun LazyListScope.SearchHistorySection(state: SettingsContentState, pad: Dp) {
    if (state.searchHistory.isNotEmpty()) {
        item(key = "search_history_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 8.dp)
                    .animateItem(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = state.onClearSearchHistory) {
                    Text(stringResource(R.string.clear_search_history))
                }
            }
        }
        items(state.searchHistory, key = { "history_$it" }) { historyItem ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad, vertical = 4.dp)
                    .animateItem()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { state.onSearchHistoryItemClick(historyItem) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = historyItem,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { state.onRemoveSearchHistoryItem(historyItem) },
                        onLongClick = {},
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        item(key = "search_history_spacer") {
            Spacer(modifier = Modifier.height(16.dp).animateItem())
        }
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
            heroVisible = true
            bannerVisible = true
            quickActionsVisible = true
            integrationsVisible = true
            categoriesVisible = true
            return@LaunchedEffect
        }

        val anim = Animatable(0f)
        anim.animateTo(1f, tween(50))
        heroVisible = true
        anim.animateTo(1f, tween(60))
        bannerVisible = true
        anim.animateTo(1f, tween(60))
        quickActionsVisible = true
        anim.animateTo(1f, tween(70))
        integrationsVisible = true
        anim.animateTo(1f, tween(70))
        categoriesVisible = true
    }

    val quickActionColumns = when (layoutMode) {
        SettingsLayoutMode.COMPACT -> SettingsDimensions.CompactColumns
        SettingsLayoutMode.MEDIUM -> SettingsDimensions.MediumColumns
        SettingsLayoutMode.EXPANDED -> SettingsDimensions.ExpandedColumns
    }

    when (layoutMode) {
        SettingsLayoutMode.COMPACT -> {
            CompactSettingsLayout(
                state = state,
                listState = listState,
                quickActionColumns = quickActionColumns,
                heroVisible = heroVisible,
                bannerVisible = bannerVisible,
                quickActionsVisible = quickActionsVisible,
                integrationsVisible = integrationsVisible,
                categoriesVisible = categoriesVisible,
                topPadding = topPadding,
                modifier = modifier,
            )
        }
        SettingsLayoutMode.MEDIUM -> {
            MediumSettingsLayout(
                state = state,
                quickActionColumns = quickActionColumns,
                heroVisible = heroVisible,
                bannerVisible = bannerVisible,
                quickActionsVisible = quickActionsVisible,
                integrationsVisible = integrationsVisible,
                categoriesVisible = categoriesVisible,
                topPadding = topPadding,
                modifier = modifier,
            )
        }
        SettingsLayoutMode.EXPANDED -> {
            ExpandedSettingsLayout(
                state = state,
                quickActionColumns = quickActionColumns,
                heroVisible = heroVisible,
                bannerVisible = bannerVisible,
                quickActionsVisible = quickActionsVisible,
                integrationsVisible = integrationsVisible,
                categoriesVisible = categoriesVisible,
                topPadding = topPadding,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CompactSettingsLayout(
    state: SettingsContentState,
    listState: LazyListState,
    quickActionColumns: Int,
    heroVisible: Boolean,
    bannerVisible: Boolean,
    quickActionsVisible: Boolean,
    integrationsVisible: Boolean,
    categoriesVisible: Boolean,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            ),
        contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
    ) {
        if (!state.isSearchActive) {
            item(key = "hero") {
                AnimatedVisibility(
                    visible = heroVisible,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) +
                        slideInVertically(
                            initialOffsetY = { -it / 5 },
                            animationSpec = SettingsAnimations.entranceSpring(),
                        ),
                ) {
                    SettingsProfileHeader(
                        state = state.profileHeader,
                        onClick = state.onProfileHeaderClick,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(top = 4.dp, bottom = spacing),
                    )
                }
            }

            item(key = "permission") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showPermissionBanner,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) +
                        expandVertically(SettingsAnimations.entranceSpring()),
                    exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                ) {
                    SettingsPermissionBanner(
                        onRequestPermission = state.onRequestPermission,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }

            item(key = "update") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showUpdateBanner,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) +
                        expandVertically(SettingsAnimations.entranceSpring()),
                    exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                ) {
                    SettingsUpdateBanner(
                        latestVersion = state.latestVersion,
                        onClick = state.onUpdateClick,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }
        }

        if (state.isSearchActive && state.searchQuery.isBlank()) {
            SearchHistorySection(state, pad)
        } else if (state.isSearchActive && !state.hasSearchResults) {
            item(key = "empty") {
                Spacer(modifier = Modifier.height(24.dp).animateItem())
                SettingsSearchEmpty(
                    modifier = Modifier.padding(horizontal = pad).animateItem(),
                )
            }
        } else {
            if (state.quickActions.isNotEmpty()) {
                item(key = "quickActions") {
                    AnimatedVisibility(
                        modifier = Modifier.animateItem(),
                        visible = quickActionsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            slideInVertically(
                                initialOffsetY = { it / 6 },
                                animationSpec = SettingsAnimations.entranceSpring(),
                            ),
                    ) {
                        SettingsQuickActionsSection(
                            actions = state.quickActions,
                            columns = quickActionColumns,
                            modifier = Modifier
                                .padding(horizontal = pad)
                                .padding(bottom = spacing),
                        )
                    }
                }
            }

            if (state.integrations.isNotEmpty()) {
                item(key = "integrations") {
                    AnimatedVisibility(
                        modifier = Modifier.animateItem(),
                        visible = integrationsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            slideInVertically(
                                initialOffsetY = { it / 6 },
                                animationSpec = SettingsAnimations.entranceSpring(),
                            ),
                    ) {
                        SettingsIntegrationsSection(
                            integrations = state.integrations,
                            modifier = Modifier
                                .padding(horizontal = pad)
                                .padding(bottom = spacing),
                        )
                    }
                }
            }

            if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                item(key = "internalSearchResults") {
                    SettingsGroupCard(
                        group = state.internalGroup,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing)
                            .animateItem(),
                    )
                }
            }

            items(
                count = state.groups.size,
                key = { state.groups[it].title },
            ) { index ->
                val group = state.groups[index]
                AnimatedVisibility(
                    modifier = Modifier.animateItem(),
                    visible = categoriesVisible,
                    enter = fadeIn(
                        SettingsAnimations.staggerTween(index)
                    ) + slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = SettingsAnimations.staggerTween(index),
                    ),
                ) {
                    SettingsGroupCard(
                        group = group,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediumSettingsLayout(
    state: SettingsContentState,
    quickActionColumns: Int,
    heroVisible: Boolean,
    bannerVisible: Boolean,
    quickActionsVisible: Boolean,
    integrationsVisible: Boolean,
    categoriesVisible: Boolean,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    Row(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(horizontal = pad),
        horizontalArrangement = Arrangement.spacedBy(pad),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(SettingsDimensions.MediumPaneLeftWeight)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            if (!state.isSearchActive) {
                item(key = "hero") {
                    AnimatedVisibility(
                        visible = heroVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsProfileHeader(
                            state = state.profileHeader,
                            onClick = state.onProfileHeaderClick,
                            modifier = Modifier.padding(top = 4.dp, bottom = spacing),
                        )
                    }
                }

                item(key = "permission") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showPermissionBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsPermissionBanner(
                            onRequestPermission = state.onRequestPermission,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }

                item(key = "update") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showUpdateBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsUpdateBanner(
                            latestVersion = state.latestVersion,
                            onClick = state.onUpdateClick,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }

            if (!state.isSearchActive || state.searchQuery.isNotBlank()) {
                if (state.quickActions.isNotEmpty()) {
                    item(key = "quickActions") {
                        AnimatedVisibility(
                            modifier = Modifier.animateItem(),
                            visible = quickActionsVisible,
                            enter = fadeIn(SettingsAnimations.entranceSpring()),
                        ) {
                            SettingsQuickActionsSection(
                                actions = state.quickActions,
                                columns = 2,
                                modifier = Modifier.padding(bottom = spacing),
                            )
                        }
                    }
                }

                if (state.integrations.isNotEmpty()) {
                    item(key = "integrations") {
                        AnimatedVisibility(
                            modifier = Modifier.animateItem(),
                            visible = integrationsVisible,
                            enter = fadeIn(SettingsAnimations.entranceSpring()),
                        ) {
                            SettingsIntegrationsSection(
                                integrations = state.integrations,
                                modifier = Modifier.padding(bottom = spacing),
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(SettingsDimensions.MediumPaneRightWeight)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            if (state.isSearchActive && state.searchQuery.isBlank()) {
                SearchHistorySection(state, 0.dp)
            } else if (state.isSearchActive && !state.hasSearchResults) {
                item(key = "empty") {
                    Spacer(modifier = Modifier.height(24.dp).animateItem())
                    SettingsSearchEmpty(modifier = Modifier.animateItem())
                }
            } else {
                if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                    item(key = "internalSearchResults") {
                        SettingsGroupCard(
                            group = state.internalGroup,
                            modifier = Modifier.padding(bottom = spacing).animateItem(),
                        )
                    }
                }

                items(
                    count = state.groups.size,
                    key = { state.groups[it].title },
                ) { index ->
                    AnimatedVisibility(
                        modifier = Modifier.animateItem(),
                        visible = categoriesVisible,
                        enter = fadeIn(
                            SettingsAnimations.staggerTween(index)
                        ) + slideInVertically(
                            initialOffsetY = { it / 5 },
                            animationSpec = SettingsAnimations.staggerTween(index),
                        ),
                    ) {
                        SettingsGroupCard(
                            group = state.groups[index],
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedSettingsLayout(
    state: SettingsContentState,
    quickActionColumns: Int,
    heroVisible: Boolean,
    bannerVisible: Boolean,
    quickActionsVisible: Boolean,
    integrationsVisible: Boolean,
    categoriesVisible: Boolean,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    Row(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(horizontal = pad),
        horizontalArrangement = Arrangement.spacedBy(pad),
    ) {
        LazyColumn(
            modifier = Modifier
                .width(SettingsDimensions.ExpandedListPaneWidth)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            if (!state.isSearchActive) {
                item(key = "hero") {
                    AnimatedVisibility(
                        visible = heroVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsProfileHeader(
                            state = state.profileHeader,
                            onClick = state.onProfileHeaderClick,
                            modifier = Modifier.padding(top = 4.dp, bottom = spacing),
                        )
                    }
                }

                item(key = "permission") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showPermissionBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsPermissionBanner(
                            onRequestPermission = state.onRequestPermission,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }

                item(key = "update") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showUpdateBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsUpdateBanner(
                            latestVersion = state.latestVersion,
                            onClick = state.onUpdateClick,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }

            if (!state.isSearchActive || state.searchQuery.isNotBlank()) {
                if (state.quickActions.isNotEmpty()) {
                    item(key = "quickActions") {
                        AnimatedVisibility(
                            modifier = Modifier.animateItem(),
                            visible = quickActionsVisible,
                            enter = fadeIn(SettingsAnimations.entranceSpring()),
                        ) {
                            SettingsQuickActionsSection(
                                actions = state.quickActions,
                                columns = 2,
                                modifier = Modifier.padding(bottom = spacing),
                            )
                        }
                    }
                }

                if (state.integrations.isNotEmpty()) {
                    item(key = "integrations") {
                        AnimatedVisibility(
                            modifier = Modifier.animateItem(),
                            visible = integrationsVisible,
                            enter = fadeIn(SettingsAnimations.entranceSpring()),
                        ) {
                            SettingsIntegrationsSection(
                                integrations = state.integrations,
                                modifier = Modifier.padding(bottom = spacing),
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            if (state.isSearchActive && state.searchQuery.isBlank()) {
                SearchHistorySection(state, 0.dp)
            } else if (state.isSearchActive && !state.hasSearchResults) {
                item(key = "empty") {
                    Spacer(modifier = Modifier.height(24.dp).animateItem())
                    SettingsSearchEmpty(modifier = Modifier.animateItem())
                }
            } else {
                if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                    item(key = "internalSearchResults") {
                        SettingsGroupCard(
                            group = state.internalGroup,
                            modifier = Modifier.padding(bottom = spacing).animateItem(),
                        )
                    }
                }

                items(
                    count = state.groups.size,
                    key = { state.groups[it].title },
                ) { index ->
                    AnimatedVisibility(
                        modifier = Modifier.animateItem(),
                        visible = categoriesVisible,
                        enter = fadeIn(
                            SettingsAnimations.staggerTween(index)
                        ) + slideInVertically(
                            initialOffsetY = { it / 5 },
                            animationSpec = SettingsAnimations.staggerTween(index),
                        ),
                    ) {
                        SettingsGroupCard(
                            group = state.groups[index],
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
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
    val title = if (state.isLoading) {
        "Loading..."
    } else if (state.isLoggedIn) {
        state.accountName.ifBlank { stringResource(R.string.account) }
    } else {
        stringResource(R.string.login)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(SettingsDimensions.HeroCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(SettingsDimensions.HeroIconSize)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoading) {
                        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else if (state.isLoggedIn && !state.accountImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(state.accountImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (state.isLoggedIn) R.drawable.account else R.drawable.login,
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(SettingsDimensions.HeroIconInnerSize),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.account),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    ) {
                        Text(
                            text = stringResource(R.string.version_name, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.BannerIconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.security),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SettingsDimensions.BannerIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.permissions_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.permissions_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    text = stringResource(R.string.allow),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun SettingsUpdateBanner(
    latestVersion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.PressScale else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "updateScale",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.BannerIconSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SettingsDimensions.BannerIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_version_available),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.version_name, latestVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun SettingsSearchEmpty(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.try_different_search_term),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsGroupCard(
    group: SettingsGroup,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = group.title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.2f,
            modifier = Modifier.padding(
                horizontal = SettingsDimensions.SectionHeaderHorizontalPadding,
                vertical = SettingsDimensions.SectionHeaderBottomPadding,
            ),
        )

        Card(
            shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.animateContentSize()
        ) {
            Column {
                group.items.forEachIndexed { index, item ->
                    SettingsRow(
                        item = item,
                        showDivider = index < group.items.size - 1,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    item: SettingsItem,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val effectiveAccent = if (item.accentColor.isSpecified) {
        item.accentColor
    } else {
        MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "rowScale",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.06f else 0f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "rowBgAlpha",
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = item.onClick,
                )
                .padding(
                    horizontal = SettingsDimensions.RowHorizontalPadding,
                    vertical = SettingsDimensions.RowVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.RowIconSize)
                    .clip(RoundedCornerShape(SettingsDimensions.RowIconCornerRadius))
                    .background(effectiveAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (item.showUpdateIndicator) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(8.dp),
                            )
                        },
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = effectiveAccent,
                            modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.showUpdateIndicator) {
                            effectiveAccent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item.badge?.let { badge ->
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = SettingsDimensions.DividerStartIndent),
                thickness = SettingsDimensions.DividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
fun SettingsQuickActionsSection(
    actions: List<SettingsQuickAction>,
    columns: Int = SettingsDimensions.CompactColumns,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val rows = actions.chunked(columns)
        rows.forEach { rowActions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowActions.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    action: SettingsQuickAction,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.TilePressScale else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "tileScale",
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.IconPressRotation else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconRotation",
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .aspectRatio(SettingsDimensions.QuickActionTileAspectRatio),
        shape = RoundedCornerShape(SettingsDimensions.QuickActionCardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = action.onClick,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            action.accentColor.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(SettingsDimensions.QuickActionIconSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(action.accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = action.icon,
                        contentDescription = action.label,
                        tint = action.accentColor,
                        modifier = Modifier
                            .size(SettingsDimensions.QuickActionIconInnerSize)
                            .graphicsLayer { rotationZ = iconRotation },
                    )
                }

                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SettingsIntegrationsSection(
    integrations: List<SettingsIntegrationAction>,
    modifier: Modifier = Modifier,
) {
    if (integrations.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            count = integrations.size,
            key = { integrations[it].label },
        ) { index ->
            IntegrationPill(action = integrations[index])
        }
    }
}

@Composable
fun IntegrationPill(
    action: SettingsIntegrationAction,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.PillPressScale else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "pillScale",
    )
    val lift by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.PillPressLift.value else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillLift",
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { translationY = lift },
        shape = RoundedCornerShape(SettingsDimensions.IntegrationPillCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = action.onClick,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.IntegrationIconSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(action.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = action.icon,
                    contentDescription = null,
                    tint = action.accentColor,
                    modifier = Modifier.size(SettingsDimensions.IntegrationIconInnerSize),
                )
            }

            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --- UPDATE DIALOG (From Original) ---

enum class DownloadStatus {
    NOT_STARTED,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

suspend fun downloadApk(
    context: Context,
    version: String,
    onProgressUpdate: (Float) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    try {
        val apkUrl = "https://github.com/cgens67/AvidTune/releases/download/$version/app-universal-release.apk"

        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, "app-universal-release-$version.apk")

        if (apkFile.exists()) {
            apkFile.delete()
        }

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
                
                if (!response.isSuccessful) {
                    return@withContext null
                }
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
                withContext(Dispatchers.Main) {
                    onProgressUpdate(progress)
                }
            }
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        
        withContext(Dispatchers.Main) {
            onProgressUpdate(1f)
        }

        return@withContext FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun installApk(context: Context, apkUri: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val pm = context.packageManager
        val isAllowed = pm.canRequestPackageInstalls()
        if (!isAllowed) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData("package:${context.packageName}".toUri())
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
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

@Composable
fun UpdateDownloadDialog(
    latestVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }
    val downloadScope = rememberCoroutineScope()

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls() && downloadedApkUri != null) {
                installApk(context, downloadedApkUri!!)
            }
        }
    }

    Dialog(onDismissRequest = {
        if (downloadStatus != DownloadStatus.DOWNLOADING) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.update_version, latestVersion),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (downloadStatus) {
                    DownloadStatus.NOT_STARTED -> {
                        Text(stringResource(R.string.download_update_prompt))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(android.R.string.cancel))
                            }
                            Button(onClick = {
                                downloadStatus = DownloadStatus.DOWNLOADING
                                downloadScope.launch {
                                    downloadedApkUri =
                                        downloadApk(context, latestVersion) { progress ->
                                            downloadProgress = progress
                                        }
                                    if (downloadedApkUri != null) {
                                        downloadStatus = DownloadStatus.COMPLETED
                                        downloadProgress = 1f
                                    } else {
                                        downloadStatus = DownloadStatus.ERROR
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.download))
                            }
                        }
                    }

                    DownloadStatus.DOWNLOADING -> {
                        Text(stringResource(R.string.downloading))
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    DownloadStatus.COMPLETED -> {
                        Text(stringResource(R.string.download_completed))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.close))
                            }
                            Button(onClick = {
                                if (downloadedApkUri != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (!context.packageManager.canRequestPackageInstalls()) {
                                            val intent =
                                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                    .setData("package:${context.packageName}".toUri())

                                            installPermissionLauncher.launch(intent)
                                        } else {
                                            installApk(context, downloadedApkUri!!)
                                        }
                                    } else {
                                        installApk(context, downloadedApkUri!!)
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.install))
                            }
                        }
                    }

                    DownloadStatus.ERROR -> {
                        Text(stringResource(R.string.download_error))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
}
