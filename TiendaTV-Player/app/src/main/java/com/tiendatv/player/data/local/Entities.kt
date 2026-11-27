package com.tiendatv.player.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object MediaType {
    const val LIVE = "LIVE"
    const val MOVIE = "MOVIE"
    const val SERIES = "SERIES"
}

object ProfileType {
    const val XTREAM = "XTREAM"
    const val M3U = "M3U"
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String,
    val serverUrl: String,
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    val isActive: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val expirationDate: String = ""
)

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["profileId", "type", "sortOrder"])
    ]
)
data class CategoryEntity(
    @PrimaryKey val uid: String,
    val profileId: Long,
    val categoryId: String,
    val name: String,
    val type: String,
    val isAdult: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["profileId", "type", "categoryId"]),
        Index(value = ["profileId", "type", "name"]),
        Index(value = ["profileId", "type", "addedTimestamp"]),
        Index(value = ["profileId", "type", "rating"]),
        Index(value = ["profileId", "epgChannelId"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey val uid: String,
    val profileId: Long,
    val streamId: String,
    val type: String,
    val name: String,
    val categoryId: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val backdropUrl: String = "",
    val containerExtension: String = "",
    val epgChannelId: String = "",
    val rating: Double = 0.0,
    val year: String = "",
    val plot: String = "",
    val addedTimestamp: Long = 0L,
    val channelNumber: Int = 0,
    val isAdult: Boolean = false
)

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["profileId", "type", "addedAt"])]
)
data class FavoriteEntity(
    @PrimaryKey val uid: String,
    val profileId: Long,
    val type: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["profileId", "lastWatchedAt"])]
)
data class WatchHistoryEntity(
    @PrimaryKey val uid: String,
    val profileId: Long,
    val mediaUid: String,
    val type: String,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String = "",
    val streamUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

@Entity(
    tableName = "epg_programs",
    indices = [Index(value = ["profileId", "channelId", "startMs", "endMs"])]
)
data class EpgProgramEntity(
    @PrimaryKey val id: String,
    val profileId: Long,
    val channelId: String,
    val title: String,
    val description: String = "",
    val startMs: Long,
    val endMs: Long
)
