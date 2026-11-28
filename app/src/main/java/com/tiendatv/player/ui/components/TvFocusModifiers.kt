package com.tiendatv.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
 * Calm, zen TV D-pad + touch interaction modifier with gentle spring scale,
 * glowing focus ring, and optional Long-Press OK / Firestick Menu support.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(16.dp),
    focusScale: Float = 1.03f,
    focusBorderWidth: Dp = 2.dp,
    focusBorderColor: Color? = null,
    onFocused: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val palette = LocalTiendaColors.current
    val targetRingColor = focusBorderColor ?: palette.focusRing
    var isFocused by remember { mutableStateOf(false) }
    var okDownTimeMs by remember { mutableLongStateOf(0L) }
    var longPressHandled by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.90f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tvFocusScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) targetRingColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tvFocusBorderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) focusBorderWidth else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
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
            if (!nowFocused) {
                okDownTimeMs = 0L
                longPressHandled = false
            }
            isFocused = nowFocused
        }
        .onKeyEvent { event ->
            val isSelectKey = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter

            if (onLongClick != null && event.type == KeyEventType.KeyUp && event.key == Key.Menu) {
                onLongClick()
                return@onKeyEvent true
            }

            if (isSelectKey) {
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (okDownTimeMs == 0L) {
                            okDownTimeMs = System.currentTimeMillis()
                            longPressHandled = false
                        } else if (onLongClick != null && !longPressHandled &&
                            System.currentTimeMillis() - okDownTimeMs >= 520L
                        ) {
                            longPressHandled = true
                            onLongClick()
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        val wasLong = longPressHandled
                        okDownTimeMs = 0L
                        longPressHandled = false
                        if (!wasLong) {
                            onClick()
                        }
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
        .focusable()
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
}

/**
 * Uses Compose's native default BringIntoView scrolling (eliminates bounce/jump on tall cards).
 */
@Composable
fun ProvideSmoothTvScrolling(content: @Composable () -> Unit) {
    content()
}
