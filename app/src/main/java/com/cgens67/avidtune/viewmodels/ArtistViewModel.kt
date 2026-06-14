package com.cgens67.avidtune.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.pages.ArtistPage
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = database.artistSongsPreview(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _globalMonthlyListeners = MutableStateFlow<String?>(null)
    val globalMonthlyListeners: StateFlow<String?> = _globalMonthlyListeners.asStateFlow()

    init {
        fetchArtistsFromYTM()
    }

    private fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            YouTube.artist(artistId)
                .onSuccess {
                    artistPage = it
                    // Fetch global stats using the artist's name once successfully loaded
                    fetchGlobalStats(it.artist.title)
                }.onFailure {
                    reportException(it)
                }
        }
    }

    private fun fetchGlobalStats(artistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try multiple highly-targeted search queries
                val queries = listOf(
                    "site:open.spotify.com \"$artistName\" \"monthly listeners\"",
                    "\"monthly listeners\" \"$artistName\" spotify"
                )
                
                var match: MatchResult? = null
                val regex = Regex("([\\d.,]+[KMBkmb]?)\\s+monthly listeners", RegexOption.IGNORE_CASE)

                for (query in queries) {
                    if (match != null) break
                    
                    try {
                        // First attempt: DuckDuckGo Lite (POST request, very bot-friendly)
                        val doc = Jsoup.connect("https://lite.duckduckgo.com/lite/")
                            .data("q", query)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .post()
                        match = regex.find(doc.text())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (match == null) {
                        try {
                            // Fallback attempt: DuckDuckGo HTML
                            val encodedQuery = URLEncoder.encode(query, "UTF-8")
                            val doc2 = Jsoup.connect("https://html.duckduckgo.com/html/?q=$encodedQuery")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                .get()
                            match = regex.find(doc2.text())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                if (match != null) {
                    // Extract and format, e.g., "1.2M" or "14,500"
                    val listeners = match.groupValues[1].uppercase()
                    _globalMonthlyListeners.value = listeners
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
