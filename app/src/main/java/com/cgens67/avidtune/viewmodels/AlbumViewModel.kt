package com.cgens67.avidtune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.utils.AppleMusicAboutAlbum
import com.cgens67.avidtune.utils.Wikipedia
import com.cgens67.avidtune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    database: MusicDatabase,
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
        viewModelScope.launch(Dispatchers.IO) {
            var desc = AppleMusicAboutAlbum.fetchAlbumDescription(albumTitle, artistName)
            if (desc == null) {
                desc = Wikipedia.fetchAlbumInfo(albumTitle, artistName)
            }
            albumDescription.value = desc
        }
    }
}
