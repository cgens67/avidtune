package com.cgens67.avidtune.discord

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.*
import com.cgens67.avidtune.ui.component.*
import com.cgens67.avidtune.ui.screens.settings.DiscordPresenceManager
import com.cgens67.avidtune.ui.screens.settings.EnhancedRichPresence
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

val DiscordRefreshTokenKey = stringPreferencesKey("discord_refresh_token")
val DiscordTokenExpiresAtKey = longPreferencesKey("discord_token_expires_at")
val DiscordAvatarUrlKey = stringPreferencesKey("discordAvatarUrl")
val DiscordShowWhenPausedKey = booleanPreferencesKey("discord_show_when_paused")

data class DiscordAuthorizationSession(val state: String, val codeVerifier: String, val authorizationUri: Uri)
data class DiscordAccount(val id: String, val username: String, val displayName: String, val avatarUrl: String?)
data class DiscordAuthSession(val accessToken: String, val refreshToken: String?, val expiresAtMillis: Long, val account: DiscordAccount?)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long = 0L
)

@Serializable
private data class UserInfoResponse(
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

object DiscordOAuthRepository {
    private const val AUTHORIZATION_ENDPOINT = "https://discord.com/oauth2/authorize"
    private const val TOKEN_ENDPOINT = "https://discord.com/api/oauth2/token"
    private const val CURRENT_USER_ENDPOINT = "https://discord.com/api/v10/users/@me"
    private const val REQUEST_TIMEOUT_MS = 12_000
    private const val EXPIRY_SKEW_MS = 60_000L

    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()

    const val applicationId: Long = 1411019391843172514L
    const val redirectUri: String = "avidtune-discord://authorize/callback"

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
            require(redirect.scheme == "avidtune-discord") { "Unexpected Discord redirect scheme" }
            require(redirect.path == "/authorize/callback") { "Unexpected Discord redirect target" }
            require(redirect.getQueryParameter("state") == session.state) { "Discord authorization state mismatch" }

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

    private fun HttpURLConnection.readResponse(): String {
        val status = responseCode
        val stream = if (status in 200..299) inputStream else errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        disconnect()
        if (status !in 200..299) throw IOException("Discord OAuth request failed with HTTP $status: $body")
        return body
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
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
