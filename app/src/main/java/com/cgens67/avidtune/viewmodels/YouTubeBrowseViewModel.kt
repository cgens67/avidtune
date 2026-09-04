package com.cgens67.avidtune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.filterExplicit
import com.cgens67.innertube.models.filterVideoSongs
import com.cgens67.innertube.pages.BrowseResult
import com.cgens67.avidtune.constants.HideExplicitKey
import com.cgens67.avidtune.constants.HideMusicVideosKey
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
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
    private val loadAiContentFilterPolicy: LoadAiContentFilterPolicyUseCase,
    private val filterAiContent: FilterAiContentUseCase
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            val policy = loadAiContentFilterPolicy()
            YouTube
                .browse(browseId, params)
                .onSuccess { browseResult ->
                    val filteredSummaries = browseResult.items.mapNotNull { summary ->
                        val filteredItems = filterAiContent(summary.items, policy)
                        if (filteredItems.isEmpty()) null
                        else summary.copy(items = filteredItems)
                    }
                    
                    result.value = browseResult.copy(items = filteredSummaries)
                                     .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                     .filterVideoSongs(context.dataStore.get(HideMusicVideosKey, false))
                }.onFailure {
                    reportException(it)
                }
        }
    }
}