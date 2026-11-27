package com.tiendatv.player.player

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.tiendatv.player.data.local.UserSettings
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    request: PlaybackRequest,
    settings: UserSettings,
    strings: AppStrings,
    epgNowTitle: String?,
    hasPrevChannel: Boolean,
    hasNextChannel: Boolean,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember(request.uid, settings.bufferPreset, settings.preferSoftwareDecoder, settings.language) {
        TiendaPlayerController(
            context = context,
            scope = scope,
            bufferPreset = settings.bufferPreset,
            preferSoftwareDecoder = settings.preferSoftwareDecoder,
            preferredLanguage = settings.language,
            initialAspectMode = settings.defaultAspectMode,
            onProgressPersist = onSaveProgress
        )
    }

    val state by controller.uiState.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    val playPauseFocus = remember { FocusRequester() }

    // Keep TV/phone screen awake during playback
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.release()
        }
    }

    LaunchedEffect(request) {
        controller.play(request)
        controlsVisible = true
        lastInteractionMs = System.currentTimeMillis()
    }

    // Auto-hide controls after 6 seconds of inactivity while playing
    LaunchedEffect(controlsVisible, lastInteractionMs, state.isPlaying, showAudioDialog, showSubtitleDialog) {
        if (controlsVisible && state.isPlaying && !showAudioDialog && !showSubtitleDialog) {
            delay(6_000L)
            controlsVisible = false
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            try {
                playPauseFocus.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    BackHandler {
        when {
            showAudioDialog -> showAudioDialog = false
            showSubtitleDialog -> showSubtitleDialog = false
            else -> onClose()
        }
    }

    fun pokeControls() {
        controlsVisible = true
        lastInteractionMs = System.currentTimeMillis()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    pokeControls()
                    when (event.key) {
                        Key.MediaPlayPause, Key.Spacebar -> {
                            controller.togglePlayPause()
                            true
                        }
                        Key.ChannelUp -> {
                            if (request.isLive && hasNextChannel) {
                                onNextChannel()
                                true
                            } else false
                        }
                        Key.ChannelDown -> {
                            if (request.isLive && hasPrevChannel) {
                                onPrevChannel()
                                true
                            } else false
                        }
                        Key.DirectionLeft -> {
                            if (!controlsVisible && !request.isLive) {
                                controller.seekBy(-10_000L)
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (!controlsVisible && !request.isLive) {
                                controller.seekBy(10_000L)
                                true
                            } else false
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
                if (controlsVisible) lastInteractionMs = System.currentTimeMillis()
            }
    ) {
        // Media3 Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = controller.exoPlayer
                    resizeMode = state.aspectMode.toMedia3ResizeMode()
                }
            },
            update = { view ->
                if (view.player !== controller.exoPlayer) {
                    view.player = controller.exoPlayer
                }
                view.resizeMode = state.aspectMode.toMedia3ResizeMode()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering / Reconnecting indicator
        if (state.isBuffering || state.isReconnecting) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xCC0D111A))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF7A45),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (state.isReconnecting) {
                        "${strings.reconnecting} (${state.reconnectAttempt}/${state.maxReconnectAttempts})"
                    } else {
                        strings.buffering
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Fatal error overlay with manual retry
        if (state.errorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE60D111A))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.playbackError,
                    color = Color(0xFFF87171),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = Color(0xCCFFFFFF),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerActionPill(
                        label = strings.retry,
                        icon = Icons.Rounded.Refresh,
                        onClick = { controller.retryManual() }
                    )
                    PlayerActionPill(
                        label = strings.back,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = onClose
                    )
                }
            }
        }

        // Sleek tvOS-inspired HUD overlay
        AnimatedVisibility(
            visible = controlsVisible && state.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB3000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xCC000000)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                // Top Bar: Back + Title + Live/HD Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.back,
                        onClick = onClose
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sub = epgNowTitle?.takeIf { it.isNotBlank() } ?: request.subtitle
                        if (sub.isNotBlank()) {
                            Text(
                                text = sub,
                                color = Color(0xCCFFFFFF),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (request.isLive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = strings.liveBadge,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (state.videoInfoBadge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x44FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = state.videoInfoBadge,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Bottom Controls: Seekbar (VOD) + Transport + Audio/Subtitles/Aspect
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    if (!request.isLive && state.durationMs > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(state.currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = state.currentPositionMs.toFloat(),
                                onValueChange = {
                                    pokeControls()
                                    controller.seekTo(it.toLong())
                                },
                                valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF7A45),
                                    activeTrackColor = Color(0xFFFF7A45),
                                    inactiveTrackColor = Color(0x55FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )
                            Text(
                                text = formatDuration(state.durationMs),
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (request.isLive && hasPrevChannel) {
                                PlayerIconButton(
                                    icon = Icons.Rounded.SkipPrevious,
                                    contentDescription = strings.prevChannel,
                                    onClick = {
                                        pokeControls()
                                        onPrevChannel()
                                    }
                                )
                            }
                            if (!request.isLive) {
                                PlayerIconButton(
                                    icon = Icons.Rounded.Replay10,
                                    contentDescription = "-10s",
                                    onClick = {
                                        pokeControls()
                                        controller.seekBy(-10_000L)
                                    }
                                )
                            }
                            PlayerIconButton(
                                icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = strings.playPause,
                                modifier = Modifier.focusRequester(playPauseFocus),
                                emphasized = true,
                                onClick = {
                                    pokeControls()
                                    controller.togglePlayPause()
                                }
                            )
                            if (!request.isLive) {
                                PlayerIconButton(
                                    icon = Icons.Rounded.Forward10,
                                    contentDescription = "+10s",
                                    onClick = {
                                        pokeControls()
                                        controller.seekBy(10_000L)
                                    }
                                )
                            }
                            if (request.isLive && hasNextChannel) {
                                PlayerIconButton(
                                    icon = Icons.Rounded.SkipNext,
                                    contentDescription = strings.nextChannel,
                                    onClick = {
                                        pokeControls()
                                        onNextChannel()
                                    }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayerActionPill(
                                label = strings.audioTracks,
                                icon = Icons.Rounded.Audiotrack,
                                onClick = {
                                    pokeControls()
                                    showAudioDialog = true
                                }
                            )
                            PlayerActionPill(
                                label = strings.subtitles,
                                icon = Icons.Rounded.Subtitles,
                                onClick = {
                                    pokeControls()
                                    showSubtitleDialog = true
                                }
                            )
                            PlayerActionPill(
                                label = aspectModeLabel(state.aspectMode, strings),
                                icon = Icons.Rounded.AspectRatio,
                                onClick = {
                                    pokeControls()
                                    controller.cycleAspectMode()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Audio Track Picker Overlay
        if (showAudioDialog) {
            TrackPickerOverlay(
                title = strings.audioTracks,
                tracks = state.audioTracks,
                includeOffOption = false,
                offLabel = strings.off,
                emptyLabel = strings.noTracksAvailable,
                onSelect = { opt ->
                    if (opt != null) controller.selectAudioTrack(opt)
                    showAudioDialog = false
                },
                onDismiss = { showAudioDialog = false }
            )
        }

        // Subtitle Track Picker Overlay
        if (showSubtitleDialog) {
            TrackPickerOverlay(
                title = strings.subtitles,
                tracks = state.subtitleTracks,
                includeOffOption = true,
                offLabel = strings.off,
                emptyLabel = strings.noTracksAvailable,
                onSelect = { opt ->
                    controller.selectSubtitleTrack(opt)
                    showSubtitleDialog = false
                },
                onDismiss = { showSubtitleDialog = false }
            )
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val size = if (emphasized) 54.dp else 44.dp
    Box(
        modifier = modifier
            .size(size)
            .tvFocusable(
                shape = CircleShape,
                focusBorderColor = Color(0xFFFF7A45),
                onClick = onClick
            )
            .clip(CircleShape)
            .background(if (emphasized) Color(0xFFFF5A1F) else Color(0x55FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(if (emphasized) 28.dp else 22.dp)
        )
    }
}

@Composable
private fun PlayerActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusBorderColor = Color(0xFFFF7A45),
                onClick = onClick
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55182234))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text = label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TrackPickerOverlay(
    title: String,
    tracks: List<TrackOption>,
    includeOffOption: Boolean,
    offLabel: String,
    emptyLabel: String,
    onSelect: (TrackOption?) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEE111827))
                .padding(20.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (tracks.isEmpty() && !includeOffOption) {
                Text(text = emptyLabel, color = Color(0xAAFFFFFF), fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (includeOffOption) {
                        item {
                            val noneSelected = tracks.none { it.isSelected }
                            TrackRowItem(
                                label = offLabel,
                                selected = noneSelected,
                                onClick = { onSelect(null) }
                            )
                        }
                    }
                    items(tracks, key = { it.id }) { track ->
                        TrackRowItem(
                            label = track.label,
                            selected = track.isSelected,
                            onClick = { onSelect(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRowItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusBorderColor = Color(0xFFFF7A45),
                onClick = onClick
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFF5A1F) else Color(0x22FFFFFF))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (selected) {
            Text(text = "✓", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private fun aspectModeLabel(mode: VideoAspectMode, strings: AppStrings): String = when (mode) {
    VideoAspectMode.FIT -> strings.aspectFit
    VideoAspectMode.ZOOM -> strings.aspectZoom
    VideoAspectMode.FILL -> strings.aspectFill
    VideoAspectMode.FIXED_WIDTH -> strings.aspectWidth
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
