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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.repository.CatalogRepository
import com.tiendatv.player.data.repository.SortMode
import com.tiendatv.player.ui.components.CatalogTopBar
import com.tiendatv.player.ui.components.CategorySidebar
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.PosterMediaCard
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

/**
 * TiviMate-inspired Movies & Series catalog screen:
 * - Left: Unified obsidian Categories sidebar (zero overlap)
 * - Top Right: Cinematic Hero Detail of the focused Movie/Series (title, rating, year, plot, artwork)
 * - Bottom Right: Dense 5-column poster grid with category title & count
 */
@Composable
fun CatalogGridScreen(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    searchQuery: String,
    totalCount: Int,
    sortMode: SortMode,
    items: List<MediaItemEntity>,
    hasMore: Boolean,
    favoriteUids: Set<String>,
    strings: AppStrings,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCycleSort: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenItem: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit
) {
    val c = LocalTiendaColors.current
    val useCategorySidebar = LocalConfiguration.current.screenWidthDp >= 640
    var pendingFocusedItem by remember(selectedCategoryId) { mutableStateOf<MediaItemEntity?>(null) }
    var settledFocusedItem by remember(selectedCategoryId) { mutableStateOf<MediaItemEntity?>(null) }
    LaunchedEffect(pendingFocusedItem) {
        if (pendingFocusedItem != null) {
            delay(220L)
            settledFocusedItem = pendingFocusedItem
        }
    }
    val heroItem = settledFocusedItem ?: items.firstOrNull()

    val selectedCatName = remember(categories, selectedCategoryId, strings) {
        when (selectedCategoryId) {
            "" -> strings.allCategories
            CatalogRepository.CAT_FAVORITES -> strings.favorites
            CatalogRepository.CAT_HISTORY -> strings.historyCategory
            else -> categories.firstOrNull { it.categoryId == selectedCategoryId }?.name ?: strings.allCategories
        }
    }
    val formattedCount = remember(totalCount) {
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(totalCount)
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
        ) {
            if (!useCategorySidebar) {
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
                    showCategoryDropdown = true,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            } else {
                // TiviMate-Style Top Hero Detail Panel (~40% height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(234.dp)
                        .background(if (c.isDark) Color(0xFF000000) else c.canvasTop)
                ) {
                    val artworkUrl = heroItem?.backdropUrl?.ifBlank { heroItem.logoUrl }.orEmpty()
                    if (artworkUrl.isNotBlank()) {
                        Crossfade(
                            targetState = artworkUrl,
                            animationSpec = tween(650),
                            label = "catalogHeroArt",
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxWidth(0.68f)
                                .fillMaxHeight()
                        ) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = heroItem?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Smooth left-to-right and bottom obsidian fade over artwork
                    val fadeBg = if (c.isDark) Color(0xFF000000) else c.canvasTop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to fadeBg,
                                    0.36f to fadeBg.copy(alpha = 0.94f),
                                    0.66f to fadeBg.copy(alpha = 0.40f),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to fadeBg.copy(alpha = 0.25f),
                                    0.45f to Color.Transparent,
                                    0.78f to fadeBg.copy(alpha = 0.72f),
                                    1.0f to fadeBg
                                )
                            )
                    )

                    // Left: Focused Title, Rating Badge, Year, Plot
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.64f)
                            .padding(start = 22.dp, top = 14.dp, bottom = 10.dp, end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (heroItem != null) {
                            Text(
                                text = heroItem.name,
                                color = c.textPrimary,
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (heroItem.rating > 0.0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(c.accent)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", heroItem.rating),
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (heroItem.year.isNotBlank()) {
                                    Text(
                                        text = heroItem.year,
                                        color = c.textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = selectedCatName,
                                    color = c.textSecondary,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (heroItem.plot.isNotBlank()) {
                                Text(
                                    text = heroItem.plot,
                                    color = c.textPrimary.copy(alpha = 0.82f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Top-Right Compact Search & Sort Bar (discreet, like TiviMate)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var isSearchFocused by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xB3000000))
                                .border(
                                    width = if (isSearchFocused) 2.dp else 1.dp,
                                    color = if (isSearchFocused) c.accent else c.glassBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .onFocusChanged { isSearchFocused = it.hasFocus }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = c.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = strings.searchPlaceholder,
                                        color = c.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                    cursorBrush = SolidColor(c.accent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .tvFocusable(
                                            shape = RoundedCornerShape(6.dp),
                                            onClick = { onSearchQueryChange("") }
                                        )
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    onClick = onCycleSort
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xB3000000))
                                .border(1.dp, c.glassBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = sortMode.name,
                                tint = c.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Category Label + Item Count Bar right above the posters (like TiviMate)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCatName,
                    color = c.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$formattedCount ${strings.itemsLabel}",
                    color = c.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bottom Dense Poster Grid (5+ columns, smooth scrolling)
            val gridState = rememberLazyGridState()
            LaunchedEffect(selectedCategoryId) {
                gridState.scrollToItem(0)
            }
            LaunchedEffect(gridState, hasMore, items.size) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                    .collect { lastVisible ->
                        if (hasMore && items.isNotEmpty() && lastVisible >= items.size - 18) {
                            onLoadMore()
                        }
                    }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 136.dp),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(items, key = { it.uid }) { media ->
                    PosterMediaCard(
                        item = media,
                        isFavorite = favoriteUids.contains(media.uid),
                        cardWidth = 136.dp,
                        onFocused = { pendingFocusedItem = media },
                        onClick = { onOpenItem(media) },
                        onToggleFavorite = { onToggleFavorite(media) }
                    )
                }

                if (hasMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassPillButton(
                                text = strings.loadMore,
                                primary = true,
                                onClick = onLoadMore
                            )
                        }
                    }
                }
            }
        }
    }
}
