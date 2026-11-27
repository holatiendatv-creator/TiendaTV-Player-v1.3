package com.tiendatv.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.ui.theme.LocalTiendaColors

@Composable
fun FrostedCanvasBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val c = LocalTiendaColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(c.canvasTop, c.canvasBottom),
                    start = Offset(0f, 0f),
                    end = Offset(1600f, 1200f)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(c.ambientGlowA, Color.Transparent),
                        center = Offset(240f, 140f),
                        radius = 780f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(c.ambientGlowB, Color.Transparent),
                        center = Offset(1520f, 920f),
                        radius = 760f
                    )
                )
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val c = LocalTiendaColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (elevated) c.glassSurfaceElevated else c.glassSurface)
            .border(1.dp, c.glassBorder, shape),
        content = content
    )
}

@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    darkGlass: Boolean = false,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 11.dp
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(24.dp)
    val fgColor = when {
        primary -> Color.White
        darkGlass -> Color.White
        else -> c.textPrimary
    }

    val bgModifier = when {
        primary -> Modifier.background(
            Brush.horizontalGradient(listOf(c.accent, c.accentLight))
        )
        darkGlass -> Modifier.background(Color(0x99181C24))
        else -> Modifier.background(c.pillBackground)
    }

    Row(
        modifier = modifier
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .then(bgModifier)
            .border(
                width = 1.dp,
                color = when {
                    primary -> Color.Transparent
                    darkGlass -> Color(0x33FFFFFF)
                    else -> c.glassBorder
                },
                shape = shape
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            color = fgColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    countText: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val c = LocalTiendaColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = c.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            if (!countText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.accentSoft)
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = countText,
                        color = c.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Text(
                text = actionText,
                color = c.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .tvFocusable(shape = RoundedCornerShape(10.dp), onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
