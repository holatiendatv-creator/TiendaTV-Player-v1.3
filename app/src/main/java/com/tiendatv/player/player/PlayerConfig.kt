package com.tiendatv.player.player

import androidx.annotation.OptIn
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

enum class BufferPreset(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
) {
    LOW(
        minBufferMs = 4_000,
        maxBufferMs = 12_000,
        bufferForPlaybackMs = 1_000,
        bufferForPlaybackAfterRebufferMs = 2_000
    ),
    BALANCED(
        minBufferMs = 12_000,
        maxBufferMs = 35_000,
        bufferForPlaybackMs = 2_000,
        bufferForPlaybackAfterRebufferMs = 4_000
    ),
    HIGH(
        minBufferMs = 25_000,
        maxBufferMs = 60_000,
        bufferForPlaybackMs = 3_500,
        bufferForPlaybackAfterRebufferMs = 6_000
    ),
    MAX(
        minBufferMs = 40_000,
        maxBufferMs = 90_000,
        bufferForPlaybackMs = 5_000,
        bufferForPlaybackAfterRebufferMs = 8_000
    );

    companion object {
        fun fromName(name: String?): BufferPreset =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BALANCED
    }
}

enum class VideoAspectMode {
    FIT,
    ZOOM,
    FILL,
    FIXED_WIDTH;

    @OptIn(UnstableApi::class)
    fun toMedia3ResizeMode(): Int = when (this) {
        FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    }

    fun next(): VideoAspectMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromName(name: String?): VideoAspectMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FIT
    }
}

data class PlaybackRequest(
    val uid: String,
    val mediaUid: String,
    val type: String,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String = "",
    val streamUrl: String,
    val resumePositionMs: Long = 0L,
    val isLive: Boolean = false
)

data class TrackOption(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean,
    val trackGroup: TrackGroup?,
    val trackIndex: Int
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isEnded: Boolean = false,
    val isReconnecting: Boolean = false,
    val reconnectAttempt: Int = 0,
    val maxReconnectAttempts: Int = 5,
    val errorMessage: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val aspectMode: VideoAspectMode = VideoAspectMode.FIT,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val videoInfoBadge: String = ""
)
