package com.cgens67.avidtune.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.filterExplicit
import com.cgens67.innertube.models.filterVideoSongs
import com.cgens67.innertube.pages.SearchSummaryPage
import com.cgens67.avidtune.constants.HideExplicitKey
import com.cgens67.avidtune.constants.HideMusicVideosKey
import com.cgens67.avidtune.models.ItemsPage
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.utils.reportException
import com.cgens67.avidtune.aicontentfilter.FilterAiContentUseCase
import com.cgens67.avidtune.aicontentfilter.LoadAiContentFilterPolicyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
    private val loadAiContentFilterPolicy: LoadAiContentFilterPolicyUseCase,
    private val filterAiContent: FilterAiContentUseCase
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            val policy = loadAiContentFilterPolicy()
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        YouTube
                            .searchSummary(query)
                            .onSuccess { result ->
                                val filteredSummaries = result.summaries.mapNotNull { summary ->
                                    val filteredItems = filterAiContent(summary.items, policy)
                                    if (filteredItems.isEmpty()) null
                                    else summary.copy(items = filteredItems)
                                }
                                summaryPage = SearchSummaryPage(filteredSummaries)
                                    .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                    .filterMusicVideos(context.dataStore.get(HideMusicVideosKey, false))
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                val filteredItems = filterAiContent(result.items, policy)
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        filteredItems
                                            .distinctBy { it.id }
                                            .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                            .filterVideoSongs(context.dataStore.get(HideMusicVideosKey, false)),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            val policy = loadAiContentFilterPolicy()
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                val filteredItems = filterAiContent(searchResult.items, policy)
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + filteredItems).distinctBy { it.id }
                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(context.dataStore.get(HideMusicVideosKey, false)),
                    searchResult.continuation
                )
            }
        }
    }
}