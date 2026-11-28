package com.tiendatv.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.tiendatv.player.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
 * Navigation rail with explicit D-pad Right handoff (zero focus trap on floating Home overlay)
 * and FocusRequester hook for TiviMate-style Back navigation.
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
        targetValue = if (isRailFocused) {
            if (floatingOverlay) 168.dp else 136.dp
        } else {
            52.dp
        },
        animationSpec = tween(durationMillis = 140),
        label = "railWidth"
    )

    val bgModifier = if (floatingOverlay) {
        if (isRailFocused) {
            Modifier.background(
                Brush.horizontalGradient(
                    0.0f to Color(0xF5000000),
                    0.78f to Color(0xE0000000),
                    1.0f to Color(0x00000000)
                )
            )
        } else {
            Modifier.background(
                Brush.horizontalGradient(
                    0.0f to Color(0xCC000000),
                    1.0f to Color(0x00000000)
                )
            )
        }
    } else {
        Modifier.background(if (c.isDark) Color(0xCC0A0D14) else c.glassRail)
    }

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
            .then(bgModifier)
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
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 14.dp,
                    bottom = 14.dp,
                    start = 6.dp,
                    end = if (floatingOverlay && isRailFocused) 18.dp else 6.dp
                ),
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
                            withStyle(SpanStyle(color = Color.White)) { append("Tienda") }
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
                    targetValue = if (selected) c.accent else if (floatingOverlay) Color(0xCCFFFFFF) else c.textSecondary,
                    animationSpec = tween(120),
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

        if (!floatingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(c.glassBorder)
            )
        }
    }
}
