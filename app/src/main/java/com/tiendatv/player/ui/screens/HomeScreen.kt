package com.tiendatv.player.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.MediaType
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.data.repository.CatalogCounts
import com.tiendatv.player.ui.components.ContinueWatchingCard
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.NavTab
import com.tiendatv.player.ui.components.PosterMediaCard
import com.tiendatv.player.ui.components.SectionHeader
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HomeScreen(
    counts: CatalogCounts,
    heroItem: MediaItemEntity?,
    continueWatching: List<WatchHistoryEntity>,
    recentlyWatched: List<WatchHistoryEntity>,
    favorites: List<MediaItemEntity>,
    featuredMovies: List<MediaItemEntity>,
    featuredSeries: List<MediaItemEntity>,
    favoriteUids: Set<String>,
    strings: AppStrings,
    onNavigateTab: (NavTab) -> Unit,
    onOpenMedia: (MediaItemEntity) -> Unit,
    onResumeHistory: (WatchHistoryEntity) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onReloadCatalog: () -> Unit,
    onExitApp: () -> Unit
) {
    val c = LocalTiendaColors.current
    val lastLiveEntry = remember(recentlyWatched) {
        recentlyWatched.firstOrNull { it.type == MediaType.LIVE }
    }

    val showcaseItems = remember(featuredMovies, featuredSeries, heroItem) {
        val mixed = buildList {
            val max = maxOf(featuredMovies.size, featuredSeries.size)
            for (i in 0 until minOf(max, 8)) {
                featuredMovies.getOrNull(i)?.let { add(it) }
                featuredSeries.getOrNull(i)?.let { add(it) }
            }
        }.distinctBy { it.uid }
        if (mixed.isNotEmpty()) mixed else listOfNotNull(heroItem)
    }

    var showcaseIndex by remember(showcaseItems) { mutableIntStateOf(0) }
    var focusedBackdropUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showcaseItems, focusedBackdropUrl) {
        if (showcaseItems.size > 1 && focusedBackdropUrl == null) {
            while (true) {
                delay(8_500L)
                showcaseIndex = (showcaseIndex + 1) % showcaseItems.size
            }
        }
    }

    val currentShowcase = showcaseItems.getOrNull(showcaseIndex) ?: heroItem
    val activeBackdropUrl = focusedBackdropUrl
        ?: currentShowcase?.backdropUrl?.ifBlank { currentShowcase.logoUrl }
        ?: ""

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen cinematic backdrop that crossfades smoothly with focus or rotation
        if (activeBackdropUrl.isNotBlank()) {
            Crossfade(
                targetState = activeBackdropUrl,
                animationSpec = tween(durationMillis = 700),
                label = "homeZenBackdrop",
                modifier = Modifier.fillMaxSize()
            ) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Deep obsidian cinema vignette for calm, high-contrast legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xE80B0D11),
                            Color(0x990B0D11),
                            Color(0x380B0D11)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x280B0D11),
                            Color(0x400B0D11),
                            Color(0xEB0B0D11),
                            Color(0xFF0B0D11)
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            // 1. Open, Cinematic Zen Hero Section
            item(key = "hero_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentShowcase != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentShowcase.year.isNotBlank()) {
                                Text(
                                    text = currentShowcase.year,
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (currentShowcase.rating > 0.0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x660B0D11))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF5B041),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f", currentShowcase.rating),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = currentShowcase.name,
                            color = Color.White,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.68f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val isFav = favoriteUids.contains(currentShowcase.uid)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassPillButton(
                                text = strings.playNow,
                                icon = Icons.Rounded.PlayArrow,
                                primary = true,
                                onClick = { onOpenMedia(currentShowcase) }
                            )
                            if (lastLiveEntry != null) {
                                GlassPillButton(
                                    text = "${strings.resumeLive}: ${lastLiveEntry.title}",
                                    icon = Icons.Rounded.LiveTv,
                                    darkGlass = true,
                                    onClick = { onResumeHistory(lastLiveEntry) }
                                )
                            }
                            GlassPillButton(
                                text = if (isFav) strings.favorited else strings.favorite,
                                icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                darkGlass = true,
                                onClick = { onToggleFavorite(currentShowcase) }
                            )
                        }
                    } else {
                        Text(
                            text = strings.emptyCatalogHint,
                            color = Color(0xCCFFFFFF),
                            fontSize = 16.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassPillButton(
                                text = strings.reloadPortal,
                                primary = true,
                                onClick = onReloadCatalog
                            )
                        }
                    }
                }
            }

            // 2. Continue Watching (with individual Remove button)
            if (continueWatching.isNotEmpty()) {
                item(key = "continue_watching_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = strings.continueWatching)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(continueWatching, key = { it.uid }) { entry ->
                                ContinueWatchingCard(
                                    entry = entry,
                                    onFocused = {
                                        if (entry.posterUrl.isNotBlank()) {
                                            focusedBackdropUrl = entry.posterUrl
                                        }
                                    },
                                    onClick = { onResumeHistory(entry) },
                                    onRemove = { onRemoveHistory(entry.uid) },
                                    removeLabel = strings.removeItem
                                )
                            }
                        }
                    }
                }
            }

            // 3. Recently Watched (with individual Remove button)
            if (recentlyWatched.isNotEmpty()) {
                item(key = "recently_watched_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(
                            title = strings.recentlyWatched,
                            actionText = strings.clearWatchHistory,
                            onActionClick = onClearHistory,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(recentlyWatched, key = { "recent_${it.uid}" }) { entry ->
                                ContinueWatchingCard(
                                    entry = entry,
                                    onFocused = {
                                        if (entry.posterUrl.isNotBlank()) {
                                            focusedBackdropUrl = entry.posterUrl
                                        }
                                    },
                                    onClick = { onResumeHistory(entry) },
                                    onRemove = { onRemoveHistory(entry.uid) },
                                    removeLabel = strings.removeItem
                                )
                            }
                        }
                    }
                }
            }

            // 4. Favorites
            if (favorites.isNotEmpty()) {
                item(key = "favorites_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = strings.favorites)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(favorites, key = { "fav_${it.uid}" }) { item ->
                                PosterMediaCard(
                                    item = item,
                                    isFavorite = true,
                                    cardWidth = 148.dp,
                                    onFocused = {
                                        val bg = item.backdropUrl.ifBlank { item.logoUrl }
                                        if (bg.isNotBlank()) focusedBackdropUrl = bg
                                    },
                                    onClick = { onOpenMedia(item) },
                                    onToggleFavorite = { onToggleFavorite(item) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Featured Movies
            if (featuredMovies.isNotEmpty()) {
                item(key = "featured_movies_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(
                            title = strings.featuredMovies,
                            actionText = strings.movies,
                            onActionClick = { onNavigateTab(NavTab.MOVIES) }
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(featuredMovies, key = { "fmov_${it.uid}" }) { item ->
                                PosterMediaCard(
                                    item = item,
                                    isFavorite = favoriteUids.contains(item.uid),
                                    cardWidth = 148.dp,
                                    onFocused = {
                                        val bg = item.backdropUrl.ifBlank { item.logoUrl }
                                        if (bg.isNotBlank()) focusedBackdropUrl = bg
                                    },
                                    onClick = { onOpenMedia(item) },
                                    onToggleFavorite = { onToggleFavorite(item) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Featured Series
            if (featuredSeries.isNotEmpty()) {
                item(key = "featured_series_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(
                            title = strings.featuredSeries,
                            actionText = strings.series,
                            onActionClick = { onNavigateTab(NavTab.SERIES) }
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(featuredSeries, key = { "fser_${it.uid}" }) { item ->
                                PosterMediaCard(
                                    item = item,
                                    isFavorite = favoriteUids.contains(item.uid),
                                    cardWidth = 148.dp,
                                    onFocused = {
                                        val bg = item.backdropUrl.ifBlank { item.logoUrl }
                                        if (bg.isNotBlank()) focusedBackdropUrl = bg
                                    },
                                    onClick = { onOpenMedia(item) },
                                    onToggleFavorite = { onToggleFavorite(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
