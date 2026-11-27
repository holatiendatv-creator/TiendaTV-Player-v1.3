package com.tiendatv.player.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY isActive DESC, id ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query("UPDATE profiles SET lastSyncedAt = :timestamp, expirationDate = :expiration WHERE id = :id")
    suspend fun updateSyncInfo(id: Long, timestamp: Long, expiration: String)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT * FROM categories
        WHERE profileId = :profileId AND type = :type
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY sortOrder ASC, name ASC
        """
    )
    suspend fun getCategories(profileId: Long, type: String, includeAdult: Boolean): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE profileId = :profileId AND type = :type")
    suspend fun deleteByProfileAndType(profileId: Long, type: String)

    @Query("DELETE FROM categories WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE profileId = :profileId AND type = :type")
    suspend fun deleteByProfileAndType(profileId: Long, type: String)

    @Query("DELETE FROM media_items WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)

    @Query("SELECT COUNT(*) FROM media_items WHERE profileId = :profileId AND type = :type AND (:includeAdult = 1 OR isAdult = 0)")
    suspend fun countByType(profileId: Long, type: String, includeAdult: Boolean): Int

    @Query(
        """
        SELECT COUNT(*) FROM media_items
        WHERE profileId = :profileId
          AND type = :type
          AND (:categoryId = '' OR categoryId = :categoryId)
          AND (:query = '' OR name LIKE '%' || :query || '%')
          AND (:includeAdult = 1 OR isAdult = 0)
        """
    )
    suspend fun countFiltered(
        profileId: Long,
        type: String,
        categoryId: String,
        query: String,
        includeAdult: Boolean
    ): Int

    @Query(
        """
        SELECT * FROM media_items
        WHERE profileId = :profileId
          AND type = :type
          AND (:categoryId = '' OR categoryId = :categoryId)
          AND (:query = '' OR name LIKE '%' || :query || '%')
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY
          CASE WHEN :sortMode = 'RECENT' THEN addedTimestamp END DESC,
          CASE WHEN :sortMode = 'RATING' THEN rating END DESC,
          CASE WHEN :sortMode = 'AZ' THEN name END ASC,
          CASE WHEN :sortMode = 'ZA' THEN name END DESC,
          channelNumber ASC,
          name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryCatalog(
        profileId: Long,
        type: String,
        categoryId: String,
        query: String,
        sortMode: String,
        includeAdult: Boolean,
        limit: Int,
        offset: Int
    ): List<MediaItemEntity>

    @Query(
        """
        SELECT * FROM media_items
        WHERE profileId = :profileId
          AND (:type = '' OR type = :type)
          AND name LIKE '%' || :query || '%'
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY rating DESC, addedTimestamp DESC, name ASC
        LIMIT :limit
        """
    )
    suspend fun searchGlobal(
        profileId: Long,
        type: String,
        query: String,
        includeAdult: Boolean,
        limit: Int = 100
    ): List<MediaItemEntity>

    @Query(
        """
        SELECT * FROM media_items
        WHERE profileId = :profileId
          AND type = :type
          AND (:includeAdult = 1 OR isAdult = 0)
        ORDER BY addedTimestamp DESC, rating DESC
        LIMIT :limit
        """
    )
    suspend fun getFeatured(profileId: Long, type: String, includeAdult: Boolean, limit: Int = 20): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): MediaItemEntity?

    @Query(
        """
        SELECT m.* FROM media_items m
        INNER JOIN favorites f ON m.uid = f.uid
        WHERE f.profileId = :profileId
          AND (:type = '' OR m.type = :type)
          AND (:includeAdult = 1 OR m.isAdult = 0)
        ORDER BY f.addedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteItems(
        profileId: Long,
        type: String = "",
        includeAdult: Boolean = false,
        limit: Int = 100
    ): List<MediaItemEntity>
}

@Dao
interface FavoriteDao {
    @Query("SELECT uid FROM favorites WHERE profileId = :profileId")
    fun observeFavoriteUids(profileId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorites WHERE profileId = :profileId")
    suspend fun countFavorites(profileId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uid = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM favorites WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}

@Dao
interface WatchHistoryDao {
    @Query(
        """
        SELECT * FROM watch_history
        WHERE profileId = :profileId
          AND durationMs > 0
          AND positionMs > 15000
          AND (CAST(positionMs AS REAL) / CAST(durationMs AS REAL)) < 0.94
        ORDER BY lastWatchedAt DESC
        LIMIT :limit
        """
    )
    fun observeContinueWatching(profileId: Long, limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Query(
        """
        SELECT * FROM watch_history
        WHERE profileId = :profileId
        ORDER BY lastWatchedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyWatched(profileId: Long, limit: Int = 25): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearProfileHistory(profileId: Long)

    @Query("DELETE FROM watch_history WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

@Dao
interface EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(programs: List<EpgProgramEntity>)

    @Query(
        """
        SELECT * FROM epg_programs
        WHERE profileId = :profileId
          AND channelId = :channelId
          AND endMs > :nowMs
        ORDER BY startMs ASC
        LIMIT :limit
        """
    )
    suspend fun getUpcomingPrograms(
        profileId: Long,
        channelId: String,
        nowMs: Long,
        limit: Int = 6
    ): List<EpgProgramEntity>

    @Query(
        """
        SELECT * FROM epg_programs
        WHERE profileId = :profileId
          AND channelId IN (:channelIds)
          AND startMs <= :nowMs AND endMs > :nowMs
        """
    )
    suspend fun getCurrentProgramsForChannels(
        profileId: Long,
        channelIds: List<String>,
        nowMs: Long
    ): List<EpgProgramEntity>

    @Query("DELETE FROM epg_programs WHERE profileId = :profileId AND endMs < :cutoffMs")
    suspend fun deleteExpired(profileId: Long, cutoffMs: Long)

    @Query("DELETE FROM epg_programs WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
