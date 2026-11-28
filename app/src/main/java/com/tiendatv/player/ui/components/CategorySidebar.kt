package com.tiendatv.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.repository.CatalogRepository
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

/**
 * Floating frosted-glass pill Categories sidebar matching HomeScreen's open glass aesthetic:
 * No solid vertical slate slab or hard divider line—pills float cleanly over the ambient backdrop.
 */
@Composable
fun CategorySidebar(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    strings: AppStrings,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalTiendaColors.current
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(184.dp)
            .background(if (c.isDark) Color.Transparent else c.glassRail)
            .padding(top = 18.dp, bottom = 12.dp, start = 6.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = strings.categoriesHeader.uppercase(),
            color = c.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier.fillMaxHeight()
        ) {
            item(key = "cat_favorites") {
                CategorySidebarRow(
                    label = strings.favorites,
                    icon = Icons.Rounded.Favorite,
                    selected = selectedCategoryId == CatalogRepository.CAT_FAVORITES,
                    onClick = { onSelectCategory(CatalogRepository.CAT_FAVORITES) }
                )
            }
            item(key = "cat_history") {
                CategorySidebarRow(
                    label = strings.historyCategory,
                    icon = Icons.Rounded.History,
                    selected = selectedCategoryId == CatalogRepository.CAT_HISTORY,
                    onClick = { onSelectCategory(CatalogRepository.CAT_HISTORY) }
                )
            }
            item(key = "cat_recent") {
                CategorySidebarRow(
                    label = strings.recentlyAddedCategory,
                    icon = Icons.Rounded.NewReleases,
                    selected = selectedCategoryId == CatalogRepository.CAT_RECENTLY_ADDED,
                    onClick = { onSelectCategory(CatalogRepository.CAT_RECENTLY_ADDED) }
                )
            }
            item(key = "all_categories") {
                CategorySidebarRow(
                    label = strings.allCategories,
                    icon = if (selectedCategoryId.isBlank()) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
                    selected = selectedCategoryId.isBlank(),
                    onClick = { onSelectCategory("") }
                )
            }
            items(categories, key = { it.uid }) { cat ->
                val isSelected = cat.categoryId == selectedCategoryId
                CategorySidebarRow(
                    label = cat.name,
                    icon = if (isSelected) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
                    selected = isSelected,
                    onClick = { onSelectCategory(cat.categoryId) }
                )
            }
        }
    }
}

@Composable
private fun CategorySidebarRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(12.dp)
    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> c.accentSoft
            c.isDark -> Color(0x8C10131C)
            else -> c.glassSurface
        },
        animationSpec = tween(140),
        label = "catRowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) c.accent.copy(alpha = 0.55f) else Color(0x20FFFFFF),
        animationSpec = tween(140),
        label = "catRowBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) c.accent else if (c.isDark) Color(0xE6FFFFFF) else c.textPrimary,
        animationSpec = tween(140),
        label = "catRowText"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = shape,
                focusScale = 1.02f,
                onClick = onClick
            )
            .clip(shape)
            .background(bgColor)
            .border(1.dp, if (c.isDark) borderColor else c.glassBorder, shape)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
