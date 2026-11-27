package com.tiendatv.player.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.repository.SortMode
import com.tiendatv.player.ui.components.CatalogTopBar
import com.tiendatv.player.ui.components.CategorySidebar
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.PosterMediaCard
import com.tiendatv.player.ui.i18n.AppStrings

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
    val useCategorySidebar = LocalConfiguration.current.screenWidthDp >= 640

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
            LaunchedEffect(gridState, hasMore, items.size) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                    .collect { lastVisible ->
                        if (hasMore && items.isNotEmpty() && lastVisible >= items.size - 18) {
                            onLoadMore()
                        }
                    }
            }

            AnimatedContent(
                targetState = selectedCategoryId,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(140))
                },
                label = "catalogCategoryFade",
                modifier = Modifier.fillMaxSize()
            ) { _ ->
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.uid }) { media ->
                        PosterMediaCard(
                            item = media,
                            isFavorite = favoriteUids.contains(media.uid),
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
