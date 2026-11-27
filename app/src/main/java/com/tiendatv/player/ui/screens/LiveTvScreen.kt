package com.tiendatv.player.ui.screens

import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.EpgProgramEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.network.NetworkModule
import com.tiendatv.player.data.repository.SortMode
import com.tiendatv.player.ui.components.CatalogTopBar
import com.tiendatv.player.ui.components.CategorySidebar
import com.tiendatv.player.ui.components.GlassCard
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun LiveTvScreen(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    searchQuery: String,
    totalCount: Int,
    sortMode: SortMode,
    channels: List<MediaItemEntity>,
    hasMore: Boolean,
    focusedChannel: MediaItemEntity?,
    currentPrograms: Map<String, EpgProgramEntity>,
    focusedChannelEpg: List<EpgProgramEntity>,
    favoriteUids: Set<String>,
    strings: AppStrings,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCycleSort: () -> Unit,
    onLoadMore: () -> Unit,
    onFocusChannel: (MediaItemEntity) -> Unit,
    onPlayChannel: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onRefreshEpg: () -> Unit
) {
    val c = LocalTiendaColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val useCategorySidebar = LocalConfiguration.current.screenWidthDp >= 640
    val target = focusedChannel ?: channels.firstOrNull()

    // Lightweight TiviMate-style live preview player, strictly released on dispose/background
    val previewPlayer = remember(context) {
        val dsFactory = OkHttpDataSource.Factory(NetworkModule.streamClient)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
            .build()
            .apply {
                playWhenReady = true
            }
    }

    DisposableEffect(previewPlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                previewPlayer.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                previewPlayer.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            previewPlayer.stop()
            previewPlayer.release()
        }
    }

    // Debounce 600ms so fast D-pad scrolling through channels stays instant and fluid
    LaunchedEffect(target?.uid) {
        val url = target?.streamUrl.orEmpty()
        if (url.isBlank()) {
            previewPlayer.stop()
            return@LaunchedEffect
        }
        delay(600L)
        previewPlayer.setMediaItem(MediaItem.fromUri(url))
        previewPlayer.prepare()
        previewPlayer.playWhenReady = true
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useCategorySidebar) {
            CategorySidebar(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                strings = strings,
                onSelectCategory = onSelectCategory
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 14.dp, end = 20.dp, start = 14.dp, bottom = 14.dp)
        ) {
            CatalogTopBar(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                searchQuery = searchQuery,
                totalCount = totalCount,
                sortMode = sortMode,
                strings = strings,
                onSelectCategory = onSelectCategory,
                onSearchQueryChange = onSearchQueryChange,
                onCycleSort = onCycleSort,
                showCategoryDropdown = !useCategorySidebar
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: Smooth, zero-glitch channel list
                val listState = rememberLazyListState()
                LaunchedEffect(selectedCategoryId) {
                    listState.scrollToItem(0)
                }
                LaunchedEffect(listState, hasMore, channels.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                        .collect { lastVisible ->
                            if (hasMore && channels.isNotEmpty() && lastVisible >= channels.size - 15) {
                                onLoadMore()
                            }
                        }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(channels, key = { it.uid }) { channel ->
                        val key = channel.epgChannelId.ifBlank { channel.streamId }
                        val nowProg = currentPrograms[key]
                        val isSelected = target?.uid == channel.uid
                        val shape = RoundedCornerShape(14.dp)
                        val rowBg by animateColorAsState(
                            targetValue = if (isSelected) c.accentSoft else Color.Transparent,
                            animationSpec = tween(150),
                            label = "chRowBg"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(
                                    shape = shape,
                                    focusScale = 1.015f,
                                    onFocused = { onFocusChannel(channel) },
                                    onClick = {
                                        previewPlayer.stop()
                                        onPlayChannel(channel)
                                    }
                                )
                                .clip(shape)
                                .background(rowBg)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.pillBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                if (channel.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${channel.channelNumber}",
                                        color = c.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    color = if (isSelected) c.textPrimary else c.textPrimary.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = nowProg?.title ?: strings.epgUnavailable,
                                    color = c.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (favoriteUids.contains(channel.uid)) {
                                Text(text = "♥", color = c.accent, fontSize = 14.sp)
                            }
                        }
                    }

                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassPillButton(text = strings.loadMore, onClick = onLoadMore)
                            }
                        }
                    }
                }

                // Right: Live Mini-Player Preview + EPG Guide
                GlassCard(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    elevated = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 16:9 Live Preview Window
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                                .tvFocusable(
                                    shape = RoundedCornerShape(14.dp),
                                    focusScale = 1.015f,
                                    onClick = {
                                        if (target != null) {
                                            previewPlayer.stop()
                                            onPlayChannel(target)
                                        }
                                    }
                                )
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        useController = false
                                        player = previewPlayer
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                update = { view ->
                                    if (view.player !== previewPlayer) {
                                        view.player = previewPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (target != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xCCEF4444))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = strings.liveBadge,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xB30B0D11))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = strings.pressOkFullscreen,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (target != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = target.name,
                                    color = c.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                val isFav = favoriteUids.contains(target.uid)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassPillButton(
                                        text = strings.playNow,
                                        icon = Icons.Rounded.PlayArrow,
                                        primary = true,
                                        horizontalPadding = 12.dp,
                                        verticalPadding = 7.dp,
                                        onClick = {
                                            previewPlayer.stop()
                                            onPlayChannel(target)
                                        }
                                    )
                                    GlassPillButton(
                                        text = "",
                                        icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        horizontalPadding = 10.dp,
                                        verticalPadding = 7.dp,
                                        onClick = { onToggleFavorite(target) }
                                    )
                                    GlassPillButton(
                                        text = "",
                                        icon = Icons.Rounded.Refresh,
                                        horizontalPadding = 10.dp,
                                        verticalPadding = 7.dp,
                                        onClick = onRefreshEpg
                                    )
                                }
                            }

                            if (focusedChannelEpg.isEmpty()) {
                                Text(
                                    text = strings.epgUnavailable,
                                    color = c.textSecondary,
                                    fontSize = 13.sp
                                )
                            } else {
                                val nowMs = System.currentTimeMillis()
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(focusedChannelEpg, key = { it.id }) { prog ->
                                        val isCurrent = nowMs in prog.startMs..prog.endMs
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCurrent) c.accentSoft else c.glassSurface)
                                                .border(1.dp, c.glassBorder, RoundedCornerShape(12.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (isCurrent) strings.epgNow else strings.epgNext,
                                                    color = if (isCurrent) c.accent else c.textSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${timeFmt.format(Date(prog.startMs))} - ${timeFmt.format(Date(prog.endMs))}",
                                                    color = c.textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = prog.title,
                                                color = c.textPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isCurrent && prog.endMs > prog.startMs) {
                                                val frac = ((nowMs - prog.startMs).toFloat() /
                                                    (prog.endMs - prog.startMs).toFloat()).coerceIn(0f, 1f)
                                                LinearProgressIndicator(
                                                    progress = { frac },
                                                    color = c.accent,
                                                    trackColor = c.glassBorder,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(3.5.dp)
                                                        .clip(RoundedCornerShape(2.dp))
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
        }
    }
}
