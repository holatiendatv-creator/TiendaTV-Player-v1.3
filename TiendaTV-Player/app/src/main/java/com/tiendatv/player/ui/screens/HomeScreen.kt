package com.tiendatv.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.tiendatv.player.data.local.MediaType
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.data.repository.CatalogCounts
import com.tiendatv.player.ui.components.ContinueWatchingCard
import com.tiendatv.player.ui.components.GlassCard
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.LandscapeHighlightCard
import com.tiendatv.player.ui.components.NavTab
import com.tiendatv.player.ui.components.PosterMediaCard
import com.tiendatv.player.ui.components.SectionHeader
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
    onToggleFavorite: (MediaItemEntity) -> Unit
) {
    val c = LocalTiendaColors.current
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val recentLive = remember(recentlyWatched) {
        recentlyWatched.firstOrNull { it.type == MediaType.LIVE }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp, start = 18.dp, end = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Greeting line matching IMG_5973
        item {
            Text(
                text = if (strings.navHome == "Inicio") "¿Qué vemos hoy?" else "What are we watching today?",
                color = c.textSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Split top row: Featured Hero Banner (left) + 3 Stacked Category Count Pills (right)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left: Featured Hero Card
                Box(modifier = Modifier.weight(1.55f)) {
                    if (heroItem != null) {
                        val isFav = favoriteUids.contains(heroItem.uid)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(274.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF0E1117))
                                .border(1.dp, c.glassBorder, RoundedCornerShape(24.dp))
                        ) {
                            val imgUrl = heroItem.backdropUrl.ifBlank { heroItem.logoUrl }
                            if (imgUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = heroItem.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xEC0B0D11),
                                                Color(0xA60B0D11),
                                                Color(0x330B0D11)
                                            )
                                        )
                                    )
                            )

                            // Optional floating EN VIVO quick-resume pill at top (matches IMG_5973)
                            if (recentLive != null) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .tvFocusable(
                                            shape = RoundedCornerShape(24.dp),
                                            onClick = { onResumeHistory(recentLive) }
                                        )
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xCC12161F))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(c.accent)
                                    )
                                    Column {
                                        Text(
                                            text = strings.liveBadge,
                                            color = c.badgeGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = recentLive.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(24.dp)
                                    .fillMaxWidth(0.76f),
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
                                    text = heroItem.name,
                                    color = Color.White,
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Metadata chips row (★ rating + year)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (heroItem.rating > 0.0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFF5B041),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1f", heroItem.rating),
                                                color = Color(0xFFF5B041),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (heroItem.year.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x40FFFFFF))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = heroItem.year,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                if (heroItem.plot.isNotBlank()) {
                                    Text(
                                        text = heroItem.plot,
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    GlassPillButton(
                                        text = strings.playNow,
                                        icon = Icons.Rounded.PlayArrow,
                                        primary = true,
                                        onClick = { onOpenMedia(heroItem) }
                                    )
                                    GlassPillButton(
                                        text = if (isFav) strings.favorited else strings.favorite,
                                        icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        darkGlass = true,
                                        onClick = { onToggleFavorite(heroItem) }
                                    )
                                }
                            }
                        }
                    } else {
                        GlassCard(modifier = Modifier.fillMaxWidth().height(274.dp)) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = strings.welcomeTitle,
                                    color = c.textPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.emptyCatalogHint,
                                    color = c.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Right: 3 Stacked Category Count Pills (matches IMG_5973)
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StackedCategoryPill(
                        title = if (strings.navHome == "Inicio") "TV en vivo" else strings.liveChannels,
                        count = fmt.format(counts.liveCount),
                        icon = Icons.Rounded.LiveTv,
                        onClick = { onNavigateTab(NavTab.LIVE) }
                    )
                    StackedCategoryPill(
                        title = strings.movies,
                        count = fmt.format(counts.movieCount),
                        icon = Icons.Rounded.Movie,
                        onClick = { onNavigateTab(NavTab.MOVIES) }
                    )
                    StackedCategoryPill(
                        title = strings.series,
                        count = fmt.format(counts.seriesCount),
                        icon = Icons.Rounded.Tv,
                        onClick = { onNavigateTab(NavTab.SERIES) }
                    )
                }
            }
        }

        // NOVEDADES widescreen landscape row (matches IMG_5973)
        if (featuredMovies.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = if (strings.navHome == "Inicio") "NOVEDADES" else "NEW RELEASES",
                        actionText = if (strings.navHome == "Inicio") "Ver todo" else "See all",
                        onActionClick = { onNavigateTab(NavTab.MOVIES) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(featuredMovies, key = { "nov_${it.uid}" }) { item ->
                            LandscapeHighlightCard(
                                item = item,
                                onClick = { onOpenMedia(item) }
                            )
                        }
                    }
                }
            }
        }

        // Continue Watching
        if (continueWatching.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = strings.continueWatching, modifier = Modifier.fillMaxWidth())
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

        // Recently Watched
        if (recentlyWatched.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = strings.recentlyWatched, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(recentlyWatched, key = { "rec_${it.uid}" }) { entry ->
                            ContinueWatchingCard(
                                entry = entry,
                                onClick = { onResumeHistory(entry) }
                            )
                        }
                    }
                }
            }
        }

        // Favorites
        if (favorites.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = strings.favorites,
                        countText = fmt.format(favorites.size),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(favorites, key = { "fav_${it.uid}" }) { item ->
                            PosterMediaCard(
                                item = item,
                                isFavorite = true,
                                onClick = { onOpenMedia(item) },
                                onToggleFavorite = { onToggleFavorite(item) }
                            )
                        }
                    }
                }
            }
        }

        // Featured Series
        if (featuredSeries.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = strings.featuredSeries,
                        countText = fmt.format(counts.seriesCount),
                        actionText = if (strings.navHome == "Inicio") "Ver todo" else "See all",
                        onActionClick = { onNavigateTab(NavTab.SERIES) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(featuredSeries, key = { "ser_${it.uid}" }) { item ->
                            PosterMediaCard(
                                item = item,
                                isFavorite = favoriteUids.contains(item.uid),
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

@Composable
private fun StackedCategoryPill(
    title: String,
    count: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(c.glassSurfaceElevated)
            .border(1.dp, c.glassBorder, shape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            color = c.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count,
            color = c.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
