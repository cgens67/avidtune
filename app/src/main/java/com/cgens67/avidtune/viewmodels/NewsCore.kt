package com.cgens67.avidtune.viewmodels

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.avidtune.constants.NewsLastReadTimestampKey
import com.cgens67.avidtune.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

// ==========================================
// MODELS
// ==========================================

@Serializable
@Immutable
data class NewsItem(
    @SerialName("id") val id: String = "",
    @SerialName("Title") val title: String,
    @SerialName("Description") val description: String = "",
    @SerialName("ImageURL")
    @Serializable(with = NewsImageUrlsSerializer::class)
    val imageUrls: List<String> = emptyList(),
    @SerialName("Important") val important: Boolean = false,
    @SerialName("Author") val author: String,
    @SerialName("Date") val timestamp: Long = 0L,
) {
    val stableKey: String
        get() = id.ifEmpty { "$timestamp|$author|$title" }
}

object NewsImageUrlsSerializer : KSerializer<List<String>> {
    private val delegate = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)

        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> emptyList()
            is JsonArray -> element.mapNotNull { item ->
                (item as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
            }
            is JsonPrimitive -> element.contentOrNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::listOf)
                ?: emptyList()
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        delegate.serialize(encoder, value)
    }
}

// ==========================================
// REPOSITORY
// ==========================================

@Singleton
class NewsRepository @Inject constructor() {

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 15000
            endpoint {
                connectTimeout = 15000
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Volatile private var metadataCache: List<NewsItem>? = null

    suspend fun fetchNews(): List<NewsItem> {
        val response = client.get(METADATA_URL) {
            headers {
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
                append(HttpHeaders.Expires, "0")
            }
        }
        val text = response.bodyAsText()
        val items = json.decodeFromString<List<NewsItem>>(text)
        metadataCache = items
        return items
    }

    suspend fun fetchNewsContent(id: String): String {
        val response = client.get("$CONTENT_BASE_URL$id") {
            headers {
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
                append(HttpHeaders.Expires, "0")
            }
        }
        return response.bodyAsText()
    }

    fun getCachedItem(id: String): NewsItem? = metadataCache?.find { it.id == id }

    private companion object {
        const val METADATA_URL =
            "https://raw.githubusercontent.com/cgens67/avidtune-news/main/metadata.json"
        const val CONTENT_BASE_URL =
            "https://raw.githubusercontent.com/cgens67/avidtune-news/main/content/"
    }
}

// ==========================================
// VIEWMODELS
// ==========================================

sealed interface NewsUiState {
    data object Loading : NewsUiState
    data class Success(val items: List<NewsItem>) : NewsUiState
    data object Empty : NewsUiState
    data class Error(val message: String) : NewsUiState
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _rawItems = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _loadState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)

    val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NewsUiState> = combine(_loadState, searchQuery, _rawItems) { loadState, query, items ->
        when (loadState) {
            is NewsUiState.Loading -> NewsUiState.Loading
            is NewsUiState.Error -> loadState
            is NewsUiState.Empty -> NewsUiState.Empty
            is NewsUiState.Success -> {
                if (query.isBlank()) {
                    loadState
                } else {
                    val q = query.trim().lowercase()
                    val filtered = items.filter { item ->
                        item.title.lowercase().contains(q) ||
                                item.author.lowercase().contains(q)
                    }
                    if (filtered.isEmpty()) NewsUiState.Empty else NewsUiState.Success(filtered)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState.Loading)

    val hasUnreadNews: StateFlow<Boolean> = combine(
        _rawItems,
        context.dataStore.data.map { prefs -> prefs[NewsLastReadTimestampKey] ?: 0L },
    ) { items, lastReadTimestamp ->
        items.isNotEmpty() && items.maxOf { it.timestamp } > lastReadTimestamp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        fetchNews()
    }

    fun fetchNews() {
        viewModelScope.launch {
            _loadState.value = NewsUiState.Loading
            runCatching {
                repository.fetchNews().sortedByDescending { it.timestamp }
            }.onSuccess { items ->
                _rawItems.value = items
                _loadState.value = if (items.isEmpty()) NewsUiState.Empty else NewsUiState.Success(items)
            }.onFailure { error ->
                _loadState.value = NewsUiState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun markAllRead() {
        val latestTimestamp = _rawItems.value.maxOfOrNull { it.timestamp } ?: return
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[NewsLastReadTimestampKey] = latestTimestamp
            }
        }
    }
}

sealed interface ViewNewsUiState {
    data object Loading : ViewNewsUiState
    data class Success(val content: String) : ViewNewsUiState
    data class Error(val message: String) : ViewNewsUiState
}

@HiltViewModel
class ViewNewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val newsId: String = savedStateHandle.get<String>("newsId") ?: ""
    val newsItem: NewsItem? = repository.getCachedItem(newsId)

    private val _contentState = MutableStateFlow<ViewNewsUiState>(ViewNewsUiState.Loading)
    val contentState: StateFlow<ViewNewsUiState> = _contentState.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _contentState.value = ViewNewsUiState.Loading
            runCatching {
                repository.fetchNewsContent(newsId)
            }.onSuccess { content ->
                _contentState.value = ViewNewsUiState.Success(content)
            }.onFailure { error ->
                _contentState.value = ViewNewsUiState.Error(error.message ?: "Unknown error")
            }
        }
    }
}