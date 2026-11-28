package com.tiendatv.player.player

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.EpgProgramEntity
import com.tiendatv.player.data.local.MediaItemEntity
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
    liveCategories: List<CategoryEntity> = emptyList(),
    selectedLiveCategoryId: String = "",
    liveChannels: List<MediaItemEntity> = emptyList(),
    currentPrograms: Map<String, EpgProgramEntity> = emptyMap(),
    onSelectLiveCategory: (String) -> Unit = {},
    onSelectLiveChannel: (MediaItemEntity) -> Unit = {},
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember(settings.bufferPreset, settings.preferSoftwareDecoder, settings.language) {
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
    var controlsVisible by remember { mutableStateOf(false) }
    var showChannelOverlay by remember { mutableStateOf(false) }
    var showZapBanner by remember { mutableStateOf(false) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showToolsDrawer by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    val playPauseFocus = remember { FocusRequester() }
    val audioButtonFocus = remember { FocusRequester() }
    val overlayFocus = remember { FocusRequester() }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen awake, pause on Home/background, and ALWAYS release this controller on dispose
    DisposableEffect(controller, lifecycleOwner) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                controller.pauseForBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.release()
        }
    }

    LaunchedEffect(request) {
        controller.play(request)
        if (request.isLive) {
            if (!showChannelOverlay) {
                controlsVisible = false
                showZapBanner = true
            }
        } else {
            controlsVisible = true
            showZapBanner = false
        }
        lastInteractionMs = System.currentTimeMillis()
    }

    // Minimal 3-second Zap Info Banner auto-hide
    LaunchedEffect(showZapBanner, request.uid) {
        if (showZapBanner) {
            delay(3_200L)
            showZapBanner = false
        }
    }

    // Auto-hide full HUD controls after 6 seconds of inactivity while playing
    LaunchedEffect(controlsVisible, lastInteractionMs, state.isPlaying, showAudioDialog, showSubtitleDialog, showChannelOverlay) {
        if (controlsVisible && state.isPlaying && !showAudioDialog && !showSubtitleDialog && !showChannelOverlay) {
            delay(6_000L)
            controlsVisible = false
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible && !showChannelOverlay) {
            try {
                playPauseFocus.requestFocus()
            } catch (_: Exception) {
            }
        } else if (!controlsVisible) {
            showToolsDrawer = false
        }
    }

    LaunchedEffect(showToolsDrawer) {
        if (showToolsDrawer && controlsVisible) {
            delay(60L)
            try {
                audioButtonFocus.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(showChannelOverlay) {
        if (showChannelOverlay) {
            controlsVisible = false
            showZapBanner = false
            delay(80L)
            try {
                overlayFocus.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    // TiviMate-style Back behavior:
    // 1st Back on Live TV opens the translucent channel browser overlay while video keeps playing!
    // 2nd Back (or Back on VOD) exits the player.
    BackHandler {
        when {
            showAudioDialog -> showAudioDialog = false
            showSubtitleDialog -> showSubtitleDialog = false
            showChannelOverlay -> onClose()
            request.isLive && liveChannels.isNotEmpty() -> {
                controlsVisible = false
                showZapBanner = false
                showChannelOverlay = true
            }
            else -> onClose()
        }
    }

    fun pokeControls() {
        showZapBanner = false
        controlsVisible = true
        lastInteractionMs = System.currentTimeMillis()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (showChannelOverlay) {
                        if (event.key == Key.DirectionRight && !controlsVisible) {
                            // Allow normal D-pad navigation inside overlay; only handled by overlay items
                            false
                        } else {
                            false
                        }
                    } else {
                        when (event.key) {
                            Key.MediaPlayPause, Key.Spacebar -> {
                                pokeControls()
                                controller.togglePlayPause()
                                true
                            }
                            Key.ChannelUp -> {
                                if (request.isLive && hasNextChannel) {
                                    controlsVisible = false
                                    showZapBanner = true
                                    onNextChannel()
                                    true
                                } else false
                            }
                            Key.ChannelDown -> {
                                if (request.isLive && hasPrevChannel) {
                                    controlsVisible = false
                                    showZapBanner = true
                                    onPrevChannel()
                                    true
                                } else false
                            }
                            Key.DirectionUp -> {
                                if (!controlsVisible && request.isLive && hasNextChannel) {
                                    showZapBanner = true
                                    onNextChannel()
                                    true
                                } else {
                                    if (!controlsVisible) {
                                        pokeControls()
                                        true
                                    } else {
                                        lastInteractionMs = System.currentTimeMillis()
                                        false
                                    }
                                }
                            }
                            Key.DirectionDown -> {
                                if (!controlsVisible && request.isLive && hasPrevChannel) {
                                    showZapBanner = true
                                    onPrevChannel()
                                    true
                                } else {
                                    if (!controlsVisible) {
                                        pokeControls()
                                        true
                                    } else {
                                        lastInteractionMs = System.currentTimeMillis()
                                        false
                                    }
                                }
                            }
                            Key.DirectionLeft -> {
                                if (!controlsVisible && request.isLive && liveChannels.isNotEmpty()) {
                                    showZapBanner = false
                                    showChannelOverlay = true
                                    true
                                } else if (!controlsVisible && !request.isLive) {
                                    pokeControls()
                                    controller.seekBy(-15_000L)
                                    true
                                } else {
                                    lastInteractionMs = System.currentTimeMillis()
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                if (!controlsVisible && !request.isLive) {
                                    pokeControls()
                                    controller.seekBy(15_000L)
                                    true
                                } else {
                                    lastInteractionMs = System.currentTimeMillis()
                                    false
                                }
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (!controlsVisible) {
                                    pokeControls()
                                    true
                                } else {
                                    lastInteractionMs = System.currentTimeMillis()
                                    false
                                }
                            }
                            else -> {
                                if (controlsVisible) lastInteractionMs = System.currentTimeMillis()
                                false
                            }
                        }
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (showChannelOverlay) {
                    showChannelOverlay = false
                } else {
                    controlsVisible = !controlsVisible
                    if (controlsVisible) {
                        showZapBanner = false
                        lastInteractionMs = System.currentTimeMillis()
                    }
                }
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
                    .background(Color(0xCC0B0D11))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF5A1F),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(34.dp)
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
                    .background(Color(0xE60B0D11))
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

        // Minimal 3-second Zen Zap Info Banner (when flipping live channels with Up/Down)
        AnimatedVisibility(
            visible = showZapBanner && !controlsVisible && !showChannelOverlay && state.errorMessage == null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(350)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xD90B0D11))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5A1F))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = epgNowTitle?.takeIf { it.isNotBlank() } ?: strings.liveBadge
                    Text(
                        text = sub,
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.videoInfoBadge.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = state.videoInfoBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // TiviMate-Style Translucent Live Channels & Categories Overlay (Browse While Watching!)
        AnimatedVisibility(
            visible = showChannelOverlay && request.isLive,
            enter = slideInHorizontally(tween(220)) { -it / 2 } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(180)) { -it / 2 } + fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEC0B0D11),
                                Color(0xCC0B0D11),
                                Color(0x330B0D11),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                // Column 1: Categories
                Column(
                    modifier = Modifier
                        .width(210.dp)
                        .fillMaxHeight()
                        .background(Color(0xD90B0D11))
                        .padding(top = 20.dp, bottom = 16.dp, start = 12.dp, end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.categoriesHeader.uppercase(),
                        color = Color(0xAAFFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item(key = "overlay_all") {
                            OverlayCategoryRow(
                                label = strings.allCategories,
                                selected = selectedLiveCategoryId.isBlank(),
                                onClick = { onSelectLiveCategory("") }
                            )
                        }
                        items(liveCategories, key = { it.uid }) { cat ->
                            OverlayCategoryRow(
                                label = cat.name,
                                selected = cat.categoryId == selectedLiveCategoryId,
                                onClick = { onSelectLiveCategory(cat.categoryId) }
                            )
                        }
                    }

                    PlayerActionPill(
                        label = strings.exitPlayer,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = onClose
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0x26FFFFFF))
                )

                // Column 2: Channels
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(Color(0xBF0E1118))
                        .padding(top = 20.dp, bottom = 16.dp, start = 10.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.channelsHeader.uppercase(),
                            color = Color(0xAAFFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "${liveChannels.size}",
                            color = Color(0x88FFFFFF),
                            fontSize = 11.sp
                        )
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        items(liveChannels, key = { it.uid }) { ch ->
                            val isPlayingNow = ch.uid == request.uid
                            val key = ch.epgChannelId.ifBlank { ch.streamId }
                            val prog = currentPrograms[key]
                            val shape = RoundedCornerShape(12.dp)
                            val itemMod = if (isPlayingNow) {
                                Modifier.focusRequester(overlayFocus)
                            } else {
                                Modifier
                            }

                            Row(
                                modifier = itemMod
                                    .fillMaxWidth()
                                    .tvFocusable(
                                        shape = shape,
                                        focusScale = 1.02f,
                                        focusBorderColor = Color(0xFFFF5A1F),
                                        onClick = {
                                            if (isPlayingNow) {
                                                showChannelOverlay = false
                                            } else {
                                                onSelectLiveChannel(ch)
                                            }
                                        }
                                    )
                                    .clip(shape)
                                    .background(
                                        if (isPlayingNow) Color(0x44FF5A1F) else Color(0x18FFFFFF)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x550B0D11)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ch.logoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ch.logoUrl,
                                            contentDescription = ch.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${ch.channelNumber}",
                                            color = Color(0xCCFFFFFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.name,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isPlayingNow) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = prog?.title ?: strings.epgUnavailable,
                                        color = Color(0xAAFFFFFF),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Transparent tap area on the right to return to fullscreen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showChannelOverlay = false }
                )
            }
        }

        // Sleek tvOS-inspired Full HUD Overlay
        AnimatedVisibility(
            visible = controlsVisible && !showChannelOverlay && !showAudioDialog && !showSubtitleDialog && state.errorMessage == null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
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
                                Color(0xD9000000)
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
                        if (liveChannels.isNotEmpty()) {
                            PlayerActionPill(
                                label = strings.channelsHeader,
                                icon = Icons.Rounded.LiveTv,
                                onClick = {
                                    controlsVisible = false
                                    showChannelOverlay = true
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
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

                // Bottom Controls: D-pad Focusable Seekbar (VOD) + Transport + Audio/Subtitles/Aspect
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    if (!request.isLive && state.durationMs > 0L) {
                        TvTimelineScrubber(
                            positionMs = state.currentPositionMs,
                            durationMs = state.durationMs,
                            scrubHint = strings.scrubHint,
                            onSeekTo = { targetMs ->
                                pokeControls()
                                controller.seekTo(targetMs)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Centered Playback Controls (TiviMate style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && !showToolsDrawer) {
                                    pokeControls()
                                    showToolsDrawer = true
                                    true
                                } else {
                                    false
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally)
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

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtle chevron indicator; D-pad Down reveals Audio / Subtitles / Aspect tools
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusScale = 1.1f,
                                    focusBorderColor = Color.White,
                                    onClick = {
                                        pokeControls()
                                        showToolsDrawer = !showToolsDrawer
                                    }
                                )
                                .clip(CircleShape)
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showToolsDrawer) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xBBFFFFFF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showToolsDrawer,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(140))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                        pokeControls()
                                        showToolsDrawer = false
                                        try {
                                            playPauseFocus.requestFocus()
                                        } catch (_: Exception) {
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            PlayerActionPill(
                                label = strings.audioTracks,
                                icon = Icons.Rounded.Audiotrack,
                                modifier = Modifier.focusRequester(audioButtonFocus),
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

/**
 * D-pad focusable + touch-friendly movie/series timeline scrubber.
 * Move D-pad onto the bar and press/hold Left or Right to scrub smoothly forward or backward.
 */
@Composable
private fun TvTimelineScrubber(
    positionMs: Long,
    durationMs: Long,
    scrubHint: String,
    onSeekTo: (Long) -> Unit
) {
    var isBarFocused by remember { mutableStateOf(false) }
    var repeatStreak by remember { mutableIntStateOf(0) }
    var lastKeyMs by remember { mutableLongStateOf(0L) }
    val barPadding by animateDpAsState(
        targetValue = if (isBarFocused) 10.dp else 4.dp,
        label = "scrubberPad"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isBarFocused) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scrubHint,
                    color = Color(0xFFFF7A45),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isBarFocused) Color(0x44FF5A1F) else Color.Transparent)
                .border(
                    width = if (isBarFocused) 2.dp else 0.dp,
                    color = if (isBarFocused) Color(0xFFFF5A1F) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged {
                    isBarFocused = it.isFocused
                    if (!it.isFocused) repeatStreak = 0
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                    ) {
                        val now = System.currentTimeMillis()
                        repeatStreak = if (now - lastKeyMs < 450L) (repeatStreak + 1).coerceAtMost(30) else 1
                        lastKeyMs = now
                        val stepMs = when {
                            repeatStreak >= 12 -> 60_000L
                            repeatStreak >= 5 -> 30_000L
                            else -> 15_000L
                        }
                        val delta = if (event.key == Key.DirectionRight) stepMs else -stepMs
                        val next = (positionMs + delta).coerceIn(0L, durationMs)
                        onSeekTo(next)
                        true
                    } else {
                        false
                    }
                }
                .focusable()
                .padding(horizontal = 10.dp, vertical = barPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(positionMs),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isBarFocused) FontWeight.Bold else FontWeight.Normal
            )
            Slider(
                value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF5A1F),
                    activeTrackColor = Color(0xFFFF5A1F),
                    inactiveTrackColor = Color(0x55FFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            Text(
                text = formatDuration(durationMs),
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun OverlayCategoryRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = shape,
                focusScale = 1.02f,
                focusBorderColor = Color(0xFFFF5A1F),
                onClick = onClick
            )
            .clip(shape)
            .background(if (selected) Color(0x44FF5A1F) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
            contentDescription = null,
            tint = if (selected) Color(0xFFFF7A45) else Color(0xAAFFFFFF),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xCCFFFFFF),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
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
                focusBorderColor = if (emphasized) Color.White else Color(0xFFFF7A45),
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(tracks, includeOffOption) {
        delay(60L)
        try {
            firstFocus.requestFocus()
        } catch (_: Exception) {
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xF50B0D11))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (tracks.isEmpty() && !includeOffOption) {
                Text(text = emptyLabel, color = Color(0xAAFFFFFF), fontSize = 14.sp)
            } else {
                val selectedIdx = tracks.indexOfFirst { it.isSelected }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (includeOffOption) {
                        item(key = "track_off") {
                            val noneSelected = tracks.none { it.isSelected }
                            val mod = if (noneSelected) Modifier.focusRequester(firstFocus) else Modifier
                            TrackRowItem(
                                label = offLabel,
                                selected = noneSelected,
                                modifier = mod,
                                onClick = { onSelect(null) }
                            )
                        }
                    }
                    items(tracks.size, key = { tracks[it].id }) { idx ->
                        val track = tracks[idx]
                        val shouldFocus = if (selectedIdx >= 0) idx == selectedIdx else (!includeOffOption && idx == 0)
                        val mod = if (shouldFocus) Modifier.focusRequester(firstFocus) else Modifier
                        TrackRowItem(
                            label = track.label,
                            selected = track.isSelected,
                            modifier = mod,
                            onClick = { onSelect(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRowItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusBorderColor = if (selected) Color.White else Color(0xFFFF7A45),
                onClick = onClick
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFF5A1F) else Color(0x26FFFFFF))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
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
