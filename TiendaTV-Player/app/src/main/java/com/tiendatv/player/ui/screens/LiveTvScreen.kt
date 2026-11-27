package com.tiendatv.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.EpgProgramEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.repository.SortMode
import com.tiendatv.player.ui.components.CatalogTopBar
import com.tiendatv.player.ui.components.GlassCard
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 14.dp, end = 20.dp, start = 8.dp, bottom = 14.dp)
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
            onCycleSort = onCycleSort
        )

        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Channel list
            LazyColumn(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels, key = { it.uid }) { channel ->
                    val key = channel.epgChannelId.ifBlank { channel.streamId }
                    val nowProg = currentPrograms[key]
                    val isSelected = focusedChannel?.uid == channel.uid
                    val shape = RoundedCornerShape(16.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(
                                shape = shape,
                                focusScale = 1.02f,
                                onFocused = { onFocusChannel(channel) },
                                onClick = { onPlayChannel(channel) }
                            )
                            .clip(shape)
                            .background(if (isSelected) c.accentSoft else c.glassSurface)
                            .border(1.dp, if (isSelected) c.accent else c.glassBorder, shape)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                color = c.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
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

            // Right: Live EPG Guide Panel
            GlassCard(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                elevated = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.epgGuide,
                            color = c.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        GlassPillButton(
                            text = strings.refreshEpg,
                            icon = Icons.Rounded.Refresh,
                            horizontalPadding = 12.dp,
                            verticalPadding = 7.dp,
                            onClick = onRefreshEpg
                        )
                    }

                    val target = focusedChannel ?: channels.firstOrNull()
                    if (target != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (target.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = target.logoUrl,
                                    contentDescription = target.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(c.pillBackground)
                                        .padding(6.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.name,
                                    color = c.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val isFav = favoriteUids.contains(target.uid)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPillButton(
                                text = strings.playNow,
                                icon = Icons.Rounded.PlayArrow,
                                primary = true,
                                onClick = { onPlayChannel(target) }
                            )
                            GlassPillButton(
                                text = if (isFav) strings.favorited else strings.favorite,
                                icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                onClick = { onToggleFavorite(target) }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (focusedChannelEpg.isEmpty()) {
                            Text(
                                text = strings.epgUnavailable,
                                color = c.textSecondary,
                                fontSize = 13.sp
                            )
                        } else {
                            val nowMs = System.currentTimeMillis()
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(focusedChannelEpg, key = { it.id }) { prog ->
                                    val isCurrent = nowMs in prog.startMs..prog.endMs
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isCurrent) c.accentSoft else c.glassSurface)
                                            .border(1.dp, c.glassBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp),
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
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (prog.description.isNotBlank()) {
                                            Text(
                                                text = prog.description,
                                                color = c.textSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isCurrent && prog.endMs > prog.startMs) {
                                            val frac = ((nowMs - prog.startMs).toFloat() /
                                                (prog.endMs - prog.startMs).toFloat()).coerceIn(0f, 1f)
                                            LinearProgressIndicator(
                                                progress = { frac },
                                                color = c.accent,
                                                trackColor = c.glassBorder,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
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
