package com.tiendatv.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Unified TV D-pad + touch interaction modifier.
 * Renders a crisp tvOS-style focus ring and subtle scale lift when focused via remote,
 * and triggers [onClick] on both touch tap and DPAD_CENTER / Enter key release.
 */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(16.dp),
    focusScale: Float = 1.04f,
    focusBorderWidth: Dp = 2.5.dp,
    focusBorderColor: Color? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val palette = LocalTiendaColors.current
    val ringColor = focusBorderColor ?: palette.focusRing
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "tvFocusScale"
    )

    this
        .scale(scale)
        .border(
            width = if (isFocused) focusBorderWidth else 0.dp,
            color = if (isFocused) ringColor else Color.Transparent,
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
