package com.tiendatv.player.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.AppLanguage
import com.tiendatv.player.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Wraps AndroidX Media3 ExoPlayer with:
 * - Hardware-accelerated decoding + automatic software decoder fallback
 * - Configurable buffer presets (Low / Balanced / High / Max)
 * - Automatic preferred audio track selection (Spanish default vs. English mode)
 * - Exponential-backoff automatic reconnect on dropped IPTV/VOD streams
 * - Live Audio & Subtitle track inspection and switching
 * - Periodic watch-progress persistence
 */
@OptIn(UnstableApi::class)
class TiendaPlayerController(
    context: Context,
    private val scope: CoroutineScope,
    private val bufferPreset: BufferPreset,
    private val preferSoftwareDecoder: Boolean,
    private val preferredLanguage: AppLanguage,
    initialAspectMode: VideoAspectMode,
    private val onProgressPersist: (positionMs: Long, durationMs: Long) -> Unit
) {
    private val _uiState = MutableStateFlow(
        PlayerUiState(
            aspectMode = initialAspectMode,
            maxReconnectAttempts = AppConfig.MAX_PLAYER_RETRIES
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val trackSelector = DefaultTrackSelector(context)
    val exoPlayer: ExoPlayer

    private var currentRequest: PlaybackRequest? = null
    private var retryCount = 0
    private var retryJob: Job? = null
    private var tickerJob: Job? = null
    private var lastSavedAtMs = 0L
    private var appliedAutoAudioForCurrentItem = false

    init {
        val preferredAudioCodes = when (preferredLanguage) {
            AppLanguage.EN -> arrayOf("en", "eng", "en-US", "en-GB")
            AppLanguage.ES -> arrayOf("es", "spa", "es-419", "es-MX", "es-ES")
        }

        trackSelector.parameters = trackSelector.buildUponParameters()
            .setPreferredAudioLanguages(*preferredAudioCodes)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                if (preferSoftwareDecoder) {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                } else {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                }
            )

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferPreset.minBufferMs,
                bufferPreset.maxBufferMs,
                bufferPreset.bufferForPlaybackMs,
                bufferPreset.bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(NetworkModule.streamClient)
            .setUserAgent(AppConfig.USER_AGENT)

        exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

        exoPlayer.playWhenReady = true
        exoPlayer.addListener(playerListener)
        startPositionTicker()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update { it.copy(isBuffering = true) }
                }
                Player.STATE_READY -> {
                    retryCount = 0
                    retryJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            isReconnecting = false,
                            reconnectAttempt = 0,
                            errorMessage = null,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
                    refreshTracks(exoPlayer.currentTracks)
                }
                Player.STATE_ENDED -> {
                    val req = currentRequest
                    if (req != null && req.isLive) {
                        scheduleReconnect("Stream ended unexpectedly")
                    } else {
                        _uiState.update { it.copy(isBuffering = false, isPlaying = false) }
                        persistProgressNow()
                    }
                }
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (!appliedAutoAudioForCurrentItem) {
                applyLabelFallbackAudioPreference(tracks)
            }
            refreshTracks(tracks)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                val badge = when {
                    videoSize.height >= 2160 -> "4K UHD"
                    videoSize.height >= 1080 -> "1080p HD"
                    videoSize.height >= 720 -> "720p HD"
                    else -> "${videoSize.height}p"
                }
                _uiState.update { it.copy(videoInfoBadge = badge) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            scheduleReconnect(error.errorCodeName + ": " + (error.localizedMessage ?: "Playback error"))
        }
    }

    /**
     * Many IPTV movies/series label audio tracks by name ("English", "Latino", "Español")
     * without standard ISO language tags. This zero-overhead check runs once per item and
     * automatically picks the matching track if ExoPlayer's ISO selector couldn't match it.
     */
    private fun applyLabelFallbackAudioPreference(tracks: Tracks) {
        val keywords = when (preferredLanguage) {
            AppLanguage.EN -> listOf("en", "eng", "english", "inglés", "ingles", "usa", "original")
            AppLanguage.ES -> listOf("es", "spa", "español", "espanol", "latino", "lat", "castellano", "spanish")
        }
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (audioGroups.isEmpty()) return

        appliedAutoAudioForCurrentItem = true
        for (group in audioGroups) {
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val fmt = group.getTrackFormat(i)
                val combined = "${fmt.language.orEmpty()} ${fmt.label.orEmpty()}".lowercase(Locale.ROOT)
                if (keywords.any { kw -> combined.contains(kw) }) {
                    if (!group.isTrackSelected(i)) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(override)
                            .build()
                    }
                    return
                }
            }
        }
    }

    fun play(request: PlaybackRequest) {
        currentRequest = request
        retryCount = 0
        appliedAutoAudioForCurrentItem = false
        retryJob?.cancel()
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .build()
        _uiState.update {
            it.copy(
                isBuffering = true,
                isReconnecting = false,
                reconnectAttempt = 0,
                errorMessage = null,
                videoInfoBadge = ""
            )
        }

        val mediaItem = MediaItem.Builder()
            .setUri(request.streamUrl)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (!request.isLive && request.resumePositionMs > 5_000L) {
            exoPlayer.seekTo(request.resumePositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    fun retryManual() {
        val req = currentRequest ?: return
        val resumeAt = if (req.isLive) 0L else exoPlayer.currentPosition.coerceAtLeast(req.resumePositionMs)
        play(req.copy(resumePositionMs = resumeAt))
    }

    private fun scheduleReconnect(reason: String) {
        val req = currentRequest ?: return
        if (retryCount >= AppConfig.MAX_PLAYER_RETRIES) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    isReconnecting = false,
                    errorMessage = reason
                )
            }
            return
        }
        retryCount++
        val delayMs = when (retryCount) {
            1 -> 1_500L
            2 -> 3_000L
            3 -> 5_000L
            4 -> 8_000L
            else -> 12_000L
        }
        _uiState.update {
            it.copy(
                isBuffering = true,
                isReconnecting = true,
                reconnectAttempt = retryCount,
                errorMessage = null
            )
        }
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delayMs)
            val resumeAt = if (req.isLive) 0L else exoPlayer.currentPosition.coerceAtLeast(req.resumePositionMs)
            val mediaItem = MediaItem.Builder().setUri(req.streamUrl).build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (!req.isLive && resumeAt > 5_000L) {
                exoPlayer.seekTo(resumeAt)
            }
            exoPlayer.playWhenReady = true
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            persistProgressNow()
        } else {
            exoPlayer.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        if (currentRequest?.isLive == true) return
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, duration.coerceAtLeast(1L))
        exoPlayer.seekTo(target)
        _uiState.update { it.copy(currentPositionMs = target) }
    }

    fun seekTo(positionMs: Long) {
        if (currentRequest?.isLive == true) return
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val target = positionMs.coerceIn(0L, duration.coerceAtLeast(1L))
        exoPlayer.seekTo(target)
        _uiState.update { it.copy(currentPositionMs = target) }
    }

    fun cycleAspectMode(): VideoAspectMode {
        val next = _uiState.value.aspectMode.next()
        _uiState.update { it.copy(aspectMode = next) }
        return next
    }

    fun selectAudioTrack(option: TrackOption) {
        val group = option.trackGroup ?: return
        val override = TrackSelectionOverride(group, option.trackIndex)
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
        refreshTracks(exoPlayer.currentTracks)
    }

    fun selectSubtitleTrack(option: TrackOption?) {
        if (option == null || option.trackGroup == null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val override = TrackSelectionOverride(option.trackGroup, option.trackIndex)
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
        }
        refreshTracks(exoPlayer.currentTracks)
    }

    private fun refreshTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackOption>()
        val subtitleList = mutableListOf<TrackOption>()

        tracks.groups.forEachIndexed { gIdx, group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    if (group.isTrackSupported(i)) {
                        val fmt = group.getTrackFormat(i)
                        val lang = fmt.language?.uppercase() ?: "UND"
                        val channels = if (fmt.channelCount > 2) " ${fmt.channelCount}.0" else ""
                        val label = fmt.label?.takeIf { it.isNotBlank() } ?: "$lang$channels"
                        audioList.add(
                            TrackOption(
                                id = "a_${gIdx}_$i",
                                label = label,
                                language = lang,
                                isSelected = group.isTrackSelected(i),
                                trackGroup = group.mediaTrackGroup,
                                trackIndex = i
                            )
                        )
                    }
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    if (group.isTrackSupported(i)) {
                        val fmt = group.getTrackFormat(i)
                        val lang = fmt.language?.uppercase() ?: "SUB"
                        val label = fmt.label?.takeIf { it.isNotBlank() } ?: lang
                        subtitleList.add(
                            TrackOption(
                                id = "s_${gIdx}_$i",
                                label = label,
                                language = lang,
                                isSelected = group.isTrackSelected(i),
                                trackGroup = group.mediaTrackGroup,
                                trackIndex = i
                            )
                        )
                    }
                }
            }
        }

        _uiState.update {
            it.copy(audioTracks = audioList, subtitleTracks = subtitleList)
        }
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                _uiState.update {
                    it.copy(currentPositionMs = pos, durationMs = dur)
                }
                val now = System.currentTimeMillis()
                if (exoPlayer.isPlaying && now - lastSavedAtMs >= 10_000L) {
                    lastSavedAtMs = now
                    onProgressPersist(pos, dur)
                }
                delay(1_000L)
            }
        }
    }

    fun persistProgressNow() {
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.coerceAtLeast(0L)
        onProgressPersist(pos, dur)
    }

    fun release() {
        persistProgressNow()
        retryJob?.cancel()
        tickerJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
