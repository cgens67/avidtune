package com.cgens67.avidtune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.BrowseEndpoint
import com.cgens67.innertube.models.filterExplicit
import com.cgens67.innertube.models.filterVideoSongs
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistItemsViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
    private val loadAiContentFilterPolicy: LoadAiContentFilterPolicyUseCase,
    private val filterAiContent: FilterAiContentUseCase
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val title = MutableStateFlow("")
    val itemsPage = MutableStateFlow<ItemsPage?>(null)

    init {
        viewModelScope.launch {
            val policy = loadAiContentFilterPolicy()
            YouTube
                .artistItems(
                    BrowseEndpoint(
                        browseId = browseId,
                        params = params,
                    ),
                ).onSuccess { artistItemsPage ->
                    title.value = artistItemsPage.title
                    itemsPage.value =
                        ItemsPage(
                            items = filterAiContent(artistItemsPage.items, policy)
                                .distinctBy { it.id }
                                .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                .filterVideoSongs(context.dataStore.get(HideMusicVideosKey, false)),
                            continuation = artistItemsPage.continuation,
                        )
                }.onFailure {
                    reportException(it)
                }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            val policy = loadAiContentFilterPolicy()
            val oldItemsPage = itemsPage.value ?: return@launch
            val continuation = oldItemsPage.continuation ?: return@launch
            YouTube
                .artistItemsContinuation(continuation)
                .onSuccess { artistItemsContinuationPage ->
                    itemsPage.update {
                        val filteredItems = filterAiContent(artistItemsContinuationPage.items, policy)
                        ItemsPage(
                            items =
                                (oldItemsPage.items + filteredItems)
                                    .distinctBy { it.id }
                                    .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                    .filterVideoSongs(context.dataStore.get(HideMusicVideosKey, false)),
                            continuation = artistItemsContinuationPage.continuation,
                        )
                    }
                }.onFailure {
                    reportException(it)
                }
        }
    }
}