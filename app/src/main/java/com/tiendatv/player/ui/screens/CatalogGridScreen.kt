package com.tiendatv.player.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.repository.SortMode
import com.tiendatv.player.ui.components.CatalogTopBar
import com.tiendatv.player.ui.components.CategorySidebar
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.PosterMediaCard
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

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
    var focusedBackdrop by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle ambient backdrop preview behind grid in Dark Cinema mode
        if (c.isDark && focusedBackdrop.isNotBlank()) {
            Crossfade(
                targetState = focusedBackdrop,
                animationSpec = tween(500),
                label = "gridBackdrop",
                modifier = Modifier.fillMaxSize()
            ) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xD90B0D11), Color(0xF20B0D11))
                        )
                    )
            )
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
                    .padding(top = 14.dp, end = 22.dp, start = 16.dp, bottom = 12.dp)
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
                    columns = GridCells.Adaptive(minSize = 158.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.uid }) { media ->
                        PosterMediaCard(
                            item = media,
                            isFavorite = favoriteUids.contains(media.uid),
                            cardWidth = 158.dp,
                            onFocused = {
                                val bg = media.backdropUrl.ifBlank { media.logoUrl }
                                if (bg.isNotBlank()) focusedBackdrop = bg
                            },
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
}
