package com.tiendatv.player.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiendatv.player.TiendaTvApp
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.AppLanguage
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.EpgProgramEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.MediaType
import com.tiendatv.player.data.local.ProfileEntity
import com.tiendatv.player.data.local.UserSettings
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.data.network.SeriesDetails
import com.tiendatv.player.data.network.SeriesEpisode
import com.tiendatv.player.data.repository.CatalogCounts
import com.tiendatv.player.data.repository.SortMode
import com.tiendatv.player.data.repository.SyncProgress
import com.tiendatv.player.player.BufferPreset
import com.tiendatv.player.player.PlaybackRequest
import com.tiendatv.player.player.TiendaPlayerController
import com.tiendatv.player.player.VideoAspectMode
import com.tiendatv.player.ui.components.NavTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogSliceState(
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String = "",
    val searchQuery: String = "",
    val sortMode: SortMode = SortMode.RECENT,
    val totalCount: Int = 0,
    val items: List<MediaItemEntity> = emptyList(),
    val hasMore: Boolean = false
)

data class MainUiState(
    val settings: UserSettings = UserSettings(),
    val profiles: List<ProfileEntity> = emptyList(),
    val isInitializing: Boolean = true,
    val activeProfile: ProfileEntity? = null,
    val selectedTab: NavTab = NavTab.HOME,
    val showAddProfileModal: Boolean = false,
    val loginBusy: Boolean = false,
    val loginError: String? = null,
    val syncProgress: SyncProgress = SyncProgress(),

    // Home data
    val counts: CatalogCounts = CatalogCounts(),
    val heroItem: MediaItemEntity? = null,
    val continueWatching: List<WatchHistoryEntity> = emptyList(),
    val recentlyWatched: List<WatchHistoryEntity> = emptyList(),
    val favoriteItems: List<MediaItemEntity> = emptyList(),
    val featuredMovies: List<MediaItemEntity> = emptyList(),
    val featuredSeries: List<MediaItemEntity> = emptyList(),
    val favoriteUids: Set<String> = emptySet(),

    // Per-section catalog slices
    val liveSlice: CatalogSliceState = CatalogSliceState(),
    val moviesSlice: CatalogSliceState = CatalogSliceState(),
    val seriesSlice: CatalogSliceState = CatalogSliceState(),

    // Live TV EPG
    val focusedLiveChannel: MediaItemEntity? = null,
    val currentProgramsMap: Map<String, EpgProgramEntity> = emptyMap(),
    val focusedChannelEpg: List<EpgProgramEntity> = emptyList(),

    // Series detail
    val activeSeries: MediaItemEntity? = null,
    val activeSeriesDetails: SeriesDetails? = null,
    val loadingSeriesDetails: Boolean = false,

    // Global search
    val globalSearchQuery: String = "",
    val globalSearchTypeFilter: String = "",
    val globalSearchResults: List<MediaItemEntity> = emptyList(),

    // Parental PIN unlock session
    val parentalUnlockedSession: Boolean = false,
    val pendingParentalItem: MediaItemEntity? = null,
    val pinError: Boolean = false,

    // Fullscreen player
    val activePlayback: PlaybackRequest? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TiendaTvApp
    private val repo = app.repository
    private val settingsStore = app.settingsStore

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var profileObserversJob: Job? = null
    private var searchDebounceJob: Job? = null
    private var epgFetchJob: Job? = null

    init {
        viewModelScope.launch {
            settingsStore.settingsFlow.collectLatest { settings ->
                val prevParental = _state.value.settings.parentalEnabled
                val prevLang = _state.value.settings.language
                _state.update { it.copy(settings = settings) }
                if (prevParental != settings.parentalEnabled || prevLang != settings.language) {
                    _state.value.activeProfile?.let { refreshAllSections(it) }
                }
            }
        }

        viewModelScope.launch {
            repo.observeProfiles().collectLatest { list ->
                _state.update { it.copy(profiles = list) }
            }
        }

        viewModelScope.launch {
            repo.observeActiveProfile().collectLatest { profile ->
                _state.update { it.copy(isInitializing = false, activeProfile = profile) }
                profileObserversJob?.cancel()
                if (profile != null) {
                    attachProfileObservers(profile)
                    refreshAllSections(profile)
                }
            }
        }
    }

    private fun includeAdultContent(): Boolean {
        val s = _state.value
        return !s.settings.parentalEnabled || s.parentalUnlockedSession
    }

    private fun attachProfileObservers(profile: ProfileEntity) {
        profileObserversJob = viewModelScope.launch {
            launch {
                repo.observeFavoriteUids(profile.id).collectLatest { uids ->
                    val set = uids.toSet()
                    val favItems = repo.getFavorites(profile.id, includeAdultContent())
                    val counts = repo.getCounts(profile.id, includeAdultContent())
                    _state.update {
                        it.copy(
                            favoriteUids = set,
                            favoriteItems = favItems,
                            counts = counts
                        )
                    }
                }
            }
            launch {
                repo.observeContinueWatching(profile.id).collectLatest { cw ->
                    _state.update { it.copy(continueWatching = cw) }
                }
            }
            launch {
                repo.observeRecentlyWatched(profile.id).collectLatest { recent ->
                    _state.update { it.copy(recentlyWatched = recent) }
                }
            }
        }
    }

    private fun refreshAllSections(profile: ProfileEntity) {
        viewModelScope.launch {
            val incAdult = includeAdultContent()
            val counts = repo.getCounts(profile.id, incAdult)
            val featuredMovies = repo.getFeatured(profile.id, MediaType.MOVIE, incAdult, 18)
            val featuredSeries = repo.getFeatured(profile.id, MediaType.SERIES, incAdult, 18)
            val favorites = repo.getFavorites(profile.id, incAdult)
            val hero = featuredMovies.firstOrNull() ?: featuredSeries.firstOrNull()

            _state.update {
                it.copy(
                    counts = counts,
                    featuredMovies = featuredMovies,
                    featuredSeries = featuredSeries,
                    favoriteItems = favorites,
                    heroItem = hero
                )
            }
            reloadCatalogSlice(MediaType.LIVE, resetOffset = true)
            reloadCatalogSlice(MediaType.MOVIE, resetOffset = true)
            reloadCatalogSlice(MediaType.SERIES, resetOffset = true)
        }
    }

    fun selectTab(tab: NavTab) {
        _state.update { it.copy(selectedTab = tab, activeSeries = null) }
    }

    fun openAddProfileModal(show: Boolean) {
        if (show) TiendaPlayerController.releaseAnyActive()
        _state.update { it.copy(showAddProfileModal = show, activePlayback = if (show) null else it.activePlayback, loginError = null) }
    }

    fun addXtreamProfile(name: String, serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _state.update { it.copy(loginError = "Completa URL, usuario y contraseña / Fill all fields") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loginBusy = true, loginError = null) }
            val result = repo.addXtreamProfile(name, serverUrl, username, password)
            result.fold(
                onSuccess = { profile ->
                    _state.update { it.copy(loginBusy = false, showAddProfileModal = false) }
                    syncProfile(profile)
                },
                onFailure = { err ->
                    _state.update { it.copy(loginBusy = false, loginError = err.message ?: "Login error") }
                }
            )
        }
    }

    fun addM3uProfile(name: String, playlistUrl: String, epgUrl: String) {
        if (playlistUrl.isBlank()) {
            _state.update { it.copy(loginError = "Ingresa la URL M3U / Enter M3U URL") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loginBusy = true, loginError = null) }
            val result = repo.addM3uProfile(name, playlistUrl, epgUrl)
            result.fold(
                onSuccess = { profile ->
                    _state.update { it.copy(loginBusy = false, showAddProfileModal = false) }
                    syncProfile(profile)
                },
                onFailure = { err ->
                    _state.update { it.copy(loginBusy = false, loginError = err.message ?: "M3U error") }
                }
            )
        }
    }

    fun switchProfile(profile: ProfileEntity) {
        TiendaPlayerController.releaseAnyActive()
        viewModelScope.launch {
            repo.switchProfile(profile.id)
            _state.update { it.copy(showAddProfileModal = false, activePlayback = null, selectedTab = NavTab.HOME) }
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        TiendaPlayerController.releaseAnyActive()
        viewModelScope.launch {
            repo.deleteProfile(profile.id)
            _state.update { it.copy(activePlayback = null) }
        }
    }

    fun syncActiveProfile() {
        val profile = _state.value.activeProfile ?: return
        syncProfile(profile)
    }

    private fun syncProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            _state.update { it.copy(syncProgress = SyncProgress(isSyncing = true, stage = "SYNC", progressFraction = 0.05f)) }
            val res = repo.syncProfileCatalog(profile) { stage, fraction ->
                _state.update {
                    it.copy(syncProgress = SyncProgress(isSyncing = true, stage = stage, progressFraction = fraction))
                }
            }
            _state.update {
                it.copy(
                    syncProgress = SyncProgress(
                        isSyncing = false,
                        error = res.exceptionOrNull()?.message
                    )
                )
            }
            refreshAllSections(profile)
        }
    }

    fun refreshEpg() {
        val profile = _state.value.activeProfile ?: return
        viewModelScope.launch {
            repo.refreshXmltvEpg(profile)
            val channels = _state.value.liveSlice.items
            val map = repo.getCurrentProgramsMap(profile.id, channels)
            _state.update { it.copy(currentProgramsMap = map) }
            _state.value.focusedLiveChannel?.let { focusLiveChannel(it) }
        }
    }

    fun focusLiveChannel(channel: MediaItemEntity) {
        val profile = _state.value.activeProfile ?: return
        _state.update { it.copy(focusedLiveChannel = channel) }
        epgFetchJob?.cancel()
        epgFetchJob = viewModelScope.launch {
            val programs = repo.getEpgForChannel(profile, channel)
            _state.update { it.copy(focusedChannelEpg = programs) }
        }
    }

    fun updateSliceCategory(type: String, categoryId: String) {
        updateSliceState(type) { it.copy(selectedCategoryId = categoryId) }
        reloadCatalogSlice(type, resetOffset = true)
    }

    fun updateSliceQuery(type: String, query: String) {
        updateSliceState(type) { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(220L)
            reloadCatalogSlice(type, resetOffset = true)
        }
    }

    fun cycleSliceSort(type: String) {
        val current = getSliceState(type).sortMode
        val all = SortMode.entries
        val next = all[(current.ordinal + 1) % all.size]
        updateSliceState(type) { it.copy(sortMode = next) }
        reloadCatalogSlice(type, resetOffset = true)
    }

    fun loadMoreSlice(type: String) {
        reloadCatalogSlice(type, resetOffset = false)
    }

    private fun getSliceState(type: String): CatalogSliceState = when (type) {
        MediaType.LIVE -> _state.value.liveSlice
        MediaType.MOVIE -> _state.value.moviesSlice
        else -> _state.value.seriesSlice
    }

    private fun updateSliceState(type: String, block: (CatalogSliceState) -> CatalogSliceState) {
        _state.update {
            when (type) {
                MediaType.LIVE -> it.copy(liveSlice = block(it.liveSlice))
                MediaType.MOVIE -> it.copy(moviesSlice = block(it.moviesSlice))
                else -> it.copy(seriesSlice = block(it.seriesSlice))
            }
        }
    }

    private fun reloadCatalogSlice(type: String, resetOffset: Boolean) {
        val profile = _state.value.activeProfile ?: return
        viewModelScope.launch {
            val incAdult = includeAdultContent()
            val slice = getSliceState(type)
            val categories = if (resetOffset) {
                prioritizeCategoriesByLanguage(
                    repo.getCategories(profile.id, type, incAdult),
                    _state.value.settings.language
                )
            } else {
                slice.categories
            }
            val offset = if (resetOffset) 0 else slice.items.size
            val (total, page) = repo.queryCatalog(
                profileId = profile.id,
                type = type,
                categoryId = slice.selectedCategoryId,
                query = slice.searchQuery,
                sortMode = slice.sortMode,
                includeAdult = incAdult,
                offset = offset,
                limit = AppConfig.CATALOG_PAGE_SIZE
            )
            val combined = if (resetOffset) page else slice.items + page
            updateSliceState(type) {
                it.copy(
                    categories = categories,
                    totalCount = total,
                    items = combined,
                    hasMore = combined.size < total
                )
            }
            if (type == MediaType.LIVE) {
                val epgMap = repo.getCurrentProgramsMap(profile.id, combined)
                _state.update { it.copy(currentProgramsMap = epgMap) }
                if (resetOffset && combined.isNotEmpty() && _state.value.focusedLiveChannel == null) {
                    focusLiveChannel(combined.first())
                }
            }
        }
    }

    fun updateGlobalSearch(query: String, typeFilter: String = _state.value.globalSearchTypeFilter) {
        _state.update { it.copy(globalSearchQuery = query, globalSearchTypeFilter = typeFilter) }
        val profile = _state.value.activeProfile ?: return
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(220L)
            val results = repo.searchGlobal(profile.id, typeFilter, query, includeAdultContent())
            _state.update { it.copy(globalSearchResults = results) }
        }
    }

    fun toggleFavorite(item: MediaItemEntity) {
        val profile = _state.value.activeProfile ?: return
        val currentlyFav = _state.value.favoriteUids.contains(item.uid)
        viewModelScope.launch {
            repo.toggleFavorite(profile.id, item, currentlyFav)
        }
    }

    fun openMedia(item: MediaItemEntity) {
        if (item.isAdult && _state.value.settings.parentalEnabled && !_state.value.parentalUnlockedSession) {
            _state.update { it.copy(pendingParentalItem = item, pinError = false) }
            return
        }
        proceedOpenMedia(item)
    }

    fun verifyParentalPin(enteredPin: String) {
        val expected = _state.value.settings.parentalPin
        if (enteredPin == expected) {
            val target = _state.value.pendingParentalItem
            _state.update {
                it.copy(
                    parentalUnlockedSession = true,
                    pendingParentalItem = null,
                    pinError = false
                )
            }
            _state.value.activeProfile?.let { refreshAllSections(it) }
            if (target != null) proceedOpenMedia(target)
        } else {
            _state.update { it.copy(pinError = true) }
        }
    }

    fun dismissParentalPrompt() {
        _state.update { it.copy(pendingParentalItem = null, pinError = false) }
    }

    private fun proceedOpenMedia(item: MediaItemEntity) {
        val profile = _state.value.activeProfile ?: return
        if (item.type == MediaType.SERIES && item.streamUrl.isBlank()) {
            _state.update {
                it.copy(activeSeries = item, activeSeriesDetails = null, loadingSeriesDetails = true)
            }
            viewModelScope.launch {
                val details = repo.getSeriesDetails(profile, item.streamId)
                _state.update { it.copy(activeSeriesDetails = details, loadingSeriesDetails = false) }
            }
        } else {
            viewModelScope.launch {
                val prev = repo.getWatchProgress(item.uid)
                val resumeMs = if (item.type == MediaType.LIVE) 0L else (prev?.positionMs ?: 0L)
                _state.update {
                    it.copy(
                        activePlayback = PlaybackRequest(
                            uid = item.uid,
                            mediaUid = item.uid,
                            type = item.type,
                            title = item.name,
                            subtitle = item.year,
                            posterUrl = item.backdropUrl.ifBlank { item.logoUrl },
                            streamUrl = item.streamUrl,
                            resumePositionMs = resumeMs,
                            isLive = item.type == MediaType.LIVE
                        )
                    )
                }
            }
        }
    }

    fun playSeriesEpisode(series: MediaItemEntity, episode: SeriesEpisode) {
        val epUid = "${series.uid}_ep_${episode.id}"
        viewModelScope.launch {
            val prev = repo.getWatchProgress(epUid)
            _state.update {
                it.copy(
                    activePlayback = PlaybackRequest(
                        uid = epUid,
                        mediaUid = series.uid,
                        type = MediaType.SERIES,
                        title = series.name,
                        subtitle = "S${episode.seasonNumber} E${episode.episodeNumber} • ${episode.title}",
                        posterUrl = episode.thumbnailUrl.ifBlank { series.backdropUrl.ifBlank { series.logoUrl } },
                        streamUrl = episode.streamUrl,
                        resumePositionMs = prev?.positionMs ?: 0L,
                        isLive = false
                    )
                )
            }
        }
    }

    fun resumeHistory(entry: WatchHistoryEntity) {
        _state.update {
            it.copy(
                activePlayback = PlaybackRequest(
                    uid = entry.uid,
                    mediaUid = entry.mediaUid,
                    type = entry.type,
                    title = entry.title,
                    subtitle = entry.subtitle,
                    posterUrl = entry.posterUrl,
                    streamUrl = entry.streamUrl,
                    resumePositionMs = if (entry.type == MediaType.LIVE) 0L else entry.positionMs,
                    isLive = entry.type == MediaType.LIVE
                )
            )
        }
    }

    fun switchLiveChannelByOffset(delta: Int) {
        val playback = _state.value.activePlayback ?: return
        if (!playback.isLive) return
        val channels = _state.value.liveSlice.items
        val idx = channels.indexOfFirst { it.uid == playback.uid }
        if (idx < 0) return
        val nextIdx = (idx + delta).coerceIn(0, channels.lastIndex)
        if (nextIdx != idx) {
            val next = channels[nextIdx]
            focusLiveChannel(next)
            proceedOpenMedia(next)
        }
    }

    fun savePlaybackProgress(positionMs: Long, durationMs: Long) {
        val profile = _state.value.activeProfile ?: return
        val req = _state.value.activePlayback ?: return
        viewModelScope.launch {
            repo.saveWatchProgress(
                WatchHistoryEntity(
                    uid = req.uid,
                    profileId = profile.id,
                    mediaUid = req.mediaUid,
                    type = req.type,
                    title = req.title,
                    subtitle = req.subtitle,
                    posterUrl = req.posterUrl,
                    streamUrl = req.streamUrl,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun closePlayer() {
        TiendaPlayerController.releaseAnyActive()
        _state.update { it.copy(activePlayback = null) }
    }

    fun closeSeriesDetail() {
        _state.update { it.copy(activeSeries = null, activeSeriesDetails = null) }
    }

    // Settings mutators
    fun setDarkCinemaMode(enabled: Boolean) = viewModelScope.launch { settingsStore.setDarkCinemaMode(enabled) }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { settingsStore.setLanguage(lang) }
    fun setBufferPreset(preset: BufferPreset) = viewModelScope.launch { settingsStore.setBufferPreset(preset) }
    fun setSoftwareDecoder(prefer: Boolean) = viewModelScope.launch { settingsStore.setPreferSoftwareDecoder(prefer) }
    fun setDefaultAspectMode(mode: VideoAspectMode) = viewModelScope.launch { settingsStore.setDefaultAspectMode(mode) }
    fun setParentalControl(enabled: Boolean, pin: String) = viewModelScope.launch {
        settingsStore.setParentalControl(enabled, pin)
        if (!enabled) _state.update { it.copy(parentalUnlockedSession = false) }
    }
    fun clearHistory() {
        val profile = _state.value.activeProfile ?: return
        viewModelScope.launch { repo.clearHistory(profile.id) }
    }
    fun removeHistoryItem(uid: String) {
        viewModelScope.launch { repo.removeHistoryItem(uid) }
    }

    private fun prioritizeCategoriesByLanguage(
        categories: List<CategoryEntity>,
        language: AppLanguage
    ): List<CategoryEntity> {
        if (language == AppLanguage.ES) return categories
        val enTokens = listOf("english", " eng", "eng ", "en |", "| en", "en-", "usa", "us |", "uk |", "canada", "ca |")
        return categories.sortedBy { cat ->
            val lower = " ${cat.name.lowercase()} "
            if (enTokens.any { lower.contains(it) }) 0 else 1
        }
    }
}
