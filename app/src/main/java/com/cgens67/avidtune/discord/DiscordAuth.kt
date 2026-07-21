@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

package com.cgens67.avidtune.discord

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.*
import com.cgens67.avidtune.ui.component.*
import com.cgens67.avidtune.utils.DiscordPresenceManager
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.db.entities.Song
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import androidx.compose.animation.core.FastOutSlowInEasing
import timber.log.Timber

val DiscordRefreshTokenKey = stringPreferencesKey("discord_refresh_token")
val DiscordTokenExpiresAtKey = longPreferencesKey("discord_token_expires_at")
val DiscordAvatarUrlKey = stringPreferencesKey("discordAvatarUrl")
val DiscordShowWhenPausedKey = booleanPreferencesKey("discord_show_when_paused")

val DiscordLargeImageTypeKey = stringPreferencesKey("discord_large_image_type")
val DiscordLargeImageCustomUrlKey = stringPreferencesKey("discord_large_image_custom_url")
val DiscordSmallImageTypeKey = stringPreferencesKey("discord_small_image_type")
val DiscordSmallImageCustomUrlKey = stringPreferencesKey("discord_small_image_custom_url")
val DiscordPresenceStatusKey = stringPreferencesKey("discord_presence_status")
val DiscordActivityPlatformKey = stringPreferencesKey("discord_activity_platform")
val DiscordActivityNameKey = stringPreferencesKey("discord_activity_name")
val DiscordActivityDetailsKey = stringPreferencesKey("discord_activity_details")
val DiscordActivityStateKey = stringPreferencesKey("discord_activity_state")
val DiscordActivityButton1LabelKey = stringPreferencesKey("discord_activity_button1_label")
val DiscordActivityButton1EnabledKey = booleanPreferencesKey("discord_activity_button1_enabled")
val DiscordActivityButton2LabelKey = stringPreferencesKey("discord_activity_button2_label")
val DiscordActivityButton2EnabledKey = booleanPreferencesKey("discord_activity_button2_enabled")
val DiscordActivityButton1UrlSourceKey = stringPreferencesKey("discord_activity_button1_url_source")
val DiscordActivityButton1CustomUrlKey = stringPreferencesKey("discord_activity_button1_custom_url")
val DiscordActivityButton2UrlSourceKey = stringPreferencesKey("discord_activity_button2_url_source")
val DiscordActivityButton2CustomUrlKey = stringPreferencesKey("discord_activity_button2_custom_url")
val DiscordActivityTypeKey = stringPreferencesKey("discord_activity_type")
val DiscordLargeTextSourceKey = stringPreferencesKey("discord_large_text_source")
val DiscordLargeTextCustomKey = stringPreferencesKey("discord_large_text_custom")

data class DiscordAuthorizationSession(val state: String, val codeVerifier: String, val authorizationUri: Uri)
data class DiscordAccount(val id: String, val username: String, val displayName: String, val avatarUrl: String?)
data class DiscordAuthSession(val accessToken: String, val refreshToken: String?, val expiresAtMillis: Long, val account: DiscordAccount?)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long = 0L
)

@Serializable
data class UserInfoResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("sub") val sub: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("picture") val picture: String? = null,
    @SerialName("discriminator") val discriminator: String? = null,
    @SerialName("preferred_username") val preferredUsername: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("nickname") val nickname: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("global_name") val globalName: String? = null
)

enum class ActivitySource { ARTIST, ALBUM, SONG, APP }
private enum class DiscordAuthorizationUiMode { Idle, Waiting, Success, Failure }

private val DiscordImageOptions = listOf("thumbnail", "artist", "appicon", "custom")
private val DiscordSmallImageOptions = listOf("thumbnail", "artist", "appicon", "custom", "dontshow")
private val DiscordActivityStatusOptions = listOf("online", "dnd", "idle", "streaming")
private val DiscordPlatformOptions = listOf("desktop", "xbox", "samsung", "ios", "android", "embedded", "ps4", "ps5")
private val DiscordActivityTypeOptions = listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "COMPETING")
private val DiscordLargeTextOptions = listOf("song", "artist", "album", "app", "custom", "dontshow")

object DiscordAuthCoordinator {
    val redirects = MutableSharedFlow<Uri>(replay = 1, extraBufferCapacity = 1)
    fun emit(uri: Uri) { redirects.tryEmit(uri) }
}

private fun HttpURLConnection.readResponse(): String {
    val stream = if (responseCode >= 400) errorStream else inputStream
    return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
}

object DiscordOAuthRepository {
    private const val AUTHORIZATION_ENDPOINT = "https://discord.com/oauth2/authorize"
    private const val TOKEN_ENDPOINT = "https://discord.com/api/oauth2/token"
    private const val CURRENT_USER_ENDPOINT = "https://discord.com/api/v10/users/@me"
    private const val REQUEST_TIMEOUT_MS = 12_000
    private const val EXPIRY_SKEW_MS = 60_000L

    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()

    const val applicationId: Long = 1165706613961789445L
    const val redirectUri: String = "discord-1165706613961789445:/authorize/callback"

    fun createAuthorizationSession(): DiscordAuthorizationSession {
        val state = randomUrlSafeString(32)
        val verifier = randomUrlSafeString(64)
        val challenge = sha256Base64Url(verifier)
        val scopes = listOf("openid", "identify", "sdk.social_layer_presence").joinToString(" ")

        val uri = Uri.parse(AUTHORIZATION_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", applicationId.toString())
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        return DiscordAuthorizationSession(state, verifier, uri)
    }

    suspend fun completeAuthorization(context: Context, session: DiscordAuthorizationSession, redirect: Uri): Result<DiscordAuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            require(redirect.scheme == "discord-1165706613961789445") { "Unexpected Discord redirect scheme" }
            require(redirect.path == "/authorize/callback") { "Unexpected Discord redirect target" }

            redirect.getQueryParameter("state") == session.state

            redirect.getQueryParameter("error")?.let { error ->
                val description = redirect.getQueryParameter("error_description")
                throw IllegalStateException(description ?: error)
            }

            val code = requireNotNull(redirect.getQueryParameter("code")) { "Discord authorization code is missing" }
            val token = exchangeAuthorizationCode(code, session.codeVerifier)
            val account = runCatching { fetchAccount(token.accessToken) }.getOrNull()
            val authSession = token.toAuthSession(account)
            storeSession(context, authSession)
            authSession
        }
    }

    suspend fun getValidAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val currentToken = prefs[DiscordTokenKey]?.trim().orEmpty()
        if (currentToken.isBlank()) return@withContext null

        val expiresAt = prefs[DiscordTokenExpiresAtKey] ?: 0L
        if (expiresAt == 0L || System.currentTimeMillis() + EXPIRY_SKEW_MS < expiresAt) {
            return@withContext currentToken
        }

        val refreshToken = prefs[DiscordRefreshTokenKey]?.trim().orEmpty()
        if (refreshToken.isBlank()) return@withContext currentToken

        refreshAccessToken(context, refreshToken).getOrNull()?.accessToken ?: currentToken
    }

    suspend fun fetchAccount(accessToken: String): DiscordAccount = withContext(Dispatchers.IO) {
        val response = getJson(CURRENT_USER_ENDPOINT, accessToken)
        val userInfo = json.decodeFromString<UserInfoResponse>(response)
        val userId = userInfo.id ?: userInfo.sub ?: ""
        val username = userInfo.preferredUsername ?: userInfo.username ?: userId
        val displayName = userInfo.nickname ?: userInfo.globalName ?: userInfo.name ?: username

        DiscordAccount(
            id = userId,
            username = username,
            displayName = displayName,
            avatarUrl = userInfo.picture?.takeIf { it.isNotBlank() } ?: buildAvatarUrl(userId, userInfo.avatar, userInfo.discriminator)
        )
    }

    suspend fun clearSession(context: Context) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs.remove(DiscordTokenKey)
                prefs.remove(DiscordRefreshTokenKey)
                prefs.remove(DiscordTokenExpiresAtKey)
                prefs.remove(DiscordUsernameKey)
                prefs.remove(DiscordNameKey)
                prefs.remove(DiscordAvatarUrlKey)
            }
        }
    }

    private suspend fun refreshAccessToken(context: Context, refreshToken: String): Result<DiscordAuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val token = postForm(
                url = TOKEN_ENDPOINT,
                params = mapOf(
                    "client_id" to applicationId.toString(),
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken
                )
            ).let { json.decodeFromString<TokenResponse>(it) }

            val account = runCatching { fetchAccount(token.accessToken) }.getOrNull()
            val session = token.toAuthSession(account, fallbackRefreshToken = refreshToken)
            storeSession(context, session)
            session
        }
    }

    private fun exchangeAuthorizationCode(code: String, codeVerifier: String): TokenResponse = postForm(
        url = TOKEN_ENDPOINT,
        params = mapOf(
            "client_id" to applicationId.toString(),
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri,
            "code_verifier" to codeVerifier
        )
    ).let { json.decodeFromString(it) }

    private suspend fun storeSession(context: Context, session: DiscordAuthSession) {
        context.dataStore.edit { prefs ->
            prefs[DiscordTokenKey] = session.accessToken
            session.refreshToken?.takeIf { it.isNotBlank() }?.let { prefs[DiscordRefreshTokenKey] = it }
            prefs[DiscordTokenExpiresAtKey] = session.expiresAtMillis
            session.account?.let { account ->
                prefs[DiscordUsernameKey] = account.username
                prefs[DiscordNameKey] = account.displayName
                prefs[DiscordAvatarUrlKey] = account.avatarUrl.orEmpty()
            }
        }
    }

    private fun buildAvatarUrl(userId: String, avatarHash: String?, discriminator: String?): String? {
        if (userId.isBlank()) return null
        avatarHash?.takeIf { it.isNotBlank() }?.let { hash ->
            val ext = if (hash.startsWith("a_")) "gif" else "png"
            return "https://cdn.discordapp.com/avatars/$userId/$hash.$ext?size=256"
        }
        val defaultIdx = discriminator?.toIntOrNull()?.takeIf { it > 0 }?.rem(5)
            ?: userId.toLongOrNull()?.let { ((it shr 22) % 6L).toInt() } ?: 0
        return "https://cdn.discordapp.com/embed/avatars/$defaultIdx.png"
    }

    private fun TokenResponse.toAuthSession(account: DiscordAccount?, fallbackRefreshToken: String? = null): DiscordAuthSession {
        val expInMillis = expiresInSeconds.coerceAtLeast(0L) * 1000L
        val expiresAt = if (expInMillis > 0L) System.currentTimeMillis() + expInMillis else 0L
        return DiscordAuthSession(accessToken, refreshToken ?: fallbackRefreshToken, expiresAt, account)
    }

    private fun postForm(url: String, params: Map<String, String>): String {
        val body = params.entries.joinToString("&") { "${it.key.urlEncode()}=${it.value.urlEncode()}" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = REQUEST_TIMEOUT_MS
            readTimeout = REQUEST_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return connection.readResponse()
    }

    private fun getJson(url: String, bearerToken: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = REQUEST_TIMEOUT_MS
            readTimeout = REQUEST_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $bearerToken")
            setRequestProperty("Accept", "application/json")
        }
        return connection.readResponse()
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
    
    private fun randomUrlSafeString(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Base64Url(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}

class DiscordOAuthCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }
    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { DiscordAuthCoordinator.emit(it) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var discordToken by rememberPreference(DiscordTokenKey, "")
    var webView: WebView? = null

    AndroidView(
        modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true

                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }
                WebStorage.getInstance().deleteAllData()

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveToken(token: String) {
                        if (token != "null" && token != "error") {
                            discordToken = token
                            scope.launch(Dispatchers.Main) {
                                webView?.loadUrl("about:blank")
                                navController.navigateUp()
                            }
                        }
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (url.contains("/channels/@me") || url.contains("/app")) {
                            view.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        var token = localStorage.getItem("token");
                                        if (token) {
                                            Android.onRetrieveToken(token.slice(1, -1));
                                        } else {
                                            var i = document.createElement('iframe');
                                            document.body.appendChild(i);
                                            setTimeout(function() {
                                                try {
                                                    var alt = i.contentWindow.localStorage.token;
                                                    if (alt) alert(alt.slice(1, -1));
                                                    else alert("null");
                                                } catch (e) { alert("error"); }
                                            }, 1000);
                                        }
                                    } catch (e) { alert("error"); }
                                })();
                                """.trimIndent(), null
                            )
                        }
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                        if (message != "null" && message != "error") {
                            discordToken = message
                            scope.launch(Dispatchers.Main) {
                                view.loadUrl("about:blank")
                                navController.navigateUp()
                            }
                        }
                        result.confirm()
                        return true
                    }
                }
                webView = this
                loadUrl("https://discord.com/login")
            }
        }
    )

    TopAppBar(
        title = { Text(stringResource(R.string.action_login)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), null)
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val song by playerConnection.currentSong.collectAsState(null)
    val isPlaying by playerConnection.isPlaying.collectAsState()

    val playbackState by playerConnection.playbackState.collectAsState()
    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var discordAvatarUrl by rememberPreference(DiscordAvatarUrlKey, "")
    var authorizedToken by rememberSaveable { mutableStateOf("") }
    var authorizedUsername by rememberSaveable { mutableStateOf("") }
    var authorizedName by rememberSaveable { mutableStateOf("") }
    var authorizedAvatarUrl by rememberSaveable { mutableStateOf("") }
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var authorizationSession by remember {
        mutableStateOf(DiscordOAuthRepository.createAuthorizationSession())
    }
    var authorizationUiModeName by rememberSaveable {
        mutableStateOf(DiscordAuthorizationUiMode.Idle.name)
    }
    var authorizationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val authorizationUiMode =
        remember(authorizationUiModeName) {
            DiscordAuthorizationUiMode.valueOf(authorizationUiModeName)
        }

    val discordTokenExpiresAt by rememberPreference(DiscordTokenExpiresAtKey, 0L)

    LaunchedEffect(discordTokenExpiresAt) {
        currentTimeMillis = System.currentTimeMillis()
        if (discordTokenExpiresAt > currentTimeMillis) {
            delay((discordTokenExpiresAt - currentTimeMillis).coerceAtLeast(1_000L))
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(discordToken) {
        val token = discordToken
        if (token.isBlank()) {
            authorizedToken = ""
            authorizedUsername = ""
            authorizedName = ""
            authorizedAvatarUrl = ""
            return@LaunchedEffect
        }

        if (token == authorizedToken) {
            authorizedToken = ""
        }

        if (token.isNotBlank()) {
            runCatching {
                DiscordOAuthRepository.fetchAccount(token)
            }.onSuccess {
                discordUsername = it.username
                discordName = it.displayName
                discordAvatarUrl = it.avatarUrl.orEmpty()
            }.onFailure {
                Timber.tag("DiscordSettings").w(it, "Discord account lookup failed")
            }
        }
    }

    val (discordRPC, onDiscordRPCChange) =
        rememberPreference(
            key = EnableDiscordRPCKey,
            defaultValue = true,
        )

    LaunchedEffect(discordToken, discordRPC) {
        if (discordRPC && discordToken.isNotBlank()) {
            Timber.tag("DiscordSettings").d("Discord Rich Presence enabled, MusicService will handle start")
        } else {
            Timber.tag("DiscordSettings").d("Discord Rich Presence disabled or not authorized, MusicService will handle stop")
        }
    }

    val activeDiscordToken = authorizedToken.ifBlank { discordToken }
    val activeDiscordUsername = authorizedUsername.ifBlank { discordUsername }
    val activeDiscordName = authorizedName.ifBlank { discordName }
    val activeDiscordAvatarUrl = authorizedAvatarUrl.ifBlank { discordAvatarUrl }
    val isLoggedIn = remember(activeDiscordToken) { activeDiscordToken.isNotBlank() }
    val isAccessTokenExpired =
        remember(isLoggedIn, discordTokenExpiresAt, currentTimeMillis) {
            isLoggedIn && discordTokenExpiresAt > 0L && currentTimeMillis >= discordTokenExpiresAt
        }
    val accountDisplayName =
        remember(isLoggedIn, activeDiscordName, activeDiscordUsername, context) {
            when {
                activeDiscordName.isNotBlank() -> activeDiscordName
                activeDiscordUsername.isNotBlank() -> activeDiscordUsername
                isLoggedIn -> context.getString(R.string.account)
                else -> context.getString(R.string.not_logged_in)
            }
        }

    val launchAuthorization: () -> Unit = {
        val session = DiscordOAuthRepository.createAuthorizationSession()
        authorizationSession = session
        authorizationMessage = null
        authorizationUiModeName = DiscordAuthorizationUiMode.Waiting.name

        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, session.authorizationUri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                },
            )
        }.onFailure {
            authorizationUiModeName = DiscordAuthorizationUiMode.Failure.name
            authorizationMessage = it.message ?: "Authorization failed"
        }
    }

    LaunchedEffect(authorizationSession.state, authorizationUiMode) {
        if (authorizationUiMode != DiscordAuthorizationUiMode.Waiting) {
            return@LaunchedEffect
        }

        DiscordAuthCoordinator.redirects.collectLatest { redirect ->
            if (redirect.getQueryParameter("state") != authorizationSession.state) {
                return@collectLatest
            }

            DiscordOAuthRepository
                .completeAuthorization(
                    context = context,
                    session = authorizationSession,
                    redirect = redirect,
                ).onSuccess { session ->
                    val account =
                        session.account
                            ?: runCatching { DiscordOAuthRepository.fetchAccount(session.accessToken) }.getOrNull()

                    authorizedToken = session.accessToken
                    authorizedUsername = account?.username.orEmpty()
                    authorizedName = account?.displayName.orEmpty()
                    authorizedAvatarUrl = account?.avatarUrl.orEmpty()
                    discordUsername = authorizedUsername
                    discordName = authorizedName
                    discordAvatarUrl = authorizedAvatarUrl
                    authorizationMessage = "Authorization successful"
                    authorizationUiModeName = DiscordAuthorizationUiMode.Success.name
                    authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
                }.onFailure {
                    authorizationMessage = it.message ?: "Authorization failed"
                    authorizationUiModeName = DiscordAuthorizationUiMode.Failure.name
                    authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
                }
        }
    }

    LaunchedEffect(authorizationUiMode) {
        if (authorizationUiMode == DiscordAuthorizationUiMode.Success ||
            authorizationUiMode == DiscordAuthorizationUiMode.Failure
        ) {
            delay(2600)
            if (authorizationUiModeName == authorizationUiMode.name) {
                authorizationUiModeName = DiscordAuthorizationUiMode.Idle.name
                authorizationMessage = null
            }
        }
    }

    BackHandler(enabled = authorizationUiMode == DiscordAuthorizationUiMode.Waiting) {
        authorizationUiModeName = DiscordAuthorizationUiMode.Idle.name
        authorizationMessage = null
        authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
    }

    val (largeImageType, onLargeImageTypeChange) =
        rememberPreference(
            key = DiscordLargeImageTypeKey,
            defaultValue = "thumbnail",
        )
    val (largeImageCustomUrl, onLargeImageCustomUrlChange) =
        rememberPreference(
            key = DiscordLargeImageCustomUrlKey,
            defaultValue = "",
        )
    val (smallImageType, onSmallImageTypeChange) =
        rememberPreference(
            key = DiscordSmallImageTypeKey,
            defaultValue = "artist",
        )
    val (smallImageCustomUrl, onSmallImageCustomUrlChange) =
        rememberPreference(
            key = DiscordSmallImageCustomUrlKey,
            defaultValue = "",
        )
    var isRefreshing by remember { mutableStateOf(false) }

    val (activityStatusSelection, onActivityStatusSelectionChange) =
        rememberPreference(
            key = DiscordPresenceStatusKey,
            defaultValue = "online",
        )

    val (platformSelection, onPlatformSelectionChange) =
        rememberPreference(
            key = DiscordActivityPlatformKey,
            defaultValue = "android",
        )

    val (nameSource, onNameSourceChange) =
        rememberEnumPreference(
            key = DiscordActivityNameKey,
            defaultValue = ActivitySource.APP,
        )
    val (detailsSource, onDetailsSourceChange) =
        rememberEnumPreference(
            key = DiscordActivityDetailsKey,
            defaultValue = ActivitySource.SONG,
        )
    val (stateSource, onStateSourceChange) =
        rememberEnumPreference(
            key = DiscordActivityStateKey,
            defaultValue = ActivitySource.ARTIST,
        )

    val (button1Label) =
        rememberPreference(
            key = DiscordActivityButton1LabelKey,
            defaultValue = "Listen on YouTube Music",
        )
    val (button1Enabled) =
        rememberPreference(
            key = DiscordActivityButton1EnabledKey,
            defaultValue = true,
        )
    val (button2Label) =
        rememberPreference(
            key = DiscordActivityButton2LabelKey,
            defaultValue = "Go to AvidTune",
        )
    val (button2Enabled) =
        rememberPreference(
            key = DiscordActivityButton2EnabledKey,
            defaultValue = true,
        )
    val (button1UrlSource) =
        rememberPreference(
            key = DiscordActivityButton1UrlSourceKey,
            defaultValue = "songurl",
        )
    val (button1CustomUrl) =
        rememberPreference(
            key = DiscordActivityButton1CustomUrlKey,
            defaultValue = "",
        )
    val (button2UrlSource) =
        rememberPreference(
            key = DiscordActivityButton2UrlSourceKey,
            defaultValue = "custom",
        )
    val (button2CustomUrl) =
        rememberPreference(
            key = DiscordActivityButton2CustomUrlKey,
            defaultValue = "https://github.com/cgens67/AvidTune",
        )

    val (activityType, onActivityTypeChange) =
        rememberPreference(
            key = DiscordActivityTypeKey,
            defaultValue = "LISTENING",
        )
    var showWhenPaused by rememberPreference(
        key = DiscordShowWhenPausedKey,
        defaultValue = false,
    )

    val (largeTextSource, onLargeTextSourceChange) =
        rememberPreference(
            key = DiscordLargeTextSourceKey,
            defaultValue = "album",
        )
    val (largeTextCustom, onLargeTextCustomChange) =
        rememberPreference(
            key = DiscordLargeTextCustomKey,
            defaultValue = "",
        )

    val (useDetails, onUseDetailsChange) = rememberPreference(
        key = com.cgens67.avidtune.constants.DiscordUseDetailsKey,
        defaultValue = false
    )

    val (sliderStyle) = rememberEnumPreference(
        key = com.cgens67.avidtune.constants.SliderStyleKey,
        defaultValue = com.cgens67.avidtune.constants.SliderStyle.SQUIGGLY
    )

    var infoDismissed by rememberPreference(
        key = com.cgens67.avidtune.constants.DiscordInfoDismissedKey,
        defaultValue = false
    )

    LaunchedEffect(largeImageType, smallImageType) {
        com.cgens67.avidtune.utils.DiscordImageResolver.clearCache()
    }

    SettingsPage(
        title = stringResource(R.string.discord_integration),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        // Banner info
        AnimatedVisibility(
            visible = !infoDismissed,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.discord_integration),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.discord_information),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                TextButton(
                    onClick = { infoDismissed = true },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }

        // Account category
        SettingsGeneralCategory(
            title = stringResource(R.string.account),
            items = listOfNotNull(
                @Composable {
                    PreferenceEntry(
                        title = {
                            Text(if (isLoggedIn) accountDisplayName else stringResource(R.string.not_logged_in))
                        },
                        description = if (isLoggedIn && activeDiscordUsername.isNotEmpty()) "@$activeDiscordUsername" else null,
                        icon = { Icon(painterResource(R.drawable.discord), null) },
                        trailingContent = {
                            if (isLoggedIn) {
                                OutlinedButton(
                                    onClick = {
                                        showLogoutConfirm = true
                                    }
                                ) {
                                    Text(stringResource(R.string.logout))
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = launchAuthorization
                                ) {
                                    Text(stringResource(R.string.action_login))
                                }
                            }
                        },
                        onClick = {
                            if (!isLoggedIn) launchAuthorization()
                        }
                    )
                },
                if (!isLoggedIn) {
                    @Composable {
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.advanced_login)) },
                            icon = { Icon(painterResource(R.drawable.token), null) },
                            onClick = { showTokenDialog = true }
                        )
                    }
                } else null
            )
        )

        // Options category
        SettingsGeneralCategory(
            title = stringResource(R.string.options),
            items = listOf(
                @Composable {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_discord_rpc)) },
                        icon = { Icon(painterResource(R.drawable.discord), null) },
                        checked = discordRPC,
                        onCheckedChange = onDiscordRPCChange,
                        isEnabled = isLoggedIn,
                    )
                },
                @Composable {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.discord_use_details)) },
                        description = stringResource(R.string.discord_use_details_description),
                        icon = { Icon(painterResource(R.drawable.info), null) },
                        checked = useDetails,
                        onCheckedChange = onUseDetailsChange,
                        isEnabled = isLoggedIn && discordRPC,
                    )
                }
            )
        )

        // Connection options
        SettingsGeneralCategory(
            title = "Discord Connection Settings",
            items = listOf(
                @Composable {
                    ListPreference(
                        title = { Text("Activity Status") },
                        icon = { Icon(painterResource(R.drawable.bedtime), null) },
                        selectedValue = activityStatusSelection,
                        values = DiscordActivityStatusOptions,
                        valueText = { discordPresenceStatusLabel(it) },
                        onValueSelected = onActivityStatusSelectionChange,
                    )
                },
                @Composable {
                    ListPreference(
                        title = { Text("Platform Status") },
                        icon = { Icon(painterResource(R.drawable.desktop_windows), null) },
                        selectedValue = platformSelection,
                        values = DiscordPlatformOptions,
                        valueText = { discordPlatformLabel(it) },
                        onValueSelected = onPlatformSelectionChange,
                    )
                }
            )
        )

        // Display configuration
        SettingsGeneralCategory(
            title = "Discord Activity Content",
            items = listOf(
                @Composable {
                    EnumListPreference(
                        title = { Text("Activity Name") },
                        selectedValue = nameSource,
                        onValueSelected = onNameSourceChange,
                        valueText = { activitySourceLabel(it) },
                        icon = { Icon(painterResource(R.drawable.text_fields), null) },
                    )
                },
                @Composable {
                    EnumListPreference(
                        title = { Text("Activity Details") },
                        selectedValue = detailsSource,
                        onValueSelected = onDetailsSourceChange,
                        valueText = { activitySourceLabel(it) },
                        icon = { Icon(painterResource(R.drawable.text_fields), null) },
                    )
                },
                @Composable {
                    EnumListPreference(
                        title = { Text("Activity State") },
                        selectedValue = stateSource,
                        onValueSelected = onStateSourceChange,
                        valueText = { activitySourceLabel(it) },
                        icon = { Icon(painterResource(R.drawable.text_fields), null) },
                    )
                },
                @Composable {
                    SwitchPreference(
                        title = { Text("Show When Paused") },
                        description = "Show your status when music is paused",
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        checked = showWhenPaused,
                        onCheckedChange = { showWhenPaused = it },
                    )
                },
                @Composable {
                    ListPreference(
                        title = { Text("Activity Type") },
                        icon = { Icon(painterResource(R.drawable.discord), null) },
                        selectedValue = activityType,
                        values = DiscordActivityTypeOptions,
                        valueText = { discordActivityTypeLabel(it) },
                        onValueSelected = onActivityTypeChange,
                    )
                }
            )
        )

        // Image options
        SettingsGeneralCategory(
            title = "Discord Image Options",
            items = listOf(
                @Composable {
                    ListPreference(
                        title = { Text("Large Image") },
                        icon = { Icon(painterResource(R.drawable.image), null) },
                        selectedValue = largeImageType,
                        values = DiscordImageOptions,
                        valueText = { discordImageTypeLabel(it) },
                        onValueSelected = onLargeImageTypeChange,
                    )
                },
                @Composable {
                    AnimatedVisibility(visible = largeImageType == "custom") {
                        EditTextPreference(
                            title = { Text("Large Image Custom URL") },
                            icon = { Icon(painterResource(R.drawable.link), null) },
                            value = largeImageCustomUrl,
                            onValueChange = rgeImageCustomUrlChange,
                            isInputValid = { true },
                        )
                    }
                },
                @Composable {
                    ListPreference(
                        title = { Text("Large Text") },
                        icon = { Icon(painterResource(R.drawable.text_fields), null) },
                        selectedValue = largeTextSource,
                        values = DiscordLargeTextOptions,
                        valueText = { discordLargeTextSourceLabel(it) },
                        onValueSelected = onLargeTextSourceChange,
                    )
                },
                @Composable {
                    AnimatedVisibility(visible = largeTextSource == "custom") {
                        EditTextPreference(
                            title = { Text("Custom Large Text") },
                            icon = { Icon(painterResource(R.drawable.text_fields), null) },
                            value = largeTextCustom,
                            onValueChange = onLargeTextCustomChange,
                            isInputValid = { true },
                        )
                    }
                },
                @Composable {
                    ListPreference(
                        title = { Text("Small Image") },
                        icon = { Icon(painterResource(R.drawable.image), null) },
                        selectedValue = smallImageType,
                        values = DiscordSmallImageOptions,
                        valueText = { discordImageTypeLabel(it) },
                        onValueSelected = onSmallImageTypeChange,
                    )
                },
                @Composable {
                    AnimatedVisibility(visible = smallImageType == "custom") {
                        EditTextPreference(
                            title = { Text("Small Image Custom URL") },
                            icon = { Icon(painterResource(R.drawable.link), null) },
                            value = smallImageCustomUrl,
                            onValueChange = smallImageCustomUrlChange,
                            isInputValid = { true },
                        )
                    }
                }
            )
        )

        // Preview section
        PreferenceGroupTitle(title = stringResource(R.string.preview))

        EnhancedRichPresence(
            song = song,
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            sliderStyle = sliderStyle
        )

        Spacer(Modifier.height(16.dp))
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out of Discord?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            DiscordOAuthRepository.clearSession(context)
                        }
                        authorizedToken = ""
                        authorizedUsername = ""
                        authorizedName = ""
                        authorizedAvatarUrl = ""
                        authorizationUiModeName = DiscordAuthorizationUiMode.Idle.name
                        authorizationMessage = null
                        authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
                        showLogoutConfirm = false
                    }
                ) {
                    Text("Log out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirm = false }
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showTokenDialog) {
        com.cgens67.avidtune.ui.component.TextFieldDialog(
            title = { Text(stringResource(R.string.advanced_login)) },
            initialTextFieldValue = TextFieldValue(discordToken),
            onDismiss = { showTokenDialog = false },
            onDone = { token ->
                discordToken = token.trim()
                showTokenDialog = false
            },
            singleLine = false,
            maxLines = 10,
            isInputValid = { it.isNotBlank() }
        )
    }
}

@Composable
private fun activitySourceLabel(source: ActivitySource): String =
    when (source) {
        ActivitySource.ARTIST -> stringResource(R.string.artist_name)
        ActivitySource.ALBUM -> "Album"
        ActivitySource.SONG -> stringResource(R.string.song_title)
        ActivitySource.APP -> stringResource(R.string.app_name)
    }

@Composable
private fun discordPresenceStatusLabel(value: String): String =
    when (value) {
        "online" -> "Online"
        "dnd" -> "Do Not Disturb"
        "idle" -> "Idle"
        "streaming" -> "Streaming"
        else -> "Online"
    }

@Composable
private fun discordPlatformLabel(value: String): String =
    when (value) {
        "desktop" -> "Desktop"
        "xbox" -> "Xbox"
        "samsung" -> "Samsung"
        "ios" -> "iOS"
        "android" -> "Android"
        "embedded" -> "Web/Embedded"
        "ps4" -> "PlayStation 4"
        "ps5" -> "PlayStation 5"
        else -> "Android"
    }

@Composable
private fun discordActivityTypeLabel(value: String): String =
    when (value) {
        "PLAYING" -> "Playing"
        "STREAMING" -> "Streaming"
        "LISTENING" -> "Listening to"
        "WATCHING" -> "Watching"
        "COMPETING" -> "Competing in"
        else -> value
    }

@Composable
private fun discordImageTypeLabel(value: String): String =
    when (value.lowercase()) {
        "thumbnail" -> "Album Artwork"
        "artist" -> "Artist Artwork"
        "appicon" -> "App Icon"
        "custom" -> "Custom URL"
        "dontshow" -> "Don't Show"
        else -> value
    }

@Composable
private fun discordLargeTextSourceLabel(value: String): String =
    when (value.lowercase()) {
        "song" -> stringResource(R.string.song_title)
        "artist" -> stringResource(R.string.artist_name)
        "album" -> "Album"
        "app" -> stringResource(R.string.app_name)
        "custom" -> "Custom"
        "dontshow" -> "Don't Show"
        else -> value
    }

@Composable
fun EnhancedRichPresence(
    song: Song?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    sliderStyle: SliderStyle
) {
    val context = LocalContext.current
    val gradientAlpha by animateFloatAsState(
        targetValue = if (song != null) 0.15f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "gradientAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = gradientAlpha),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(4.dp, CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.discord),
                                contentDescription = "Discord",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "AvidTune",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Listening on Discord",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (song != null && isPlaying) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            ),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = song?.song?.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .run {
                                            if (song == null) {
                                                background(MaterialTheme.colorScheme.surfaceVariant)
                                            } else this
                                        },
                                    contentScale = ContentScale.Crop
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.1f)
                                                )
                                            )
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    val artistAvatar = song?.artists?.firstOrNull()?.thumbnailUrl

                                    Card(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (artistAvatar != null) {
                                                AsyncImage(
                                                    model = artistAvatar,
                                                    contentDescription = "Artist",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(
                                                                    MaterialTheme.colorScheme.primary,
                                                                    MaterialTheme.colorScheme.secondary
                                                                )
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.avidtune),
                                                        contentDescription = "AvidTune",
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .alpha(0.9f),
                                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song?.song?.title ?: "No song playing",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = song?.artists?.joinToString { it.name } ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        song?.song?.albumName?.let { albumTitle ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = albumTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (song != null) {
                    Spacer(modifier = Modifier.height(20.dp))

                    EnhancedProgressBar(
                        position = position,
                        duration = duration,
                        isPlaying = isPlaying,
                        sliderStyle = sliderStyle
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        enabled = song != null,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://music.youtube.com/watch?v=${song?.id}".toUri()
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "YouTube Music",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("YouTube Music", maxLines = 1, fontWeight = FontWeight.Medium)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/cgens67/AvidTune".toUri()
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.avidtune),
                                contentDescription = "AvidTune",
                                modifier = Modifier.size(12.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("AvidTune", maxLines = 1, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedProgressBar(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    sliderStyle: SliderStyle
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (sliderStyle) {
            SliderStyle.DEFAULT -> {
                Slider(
                    value = position.toFloat(),
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    onValueChange = {},
                    enabled = false,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                        disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledThumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            SliderStyle.SQUIGGLY -> {
                com.cgens67.avidtune.ui.player.PlayerSliderV4(
                    sliderStyle = SliderStyle.SQUIGGLY,
                    sliderPosition = null,
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    textBackgroundColor = MaterialTheme.colorScheme.primary,
                    onValueChange = {},
                    onValueChangeFinished = {}
                )
            }

            SliderStyle.SLIM -> {
                LinearProgressIndicator(
                    progress = { (position.toFloat() / duration.toFloat().coerceAtLeast(1f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = makeTimeString(position),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = makeTimeString(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
