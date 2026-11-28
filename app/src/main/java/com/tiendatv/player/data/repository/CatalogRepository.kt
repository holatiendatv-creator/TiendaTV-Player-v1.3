package com.tiendatv.player.data.repository

import androidx.room.withTransaction
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.EpgProgramEntity
import com.tiendatv.player.data.local.FavoriteEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.MediaType
import com.tiendatv.player.data.local.ProfileEntity
import com.tiendatv.player.data.local.ProfileType
import com.tiendatv.player.data.local.TiendaTvDatabase
import com.tiendatv.player.data.local.WatchHistoryEntity
import com.tiendatv.player.data.network.EpgParser
import com.tiendatv.player.data.network.M3uParser
import com.tiendatv.player.data.network.SeriesDetails
import com.tiendatv.player.data.network.XtreamApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

enum class SortMode {
    RECENT,
    AZ,
    ZA,
    RATING
}

data class CatalogCounts(
    val liveCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val favoriteCount: Int = 0
)

data class SyncProgress(
    val isSyncing: Boolean = false,
    val stage: String = "",
    val progressFraction: Float = 0f,
    val error: String? = null
)

class CatalogRepository(
    private val db: TiendaTvDatabase,
    private val xtreamClient: XtreamApiClient = XtreamApiClient(),
    private val m3uParser: M3uParser = M3uParser(),
    private val epgParser: EpgParser = EpgParser()
) {
    private val profileDao = db.profileDao()
    private val categoryDao = db.categoryDao()
    private val mediaDao = db.mediaItemDao()
    private val favoriteDao = db.favoriteDao()
    private val historyDao = db.watchHistoryDao()
    private val epgDao = db.epgDao()

    fun observeProfiles(): Flow<List<ProfileEntity>> = profileDao.observeAll()
    fun observeActiveProfile(): Flow<ProfileEntity?> = profileDao.observeActive()
    fun observeFavoriteUids(profileId: Long): Flow<List<String>> = favoriteDao.observeFavoriteUids(profileId)
    fun observeContinueWatching(profileId: Long): Flow<List<WatchHistoryEntity>> =
        historyDao.observeContinueWatching(profileId)
    fun observeRecentlyWatched(profileId: Long): Flow<List<WatchHistoryEntity>> =
        historyDao.observeRecentlyWatched(profileId)

    suspend fun addXtreamProfile(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<ProfileEntity> = withContext(Dispatchers.IO) {
        val normalized = xtreamClient.normalizeServerUrl(serverUrl)
        val auth = xtreamClient.authenticate(normalized, username, password)
        if (!auth.isAuthenticated) {
            return@withContext Result.failure(
                IllegalStateException(auth.errorMessage ?: "Authentication failed")
            )
        }
        val profileId = db.withTransaction {
            profileDao.deactivateAll()
            profileDao.insert(
                ProfileEntity(
                    name = name.trim().ifBlank { username.trim() },
                    type = ProfileType.XTREAM,
                    serverUrl = normalized,
                    username = username.trim(),
                    password = password.trim(),
                    epgUrl = xtreamClient.buildXmltvUrl(normalized, username.trim(), password.trim()),
                    isActive = true,
                    expirationDate = auth.expirationDate
                )
            )
        }
        val saved = profileDao.getById(profileId)
            ?: return@withContext Result.failure(IllegalStateException("Failed to save profile"))
        Result.success(saved)
    }

    suspend fun addM3uProfile(
        name: String,
        playlistUrl: String,
        epgUrl: String
    ): Result<ProfileEntity> = withContext(Dispatchers.IO) {
        val cleanUrl = playlistUrl.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext Result.failure(IllegalArgumentException("Playlist URL must start with http:// or https://"))
        }
        val profileId = db.withTransaction {
            profileDao.deactivateAll()
            profileDao.insert(
                ProfileEntity(
                    name = name.trim().ifBlank { "M3U Playlist" },
                    type = ProfileType.M3U,
                    serverUrl = cleanUrl,
                    epgUrl = epgUrl.trim(),
                    isActive = true
                )
            )
        }
        val saved = profileDao.getById(profileId)
            ?: return@withContext Result.failure(IllegalStateException("Failed to save profile"))
        Result.success(saved)
    }

    suspend fun switchProfile(profileId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            profileDao.deactivateAll()
            profileDao.activate(profileId)
        }
    }

    suspend fun deleteProfile(profileId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            mediaDao.deleteByProfile(profileId)
            categoryDao.deleteByProfile(profileId)
            favoriteDao.deleteByProfile(profileId)
            historyDao.clearProfileHistory(profileId)
            epgDao.deleteByProfile(profileId)
            profileDao.deleteById(profileId)
        }
    }

    suspend fun syncProfileCatalog(
        profile: ProfileEntity,
        onProgress: (stage: String, fraction: Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (profile.type == ProfileType.XTREAM) {
                onProgress("LIVE", 0.10f)
                syncXtreamType(profile, MediaType.LIVE)
                onProgress("MOVIE", 0.45f)
                syncXtreamType(profile, MediaType.MOVIE)
                onProgress("SERIES", 0.80f)
                syncXtreamType(profile, MediaType.SERIES)
                profileDao.updateSyncInfo(profile.id, System.currentTimeMillis(), profile.expirationDate)
            } else {
                onProgress("M3U", 0.20f)
                categoryDao.deleteByProfile(profile.id)
                mediaDao.deleteByProfile(profile.id)
                val summary = m3uParser.parseUrl(
                    profileId = profile.id,
                    playlistUrl = profile.serverUrl,
                    onCategoriesReady = { cats -> categoryDao.insertAll(cats) },
                    onMediaBatch = { batch -> mediaDao.insertBatch(batch) }
                )
                val finalEpgUrl = profile.epgUrl.ifBlank { summary.detectedEpgUrl }
                if (finalEpgUrl != profile.epgUrl) {
                    profileDao.update(profile.copy(epgUrl = finalEpgUrl, lastSyncedAt = System.currentTimeMillis()))
                } else {
                    profileDao.updateSyncInfo(profile.id, System.currentTimeMillis(), "")
                }
            }
            onProgress("DONE", 1.0f)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncXtreamType(profile: ProfileEntity, type: String) {
        val categories = xtreamClient.fetchCategories(
            profileId = profile.id,
            serverUrl = profile.serverUrl,
            username = profile.username,
            password = profile.password,
            type = type
        )
        val adultCatIds = categories.filter { it.isAdult }.map { it.categoryId }.toSet()

        categoryDao.deleteByProfileAndType(profile.id, type)
        if (categories.isNotEmpty()) {
            categoryDao.insertAll(categories)
        }

        mediaDao.deleteByProfileAndType(profile.id, type)
        xtreamClient.streamCatalogItems(
            profileId = profile.id,
            serverUrl = profile.serverUrl,
            username = profile.username,
            password = profile.password,
            type = type,
            adultCategoryIds = adultCatIds
        ) { batch ->
            mediaDao.insertBatch(batch)
        }
    }

    suspend fun refreshXmltvEpg(profile: ProfileEntity): Int = withContext(Dispatchers.IO) {
        if (profile.epgUrl.isBlank()) return@withContext 0
        epgDao.deleteExpired(profile.id, System.currentTimeMillis() - 3600_000L)
        epgParser.importXmltv(profile.id, profile.epgUrl) { batch ->
            epgDao.insertBatch(batch)
        }
    }

    suspend fun getEpgForChannel(
        profile: ProfileEntity,
        channel: MediaItemEntity
    ): List<EpgProgramEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val channelKey = channel.epgChannelId.ifBlank { channel.streamId }
        val cached = epgDao.getUpcomingPrograms(profile.id, channelKey, now)
        if (cached.isNotEmpty() || profile.type != ProfileType.XTREAM) {
            return@withContext cached
        }
        val fetched = xtreamClient.fetchShortEpg(
            profileId = profile.id,
            serverUrl = profile.serverUrl,
            username = profile.username,
            password = profile.password,
            streamId = channel.streamId,
            channelKey = channelKey
        )
        if (fetched.isNotEmpty()) {
            epgDao.insertBatch(fetched)
        }
        fetched
    }

    suspend fun getCurrentProgramsMap(
        profileId: Long,
        channels: List<MediaItemEntity>
    ): Map<String, EpgProgramEntity> = withContext(Dispatchers.IO) {
        val keys = channels.map { it.epgChannelId.ifBlank { it.streamId } }.distinct().take(150)
        if (keys.isEmpty()) return@withContext emptyMap()
        val now = System.currentTimeMillis()
        epgDao.getCurrentProgramsForChannels(profileId, keys, now).associateBy { it.channelId }
    }

    suspend fun getCounts(profileId: Long, includeAdult: Boolean): CatalogCounts = withContext(Dispatchers.IO) {
        CatalogCounts(
            liveCount = mediaDao.countByType(profileId, MediaType.LIVE, includeAdult),
            movieCount = mediaDao.countByType(profileId, MediaType.MOVIE, includeAdult),
            seriesCount = mediaDao.countByType(profileId, MediaType.SERIES, includeAdult),
            favoriteCount = favoriteDao.countFavorites(profileId)
        )
    }

    suspend fun getCategories(profileId: Long, type: String, includeAdult: Boolean): List<CategoryEntity> =
        withContext(Dispatchers.IO) {
            categoryDao.getCategories(profileId, type, includeAdult)
        }

    companion object {
        const val CAT_FAVORITES = "__FAVORITES__"
        const val CAT_HISTORY = "__HISTORY__"
        const val CAT_RECENTLY_ADDED = "__RECENT__"
    }

    suspend fun queryCatalog(
        profileId: Long,
        type: String,
        categoryId: String,
        query: String,
        sortMode: SortMode,
        includeAdult: Boolean,
        offset: Int,
        limit: Int = AppConfig.CATALOG_PAGE_SIZE
    ): Pair<Int, List<MediaItemEntity>> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (categoryId == CAT_FAVORITES || categoryId == CAT_HISTORY || categoryId == CAT_RECENTLY_ADDED) {
            val base = when (categoryId) {
                CAT_FAVORITES -> mediaDao.getFavoriteItems(profileId, type, includeAdult, 250)
                CAT_HISTORY -> mediaDao.getHistoryItems(profileId, type, includeAdult, 250)
                else -> mediaDao.getFeatured(profileId, type, includeAdult, 30)
            }
            val filtered = if (q.isBlank()) base else base.filter { it.name.contains(q, ignoreCase = true) }
            val page = if (offset >= filtered.size) emptyList() else filtered.subList(offset, minOf(offset + limit, filtered.size))
            return@withContext filtered.size to page
        }
        val total = mediaDao.countFiltered(profileId, type, categoryId, q, includeAdult)
        val items = mediaDao.queryCatalog(
            profileId = profileId,
            type = type,
            categoryId = categoryId,
            query = q,
            sortMode = sortMode.name,
            includeAdult = includeAdult,
            limit = limit,
            offset = offset
        )
        total to items
    }

    suspend fun searchGlobal(
        profileId: Long,
        type: String,
        query: String,
        includeAdult: Boolean
    ): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        mediaDao.searchGlobal(profileId, type, query.trim(), includeAdult)
    }

    suspend fun getFeatured(profileId: Long, type: String, includeAdult: Boolean, limit: Int = 18): List<MediaItemEntity> =
        withContext(Dispatchers.IO) {
            mediaDao.getFeatured(profileId, type, includeAdult, limit)
        }

    suspend fun getFavorites(profileId: Long, includeAdult: Boolean): List<MediaItemEntity> =
        withContext(Dispatchers.IO) {
            mediaDao.getFavoriteItems(profileId, "", includeAdult)
        }

    suspend fun toggleFavorite(profileId: Long, item: MediaItemEntity, currentlyFavorite: Boolean) =
        withContext(Dispatchers.IO) {
            if (currentlyFavorite) {
                favoriteDao.delete(item.uid)
            } else {
                favoriteDao.insert(
                    FavoriteEntity(
                        uid = item.uid,
                        profileId = profileId,
                        type = item.type
                    )
                )
            }
        }

    suspend fun getSeriesDetails(profile: ProfileEntity, seriesId: String): SeriesDetails? =
        xtreamClient.fetchSeriesDetails(profile.serverUrl, profile.username, profile.password, seriesId)

    suspend fun saveWatchProgress(entry: WatchHistoryEntity) = withContext(Dispatchers.IO) {
        historyDao.upsert(entry)
    }

    suspend fun getWatchProgress(uid: String): WatchHistoryEntity? = withContext(Dispatchers.IO) {
        historyDao.getByUid(uid)
    }

    suspend fun clearHistory(profileId: Long) = withContext(Dispatchers.IO) {
        historyDao.clearProfileHistory(profileId)
    }

    suspend fun removeHistoryItem(uid: String) = withContext(Dispatchers.IO) {
        historyDao.deleteByUid(uid)
    }

    suspend fun getSeriesHistory(profileId: Long, mediaUid: String): List<WatchHistoryEntity> = withContext(Dispatchers.IO) {
        historyDao.getSeriesHistory(profileId, mediaUid)
    }

    suspend fun getMediaItemByUid(uid: String): MediaItemEntity? = withContext(Dispatchers.IO) {
        mediaDao.getByUid(uid)
    }
}
