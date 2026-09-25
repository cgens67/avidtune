package com.cgens67.avidtune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.ui.component.LocaleManager
import com.cgens67.avidtune.utils.AppleMusicAboutAlbum
import com.cgens67.avidtune.utils.TranslationHelper
import com.cgens67.avidtune.utils.Wikipedia
import com.cgens67.avidtune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    val originalDescription = MutableStateFlow<String?>(null)
    val albumDescription = MutableStateFlow<String?>(null)
    val isTranslated = MutableStateFlow(false)
    val canTranslate = MutableStateFlow(false)
    val isExplicit = MutableStateFlow(false)

    private var isFetchingDescription = false

    init {
        viewModelScope.launch {
            val album = database.album(albumId).first()
            album?.let {
                fetchDescription(it.album.title, it.artists.firstOrNull()?.name)
            }

            YouTube
                .album(albumId)
                .onSuccess {
                    playlistId.value = it.album.playlistId
                    otherVersions.value = it.otherVersions
                    isExplicit.value = it.songs.any { song -> song.explicit } ||
                        it.otherVersions.any { version -> version.id == albumId && version.explicit }
                    database.transaction {
                        if (album == null) {
                            insert(it)
                        } else {
                            update(album.album, it, album.artists)
                        }
                    }

                    if (albumDescription.value == null) {
                        val title = it.album.title
                        val artist = it.album.artists?.firstOrNull()?.name ?: album?.artists?.firstOrNull()?.name
                        fetchDescription(title, artist)
                    }
                }.onFailure {
                    reportException(it)
                    if (it.message?.contains("NOT_FOUND") == true) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                }
        }
    }

    private fun resolveTargetLanguage(context: Context): String {
        val effectiveCode = runCatching {
            LocaleManager.getInstance(context).getEffectiveLanguageCode()
        }.getOrNull()

        val locale = Locale.getDefault()
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

    private fun fetchDescription(albumTitle: String, artistName: String?) {
        if (isFetchingDescription) return
        isFetchingDescription = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetLang = resolveTargetLanguage(context)
                val storefront = runCatching { YouTube.locale.gl.lowercase() }
                    .getOrNull()
                    ?.takeIf { it.length == 2 && it != "system" }
                    ?: Locale.getDefault().country.lowercase().takeIf { it.length == 2 }
                    ?: "us"

                // 1. Try Apple Music with user's storefront & locale
                var desc = AppleMusicAboutAlbum.fetchAlbumDescription(albumTitle, artistName, storefront, targetLang)

                // 2. Try Wikipedia in user's locale
                val wikiLang = targetLang.substringBefore("-").lowercase()
                if (desc == null && !wikiLang.startsWith("en")) {
                    desc = Wikipedia.fetchAlbumInfo(albumTitle, artistName, wikiLang)
                }

                // 3. Try Apple Music in English (US) if localized version wasn't found
                if (desc == null) {
                    desc = AppleMusicAboutAlbum.fetchAlbumDescription(albumTitle, artistName, "us", "en-US")
                }

                // 4. Try Wikipedia in English
                if (desc == null) {
                    desc = Wikipedia.fetchAlbumInfo(albumTitle, artistName, "en")
                }

                if (desc != null) {
                    originalDescription.value = desc
                    canTranslate.value = true

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
                            albumDescription.value = translated
                            isTranslated.value = true
                        } else {
                            albumDescription.value = desc
                            isTranslated.value = false
                        }
                    } else {
                        albumDescription.value = desc
                        isTranslated.value = false
                    }
                }
            } finally {
                isFetchingDescription = false
            }
        }
    }

    fun toggleDescriptionTranslation() {
        viewModelScope.launch(Dispatchers.IO) {
            val orig = originalDescription.value ?: return@launch
            if (isTranslated.value) {
                albumDescription.value = orig
                isTranslated.value = false
            } else {
                val targetLang = resolveTargetLanguage(context)
                val translated = TranslationHelper.translate(orig, targetLang)
                if (!translated.isNullOrBlank()) {
                    albumDescription.value = translated
                    isTranslated.value = true
                }
            }
        }
    }
}
