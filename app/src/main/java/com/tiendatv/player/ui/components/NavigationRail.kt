package com.tiendatv.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.R
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
 * Floating frosted-glass pill navigation rail matching HomeScreen's exact button style:
 * No solid vertical slab or hard divider line—clean floating glass pills over the dark cinema canvas.
 */
@Composable
fun TiendaNavigationRail(
    selectedTab: NavTab,
    strings: AppStrings,
    onSelectTab: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    floatingOverlay: Boolean = false,
    railFocusRequester: FocusRequester? = null,
    onExitRight: (() -> Unit)? = null,
    onFocusStateChanged: ((Boolean) -> Unit)? = null
) {
    val c = LocalTiendaColors.current
    var isRailFocused by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (isRailFocused) 144.dp else 56.dp,
        animationSpec = tween(durationMillis = 130),
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
            .background(if (c.isDark) Color.Transparent else c.glassRail)
            .onFocusChanged {
                val focused = it.hasFocus
                if (focused != isRailFocused) {
                    isRailFocused = focused
                    onFocusStateChanged?.invoke(focused)
                }
            }
            .onPreviewKeyEvent { event ->
                if (onExitRight != null &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionRight
                ) {
                    onExitRight()
                    true
                } else {
                    false
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_fg),
                contentDescription = "TiendaTV",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            if (isRailFocused) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = c.textPrimary)) { append("Tienda") }
                        withStyle(SpanStyle(color = c.accent)) { append("TV") }
                    },
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        items.forEach { (tab, icon, label) ->
            val selected = tab == selectedTab
            val shape = RoundedCornerShape(12.dp)
            val pillBg by animateColorAsState(
                targetValue = when {
                    selected -> c.accentSoft
                    c.isDark -> Color(0x9E10131C)
                    else -> c.glassSurface
                },
                animationSpec = tween(130),
                label = "railItemBg"
            )
            val pillBorder by animateColorAsState(
                targetValue = if (selected) c.accent.copy(alpha = 0.55f) else Color(0x24FFFFFF),
                animationSpec = tween(130),
                label = "railItemBorder"
            )
            val tint by animateColorAsState(
                targetValue = if (selected) c.accent else Color(0xE6FFFFFF),
                animationSpec = tween(130),
                label = "railItemTint"
            )
            val itemMod = if (selected && railFocusRequester != null) {
                Modifier.focusRequester(railFocusRequester)
            } else {
                Modifier
            }

            Row(
                modifier = itemMod
                    .fillMaxWidth()
                    .tvFocusable(
                        shape = shape,
                        focusScale = 1.03f,
                        onClick = { onSelectTab(tab) }
                    )
                    .clip(shape)
                    .background(pillBg)
                    .border(1.dp, if (c.isDark) pillBorder else c.glassBorder, shape)
                    .padding(vertical = 10.dp, horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (c.isDark) tint else if (selected) c.accent else c.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                if (isRailFocused) {
                    Text(
                        text = label,
                        color = if (c.isDark) tint else if (selected) c.accent else c.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}
