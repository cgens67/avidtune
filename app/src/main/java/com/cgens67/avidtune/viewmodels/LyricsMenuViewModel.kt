package com.cgens67.avidtune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.db.entities.LyricsEntity
import com.cgens67.avidtune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.cgens67.avidtune.lyrics.LyricsHelper
import com.cgens67.avidtune.lyrics.LyricsResult
import com.cgens67.avidtune.models.MediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
) : ViewModel() {
    private var job: Job? = null
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)

    fun search(
        mediaId: String,
        title: String,
        artist: String,
        duration: Int,
    ) {
        isLoading.value = true
        results.value = emptyList()
        job?.cancel()
        job =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    lyricsHelper.getAllLyrics(mediaId, title, artist, duration) { result ->
                        if (result.lyrics.isNotBlank() && result.lyrics != LYRICS_NOT_FOUND) {
                            results.update { currentList ->
                                if (currentList.none { it.providerName == result.providerName && it.lyrics == result.lyrics }) {
                                    currentList + result
                                } else {
                                    currentList
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Timber.e(e, "Lyrics search failed")
                } finally {
                    isLoading.value = false
                }
            }
    }

    fun cancelSearch() {
        job?.cancel()
        job = null
        isLoading.value = false
    }

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        lyricsEntity: LyricsEntity?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                lyricsEntity?.let { database.delete(it) }
                val result = lyricsHelper.getLyrics(mediaMetadata)
                val textToSave = "[provider:${result.providerName}]\n${result.lyrics}"
                database.upsert(LyricsEntity(mediaMetadata.id, textToSave))
            } catch (e: Throwable) {
                Timber.e(e, "Failed to refetch lyrics")
            }
        }
    }
}
