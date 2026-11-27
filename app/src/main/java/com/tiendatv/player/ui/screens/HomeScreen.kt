package com.tiendatv.player.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.data.repository.CatalogCounts
import com.tiendatv.player.ui.components.ContinueWatchingCard
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.NavTab
import com.tiendatv.player.ui.components.PosterMediaCard
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors
import java.text.NumberFormat
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
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onReloadCatalog: () -> Unit,
    onExitApp: () -> Unit
) {
    val c = LocalTiendaColors.current
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

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

    // v1.9: no auto-rotate. The 7.5s timer + 850ms full-screen crossfade
    // was the biggest Home hiccup — the hero now stays on the first item.

    val currentShowcase = showcaseItems.getOrNull(showcaseIndex) ?: heroItem

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentShowcase != null) {
            Crossfade(
                targetState = currentShowcase,
                animationSpec = tween(durationMillis = 300),
                label = "homeBackdropCrossfade",
                modifier = Modifier.fillMaxSize()
            ) { item ->
                val bgUrl = item.backdropUrl.ifBlank { item.logoUrl }
                if (bgUrl.isNotBlank()) {
                    AsyncImage(
                        model = bgUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xEB0B0D11),
                            Color(0x9E0B0D11),
                            Color(0x450B0D11)
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
                            Color(0x330B0D11),
                            Color.Transparent,
                            Color(0xD90B0D11)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(236.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xBF12151E))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(c.accent, c.accentLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Row {
                        Text(
                            text = "Tienda",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "TV",
                            color = c.accent,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                HomeMenuRow(
                    icon = Icons.Rounded.LiveTv,
                    label = strings.liveChannels,
                    badge = fmt.format(counts.liveCount),
                    onClick = { onNavigateTab(NavTab.LIVE) }
                )
                HomeMenuRow(
                    icon = Icons.Rounded.Movie,
                    label = strings.movies,
                    badge = fmt.format(counts.movieCount),
                    onClick = { onNavigateTab(NavTab.MOVIES) }
                )
                HomeMenuRow(
                    icon = Icons.Rounded.Tv,
                    label = strings.series,
                    badge = fmt.format(counts.seriesCount),
                    onClick = { onNavigateTab(NavTab.SERIES) }
                )
                HomeMenuRow(
                    icon = Icons.Rounded.Search,
                    label = strings.navSearch,
                    onClick = { onNavigateTab(NavTab.SEARCH) }
                )
                HomeMenuRow(
                    icon = Icons.Rounded.Settings,
                    label = strings.navSettings,
                    onClick = { onNavigateTab(NavTab.SETTINGS) }
                )
                HomeMenuRow(
                    icon = Icons.Rounded.Refresh,
                    label = strings.reloadPortal,
                    onClick = onReloadCatalog
                )

                Spacer(modifier = Modifier.weight(1f))

                HomeMenuRow(
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    label = strings.exitApp,
                    onClick = onExitApp
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                if (currentShowcase != null) {
                    item {
                        val isFav = favoriteUids.contains(currentShowcase.uid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (strings.navHome == "Inicio") "DESTACADO" else "FEATURED",
                                    color = c.badgeGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.4.sp
                                )
                                Text(
                                    text = currentShowcase.name,
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (currentShowcase.rating > 0.0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFF5B041),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1f", currentShowcase.rating),
                                                color = Color(0xFFF5B041),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (currentShowcase.year.isNotBlank()) {
                                        Text(
                                            text = currentShowcase.year,
                                            color = Color(0xCCFFFFFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (currentShowcase.plot.isNotBlank()) {
                                    Text(
                                        text = currentShowcase.plot,
                                        color = Color(0xD9FFFFFF),
                                        fontSize = 14.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    GlassPillButton(
                                        text = strings.playNow,
                                        icon = Icons.Rounded.PlayArrow,
                                        primary = true,
                                        onClick = { onOpenMedia(currentShowcase) }
                                    )
                                    GlassPillButton(
                                        text = if (isFav) strings.favorited else strings.favorite,
                                        icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        darkGlass = true,
                                        onClick = { onToggleFavorite(currentShowcase) }
                                    )
                                }
                            }

                            if (currentShowcase.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentShowcase.logoUrl,
                                    contentDescription = currentShowcase.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(134.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.5.dp, Color(0x4DFFFFFF), RoundedCornerShape(16.dp))
                                )
                            }
                        }
                    }
                }

                if (continueWatching.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.continueWatching.uppercase(),
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(continueWatching, key = { it.uid }) { entry ->
                                    ContinueWatchingCard(
                                        entry = entry,
                                        onClick = { onResumeHistory(entry) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (featuredMovies.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.featuredMovies.uppercase(),
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(featuredMovies, key = { "hm_${it.uid}" }) { item ->
                                    PosterMediaCard(
                                        item = item,
                                        isFavorite = favoriteUids.contains(item.uid),
                                        onClick = { onOpenMedia(item) },
                                        onToggleFavorite = { onToggleFavorite(item) },
                                        cardWidth = 142.dp
                                    )
                                }
                            }
                        }
                    }
                }

                if (featuredSeries.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.featuredSeries.uppercase(),
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(featuredSeries, key = { "hs_${it.uid}" }) { item ->
                                    PosterMediaCard(
                                        item = item,
                                        isFavorite = favoriteUids.contains(item.uid),
                                        onClick = { onOpenMedia(item) },
                                        onToggleFavorite = { onToggleFavorite(item) },
                                        cardWidth = 142.dp
                                    )
                                }
                            }
                        }
                    }
                }

                if (favorites.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.favorites.uppercase(),
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(favorites, key = { "hf_${it.uid}" }) { item ->
                                    PosterMediaCard(
                                        item = item,
                                        isFavorite = true,
                                        onClick = { onOpenMedia(item) },
                                        onToggleFavorite = { onToggleFavorite(item) },
                                        cardWidth = 142.dp
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

@Composable
private fun HomeMenuRow(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = shape, focusScale = 1.03f, onClick = onClick)
            .clip(shape)
            .background(Color(0x24FFFFFF))
            .border(1.dp, Color(0x1EFFFFFF), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = c.accent,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!badge.isNullOrBlank()) {
            Text(
                text = badge,
                color = Color(0xAAFFFFFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
