package com.tiendatv.player.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

private data class HomeHeroDisplay(
    val uid: String,
    val title: String,
    val subtitle: String,
    val year: String,
    val rating: Double,
    val backdropUrl: String,
    val mediaItem: MediaItemEntity? = null,
    val historyEntry: WatchHistoryEntity? = null
)

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
    var pendingFocusHero by remember { mutableStateOf<HomeHeroDisplay?>(null) }
    var settledFocusHero by remember { mutableStateOf<HomeHeroDisplay?>(null) }

    // 280ms settle debounce: keeps backdrop calm while gliding quickly across cards
    LaunchedEffect(pendingFocusHero) {
        if (pendingFocusHero != null) {
            delay(280L)
            settledFocusHero = pendingFocusHero
        }
    }

    // Gentle auto-rotation only when user hasn't focused a specific card yet
    LaunchedEffect(showcaseItems, settledFocusHero) {
        if (showcaseItems.size > 1 && settledFocusHero == null) {
            while (true) {
                delay(9_000L)
                showcaseIndex = (showcaseIndex + 1) % showcaseItems.size
            }
        }
    }

    val defaultShowcase = showcaseItems.getOrNull(showcaseIndex) ?: heroItem
    val activeHero = settledFocusHero ?: defaultShowcase?.let {
        HomeHeroDisplay(
            uid = it.uid,
            title = it.name,
            subtitle = it.plot,
            year = it.year,
            rating = it.rating,
            backdropUrl = it.backdropUrl.ifBlank { it.logoUrl },
            mediaItem = it
        )
    }

    // Subtle Apple TV-style ambient breathing drift on the full-bleed backdrop
    val driftTransition = rememberInfiniteTransition(label = "heroDrift")
    val ambientScale by driftTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientBackdropScale"
    )

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        // Silky 900ms crossfading full-bleed backdrop
        val backdropUrl = activeHero?.backdropUrl.orEmpty()
        if (backdropUrl.isNotBlank()) {
            Crossfade(
                targetState = backdropUrl,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                label = "homeZenBackdrop",
                modifier = Modifier.fillMaxSize()
            ) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = ambientScale
                            scaleY = ambientScale
                        }
                )
            }
        }

        // Feather-soft cinema vignette blending seamlessly into pure #000000 pitch black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xFF000000),
                        0.38f to Color(0xB3000000),
                        0.68f to Color(0x40000000),
                        1.0f to Color(0x18000000)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x33000000),
                        0.36f to Color(0x1A000000),
                        0.68f to Color(0xE6000000),
                        1.0f to Color(0xFF000000)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            // 1. Synced, Calm Cinematic Hero Section
            item(key = "hero_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeHero != null) {
                        Crossfade(
                            targetState = activeHero,
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                            label = "heroMetaFade"
                        ) { hero ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (hero.year.isNotBlank()) {
                                        Text(
                                            text = hero.year,
                                            color = Color(0xCCFFFFFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (hero.rating > 0.0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(c.accent)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1f", hero.rating),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = hero.title,
                                    color = Color.White,
                                    fontSize = 34.sp,
                                    lineHeight = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(0.66f)
                                )

                                if (hero.subtitle.isNotBlank()) {
                                    Text(
                                        text = hero.subtitle,
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(0.58f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val mediaRef = activeHero.mediaItem
                        val histRef = activeHero.historyEntry
                        val isFav = mediaRef != null && favoriteUids.contains(mediaRef.uid)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassPillButton(
                                text = strings.playNow,
                                icon = Icons.Rounded.PlayArrow,
                                primary = true,
                                onClick = {
                                    when {
                                        mediaRef != null -> onOpenMedia(mediaRef)
                                        histRef != null -> onResumeHistory(histRef)
                                    }
                                }
                            )
                            if (lastLiveEntry != null) {
                                GlassPillButton(
                                    text = "${strings.resumeLive}: ${lastLiveEntry.title}",
                                    icon = Icons.Rounded.LiveTv,
                                    darkGlass = true,
                                    onClick = { onResumeHistory(lastLiveEntry) }
                                )
                            }
                            if (mediaRef != null) {
                                GlassPillButton(
                                    text = if (isFav) strings.favorited else strings.favorite,
                                    icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    darkGlass = true,
                                    onClick = { onToggleFavorite(mediaRef) }
                                )
                            }
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

            // 2. Continue Watching (Long-Press OK / Menu to remove discreetly)
            if (continueWatching.isNotEmpty()) {
                item(key = "continue_watching_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = strings.continueWatching)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(continueWatching, key = { it.uid }) { entry ->
                                ContinueWatchingCard(
                                    entry = entry,
                                    onFocused = {
                                        pendingFocusHero = HomeHeroDisplay(
                                            uid = entry.uid,
                                            title = entry.title,
                                            subtitle = entry.subtitle,
                                            year = "",
                                            rating = 0.0,
                                            backdropUrl = entry.posterUrl,
                                            historyEntry = entry
                                        )
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

            // 3. Recently Watched (Long-Press OK / Menu to remove discreetly)
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
                                        pendingFocusHero = HomeHeroDisplay(
                                            uid = entry.uid,
                                            title = entry.title,
                                            subtitle = entry.subtitle,
                                            year = "",
                                            rating = 0.0,
                                            backdropUrl = entry.posterUrl,
                                            historyEntry = entry
                                        )
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
                                        pendingFocusHero = HomeHeroDisplay(
                                            uid = item.uid,
                                            title = item.name,
                                            subtitle = item.plot,
                                            year = item.year,
                                            rating = item.rating,
                                            backdropUrl = item.backdropUrl.ifBlank { item.logoUrl },
                                            mediaItem = item
                                        )
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
                                        pendingFocusHero = HomeHeroDisplay(
                                            uid = item.uid,
                                            title = item.name,
                                            subtitle = item.plot,
                                            year = item.year,
                                            rating = item.rating,
                                            backdropUrl = item.backdropUrl.ifBlank { item.logoUrl },
                                            mediaItem = item
                                        )
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
                                        pendingFocusHero = HomeHeroDisplay(
                                            uid = item.uid,
                                            title = item.name,
                                            subtitle = item.plot,
                                            year = item.year,
                                            rating = item.rating,
                                            backdropUrl = item.backdropUrl.ifBlank { item.logoUrl },
                                            mediaItem = item
                                        )
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
