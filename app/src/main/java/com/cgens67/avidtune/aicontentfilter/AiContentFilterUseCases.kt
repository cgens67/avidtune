/*
 * Most of this code is from ArchiveTune, special thanks to them!
 * 
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.cgens67.avidtune.aicontentfilter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.YTItem
import javax.inject.Inject

class ObserveAiContentFilterUseCase
    @Inject
    constructor(
        private val repository: AiContentFilterRepository,
    ) {
        operator fun invoke(): Flow<Pair<AiContentFilterSettings, AiContentFilterStatus>> =
            combine(repository.observeSettings(), repository.observeStatus(), ::Pair)
    }

class UpdateAiContentFilterSettingsUseCase
    @Inject
    constructor(
        private val repository: AiContentFilterRepository,
    ) {
        suspend fun setEnabled(enabled: Boolean) {
            repository.setEnabled(enabled)
        }

        suspend fun setIncludeModerateConfidence(enabled: Boolean) {
            repository.setIncludeModerateConfidence(enabled)
        }
    }

class RefreshAiContentFilterUseCase
    @Inject
    constructor(
        private val repository: AiContentFilterRepository,
    ) {
        suspend operator fun invoke(force: Boolean): AiContentFilterRefreshResult = repository.refreshIfStale(force)
    }

class LoadAiContentFilterPolicyUseCase
    @Inject
    constructor(
        private val repository: AiContentFilterRepository,
    ) {
        suspend operator fun invoke(): AiContentFilterPolicy {
            val settings = repository.getSettings()
            if (!settings.enabled) return AiContentFilterPolicy.Disabled

            repository.refreshIfStale()
            val lists = repository.loadLists()
            val blockedKeys =
                if (settings.includeModerateConfidence) {
                    lists.blocklist + lists.warnlist
                } else {
                    lists.blocklist
                }
            return AiContentFilterPolicy(
                enabled = true,
                blockedChannelKeys = blockedKeys,
            )
        }
    }

class FilterAiContentUseCase
    @Inject
    constructor() {
        operator fun <T : YTItem> invoke(
            items: List<T>,
            policy: AiContentFilterPolicy,
        ): List<T> {
            if (!policy.enabled || policy.blockedChannelKeys.isEmpty()) return items
            return items.filterNot { item -> item.matches(policy.blockedChannelKeys) }
        }

        private fun YTItem.matches(blockedChannelKeys: Set<String>): Boolean = creatorKeys().any(blockedChannelKeys::contains)

        private fun YTItem.creatorKeys(): Sequence<String> =
            when (this) {
                is SongItem -> {
                    artists.asSequence().flatMap { artist -> artist.keys() }
                }

                is AlbumItem -> {
                    artists.orEmpty().asSequence().flatMap { artist -> artist.keys() }
                }

                is PlaylistItem -> {
                    author?.keys().orEmpty()
                }

                is ArtistItem -> {
                    sequenceOf(title, id, channelId)
                        .filterNotNull()
                        .mapNotNull(::normalizeChannelKey)
                }

                is PodcastItem -> author?.keys().orEmpty()

                is EpisodeItem -> podcast?.keys().orEmpty() + author?.keys().orEmpty()
            }

        private fun Artist.keys(): Sequence<String> =
            sequenceOf(name, id)
                .filterNotNull()
                .mapNotNull(::normalizeChannelKey)

        private fun Album.keys(): Sequence<String> =
            sequenceOf(name, id)
                .filterNotNull()
                .mapNotNull(::normalizeChannelKey)
    }