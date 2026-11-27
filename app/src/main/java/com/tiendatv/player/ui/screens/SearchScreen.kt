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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.MediaType
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.PosterMediaCard
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

@Composable
fun SearchScreen(
    query: String,
    selectedTypeFilter: String,
    results: List<MediaItemEntity>,
    favoriteUids: Set<String>,
    strings: AppStrings,
    onQueryChange: (String) -> Unit,
    onSelectTypeFilter: (String) -> Unit,
    onOpenItem: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit
) {
    val c = LocalTiendaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 14.dp, end = 20.dp, start = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = strings.searchTitle,
            color = c.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        // Search input box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.glassSurfaceElevated)
                .border(1.dp, c.glassBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(22.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = strings.searchMinChars,
                        color = c.textSecondary,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = { onQueryChange("") })
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = c.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Type filter pills
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPillButton(
                text = strings.filterAll,
                primary = selectedTypeFilter.isBlank(),
                onClick = { onSelectTypeFilter("") }
            )
            GlassPillButton(
                text = strings.liveChannels,
                primary = selectedTypeFilter == MediaType.LIVE,
                onClick = { onSelectTypeFilter(MediaType.LIVE) }
            )
            GlassPillButton(
                text = strings.movies,
                primary = selectedTypeFilter == MediaType.MOVIE,
                onClick = { onSelectTypeFilter(MediaType.MOVIE) }
            )
            GlassPillButton(
                text = strings.series,
                primary = selectedTypeFilter == MediaType.SERIES,
                onClick = { onSelectTypeFilter(MediaType.SERIES) }
            )
        }

        if (query.trim().length < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = strings.searchMinChars, color = c.textSecondary, fontSize = 14.sp)
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = strings.searchNoResults, color = c.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.uid }) { media ->
                    PosterMediaCard(
                        item = media,
                        isFavorite = favoriteUids.contains(media.uid),
                        onClick = { onOpenItem(media) },
                        onToggleFavorite = { onToggleFavorite(media) }
                    )
                }
            }
        }
    }
}
