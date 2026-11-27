package com.tiendatv.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.repository.SortMode
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CatalogTopBar(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    searchQuery: String,
    totalCount: Int,
    sortMode: SortMode,
    strings: AppStrings,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCycleSort: () -> Unit,
    modifier: Modifier = Modifier,
    searchHint: String = strings.searchPlaceholder,
    showCategoryDropdown: Boolean = true
) {
    val c = LocalTiendaColors.current
    var showCategoryModal by remember { mutableStateOf(false) }

    val selectedCatName = remember(categories, selectedCategoryId, strings) {
        if (selectedCategoryId.isBlank()) {
            strings.allCategories
        } else {
            categories.firstOrNull { it.categoryId == selectedCategoryId }?.name ?: strings.allCategories
        }
    }

    val formattedCount = remember(totalCount) {
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(totalCount)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showCategoryDropdown) {
                Row(
                    modifier = Modifier
                        .width(220.dp)
                        .tvFocusable(
                            shape = RoundedCornerShape(16.dp),
                            onClick = { showCategoryModal = true }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.glassSurfaceElevated)
                        .border(1.dp, c.glassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(c.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = selectedCatName,
                        color = c.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.UnfoldMore,
                        contentDescription = null,
                        tint = c.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Rounded pill Search Bar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.glassSurfaceElevated)
                    .border(1.dp, c.glassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = searchHint,
                            color = c.textSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = c.textPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .tvFocusable(
                                shape = RoundedCornerShape(8.dp),
                                onClick = { onSearchQueryChange("") }
                            )
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = c.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Clean numeric item count
            Text(
                text = formattedCount,
                color = c.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            // Compact square rounded Sort Toggle button (↑↓)
            val sortShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .tvFocusable(shape = sortShape, onClick = onCycleSort)
                    .clip(sortShape)
                    .background(c.accentSoft)
                    .border(1.dp, c.glassBorder, sortShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = sortMode.name,
                    tint = c.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = c.glassBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))
    }

    if (showCategoryModal) {
        Dialog(onDismissRequest = { showCategoryModal = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                elevated = true
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = strings.categoriesHeader,
                        color = c.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            CategoryChoiceRow(
                                label = strings.allCategories,
                                selected = selectedCategoryId.isBlank(),
                                onClick = {
                                    onSelectCategory("")
                                    showCategoryModal = false
                                }
                            )
                        }
                        items(categories, key = { it.uid }) { cat ->
                            CategoryChoiceRow(
                                label = cat.name,
                                selected = cat.categoryId == selectedCategoryId,
                                onClick = {
                                    onSelectCategory(cat.categoryId)
                                    showCategoryModal = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(if (selected) c.accentSoft else c.glassSurface)
            .border(1.dp, if (selected) c.accent else c.glassBorder, shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) c.accent else c.textPrimary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text(text = "✓", color = c.accent, fontWeight = FontWeight.Bold)
        }
    }
}
