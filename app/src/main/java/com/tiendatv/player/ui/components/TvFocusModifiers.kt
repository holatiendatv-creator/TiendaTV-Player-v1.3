package com.tiendatv.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tiendatv.player.ui.theme.LocalTiendaColors

/**
 * Silky spring-animated TV D-pad + touch interaction modifier.
 */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(16.dp),
    focusScale: Float = 1.045f,
    focusBorderWidth: Dp = 2.5.dp,
    focusBorderColor: Color? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val palette = LocalTiendaColors.current
    val targetRingColor = focusBorderColor ?: palette.focusRing
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tvFocusScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) targetRingColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tvFocusBorderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) focusBorderWidth else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tvFocusBorderWidth"
    )

    this
        .scale(scale)
        .border(
            width = borderWidth,
            color = borderColor,
            shape = shape
        )
        .onFocusChanged { state ->
            val nowFocused = state.isFocused
            if (nowFocused && !isFocused) {
                onFocused?.invoke()
            }
            isFocused = nowFocused
        }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp &&
                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
            ) {
                onClick()
                true
            } else {
                false
            }
        }
        .focusable()
        .clickable(onClick = onClick)
}

/**
 * Provides smooth pivot-style BringIntoView scrolling across LazyColumn, LazyRow, and LazyVerticalGrid
 * so D-pad navigation glides instead of snapping abruptly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProvideSmoothTvScrolling(content: @Composable () -> Unit) {
    val pivotSpec = remember {
        object : BringIntoViewSpec {
            override val scrollAnimationSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )

            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                val targetCenter = (containerSize - size) * 0.38f
                val maxOffset = (containerSize - size).coerceAtLeast(0f)
                val clampedTarget = targetCenter.coerceIn(0f, maxOffset)
                return offset - clampedTarget
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
        content()
    }
}
