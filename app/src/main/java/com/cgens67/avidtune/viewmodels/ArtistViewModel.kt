package com.cgens67.avidtune.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.pages.ArtistPage
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.ui.component.LocaleManager
import com.cgens67.avidtune.utils.TranslationHelper
import com.cgens67.avidtune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = database.artistSongsPreview(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val originalDescription = MutableStateFlow<String?>(null)
    val artistDescription = MutableStateFlow<String?>(null)
    val isTranslated = MutableStateFlow(false)
    val canTranslate = MutableStateFlow(false)

    init {
        fetchArtistsFromYTM()
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            YouTube.artist(artistId)
                .onSuccess {
                    artistPage = it
                    val rawDesc = it.description?.substringBefore("From Wikipedia")?.trim()
                    if (!rawDesc.isNullOrBlank()) {
                        originalDescription.value = rawDesc
                        canTranslate.value = true
                        processTranslation(rawDesc)
                    }
                }.onFailure {
                    reportException(it)
                }
        }
    }

    private fun resolveTargetLanguage(context: Context): String {
        val effectiveCode = runCatching {
            LocaleManager.getInstance(context).getEffectiveLanguageCode()
        }.getOrNull()

        val locale = java.util.Locale.getDefault()
        val tag = locale.toLanguageTag()
        val lang = locale.language

        val code = when {
            !effectiveCode.isNullOrBlank() && effectiveCode != "system" && effectiveCode != "SYSTEM_DEFAULT" -> effectiveCode
            tag.isNotBlank() -> tag
            lang.isNotBlank() -> lang
            else -> "en"
        }

        val lower = code.lowercase()
        return when {
            lower.startsWith("zh-tw") || lower.startsWith("zh-hk") || lower.startsWith("zh-hant") -> "zh-TW"
            lower.startsWith("zh") -> "zh-CN"
            lower.startsWith("pt-pt") -> "pt-PT"
            lower.startsWith("pt") -> "pt"
            lower == "he" || lower == "iw" -> "iw"
            lower.contains("-") -> lower.substringBefore("-")
            else -> lower
        }
    }

    private fun processTranslation(desc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetLang = resolveTargetLanguage(context)
            val detectedLang = runCatching { TranslationHelper.detectLanguage(desc.take(200)) }.getOrNull()?.lowercase()
            val targetBase = targetLang.substringBefore("-").lowercase()

            val needsTranslation = when {
                detectedLang != null && detectedLang != "und" && !detectedLang.startsWith(targetBase) -> true
                !targetBase.startsWith("en") && desc.any { it in 'a'..'z' || it in 'A'..'Z' } -> true
                else -> false
            }

            if (needsTranslation) {
                val translated = TranslationHelper.translate(desc, targetLang)
                if (!translated.isNullOrBlank() && translated.trim() != desc.trim()) {
                    artistDescription.value = translated
                    isTranslated.value = true
                } else {
                    artistDescription.value = desc
                    isTranslated.value = false
                }
            } else {
                artistDescription.value = desc
                isTranslated.value = false
            }
        }
    }

    fun toggleDescriptionTranslation() {
        viewModelScope.launch(Dispatchers.IO) {
            val orig = originalDescription.value ?: return@launch
            if (isTranslated.value) {
                artistDescription.value = orig
                isTranslated.value = false
            } else {
                val targetLang = resolveTargetLanguage(context)
                val translated = TranslationHelper.translate(orig, targetLang)
                if (!translated.isNullOrBlank()) {
                    artistDescription.value = translated
                    isTranslated.value = true
                }
            }
        }
    }
}
