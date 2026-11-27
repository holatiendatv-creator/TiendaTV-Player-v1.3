package com.tiendatv.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.ui.theme.LocalTiendaColors
import java.util.Locale

/**
 * TiviMate-inspired clean borderless poster card with bottom-left rating badge and title underneath.
 */
@Composable
fun PosterMediaCard(
    item: MediaItemEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 148.dp,
    onFocused: (() -> Unit)? = null
) {
    val c = LocalTiendaColors.current
    val posterShape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier.width(cardWidth),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .tvFocusable(
                    shape = posterShape,
                    focusScale = 1.035f,
                    onFocused = onFocused,
                    onClick = onClick
                )
                .clip(posterShape)
                .background(if (c.isDark) Color(0xFF161A22) else Color(0xFFDCE3EE))
        ) {
            if (item.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = item.name.take(2).uppercase(Locale.ROOT),
                        color = c.textSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (item.rating > 0.0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", item.rating),
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xB30B0D11)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Favorite",
                        tint = c.accent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.name,
            color = c.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun LandscapeHighlightCard(
    item: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .width(228.dp)
            .height(128.dp)
            .tvFocusable(
                shape = shape,
                focusScale = 1.03f,
                onFocused = onFocused,
                onClick = onClick
            )
            .clip(shape)
            .background(if (c.isDark) Color(0xFF161A22) else Color(0xFFCBD5E1))
    ) {
        val img = item.backdropUrl.ifBlank { item.logoUrl }
        if (img.isNotBlank()) {
            AsyncImage(
                model = img,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC0B0D11))
                    )
                )
        )
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/**
 * Continue Watching / Recently Watched card with a D-pad-reachable "Quitar / Remove" pill directly below.
 */
@Composable
fun ContinueWatchingCard(
    entry: WatchHistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    removeLabel: String = "Quitar"
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier.width(224.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .tvFocusable(
                    shape = shape,
                    focusScale = 1.03f,
                    onFocused = onFocused,
                    onClick = onClick
                )
                .clip(shape)
                .background(if (c.isDark) Color(0xFF161A22) else Color(0xFFCBD5E1))
        ) {
            if (entry.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.posterUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x22000000), Color(0xDD0B0D11))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(c.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.subtitle.isNotBlank()) {
                    Text(
                        text = entry.subtitle,
                        color = Color(0xCCFFFFFF),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (entry.progressFraction > 0f) {
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { entry.progressFraction },
                        color = c.accent,
                        trackColor = Color(0x55FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        if (onRemove != null) {
            Row(
                modifier = Modifier
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusScale = 1.03f,
                        onClick = onRemove
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = removeLabel,
                    tint = Color(0xDDFFFFFF),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = removeLabel,
                    color = Color(0xDDFFFFFF),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
