package com.tiendatv.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// v1.9 crisp type scale — the only sizes in the app. Title 28 / body 13 /
// section 13 uppercase / small 11. No more 14.5 / 17 / 19 / 22 / 30sp scatter.
val TiendaTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.0.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
)

@Immutable
data class TiendaGlassColors(
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasBottom: Color,
    val ambientGlowA: Color,
    val ambientGlowB: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassRail: Color,
    val glassBorder: Color,
    val focusRing: Color,
    val accent: Color,
    val accentLight: Color,
    val accentSoft: Color,
    val badgeGold: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val pillBackground: Color,
    val danger: Color,
    val success: Color
)

/**
 * Warm Ivory & Sandstone Light Mode tailored specifically to harmonize
 * with TiendaTV's orange-coral (#E84D16 / #FF5A1F) brand palette.
 */
val LightGlassPalette = TiendaGlassColors(
    isDark = false,
    canvasTop = Color(0xFFFAF7F2),        // Warm Pearl Ivory
    canvasBottom = Color(0xFFF1EAE0),     // Soft Warm Sand
    ambientGlowA = Color(0x22FF5A1F),     // Soft Peach-Orange Glow
    ambientGlowB = Color(0x20F5B041),     // Soft Champagne Gold Glow
    glassSurface = Color(0xD9FFFFFF),
    glassSurfaceElevated = Color(0xF5FFFFFF),
    glassRail = Color(0xE8FBF8F3),        // Warm Frosted Linen
    glassBorder = Color(0x1F2C1E16),      // Subtle Warm Espresso Hairline
    focusRing = Color(0xFFE84D16),
    accent = Color(0xFFE84D16),           // Crisp Burnt TiendaTV Orange for high light-mode contrast
    accentLight = Color(0xFFFF6E3B),
    accentSoft = Color(0x1EE84D16),
    badgeGold = Color(0xFFC27803),
    textPrimary = Color(0xFF1C1917),      // Warm Espresso Charcoal
    textSecondary = Color(0xFF6B5E55),    // Warm Stone
    pillBackground = Color(0xDBFFFFFF),
    danger = Color(0xFFDC2626),
    success = Color(0xFF15803D)
)

/**
 * v1.9 Dark Cinema: pure black canvas, opaque iOS-style surfaces (#1C1C1E),
 * white 70% secondary text, thin white focus ring. Orange is kept for
 * Play / progress only — it is no longer the focus ring, rail tint, and
 * badge colour all at once.
 */
val DarkCinemaPalette = TiendaGlassColors(
    isDark = true,
    canvasTop = Color(0xFF000000),
    canvasBottom = Color(0xFF000000),
    ambientGlowA = Color(0x00000000),
    ambientGlowB = Color(0x00000000),
    glassSurface = Color(0xFF1C1C1E),
    glassSurfaceElevated = Color(0xFF2C2C2E),
    glassRail = Color(0xFF000000),
    glassBorder = Color(0x1FFFFFFF),
    focusRing = Color(0xFFFFFFFF),
    accent = Color(0xFFFF5A1F),
    accentLight = Color(0xFFFF7A45),
    accentSoft = Color(0x2EFF5A1F),
    badgeGold = Color(0xFFF5B041),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xB3FFFFFF),
    pillBackground = Color(0xFF1C1C1E),
    danger = Color(0xFFF87171),
    success = Color(0xFF4ADE80)
)

val LocalTiendaColors = staticCompositionLocalOf { DarkCinemaPalette }

@Composable
fun TiendaTvTheme(
    darkCinemaMode: Boolean,
    content: @Composable () -> Unit
) {
    val palette = if (darkCinemaMode) DarkCinemaPalette else LightGlassPalette
    val materialScheme = if (darkCinemaMode) {
        darkColorScheme(
            primary = palette.accent,
            background = palette.canvasTop,
            surface = palette.glassSurface,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            background = palette.canvasTop,
            surface = palette.glassSurface,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    }

    CompositionLocalProvider(LocalTiendaColors provides palette) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = TiendaTypography,
            content = content
        )
    }
}
