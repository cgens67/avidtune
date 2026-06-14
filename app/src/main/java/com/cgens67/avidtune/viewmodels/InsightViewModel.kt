package com.cgens67.avidtune.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AccountInfo
import com.cgens67.avidtune.R
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.PlaylistEntity
import com.cgens67.avidtune.db.entities.SongWithStats
import com.cgens67.avidtune.ui.screens.insight.PlaylistCreationState
import com.cgens67.avidtune.ui.screens.insight.WrappedConstants
import com.cgens67.avidtune.ui.screens.insight.WrappedScreenType
import com.cgens67.avidtune.ui.screens.insight.WrappedState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val databaseDao: MusicDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WrappedState())
    val state = _state.asStateFlow()

    fun createPlaylist(imageResName: String) {
        if (_state.value.playlistCreationState != PlaylistCreationState.Idle) return

        _state.update { it.copy(playlistCreationState = PlaylistCreationState.Creating) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val fromTimestamp = Calendar.getInstance().apply {
                        set(WrappedConstants.YEAR, Calendar.JANUARY, 1, 0, 0, 0)
                    }.timeInMillis
                    val toTimestamp = Calendar.getInstance().apply {
                        set(WrappedConstants.YEAR, Calendar.DECEMBER, 31, 23, 59, 59)
                    }.timeInMillis
                    
                    val allSongs = databaseDao.mostPlayedSongsStats(fromTimestamp, toTimeStamp = toTimestamp, limit = 100).first()
                    val playlistId = UUID.randomUUID().toString()

                    val drawableId = context.resources.getIdentifier(imageResName, "drawable", context.packageName)
                    val targetResId = if (drawableId != 0) drawableId else R.drawable.previewalbum
                    val bitmap = BitmapFactory.decodeResource(context.resources, targetResId)
                    
                    val file = File(context.cacheDir, "$playlistId.png")
                    FileOutputStream(file).use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }

                    val newPlaylist = PlaylistEntity(
                        id = playlistId,
                        name = WrappedConstants.PLAYLIST_NAME,
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true
                    )
                    databaseDao.insert(newPlaylist)

                    val createdPlaylist = databaseDao.playlist(playlistId).firstOrNull()
                    if (createdPlaylist != null) {
                        val songIds = allSongs.map { it.id }
                        databaseDao.addSongToPlaylist(createdPlaylist, songIds)
                        
                        // Copy image file to persistent playlist images directory
                        com.cgens67.avidtune.utils.saveCustomPlaylistImage(context, playlistId, android.net.Uri.fromFile(file))
                    }
                }
                _state.update { it.copy(playlistCreationState = PlaylistCreationState.Success) }
            } catch (e: Exception) {
                Timber.tag("WrappedManager").e(e, "Error saving wrapped playlist")
                _state.update { it.copy(playlistCreationState = PlaylistCreationState.Idle) }
            }
        }
    }

    private suspend fun generatePlaylistMap() {
        val topSongs = _state.value.topSongs
        val topArtists = _state.value.topArtists
        if (topSongs.isEmpty()) {
            _state.update { it.copy(trackMap = emptyMap()) }
            return
        }

        withContext(Dispatchers.IO) {
            val playlistMap = mutableMapOf<WrappedScreenType, String>()

            val introSongPool = topSongs.subList(minOf(5, topSongs.size), topSongs.size)
            val introSong = introSongPool.randomOrNull()?.id ?: topSongs.last().id
            playlistMap[WrappedScreenType.Welcome] = introSong
            playlistMap[WrappedScreenType.MinutesTease] = introSong
            playlistMap[WrappedScreenType.MinutesReveal] = introSong

            val topSong = topSongs.first()
            playlistMap[WrappedScreenType.TotalSongs] = topSong.id
            playlistMap[WrappedScreenType.TopSongReveal] = topSong.id
            playlistMap[WrappedScreenType.Top5Songs] = topSong.id

            val topAlbum = _state.value.topAlbum
            val albumSong = topAlbum?.let { album ->
                val albumSongs = databaseDao.albumSongs(album.id).first()
                albumSongs.randomOrNull()?.id
            } ?: topSong.id 
            
            playlistMap[WrappedScreenType.TotalAlbums] = albumSong
            playlistMap[WrappedScreenType.TopAlbumReveal] = albumSong
            playlistMap[WrappedScreenType.Top5Albums] = albumSong

            val topArtist = topArtists.firstOrNull()
            val artistSong = topArtist?.let { artist ->
                val artistTopSongs = databaseDao.artistSongsByPlayTimeAsc(artist.id).first()
                if (artistTopSongs.isNotEmpty()) {
                    val artistTopSong = artistTopSongs.first()
                    if (artistTopSong.id == topSong.id) {
                        artistTopSongs.getOrNull(1)?.id ?: artistTopSongs.filter { it.id != topSong.id }.randomOrNull()?.id ?: artistTopSong.id
                    } else {
                        artistTopSong.id
                    }
                } else topSong.id
            } ?: topSong.id 
            
            playlistMap[WrappedScreenType.TotalArtists] = artistSong
            playlistMap[WrappedScreenType.TopArtistReveal] = artistSong
            playlistMap[WrappedScreenType.Top5Artists] = artistSong

            val endSongPool = topSongs.subList(minOf(2, topSongs.size), minOf(5, topSongs.size))
            val endSong = endSongPool.randomOrNull()?.id ?: topSongs.getOrNull(2)?.id ?: topSong.id
            playlistMap[WrappedScreenType.Playlist] = endSong
            playlistMap[WrappedScreenType.Conclusion] = "2-p9DM2Xvsc"

            _state.update { it.copy(trackMap = playlistMap) }
        }
    }

    suspend fun prepare() {
        if (_state.value.isDataReady) return

        val fromTimestamp = Calendar.getInstance().apply {
            set(WrappedConstants.YEAR, Calendar.JANUARY, 1, 0, 0, 0)
        }.timeInMillis

        val toTimestamp = Calendar.getInstance().apply {
            set(WrappedConstants.YEAR, Calendar.DECEMBER, 31, 23, 59, 59)
        }.timeInMillis

        withContext(Dispatchers.IO) {
            val accountInfoDeferred = async { YouTube.accountInfo().getOrNull() }
            
            val allSongsStats = databaseDao.mostPlayedSongsStats(fromTimestamp, toTimeStamp = toTimestamp, limit = 1000).first()
            val allAlbumsStats = databaseDao.mostPlayedAlbums(fromTimestamp, toTimeStamp = toTimestamp, limit = 1000).first()
            val allArtistsStats = databaseDao.mostPlayedArtists(fromTimestamp, toTimeStamp = toTimestamp, limit = 1000).first()

            val topSongsResult = allSongsStats.take(30)
            val topAlbumsResult = allAlbumsStats.take(5)
            val topArtistsResult = allArtistsStats.filter { it.artist.isYouTubeArtist }.take(5)

            val uniqueSongCount = allSongsStats.size
            val uniqueArtistCount = allArtistsStats.size
            val uniqueAlbumCount = allAlbumsStats.size
            val totalPlayTimeMs = allSongsStats.sumOf { it.timeListened ?: 0L }

            val accountInfo = accountInfoDeferred.await()

            _state.update {
                it.copy(
                    accountInfo = accountInfo,
                    topSongs = topSongsResult,
                    topArtists = topArtistsResult,
                    top5Albums = topAlbumsResult,
                    topAlbum = topAlbumsResult.firstOrNull(),
                    uniqueSongCount = uniqueSongCount,
                    uniqueArtistCount = uniqueArtistCount,
                    totalAlbums = uniqueAlbumCount,
                    totalMinutes = totalPlayTimeMs / 1000 / 60
                )
            }
        }

        generatePlaylistMap()
        _state.update { it.copy(isDataReady = true) }
    }
}
