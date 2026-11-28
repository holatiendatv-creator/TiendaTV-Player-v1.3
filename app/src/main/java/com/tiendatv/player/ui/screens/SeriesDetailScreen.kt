package com.tiendatv.player.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.data.network.SeriesDetails
import com.tiendatv.player.data.network.SeriesEpisode
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

@Composable
fun SeriesDetailScreen(
    series: MediaItemEntity,
    details: SeriesDetails?,
    episodeHistory: Map<String, WatchHistoryEntity>,
    lastWatchedEpisodeUid: String?,
    isLoading: Boolean,
    strings: AppStrings,
    onPlayEpisode: (SeriesEpisode) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalTiendaColors.current
    BackHandler(onBack = onBack)

    val seasonNumbers = remember(details) {
        details?.seasons?.keys?.toList()?.sorted() ?: emptyList()
    }
    val allEpisodes = remember(details) {
        details?.seasons?.toSortedMap()?.values?.flatten().orEmpty()
    }
    val resumeTargetEpisode = remember(allEpisodes, lastWatchedEpisodeUid, series.uid) {
        if (lastWatchedEpisodeUid != null) {
            allEpisodes.firstOrNull { "${series.uid}_ep_${it.id}" == lastWatchedEpisodeUid }
        } else {
            null
        }
    }

    var selectedSeason by remember(seasonNumbers, resumeTargetEpisode) {
        mutableIntStateOf(
            resumeTargetEpisode?.seasonNumber
                ?: seasonNumbers.firstOrNull()
                ?: 1
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 14.dp, end = 20.dp, start = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header banner with quick Resume Episode pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF000000))
                .border(1.dp, c.glassBorder, RoundedCornerShape(22.dp))
        ) {
            val bg = details?.backdropUrl?.ifBlank { series.backdropUrl.ifBlank { series.logoUrl } }
                ?: series.backdropUrl.ifBlank { series.logoUrl }
            if (bg.isNotBlank()) {
                AsyncImage(
                    model = bg,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xF0000000), Color(0xA6000000), Color(0x28000000))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassPillButton(
                        text = strings.back,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        darkGlass = true,
                        onClick = onBack,
                        horizontalPadding = 14.dp,
                        verticalPadding = 8.dp
                    )
                    val quickEp = resumeTargetEpisode ?: allEpisodes.firstOrNull()
                    if (quickEp != null) {
                        val label = if (resumeTargetEpisode != null) {
                            "${strings.resumeEpisode} S${quickEp.seasonNumber} E${quickEp.episodeNumber}"
                        } else {
                            "${strings.playNow} S${quickEp.seasonNumber} E${quickEp.episodeNumber}"
                        }
                        GlassPillButton(
                            text = label,
                            icon = Icons.Rounded.PlayArrow,
                            primary = true,
                            onClick = { onPlayEpisode(quickEp) },
                            horizontalPadding = 16.dp,
                            verticalPadding = 8.dp
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = details?.title?.ifBlank { series.name } ?: series.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val plot = details?.plot?.ifBlank { series.plot } ?: series.plot
                    if (plot.isNotBlank()) {
                        Text(
                            text = plot,
                            color = Color(0xCCFFFFFF),
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = c.accent)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = strings.loadingSeries, color = c.textSecondary, fontSize = 14.sp)
                }
            }
        } else {
            // Season selector pills
            if (seasonNumbers.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(seasonNumbers, key = { it }) { seasonNum ->
                        GlassPillButton(
                            text = "${strings.season} $seasonNum",
                            primary = seasonNum == selectedSeason,
                            onClick = { selectedSeason = seasonNum }
                        )
                    }
                }
            }

            // Episodes list with saved progress bars & last-watched highlight
            val episodes = details?.seasons?.get(selectedSeason).orEmpty()
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(episodes, key = { it.id }) { ep ->
                    val epUid = "${series.uid}_ep_${ep.id}"
                    val hist = episodeHistory[epUid]
                    val isLastWatched = epUid == lastWatchedEpisodeUid
                    val shape = RoundedCornerShape(16.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(shape = shape, focusScale = 1.02f, onClick = { onPlayEpisode(ep) })
                            .clip(shape)
                            .background(if (isLastWatched) c.accentSoft else c.glassSurface)
                            .border(
                                1.dp,
                                if (isLastWatched) c.accent.copy(alpha = 0.65f) else c.glassBorder,
                                shape
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(124.dp)
                                .height(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.pillBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ep.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ep.thumbnailUrl,
                                    contentDescription = ep.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(Color(0x99000000))
                                    .padding(4.dp)
                            )
                            if (hist != null && hist.progressFraction > 0f) {
                                LinearProgressIndicator(
                                    progress = { hist.progressFraction },
                                    color = c.accent,
                                    trackColor = Color(0x66000000),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(4.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "S${ep.seasonNumber} E${ep.episodeNumber} • ${ep.title}",
                                    color = c.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isLastWatched) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(c.accent)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = strings.resumeEpisode,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (ep.plot.isNotBlank()) {
                                Text(
                                    text = ep.plot,
                                    color = c.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (ep.duration.isNotBlank()) {
                            Text(
                                text = ep.duration,
                                color = c.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
