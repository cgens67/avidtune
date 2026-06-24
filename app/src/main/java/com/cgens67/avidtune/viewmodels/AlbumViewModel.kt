package com.cgens67.avidtune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.utils.AppleMusicAboutAlbum
import com.cgens67.avidtune.utils.TranslationHelper
import com.cgens67.avidtune.utils.Wikipedia
import com.cgens67.avidtune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
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

    val albumDescription = MutableStateFlow<String?>(null)
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

    private fun fetchDescription(albumTitle: String, artistName: String?) {
        if (isFetchingDescription) return
        isFetchingDescription = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hl = YouTube.locale.hl
                val gl = YouTube.locale.gl.lowercase()
                val langCode = hl.substringBefore("-").lowercase()
                
                val googleTranslateLang = when {
                    hl.lowercase().startsWith("zh-tw") || hl.lowercase().startsWith("zh-hk") -> "zh-TW"
                    hl.lowercase().startsWith("zh") -> "zh-CN"
                    else -> langCode
                }

                // 1. Try Apple Music with user's locale
                var desc = AppleMusicAboutAlbum.fetchAlbumDescription(albumTitle, artistName, gl, hl)
                
                // 2. Try Wikipedia in user's locale
                if (desc == null && !langCode.startsWith("en")) {
                    desc = Wikipedia.fetchAlbumInfo(albumTitle, artistName, langCode)
                }
                
                // 3. Try Apple Music in English (US) if localized version wasn't found
                if (desc == null) {
                    desc = AppleMusicAboutAlbum.fetchAlbumDescription(albumTitle, artistName, "us", "en-US")
                }
                
                // 4. Try Wikipedia in English
                if (desc == null) {
                    desc = Wikipedia.fetchAlbumInfo(albumTitle, artistName, "en")
                }

                // 5. If a description is found and the target language is not English, translate it.
                if (desc != null && !langCode.startsWith("en")) {
                    val translatedDesc = TranslationHelper.translate(desc, googleTranslateLang)
                    if (!translatedDesc.isNullOrBlank()) {
                        desc = translatedDesc
                    }
                }

                withContext(Dispatchers.Main) {
                    albumDescription.value = desc
                }
            } finally {
                isFetchingDescription = false
            }
        }
    }
}
