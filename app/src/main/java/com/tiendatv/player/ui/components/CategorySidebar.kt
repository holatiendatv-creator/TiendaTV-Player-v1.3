package com.tiendatv.player.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

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
            .width(210.dp)
            .background(c.glassSurface)
            .border(width = 1.dp, color = c.glassBorder)
            .padding(top = 16.dp, bottom = 12.dp, start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = strings.categoriesHeader,
            color = c.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxHeight()
        ) {
            item(key = "all_categories") {
                CategorySidebarRow(
                    label = strings.allCategories,
                    selected = selectedCategoryId.isBlank(),
                    onClick = { onSelectCategory("") }
                )
            }
            items(categories, key = { it.uid }) { cat ->
                CategorySidebarRow(
                    label = cat.name,
                    selected = cat.categoryId == selectedCategoryId,
                    onClick = { onSelectCategory(cat.categoryId) }
                )
            }
        }
    }
}

@Composable
private fun CategorySidebarRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(12.dp)
    val bgColor by animateColorAsState(
        targetValue = if (selected) c.accentSoft else Color.Transparent,
        label = "catRowBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) c.accent else c.textSecondary,
        label = "catRowText"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = shape,
                focusScale = 1.025f,
                onClick = onClick
            )
            .clip(shape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (selected) c.accent.copy(alpha = 0.55f) else Color.Transparent,
                shape = shape
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
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
