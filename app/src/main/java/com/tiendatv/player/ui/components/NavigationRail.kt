package com.tiendatv.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

enum class NavTab {
    HOME,
    LIVE,
    MOVIES,
    SERIES,
    SEARCH,
    SETTINGS
}

@Composable
fun TiendaNavigationRail(
    selectedTab: NavTab,
    strings: AppStrings,
    onSelectTab: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalTiendaColors.current
    var isRailFocused by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (isRailFocused) 204.dp else 72.dp,
        animationSpec = tween(150),
        label = "railWidth"
    )

    val items = listOf(
        Triple(NavTab.HOME, Icons.Rounded.Home, strings.navHome),
        Triple(NavTab.LIVE, Icons.Rounded.LiveTv, strings.navLive),
        Triple(NavTab.MOVIES, Icons.Rounded.Movie, strings.navMovies),
        Triple(NavTab.SERIES, Icons.Rounded.Tv, strings.navSeries),
        Triple(NavTab.SEARCH, Icons.Rounded.Search, strings.navSearch),
        Triple(NavTab.SETTINGS, Icons.Rounded.Settings, strings.navSettings)
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .onFocusChanged { isRailFocused = it.hasFocus }
            .background(c.glassRail)
            .border(width = 1.dp, color = c.glassBorder)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = if (isRailFocused) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isRailFocused) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isRailFocused) Arrangement.Start else Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(c.accent, c.accentLight))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LiveTv,
                    contentDescription = "TiendaTV",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            AnimatedVisibility(
                visible = isRailFocused,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = c.textPrimary)) { append("Tienda") }
                        withStyle(SpanStyle(color = c.accent)) { append("TV") }
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        items.forEach { (tab, icon, label) ->
            val selected = tab == selectedTab
            val shape = RoundedCornerShape(14.dp)
            val pillBg by animateColorAsState(
                targetValue = if (selected) c.accentSoft else Color.Transparent,
                label = "railItemBg"
            )
            val tint by animateColorAsState(
                targetValue = if (selected) Color.White else c.textSecondary,
                label = "railItemTint"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusable(
                        shape = shape,
                        focusScale = 1.03f,
                        onClick = { onSelectTab(tab) }
                    )
                    .clip(shape)
                    .background(pillBg)
                    .padding(vertical = 10.dp, horizontal = if (isRailFocused) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRailFocused) Arrangement.Start else Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                AnimatedVisibility(
                    visible = isRailFocused,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Text(
                        text = label,
                        color = tint,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}
