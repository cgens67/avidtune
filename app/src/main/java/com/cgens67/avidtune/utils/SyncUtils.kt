package com.cgens67.avidtune.utils

import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.utils.completed
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.db.entities.ArtistEntity
import com.cgens67.avidtune.db.entities.PlaylistEntity
import com.cgens67.avidtune.db.entities.PlaylistSongMap
import com.cgens67.avidtune.db.entities.SongEntity
import com.cgens67.avidtune.models.toMediaMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SyncUtils @Inject constructor(
    val database: MusicDatabase,
) {
    suspend fun syncLikedSongs() {
        YouTube.playlist("LM").completed().onSuccess { page ->
            val songs = page.songs.reversed()

            database.likedSongsByNameAsc().first()
                .filterNot { it.id in songs.map(SongItem::id) }
                .forEach { database.update(it.song.localToggleLike()) }

            songs.forEach { song ->
                val dbSong = database.song(song.id).firstOrNull()
                database.transaction {
                    when (dbSong) {
                        null -> insert(song.toMediaMetadata(), SongEntity::localToggleLike)
                        else -> if (!dbSong.song.liked) update(dbSong.song.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncLibrarySongs() {
        YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
            val songs = page.items.filterIsInstance<SongItem>().reversed()

            database.songsByNameAsc().first()
                .filterNot { it.id in songs.map(SongItem::id) }
                .forEach { database.update(it.song.toggleLibrary()) }

            songs.forEach { song ->
                val dbSong = database.song(song.id).firstOrNull()
                database.transaction {
                    when (dbSong) {
                        null -> insert(song.toMediaMetadata(), SongEntity::toggleLibrary)
                        else -> if (dbSong.song.inLibrary == null) update(dbSong.song.toggleLibrary())
                    }
                }
            }
        }
    }

    suspend fun syncLikedAlbums() {
        YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
            val albums = page.items.filterIsInstance<AlbumItem>().reversed()

            database.albumsLikedByNameAsc().first()
                .filterNot { it.id in albums.map(AlbumItem::id) }
                .forEach { database.update(it.album.localToggleLike()) }

            albums.forEach { album ->
                val dbAlbum = database.album(album.id).firstOrNull()
                YouTube.album(album.browseId).onSuccess { albumPage ->
                    when (dbAlbum) {
                        null -> {
                            database.insert(albumPage)
                            database.album(album.id).firstOrNull()?.let {
                                database.update(it.album.localToggleLike())
                            }
                        }
                        else -> if (dbAlbum.album.bookmarkedAt == null)
                            database.update(dbAlbum.album.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncArtistsSubscriptions() {
        YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
            val artists = page.items.filterIsInstance<ArtistItem>()

            database.artistsBookmarkedByNameAsc().first()
                .filterNot { it.id in artists.map(ArtistItem::id) }
                .forEach { database.update(it.artist.localToggleLike()) }

            artists.forEach { artist ->
                val dbArtist = database.artist(artist.id).firstOrNull()
                database.transaction {
                    when (dbArtist) {
                        null -> {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.channelId,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        }
                        else -> if (dbArtist.artist.bookmarkedAt == null)
                            update(dbArtist.artist.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncSavedPlaylists() {
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
            val playlistList = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" ||  it.id == "SE" }
                .reversed()
            val dbPlaylists = database.playlistsByNameAsc().first()

            dbPlaylists.filterNot { it.playlist.browseId in playlistList.map(PlaylistItem::id) }
                .filterNot { it.playlist.browseId == null }
                .forEach { database.update(it.playlist.localToggleLike()) }

            playlistList.onEach { playlist ->
                var playlistEntity = dbPlaylists.find { playlist.id == it.playlist.browseId }?.playlist
                if (playlistEntity == null) {
                    playlistEntity = PlaylistEntity(
                        name = playlist.title,
                        browseId = playlist.id,
                        isEditable = playlist.isEditable,
                        bookmarkedAt = LocalDateTime.now(),
                        remoteSongCount = playlist.songCountText?.let {
                            Regex("""\d+""").find(it)?.value?.toIntOrNull()
                        }
                    )

                    database.insert(playlistEntity)
                } else database.update(playlistEntity, playlist)

                syncPlaylist(playlist.id, playlistEntity.id)
            }
        }
    }

    suspend fun syncPlaylist(browseId: String, playlistId: String) {
        val playlistPage = YouTube.playlist(browseId).completed().getOrNull() ?: return
        database.transaction {
            clearPlaylist(playlistId)
            playlistPage.songs
                .map(SongItem::toMediaMetadata)
                .onEach(::insert)
                .mapIndexed { position, song ->
                    PlaylistSongMap(
                        songId = song.id,
                        playlistId = playlistId,
                        position = position,
                        setVideoId = song.setVideoId
                    )
                }.forEach(::insert)
        }
    }
}