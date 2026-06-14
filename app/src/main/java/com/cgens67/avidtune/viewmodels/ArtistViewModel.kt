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
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneOffset
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

    private val thirtyDaysAgo = LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC).toEpochMilli()
    
    val monthlyPlayCount = database.artistPlayCountSince(artistId, thirtyDaysAgo)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
        
    val totalPlayCount = database.artistTotalPlayCount(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

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
                // Scrape Spotify's public meta description safely via DuckDuckGo Lite to avoid API keys or CAPTCHAs
                val query = "site:open.spotify.com/artist \"$artistName\""
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://html.duckduckgo.com/html/?q=$encodedQuery&kl=us-en"
                
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .get()
                    
                val snippet = doc.select(".result__snippet").firstOrNull()?.text()
                
                // Extract metrics like "104.3M monthly listeners" or "14,500 monthly listeners"
                val regex = Regex("([0-9.,]+[KMBkmb]?)\\s+monthly listeners", RegexOption.IGNORE_CASE)
                val match = snippet?.let { regex.find(it) }
                
                if (match != null) {
                    _globalMonthlyListeners.value = match.groupValues[1]
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
