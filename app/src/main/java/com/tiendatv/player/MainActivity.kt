package com.tiendatv.player

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
            AnimatedContent(
                targetState = state.activePlayback,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260))) togetherWith
                        (fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)))
                },
                label = "playerRootTransition"
            ) { activePlayback ->
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

                    PlayerScreen(
                        request = activePlayback,
                        settings = state.settings,
                        strings = strings,
                        epgNowTitle = nowEpg,
                        hasPrevChannel = activePlayback.isLive && liveIdx > 0,
                        hasNextChannel = activePlayback.isLive && liveIdx >= 0 && liveIdx < liveList.lastIndex,
                        onPrevChannel = { viewModel.switchLiveChannelByOffset(-1) },
                        onNextChannel = { viewModel.switchLiveChannelByOffset(1) },
                        onSaveProgress = { pos, dur -> viewModel.savePlaybackProgress(pos, dur) },
                        onClose = { viewModel.closePlayer() }
                    )
                } else {
                    FrostedCanvasBackground(modifier = Modifier.safeDrawingPadding()) {
                        val showLogin = state.activeProfile == null || state.showAddProfileModal
                        if (showLogin) {
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
                            val activity = LocalContext.current as? Activity
                            if (state.syncProgress.isSyncing) {
                                CleanFullScreenSync(
                                    stage = state.syncProgress.stage,
                                    progress = state.syncProgress.progressFraction,
                                    title = strings.syncingCatalog
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    if (state.selectedTab != NavTab.HOME || state.activeSeries != null) {
                                        TiendaNavigationRail(
                                            selectedTab = state.selectedTab,
                                            strings = strings,
                                            onSelectTab = { viewModel.selectTab(it) }
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                    ) {
                                        AnimatedContent(
                                            targetState = state.activeSeries,
                                            transitionSpec = {
                                                if (targetState != null) {
                                                    (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 16 }) togetherWith
                                                        fadeOut(tween(160))
                                                } else {
                                                    fadeIn(tween(200)) togetherWith
                                                        (fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 16 })
                                                }
                                            },
                                            label = "seriesDetailTransition"
                                        ) { openSeries ->
                                            if (openSeries != null) {
                                                SeriesDetailScreen(
                                                    series = openSeries,
                                                    details = state.activeSeriesDetails,
                                                    isLoading = state.loadingSeriesDetails,
                                                    strings = strings,
                                                    onPlayEpisode = { ep ->
                                                        viewModel.playSeriesEpisode(openSeries, ep)
                                                    },
                                                    onBack = { viewModel.closeSeriesDetail() }
                                                )
                                            } else {
                                                AnimatedContent(
                                                    targetState = state.selectedTab,
                                                    transitionSpec = {
                                                        val forward = targetState.ordinal >= initialState.ordinal
                                                        val enter = fadeIn(tween(220)) + slideInHorizontally(tween(220)) { w ->
                                                            if (forward) w / 20 else -w / 20
                                                        }
                                                        val exit = fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { w ->
                                                            if (forward) -w / 24 else w / 24
                                                        }
                                                        enter togetherWith exit
                                                    },
                                                    label = "tabTransition"
                                                ) { tab ->
                                                    when (tab) {
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
