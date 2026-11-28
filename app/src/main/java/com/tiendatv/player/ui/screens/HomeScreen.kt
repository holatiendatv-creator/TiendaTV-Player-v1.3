package com.tiendatv.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tiendatv.player.R
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.data.repository.CatalogCounts
import com.tiendatv.player.ui.components.NavTab
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors
import kotlinx.coroutines.delay

/**
 * Clean, IBO-inspired pure cinema Home screen:
 * - 100% edge-to-edge full-bleed rotating Movies & Series showcase
 * - Cycles through 5 lightweight GPU-accelerated cinema transitions (Blinds, Wipe, Depth Dissolve, Split Shutter, Fade-Through-Black)
 * - Choreographed 3-beat reveal: Backdrop -> Mini Poster -> Title & Description
 * - Fixed, zero-trap left vertical menu with circular TIENDA TV emblem
 */
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
    onExitApp: () -> Unit,
    primaryFocusRequester: FocusRequester? = null
) {
    val c = LocalTiendaColors.current

    // Build curated showcase list prioritizing items with rich artwork and synopsis
    val showcaseItems = remember(featuredMovies, featuredSeries, heroItem) {
        val mixed = buildList {
            val max = maxOf(featuredMovies.size, featuredSeries.size)
            for (i in 0 until minOf(max, 12)) {
                featuredMovies.getOrNull(i)?.let { add(it) }
                featuredSeries.getOrNull(i)?.let { add(it) }
            }
        }.distinctBy { it.uid }
        val rich = mixed.filter { it.backdropUrl.isNotBlank() || it.logoUrl.isNotBlank() }
        val finalList = if (rich.isNotEmpty()) rich else mixed
        val base = if (finalList.isNotEmpty()) finalList else listOfNotNull(heroItem)
        base.shuffled()
    }

    var currentIdx by remember(showcaseItems) { mutableIntStateOf(0) }
    var previousIdx by remember(showcaseItems) { mutableIntStateOf(0) }
    val transitionAnim = remember { Animatable(1f) }

    // Choreographed 3-beat reveal state: Backdrop -> Mini Poster -> Title & Description
    var posterVisible by remember { mutableStateOf(true) }
    var descVisible by remember { mutableStateOf(true) }

    val currentItem = showcaseItems.getOrNull(currentIdx) ?: heroItem
    val previousItem = showcaseItems.getOrNull(previousIdx)
    val transitionStyle = currentIdx % 5

    // Auto-rotate showcase every 7 seconds with 2-second choreographed poster + description reveal
    LaunchedEffect(showcaseItems) {
        if (showcaseItems.isNotEmpty()) {
            // Initial slide: show backdrop first, then poster & description at ~2s
            posterVisible = false
            descVisible = false
            delay(2_000L)
            posterVisible = true
            delay(350L)
            descVisible = true
        }
        if (showcaseItems.size > 1) {
            while (true) {
                delay(4_650L) // Total 7s per slide (2s reveal + 4.65s dwell + ~0.35s fade-out)
                posterVisible = false
                descVisible = false
                delay(180L)
                previousIdx = currentIdx
                currentIdx = (currentIdx + 1) % showcaseItems.size
                transitionAnim.snapTo(0f)
                transitionAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1150, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
                )
                // Wait to reach ~2s from transition start, then reveal poster followed by description card
                delay(850L)
                posterVisible = true
                delay(350L)
                descVisible = true
            }
        }
    }

    // Request initial focus on the top Live TV menu item so D-pad is immediately active
    val defaultMenuFocus = remember { FocusRequester() }
    val menuFocus = primaryFocusRequester ?: defaultMenuFocus
    LaunchedEffect(Unit) {
        delay(80L)
        try {
            menuFocus.requestFocus()
        } catch (_: Exception) {
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
    ) {
        // LAYER 1: Full-Bleed Rotating Showcase Backdrop with 5 Cycling Transitions
        if (currentItem != null) {
            val prevUrl = previousItem?.backdropUrl?.ifBlank { previousItem.logoUrl }.orEmpty()
            val currUrl = currentItem.backdropUrl.ifBlank { currentItem.logoUrl }
            val p = transitionAnim.value

            // Outgoing backdrop underneath during transition
            if (p < 1f && prevUrl.isNotBlank() && prevUrl != currUrl) {
                val outAlpha = when (transitionStyle) {
                    4 -> (1f - (p * 2f).coerceIn(0f, 1f)) // Fade-Through-Black
                    2 -> (1f - p).coerceIn(0f, 1f)         // Depth Dissolve
                    else -> 1f
                }
                AsyncImage(
                    model = prevUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = outAlpha }
                )
            }

            // Incoming backdrop with cycling cinema transition
            if (currUrl.isNotBlank()) {
                val incomingModifier = when (transitionStyle) {
                    // 0: Vertical Cinema Blinds (IBO signature staggered vertical strips)
                    0 -> {
                        val blindsShape = GenericShape { size, _ ->
                            val strips = 7
                            val stripW = size.width / strips
                            for (i in 0 until strips) {
                                val stagger = i * 0.06f
                                val localP = ((p - stagger) / (1f - stagger)).coerceIn(0f, 1f)
                                val w = stripW * localP
                                if (w > 0f) {
                                    addRect(Rect(i * stripW, 0f, i * stripW + w, size.height))
                                }
                            }
                        }
                        Modifier
                            .fillMaxSize()
                            .clip(blindsShape)
                    }
                    // 1: Horizontal Curtain Wipe (smooth left-to-right reveal)
                    1 -> {
                        val wipeShape = GenericShape { size, _ ->
                            addRect(Rect(0f, 0f, size.width * p, size.height))
                        }
                        Modifier
                            .fillMaxSize()
                            .clip(wipeShape)
                    }
                    // 2: Depth Dissolve (gentle zoom settle 1.06x -> 1.00x + fade)
                    2 -> {
                        val s = 1.06f - (0.06f * p)
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = p
                                scaleX = s
                                scaleY = s
                            }
                    }
                    // 3: Center Split Shutter (parts smoothly from center outward)
                    3 -> {
                        val splitShape = GenericShape { size, _ ->
                            val halfW = (size.width * 0.5f) * p
                            val cx = size.width * 0.5f
                            addRect(Rect(cx - halfW, 0f, cx + halfW, size.height))
                        }
                        Modifier
                            .fillMaxSize()
                            .clip(splitShape)
                    }
                    // 4: Classic Cinema Fade-Through-Black
                    else -> {
                        val inAlpha = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = inAlpha }
                    }
                }

                AsyncImage(
                    model = currUrl,
                    contentDescription = currentItem.name,
                    contentScale = ContentScale.Crop,
                    modifier = incomingModifier
                )
            }
        }

        // Subtle left-side scrim behind the vertical menu + soft bottom shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xD9000000),
                        0.26f to Color(0x88000000),
                        0.45f to Color(0x00000000)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x18000000),
                        0.62f to Color(0x00000000),
                        1.0f to Color(0xCC000000)
                    )
                )
        )

        // LAYER 2: Clean Fixed Left Vertical Menu + Bottom Showcase Strip
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Fixed Vertical Menu (IBO-style, zero collapse, zero focus traps)
            Column(
                modifier = Modifier
                    .width(204.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular TIENDA TV brand logo at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_fg),
                        contentDescription = "TiendaTV",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Row {
                        Text(
                            text = "Tienda",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "TV",
                            color = c.accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                HomeMenuTile(
                    icon = Icons.Rounded.LiveTv,
                    label = strings.liveChannels,
                    modifier = Modifier.focusRequester(menuFocus),
                    onClick = { onNavigateTab(NavTab.LIVE) }
                )
                HomeMenuTile(
                    icon = Icons.Rounded.Movie,
                    label = strings.movies,
                    onClick = { onNavigateTab(NavTab.MOVIES) }
                )
                HomeMenuTile(
                    icon = Icons.Rounded.Tv,
                    label = strings.series,
                    onClick = { onNavigateTab(NavTab.SERIES) }
                )
                HomeMenuTile(
                    icon = Icons.Rounded.Search,
                    label = strings.navSearch,
                    onClick = { onNavigateTab(NavTab.SEARCH) }
                )
                HomeMenuTile(
                    icon = Icons.Rounded.Settings,
                    label = strings.navSettings,
                    onClick = { onNavigateTab(NavTab.SETTINGS) }
                )
                HomeMenuTile(
                    icon = Icons.Rounded.Refresh,
                    label = strings.reloadPortal,
                    onClick = onReloadCatalog
                )
                HomeMenuTile(
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    label = strings.exitApp,
                    onClick = onExitApp
                )
            }

            // Right Showcase Area: Open top 65%, Choreographed Lower-Third Bar + Mini Poster
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (currentItem != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Beat 3: Clean translucent dark bar with compact title + description
                        Box(modifier = Modifier.weight(1f)) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = descVisible,
                                enter = fadeIn(tween(560, easing = FastOutSlowInEasing)) + slideInVertically(tween(560, easing = FastOutSlowInEasing)) { it / 5 },
                                exit = fadeOut(tween(180))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xA805070B))
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = currentItem.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val synopsis = currentItem.plot.ifBlank {
                                        if (currentItem.year.isNotBlank()) currentItem.year else ""
                                    }
                                    if (synopsis.isNotBlank()) {
                                        Text(
                                            text = synopsis,
                                            color = Color(0xE6FFFFFF),
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Beat 2: Crisp Mini Poster on bottom-right (clickable via D-pad Right to open title)
                        val posterUrl = currentItem.logoUrl.ifBlank { currentItem.backdropUrl }
                        AnimatedVisibility(
                            visible = posterVisible && posterUrl.isNotBlank(),
                            enter = fadeIn(tween(520, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(520, easing = FastOutSlowInEasing)) { it / 4 },
                            exit = fadeOut(tween(180))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(182.dp)
                                    .shadow(14.dp, RoundedCornerShape(12.dp))
                                    .tvFocusable(
                                        shape = RoundedCornerShape(12.dp),
                                        focusScale = 1.05f,
                                        focusBorderColor = Color.White,
                                        onClick = { onOpenMedia(currentItem) }
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0D0E12))
                            ) {
                                AsyncImage(
                                    model = posterUrl,
                                    contentDescription = currentItem.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMenuTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = shape,
                focusScale = 1.03f,
                focusBorderWidth = 2.dp,
                focusBorderColor = c.accent,
                onClick = onClick
            )
            .clip(shape)
            .background(Color(0xB810131C))
            .border(1.dp, Color(0x26FFFFFF), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
