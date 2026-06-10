@file:Suppress("DEPRECATION")

package com.cgens67.avidtune.ui.component

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.LanguageCodeToName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import timber.log.Timber

/** Data model to represent a language */
data class LanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val completionStatus: CompletionStatus = CompletionStatus.COMPLETE,
    val isSystemDefault: Boolean = false,
    val flag: String = ""
)

/** Translation completion status */
enum class CompletionStatus(@StringRes val labelRes: Int?, val color: @Composable () -> Color) {
    COMPLETE(null, { Color.Transparent }),
    INCOMPLETE(R.string.status_incomplete, { MaterialTheme.colorScheme.tertiary }),
    BETA(R.string.status_beta, { MaterialTheme.colorScheme.primary }),
    EXPERIMENTAL(R.string.status_experimental, { MaterialTheme.colorScheme.secondary })
}

/** States of the language change operation */
sealed class LanguageChangeState {
    object Idle : LanguageChangeState()
    object Changing : LanguageChangeState()
    object Success : LanguageChangeState()
    data class Error(val message: String) : LanguageChangeState()
}

class LocaleManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LocaleManager"
        private const val PREF_NAME = "locale_preferences"
        private const val PREF_LANGUAGE_KEY = "selected_language"
        private const val SYSTEM_DEFAULT = "system_default"
        private const val ANIMATION_DELAY = 200L

        @Volatile
        private var instance: LocaleManager? = null

        fun getInstance(context: Context): LocaleManager {
            return instance ?: synchronized(this) {
                instance ?: LocaleManager(context.applicationContext).also { instance = it }
            }
        }

        // Flag and translation state mapping
        private val LANGUAGE_METADATA = mapOf(
            "en" to LanguageMetadata("🇺🇸", CompletionStatus.COMPLETE),
            "es" to LanguageMetadata("🇪🇸", CompletionStatus.COMPLETE),
            "fr" to LanguageMetadata("🇫🇷", CompletionStatus.COMPLETE),
            "de" to LanguageMetadata("🇩🇪", CompletionStatus.COMPLETE),
            "it" to LanguageMetadata("🇮🇹", CompletionStatus.COMPLETE),
            "pt" to LanguageMetadata("🇧🇷", CompletionStatus.COMPLETE),
            "pt-PT" to LanguageMetadata("🇵🇹", CompletionStatus.COMPLETE),
            "ru" to LanguageMetadata("🇷🇺", CompletionStatus.COMPLETE),
            "zh-CN" to LanguageMetadata("🇨🇳", CompletionStatus.COMPLETE),
            "zh-TW" to LanguageMetadata("🇹🇼", CompletionStatus.COMPLETE),
            "ja" to LanguageMetadata("🇯🇵", CompletionStatus.COMPLETE),
            "ko" to LanguageMetadata("🇰🇷", CompletionStatus.COMPLETE),
            "ar" to LanguageMetadata("🇸🇦", CompletionStatus.BETA),
            "hi" to LanguageMetadata("🇮🇳", CompletionStatus.BETA),
            "th" to LanguageMetadata("🇹🇭", CompletionStatus.INCOMPLETE),
            "vi" to LanguageMetadata("🇻🇳", CompletionStatus.INCOMPLETE),
            "tr" to LanguageMetadata("🇹🇷", CompletionStatus.BETA),
            "pl" to LanguageMetadata("🇵🇱", CompletionStatus.INCOMPLETE),
            "nl" to LanguageMetadata("🇳🇱", CompletionStatus.INCOMPLETE),
            "id" to LanguageMetadata("🇮🇩", CompletionStatus.BETA),
            "uk" to LanguageMetadata("🇺🇦", CompletionStatus.BETA),
            "he" to LanguageMetadata("🇮🇱", CompletionStatus.BETA)
        )

        private data class LanguageMetadata(
            val flag: String,
            val completionStatus: CompletionStatus
        )
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _currentLanguage = MutableStateFlow(getSelectedLanguageCode())
    private val _changeState = MutableStateFlow<LanguageChangeState>(LanguageChangeState.Idle)

    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    val changeState: StateFlow<LanguageChangeState> = _changeState.asStateFlow()

    private var _cachedLanguages: List<LanguageItem>? = null
    private var _cachedSystemLanguage: String? = null

    fun getSelectedLanguageCode(): String {
        return sharedPreferences.getString(PREF_LANGUAGE_KEY, SYSTEM_DEFAULT) ?: SYSTEM_DEFAULT
    }

    fun getEffectiveLanguageCode(): String {
        val saved = getSelectedLanguageCode()
        return if (saved == SYSTEM_DEFAULT) getSystemLanguageCode() else saved
    }

    private fun getSystemLanguageCode(): String {
        return _cachedSystemLanguage ?: run {
            val systemCode = try {
                val localeList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ConfigurationCompat.getLocales(Resources.getSystem().configuration)
                } else {
                    LocaleListCompat.create(Locale.getDefault())
                }

                val systemLocale = if (localeList.isEmpty) Locale.getDefault() else localeList[0]
                    ?: Locale.getDefault()

                formatLocaleCode(systemLocale)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error getting system language")
                "en"
            }
            _cachedSystemLanguage = systemCode
            systemCode
        }
    }

    private fun detectAvailableLanguages(): List<String> {
        // Return STRICTLY the languages defined in your curated LANGUAGE_METADATA map
        return LANGUAGE_METADATA.keys.toList()
    }

    private fun formatLocaleCode(locale: Locale): String {
        val language = locale.language
        val country = locale.country

        return when {
            language == "zh" && country.isNotEmpty() -> {
                when (country) {
                    "CN" -> "zh-CN"
                    "TW", "HK" -> "zh-TW"
                    else -> "zh-CN"
                }
            }
            language == "pt" && country == "BR" -> "pt"
            language == "pt" && country == "PT" -> "pt-PT"
            country.isNotEmpty() -> "$language-$country"
            else -> language
        }
    }

    /**
     * Converts a locale code to Locale
     */
    private fun parseLocaleCode(code: String): Locale {
        return when {
            code == "zh-rCN" || code == "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            code == "zh-rTW" || code == "zh-TW" -> Locale.TRADITIONAL_CHINESE
            code.contains("-r") -> {
                val parts = code.split("-r")
                Locale(parts[0], parts[1])
            }
            code.contains("-") -> {
                val parts = code.split("-")
                Locale(parts[0], parts[1])
            }
            else -> Locale(code)
        }
    }

    fun getAvailableLanguages(): List<LanguageItem> {
        return _cachedLanguages ?: run {
            val systemLanguageCode = getSystemLanguageCode()
            val availableLocaleCodes = detectAvailableLanguages()

            val languages = mutableListOf<LanguageItem>()

            // Add system option
            val systemDisplayName = try {
                val locale = parseLocaleCode(systemLanguageCode)
                locale.displayLanguage.replaceFirstChar { it.uppercase() }
            } catch (e: Exception) {
                systemLanguageCode
            }

            languages.add(
                LanguageItem(
                    code = SYSTEM_DEFAULT,
                    displayName = context.getString(R.string.system_language, systemDisplayName),
                    nativeName = systemDisplayName,
                    completionStatus = CompletionStatus.COMPLETE,
                    isSystemDefault = true,
                    flag = "🌐"
                )
            )

            // Add detected languages
            availableLocaleCodes.forEach { localeCode ->
                try {
                    val locale = parseLocaleCode(localeCode)
                    val displayName = locale.getDisplayLanguage(Locale.ENGLISH)
                        .replaceFirstChar { it.uppercase() }
                    
                    // Directly use our mapped Native Name to prevent System guesswork
                    // Map "he" back to "iw" for proper lookup in LanguageCodeToName if needed
                    val lookupCode = if (localeCode == "he") "iw" else localeCode
                    val nativeName = LanguageCodeToName[lookupCode]
                        ?: locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }

                    // Get metadata (flag and status)
                    val metadata = LANGUAGE_METADATA[localeCode]
                        ?: LanguageMetadata("🌍", CompletionStatus.COMPLETE)

                    languages.add(
                        LanguageItem(
                            code = localeCode,
                            displayName = displayName,
                            nativeName = nativeName,
                            completionStatus = metadata.completionStatus,
                            isSystemDefault = false,
                            flag = metadata.flag
                        )
                    )
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error processing locale: $localeCode")
                }
            }

            // Sort by: system first, then complete, then alphabetically
            val sorted = languages.sortedWith(
                compareBy<LanguageItem> { !it.isSystemDefault }
                    .thenBy { it.completionStatus.ordinal }
                    .thenBy { it.displayName }
            )

            _cachedLanguages = sorted
            sorted
        }
    }

    suspend fun updateLanguage(languageCode: String): Boolean {
        if (_changeState.value is LanguageChangeState.Changing) {
            return false
        }

        return try {
            _changeState.value = LanguageChangeState.Changing
            Timber.tag(TAG).d("Changing language to: $languageCode")

            delay(ANIMATION_DELAY)

            val editor = sharedPreferences.edit()
            editor.putString(PREF_LANGUAGE_KEY, languageCode)
            val saved = editor.commit()

            if (!saved) {
                throw Exception("Could not save preference")
            }

            _currentLanguage.value = languageCode

            val effectiveLanguageCode = if (languageCode == SYSTEM_DEFAULT) {
                getSystemLanguageCode()
            } else {
                languageCode
            }

            val locale = parseLocaleCode(effectiveLanguageCode)
            applyLocaleToApp(locale)

            _changeState.value = LanguageChangeState.Success

            Timber.tag(TAG)
                .d("Language updated: $languageCode (effective: $effectiveLanguageCode)")
                
            // Give UI time to show success state (Check mark)
            delay(400)
            
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error updating language to $languageCode")
            _changeState.value = LanguageChangeState.Error(e.message ?: "Unknown error")
            false
        }
    }

    fun clearCache() {
        _cachedLanguages = null
        _cachedSystemLanguage = null
    }

    private fun applyLocaleToApp(locale: Locale) {
        try {
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                config.setLocales(localeList)
                config.setLocale(locale)
            } else {
                config.locale = locale
            }

            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error applying language configuration")
        }
    }

    fun applyLocaleToContext(baseContext: Context): Context {
        return try {
            val languageCode = getEffectiveLanguageCode()
            val locale = parseLocaleCode(languageCode)

            Locale.setDefault(locale)
            val config = Configuration(baseContext.resources.configuration)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                config.setLocales(localeList)
                baseContext.createConfigurationContext(config)
            } else {
                config.locale = locale
                @Suppress("DEPRECATION")
                baseContext.resources.updateConfiguration(
                    config,
                    baseContext.resources.displayMetrics
                )
                baseContext
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error applying language to context")
            baseContext
        }
    }

    fun restartApp(context: Context) {
        try {
            // Gracefully restart the UI activity without stopping the MusicService so audio doesn't pause or crash.
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) {
                    val activity = currentContext
                    Handler(Looper.getMainLooper()).post {
                        activity.recreate()
                    }
                    return
                }
                currentContext = currentContext.baseContext
            }

            // Fallback if context is not an Activity
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error restarting application")
        }
    }

    fun resetChangeState() {
        _changeState.value = LanguageChangeState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val localeManager = remember { LocaleManager.getInstance(context) }
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val currentLanguage by localeManager.currentLanguage.collectAsState()
    val changeState by localeManager.changeState.collectAsState()
    val availableLanguages by remember { derivedStateOf { localeManager.getAvailableLanguages() } }

    var selectedLanguageCode by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val filteredLanguages = remember(availableLanguages, searchQuery) {
        if (searchQuery.isBlank()) {
            availableLanguages
        } else {
            availableLanguages.filter { language ->
                language.displayName.contains(searchQuery, ignoreCase = true) ||
                        language.nativeName.contains(searchQuery, ignoreCase = true) ||
                        language.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(selectedLanguageCode) {
        selectedLanguageCode?.let { languageCode ->
            if (localeManager.updateLanguage(languageCode)) {
                localeManager.restartApp(context)
            }
            selectedLanguageCode = null
        }
    }

    LaunchedEffect(filteredLanguages, currentLanguage) {
        val selectedIndex = filteredLanguages.indexOfFirst { it.code == currentLanguage }
        if (selectedIndex != -1) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            localeManager.resetChangeState()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y < 0f) {
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .weight(1f, fill = false)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = {
                    searchQuery = ""
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                focusRequester = focusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )

            AnimatedVisibility(
                visible = changeState is LanguageChangeState.Changing ||
                        changeState is LanguageChangeState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ChangeStateIndicator(
                    isChanging = changeState is LanguageChangeState.Changing
                )
            }

            if (filteredLanguages.isEmpty()) {
                EmptySearchResult(modifier = Modifier.padding(vertical = 32.dp))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filteredLanguages,
                        key = { it.code }
                    ) { language ->
                        val isSelected = language.code == currentLanguage
                        val isEnabled = changeState !is LanguageChangeState.Changing

                        LanguageItem(
                            language = language,
                            isSelected = isSelected,
                            isEnabled = isEnabled,
                            onClick = {
                                if (isEnabled && !isSelected) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    selectedLanguageCode = language.code
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_language_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeStateIndicator(
    isChanging: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isChanging) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.applying),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.restarting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySearchResult(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_results_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.try_another_term),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun LanguageItem(
    language: LanguageItem,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag
            Text(
                text = language.flag,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (language.nativeName.isNotEmpty() &&
                    language.nativeName != language.displayName &&
                    !language.isSystemDefault
                ) {
                    Text(
                        text = language.nativeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Status badge
            if (language.completionStatus != CompletionStatus.COMPLETE) {
                val statusColor = language.completionStatus.color()
                val labelRes = language.completionStatus.labelRes

                if (labelRes != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Radio button
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isEnabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun LanguagePreference(
    modifier: Modifier = Modifier
) {
    var showLanguageSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localeManager = remember { LocaleManager.getInstance(context) }
    val currentLanguage by localeManager.currentLanguage.collectAsState()
    val changeState by localeManager.changeState.collectAsState()

    val currentLanguageDisplay = remember(currentLanguage) {
        val selectedCode = localeManager.getSelectedLanguageCode()
        localeManager.getAvailableLanguages()
            .find { it.code == selectedCode }
            ?.let { language ->
                if (language.isSystemDefault) {
                    language.nativeName
                } else {
                    "${language.nativeName} ${language.flag}".trim()
                }
            } ?: selectedCode
    }

    val isChanging = changeState is LanguageChangeState.Changing

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isChanging) {
                showLanguageSelector = true
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.translate),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isChanging) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (isChanging) {
                        stringResource(R.string.changing_language)
                    } else {
                        currentLanguageDisplay
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isChanging) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (isChanging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.configure_app_language),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showLanguageSelector) {
        LanguageSelector(
            onDismiss = { showLanguageSelector = false }
        )
    }
}


abstract class LocaleAwareApplication : android.app.Application() {

    private val localeManager by lazy { LocaleManager.getInstance(this) }

    override fun attachBaseContext(base: Context) {
        try {
            val updatedContext = LocaleManager.getInstance(base).applyLocaleToContext(base)
            super.attachBaseContext(updatedContext)
        } catch (e: Exception) {
            Timber.tag("LocaleAwareApplication").e(e, "Error applying language")
            super.attachBaseContext(base)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            localeManager
            Timber.tag("LocaleAwareApplication").d("LocaleManager initialized")
        } catch (e: Exception) {
            Timber.tag("LocaleAwareApplication").e(e, "Error initializing LocaleManager")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.clearCache()
    }
}
