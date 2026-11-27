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
    var selectedSeason by remember(seasonNumbers) {
        mutableIntStateOf(seasonNumbers.firstOrNull() ?: 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 14.dp, end = 20.dp, start = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF111827))
                .border(1.dp, c.glassBorder, RoundedCornerShape(24.dp))
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
                            colors = listOf(Color(0xE60B1220), Color(0x990B1220), Color(0x330B1220))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                GlassPillButton(
                    text = strings.back,
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onBack,
                    horizontalPadding = 14.dp,
                    verticalPadding = 8.dp
                )
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

            // Episodes list
            val episodes = details?.seasons?.get(selectedSeason).orEmpty()
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(episodes, key = { it.id }) { ep ->
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(shape = shape, focusScale = 1.02f, onClick = { onPlayEpisode(ep) })
                            .clip(shape)
                            .background(c.glassSurface)
                            .border(1.dp, c.glassBorder, shape)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(118.dp)
                                .height(68.dp)
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
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "S${ep.seasonNumber} E${ep.episodeNumber} • ${ep.title}",
                                color = c.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
