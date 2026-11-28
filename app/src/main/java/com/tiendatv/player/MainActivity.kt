package com.tiendatv.player

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import com.tiendatv.player.ui.components.GlassPillButton
import kotlinx.coroutines.delay
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.data.local.MediaType
import com.tiendatv.player.player.PlayerScreen
import com.tiendatv.player.player.TiendaPlayerController
import com.tiendatv.player.ui.components.FrostedCanvasBackground
import com.tiendatv.player.ui.components.NavTab
import com.tiendatv.player.ui.components.PinEntryDialog
import com.tiendatv.player.ui.components.ProvideSmoothTvScrolling
import com.tiendatv.player.ui.components.TiendaNavigationRail
import com.tiendatv.player.ui.i18n.stringsFor
import com.tiendatv.player.ui.screens.CatalogGridScreen
import com.tiendatv.player.ui.screens.HomeScreen
import com.tiendatv.player.ui.screens.LiveTvScreen
import com.tiendatv.player.ui.screens.LoginScreen
import com.tiendatv.player.ui.screens.SearchScreen
import com.tiendatv.player.ui.screens.SeriesDetailScreen
import com.tiendatv.player.ui.screens.SettingsScreen
import com.tiendatv.player.ui.state.MainViewModel
import com.tiendatv.player.ui.theme.LocalTiendaColors
import com.tiendatv.player.ui.theme.TiendaTvTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TiendaTvRoot(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        TiendaPlayerController.releaseAnyActive()
        super.onDestroy()
    }
}

@Composable
private fun TiendaTvRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val strings = remember(state.settings.language) { stringsFor(state.settings.language) }

    TiendaTvTheme(darkCinemaMode = state.settings.darkCinemaMode) {
        ProvideSmoothTvScrolling {
            val activePlayback = state.activePlayback
            if (activePlayback != null) {
                val liveList = state.liveSlice.items
                val liveIdx = remember(activePlayback.uid, liveList) {
                    liveList.indexOfFirst { it.uid == activePlayback.uid }
                }
                val nowEpg = remember(activePlayback.uid, state.currentProgramsMap, liveList) {
                    val ch = liveList.getOrNull(liveIdx)
                    val key = ch?.epgChannelId?.ifBlank { ch.streamId }
                    if (key != null) state.currentProgramsMap[key]?.title else null
                }
                val seriesEpisodes = remember(state.activeSeriesDetails) {
                    state.activeSeriesDetails?.seasons?.toSortedMap()?.values?.flatten().orEmpty()
                }
                val seriesEpIdx = remember(activePlayback.uid, state.activeSeries?.uid, seriesEpisodes) {
                    val sUid = state.activeSeries?.uid
                    if (sUid != null && activePlayback.type == MediaType.SERIES) {
                        seriesEpisodes.indexOfFirst { "${sUid}_ep_${it.id}" == activePlayback.uid }
                    } else -1
                }

                PlayerScreen(
                    request = activePlayback,
                    settings = state.settings,
                    strings = strings,
                    epgNowTitle = nowEpg,
                    hasPrevChannel = activePlayback.isLive && liveIdx > 0,
                    hasNextChannel = activePlayback.isLive && liveIdx >= 0 && liveIdx < liveList.lastIndex,
                    hasPrevEpisode = !activePlayback.isLive && seriesEpIdx > 0,
                    hasNextEpisode = !activePlayback.isLive && seriesEpIdx >= 0 && seriesEpIdx < seriesEpisodes.lastIndex,
                    onPrevEpisode = { viewModel.switchSeriesEpisodeByOffset(-1) },
                    onNextEpisode = { viewModel.switchSeriesEpisodeByOffset(1) },
                    liveCategories = state.liveSlice.categories,
                    selectedLiveCategoryId = state.liveSlice.selectedCategoryId,
                    liveChannels = liveList,
                    currentPrograms = state.currentProgramsMap,
                    onSelectLiveCategory = { viewModel.updateSliceCategory(MediaType.LIVE, it) },
                    onSelectLiveChannel = { ch ->
                        viewModel.focusLiveChannel(ch)
                        viewModel.openMedia(ch)
                    },
                    onPrevChannel = { viewModel.switchLiveChannelByOffset(-1) },
                    onNextChannel = { viewModel.switchLiveChannelByOffset(1) },
                    onSaveProgress = { pos, dur -> viewModel.savePlaybackProgress(pos, dur) },
                    onClose = { viewModel.closePlayer() }
                )
            } else {
                FrostedCanvasBackground(modifier = Modifier.safeDrawingPadding()) {
                    val activity = LocalContext.current as? Activity
                    var showExitDialog by remember { mutableStateOf(false) }
                    val showLogin = !state.isInitializing && (state.activeProfile == null || state.showAddProfileModal)
                    if (state.isInitializing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    } else if (showLogin) {
                        LoginScreen(
                            profiles = state.profiles,
                            isBusy = state.loginBusy,
                            errorMessage = state.loginError,
                            strings = strings,
                            onAddXtream = { name, url, user, pass ->
                                viewModel.addXtreamProfile(name, url, user, pass)
                            },
                            onAddM3u = { name, m3uUrl, epgUrl ->
                                viewModel.addM3uProfile(name, m3uUrl, epgUrl)
                            },
                            onSelectProfile = { viewModel.switchProfile(it) },
                            onCancel = if (state.activeProfile != null) {
                                { viewModel.openAddProfileModal(false) }
                            } else null
                        )
                    } else {
                        val railFocusRequester = remember { FocusRequester() }
                        val homeFocusRequester = remember { FocusRequester() }
                        var isRailFocused by remember { mutableStateOf(false) }

                        BackHandler(enabled = state.activeSeries == null && !state.syncProgress.isSyncing) {
                            when {
                                showExitDialog -> showExitDialog = false
                                state.selectedTab != NavTab.HOME -> {
                                    if (!isRailFocused) {
                                        try { railFocusRequester.requestFocus() } catch (_: Exception) {
                                            viewModel.selectTab(NavTab.HOME)
                                        }
                                    } else {
                                        viewModel.selectTab(NavTab.HOME)
                                    }
                                }
                                isRailFocused -> {
                                    try { homeFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                else -> showExitDialog = true
                            }
                        }

                        if (state.syncProgress.isSyncing) {
                            CleanFullScreenSync(
                                stage = state.syncProgress.stage,
                                progress = state.syncProgress.progressFraction,
                                title = strings.syncingCatalog
                            )
                        } else {
                            val isHomeMode = state.selectedTab == NavTab.HOME && state.activeSeries == null
                            AnimatedContent(
                                targetState = isHomeMode,
                                transitionSpec = {
                                    if (!targetState) {
                                        // Entering Live TV / Movies / Series from Home: silky fade + subtle depth zoom
                                        (fadeIn(tween(340, easing = FastOutSlowInEasing)) +
                                            scaleIn(initialScale = 0.985f, animationSpec = tween(340, easing = FastOutSlowInEasing))) togetherWith
                                            fadeOut(tween(220))
                                    } else {
                                        // Returning to Home: calm dissolve
                                        fadeIn(tween(320, easing = FastOutSlowInEasing)) togetherWith
                                            (fadeOut(tween(220)) + scaleOut(targetScale = 0.985f, animationSpec = tween(220)))
                                    }
                                },
                                label = "homeContentCinemaTransition"
                            ) { showingHome ->
                                if (showingHome) {
                                    HomeScreen(
                                        counts = state.counts,
                                        heroItem = state.heroItem,
                                        continueWatching = state.continueWatching,
                                        recentlyWatched = state.recentlyWatched,
                                        favorites = state.favoriteItems,
                                        featuredMovies = state.featuredMovies,
                                        featuredSeries = state.featuredSeries,
                                        favoriteUids = state.favoriteUids,
                                        strings = strings,
                                        onNavigateTab = { viewModel.selectTab(it) },
                                        onOpenMedia = { viewModel.openMedia(it) },
                                        onResumeHistory = { viewModel.resumeHistory(it) },
                                        onRemoveHistory = { viewModel.removeHistoryItem(it) },
                                        onClearHistory = { viewModel.clearHistory() },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onReloadCatalog = { viewModel.syncActiveProfile() },
                                        onExitApp = { showExitDialog = true },
                                        primaryFocusRequester = homeFocusRequester
                                    )
                                } else {
                                    Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                TiendaNavigationRail(
                                    selectedTab = state.selectedTab,
                                    strings = strings,
                                    onSelectTab = { viewModel.selectTab(it) },
                                    floatingOverlay = false,
                                    railFocusRequester = railFocusRequester,
                                    onFocusStateChanged = { isRailFocused = it }
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                ) {
                                    AnimatedContent(
                                        targetState = state.activeSeries,
                                        transitionSpec = {
                                            if (targetState != null) {
                                                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 18 }) togetherWith
                                                    fadeOut(tween(150))
                                            } else {
                                                fadeIn(tween(180)) togetherWith
                                                    (fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 18 })
                                            }
                                        },
                                        label = "seriesDetailTransition"
                                    ) { openSeries ->
                                        if (openSeries != null) {
                                            SeriesDetailScreen(
                                                series = openSeries,
                                                details = state.activeSeriesDetails,
                                                episodeHistory = state.activeSeriesHistory,
                                                lastWatchedEpisodeUid = state.lastWatchedSeriesEpisodeUid,
                                                isLoading = state.loadingSeriesDetails,
                                                strings = strings,
                                                onPlayEpisode = { ep ->
                                                    viewModel.playSeriesEpisode(openSeries, ep)
                                                },
                                                onBack = { viewModel.closeSeriesDetail() }
                                            )
                                        } else {
                                            when (state.selectedTab) {
                                                    NavTab.HOME -> HomeScreen(
                                                        counts = state.counts,
                                                        heroItem = state.heroItem,
                                                        continueWatching = state.continueWatching,
                                                        recentlyWatched = state.recentlyWatched,
                                                        favorites = state.favoriteItems,
                                                        featuredMovies = state.featuredMovies,
                                                        featuredSeries = state.featuredSeries,
                                                        favoriteUids = state.favoriteUids,
                                                        strings = strings,
                                                        onNavigateTab = { viewModel.selectTab(it) },
                                                        onOpenMedia = { viewModel.openMedia(it) },
                                                        onResumeHistory = { viewModel.resumeHistory(it) },
                                                        onRemoveHistory = { viewModel.removeHistoryItem(it) },
                                                        onClearHistory = { viewModel.clearHistory() },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                        onReloadCatalog = { viewModel.syncActiveProfile() },
                                                        onExitApp = { activity?.finish() }
                                                    )

                                                    NavTab.LIVE -> LiveTvScreen(
                                                        categories = state.liveSlice.categories,
                                                        selectedCategoryId = state.liveSlice.selectedCategoryId,
                                                        searchQuery = state.liveSlice.searchQuery,
                                                        totalCount = state.liveSlice.totalCount,
                                                        sortMode = state.liveSlice.sortMode,
                                                        channels = state.liveSlice.items,
                                                        hasMore = state.liveSlice.hasMore,
                                                        focusedChannel = state.focusedLiveChannel,
                                                        currentPrograms = state.currentProgramsMap,
                                                        focusedChannelEpg = state.focusedChannelEpg,
                                                        favoriteUids = state.favoriteUids,
                                                        strings = strings,
                                                        onSelectCategory = { viewModel.updateSliceCategory(MediaType.LIVE, it) },
                                                        onSearchQueryChange = { viewModel.updateSliceQuery(MediaType.LIVE, it) },
                                                        onCycleSort = { viewModel.cycleSliceSort(MediaType.LIVE) },
                                                        onLoadMore = { viewModel.loadMoreSlice(MediaType.LIVE) },
                                                        onFocusChannel = { viewModel.focusLiveChannel(it) },
                                                        onPlayChannel = { viewModel.openMedia(it) },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                        onRefreshEpg = { viewModel.refreshEpg() }
                                                    )

                                                    NavTab.MOVIES -> CatalogGridScreen(
                                                        categories = state.moviesSlice.categories,
                                                        selectedCategoryId = state.moviesSlice.selectedCategoryId,
                                                        searchQuery = state.moviesSlice.searchQuery,
                                                        totalCount = state.moviesSlice.totalCount,
                                                        sortMode = state.moviesSlice.sortMode,
                                                        items = state.moviesSlice.items,
                                                        hasMore = state.moviesSlice.hasMore,
                                                        favoriteUids = state.favoriteUids,
                                                        strings = strings,
                                                        onSelectCategory = { viewModel.updateSliceCategory(MediaType.MOVIE, it) },
                                                        onSearchQueryChange = { viewModel.updateSliceQuery(MediaType.MOVIE, it) },
                                                        onCycleSort = { viewModel.cycleSliceSort(MediaType.MOVIE) },
                                                        onLoadMore = { viewModel.loadMoreSlice(MediaType.MOVIE) },
                                                        onOpenItem = { viewModel.openMedia(it) },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                                                    )

                                                    NavTab.SERIES -> CatalogGridScreen(
                                                        categories = state.seriesSlice.categories,
                                                        selectedCategoryId = state.seriesSlice.selectedCategoryId,
                                                        searchQuery = state.seriesSlice.searchQuery,
                                                        totalCount = state.seriesSlice.totalCount,
                                                        sortMode = state.seriesSlice.sortMode,
                                                        items = state.seriesSlice.items,
                                                        hasMore = state.seriesSlice.hasMore,
                                                        favoriteUids = state.favoriteUids,
                                                        strings = strings,
                                                        onSelectCategory = { viewModel.updateSliceCategory(MediaType.SERIES, it) },
                                                        onSearchQueryChange = { viewModel.updateSliceQuery(MediaType.SERIES, it) },
                                                        onCycleSort = { viewModel.cycleSliceSort(MediaType.SERIES) },
                                                        onLoadMore = { viewModel.loadMoreSlice(MediaType.SERIES) },
                                                        onOpenItem = { viewModel.openMedia(it) },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                                                    )

                                                    NavTab.SEARCH -> SearchScreen(
                                                        query = state.globalSearchQuery,
                                                        selectedTypeFilter = state.globalSearchTypeFilter,
                                                        results = state.globalSearchResults,
                                                        favoriteUids = state.favoriteUids,
                                                        strings = strings,
                                                        onQueryChange = { viewModel.updateGlobalSearch(it) },
                                                        onSelectTypeFilter = {
                                                            viewModel.updateGlobalSearch(state.globalSearchQuery, it)
                                                        },
                                                        onOpenItem = { viewModel.openMedia(it) },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                                                    )

                                                    NavTab.SETTINGS -> SettingsScreen(
                                                        settings = state.settings,
                                                        profiles = state.profiles,
                                                        strings = strings,
                                                        onToggleDarkCinema = { viewModel.setDarkCinemaMode(it) },
                                                        onSelectLanguage = { viewModel.setLanguage(it) },
                                                        onSelectBuffer = { viewModel.setBufferPreset(it) },
                                                        onToggleSoftwareDecoder = { viewModel.setSoftwareDecoder(it) },
                                                        onSelectAspectMode = { viewModel.setDefaultAspectMode(it) },
                                                        onUpdateParental = { enabled, pin ->
                                                            viewModel.setParentalControl(enabled, pin)
                                                        },
                                                        onClearHistory = { viewModel.clearHistory() },
                                                        onSyncActiveProfile = { viewModel.syncActiveProfile() },
                                                        onSwitchProfile = { viewModel.switchProfile(it) },
                                                        onDeleteProfile = { viewModel.deleteProfile(it) },
                                                        onOpenAddProfile = { viewModel.openAddProfileModal(true) }
                                                    )
                                                }
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                    }

                    if (showExitDialog) {
                        val cancelFocus = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            delay(60L)
                            try { cancelFocus.requestFocus() } catch (_: Exception) {}
                        }
                        Dialog(onDismissRequest = { showExitDialog = false }) {
                            Column(
                                modifier = Modifier
                                    .width(340.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xF5050507))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = strings.exitAppPrompt,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassPillButton(
                                        text = strings.cancel,
                                        darkGlass = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(cancelFocus),
                                        onClick = { showExitDialog = false }
                                    )
                                    GlassPillButton(
                                        text = strings.exitApp,
                                        primary = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = { activity?.finish() }
                                    )
                                }
                            }
                        }
                    }

                    if (state.pendingParentalItem != null) {
                        PinEntryDialog(
                            title = strings.enterPinTitle,
                            strings = strings,
                            errorText = if (state.pinError) strings.wrongPin else null,
                            onConfirm = { viewModel.verifyParentalPin(it) },
                            onDismiss = { viewModel.dismissParentalPrompt() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanFullScreenSync(stage: String, progress: Float, title: String) {
    val c = LocalTiendaColors.current
    val stageLabel = when (stage) {
        "LIVE" -> "TV en vivo"
        "MOVIE" -> "Películas"
        "SERIES" -> "Series"
        else -> stage
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(c.accent, c.accentLight))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LiveTv,
                    contentDescription = "TiendaTV",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = "TiendaTV",
                color = c.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (stageLabel.isNotBlank()) "$title • $stageLabel" else title,
                color = c.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = c.accent,
                trackColor = c.glassBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}
