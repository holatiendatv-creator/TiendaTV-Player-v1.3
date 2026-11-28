package com.tiendatv.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

/**
 * Slim, quiet, zero-overlap navigation rail:
 * Compact 52dp icon rail when browsing, gently expands to 136dp when focused
 * to minimize horizontal shift and visual noise.
 */
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
        targetValue = if (isRailFocused) 136.dp else 52.dp,
        animationSpec = tween(durationMillis = 110),
        label = "railWidth"
    )

    val railSurface = if (c.isDark) Color(0xFF000000) else c.glassRail

    val items = listOf(
        Triple(NavTab.HOME, Icons.Rounded.Home, strings.navHome),
        Triple(NavTab.LIVE, Icons.Rounded.LiveTv, strings.navLive),
        Triple(NavTab.MOVIES, Icons.Rounded.Movie, strings.navMovies),
        Triple(NavTab.SERIES, Icons.Rounded.Tv, strings.navSeries),
        Triple(NavTab.SEARCH, Icons.Rounded.Search, strings.navSearch),
        Triple(NavTab.SETTINGS, Icons.Rounded.Settings, strings.navSettings)
    )

    Row(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .background(railSurface)
            .onFocusChanged { isRailFocused = it.hasFocus }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.linearGradient(listOf(c.accent, c.accentLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LiveTv,
                        contentDescription = "TiendaTV",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (isRailFocused) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = c.textPrimary)) { append("Tienda") }
                            withStyle(SpanStyle(color = c.accent)) { append("TV") }
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { (tab, icon, label) ->
                val selected = tab == selectedTab
                val shape = RoundedCornerShape(10.dp)
                val pillBg by animateColorAsState(
                    targetValue = if (selected) c.accentSoft else Color.Transparent,
                    animationSpec = tween(120),
                    label = "railItemBg"
                )
                val tint by animateColorAsState(
                    targetValue = if (selected) c.accent else c.textSecondary,
                    animationSpec = tween(120),
                    label = "railItemTint"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable(
                            shape = shape,
                            focusScale = 1.02f,
                            onClick = { onSelectTab(tab) }
                        )
                        .clip(shape)
                        .background(pillBg)
                        .padding(vertical = 9.dp, horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    if (isRailFocused) {
                        Text(
                            text = label,
                            color = tint,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 9.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(c.glassBorder)
        )
    }
}
