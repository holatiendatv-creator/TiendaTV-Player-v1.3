package com.tiendatv.player.data.network

import android.util.Base64
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.EpgProgramEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class XtreamAuthResult(
    val isAuthenticated: Boolean,
    val status: String,
    val expirationDate: String,
    val maxConnections: String,
    val errorMessage: String? = null
)

data class SeriesEpisode(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val plot: String,
    val duration: String,
    val thumbnailUrl: String,
    val streamUrl: String,
    val containerExtension: String
)

data class SeriesDetails(
    val title: String,
    val plot: String,
    val cast: String,
    val director: String,
    val genre: String,
    val releaseDate: String,
    val rating: String,
    val coverUrl: String,
    val backdropUrl: String,
    val seasons: Map<Int, List<SeriesEpisode>>
)

class XtreamApiClient(
    private val client: OkHttpClient = NetworkModule.apiClient
) {

    fun normalizeServerUrl(raw: String): String {
        val trimmed = raw.trim().removeSuffix("/")
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    private fun buildApiUrl(
        serverUrl: String,
        username: String,
        password: String,
        action: String? = null,
        extraParams: Map<String, String> = emptyMap()
    ): String {
        val base = normalizeServerUrl(serverUrl)
        val u = URLEncoder.encode(username.trim(), "UTF-8")
        val p = URLEncoder.encode(password.trim(), "UTF-8")
        val sb = StringBuilder("$base/player_api.php?username=$u&password=$p")
        if (!action.isNullOrBlank()) {
            sb.append("&action=").append(action)
        }
        extraParams.forEach { (k, v) ->
            sb.append("&").append(k).append("=").append(URLEncoder.encode(v, "UTF-8"))
        }
        return sb.toString()
    }

    fun buildXmltvUrl(serverUrl: String, username: String, password: String): String {
        val base = normalizeServerUrl(serverUrl)
        val u = URLEncoder.encode(username.trim(), "UTF-8")
        val p = URLEncoder.encode(password.trim(), "UTF-8")
        return "$base/xmltv.php?username=$u&password=$p"
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): XtreamAuthResult = withContext(Dispatchers.IO) {
        try {
            val url = buildApiUrl(serverUrl, username, password)
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext XtreamAuthResult(
                        isAuthenticated = false,
                        status = "HTTP ${response.code}",
                        expirationDate = "",
                        maxConnections = "",
                        errorMessage = "HTTP ${response.code}: ${response.message}"
                    )
                }
                val body = response.body?.string().orEmpty()
                val root = JsonParser.parseString(body).asJsonObject
                val userInfo = root.getAsJsonObject("user_info")
                val authInt = userInfo?.get("auth")?.asInt ?: 0
                val status = userInfo?.get("status")?.safeString().orEmpty()
                val expRaw = userInfo?.get("exp_date")?.safeString().orEmpty()
                val expFormatted = expRaw.toLongOrNull()?.let { sec ->
                    if (sec > 0) {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(sec * 1000L))
                    } else ""
                } ?: expRaw
                val maxConn = userInfo?.get("max_connections")?.safeString().orEmpty()
                val ok = authInt == 1
                XtreamAuthResult(
                    isAuthenticated = ok,
                    status = status,
                    expirationDate = expFormatted,
                    maxConnections = maxConn,
                    errorMessage = if (ok) null else "Invalid credentials or inactive account ($status)"
                )
            }
        } catch (e: Exception) {
            XtreamAuthResult(
                isAuthenticated = false,
                status = "Error",
                expirationDate = "",
                maxConnections = "",
                errorMessage = e.localizedMessage ?: "Connection failed"
            )
        }
    }

    suspend fun fetchCategories(
        profileId: Long,
        serverUrl: String,
        username: String,
        password: String,
        type: String
    ): List<CategoryEntity> = withContext(Dispatchers.IO) {
        val action = when (type) {
            MediaType.LIVE -> "get_live_categories"
            MediaType.MOVIE -> "get_vod_categories"
            MediaType.SERIES -> "get_series_categories"
            else -> return@withContext emptyList()
        }
        val url = buildApiUrl(serverUrl, username, password, action)
        val result = mutableListOf<CategoryEntity>()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val stream = response.body?.byteStream() ?: return@withContext emptyList()
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.isLenient = true
                if (reader.peek() != JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                reader.beginArray()
                var order = 0
                while (reader.hasNext()) {
                    var catId = ""
                    var catName = ""
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "category_id" -> catId = reader.nextFlexibleString()
                            "category_name" -> catName = reader.nextFlexibleString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (catId.isNotBlank() && catName.isNotBlank()) {
                        result.add(
                            CategoryEntity(
                                uid = "${profileId}_${type}_$catId",
                                profileId = profileId,
                                categoryId = catId,
                                name = catName.trim(),
                                type = type,
                                isAdult = isAdultName(catName),
                                sortOrder = order++
                            )
                        )
                    }
                }
                reader.endArray()
            }
        }
        result
    }

    /**
     * Streams JSON items from Xtream API and delivers batches of [AppConfig.DB_INSERT_BATCH_SIZE]
     * to [onBatch] so 80,000+ movies / 17,000+ series parse with minimal RAM on Firestick.
     */
    suspend fun streamCatalogItems(
        profileId: Long,
        serverUrl: String,
        username: String,
        password: String,
        type: String,
        adultCategoryIds: Set<String>,
        onBatch: suspend (List<MediaItemEntity>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val base = normalizeServerUrl(serverUrl)
        val action = when (type) {
            MediaType.LIVE -> "get_live_streams"
            MediaType.MOVIE -> "get_vod_streams"
            MediaType.SERIES -> "get_series"
            else -> return@withContext 0
        }
        val url = buildApiUrl(base, username, password, action)
        val request = Request.Builder().url(url).get().build()
        var total = 0
        val batch = ArrayList<MediaItemEntity>(AppConfig.DB_INSERT_BATCH_SIZE)

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext 0
            val stream = response.body?.byteStream() ?: return@withContext 0
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.isLenient = true
                if (reader.peek() != JsonToken.BEGIN_ARRAY) return@withContext 0
                reader.beginArray()
                var index = 0
                while (reader.hasNext()) {
                    var streamId = ""
                    var name = ""
                    var categoryId = ""
                    var logo = ""
                    var backdrop = ""
                    var ext = if (type == MediaType.LIVE) "ts" else "mp4"
                    var epgId = ""
                    var rating = 0.0
                    var year = ""
                    var plot = ""
                    var added = 0L
                    var num = index + 1
                    var explicitAdult = false

                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "stream_id", "series_id" -> streamId = reader.nextFlexibleString()
                            "name" -> name = reader.nextFlexibleString()
                            "category_id" -> categoryId = reader.nextFlexibleString()
                            "stream_icon", "cover" -> logo = reader.nextFlexibleString()
                            "backdrop_path" -> backdrop = reader.nextFirstBackdrop()
                            "container_extension" -> {
                                val v = reader.nextFlexibleString()
                                if (v.isNotBlank()) ext = v
                            }
                            "epg_channel_id" -> epgId = reader.nextFlexibleString()
                            "rating_5based" -> rating = reader.nextFlexibleDouble()
                            "rating" -> {
                                val r = reader.nextFlexibleDouble()
                                if (rating == 0.0 && r > 0.0) {
                                    rating = if (r > 5.0) r / 2.0 else r
                                }
                            }
                            "year", "releaseDate" -> {
                                val y = reader.nextFlexibleString()
                                if (y.length >= 4 && year.isBlank()) year = y.take(4)
                            }
                            "plot" -> plot = reader.nextFlexibleString()
                            "added", "last_modified" -> added = reader.nextFlexibleLong()
                            "num" -> num = reader.nextFlexibleLong().toInt().takeIf { it > 0 } ?: (index + 1)
                            "is_adult" -> explicitAdult = reader.nextFlexibleString() == "1"
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    index++

                    if (streamId.isNotBlank() && name.isNotBlank()) {
                        val streamUrl = when (type) {
                            MediaType.LIVE -> "$base/live/$username/$password/$streamId.ts"
                            MediaType.MOVIE -> "$base/movie/$username/$password/$streamId.$ext"
                            MediaType.SERIES -> ""
                            else -> ""
                        }
                        val isAdult = explicitAdult || adultCategoryIds.contains(categoryId) || isAdultName(name)
                        batch.add(
                            MediaItemEntity(
                                uid = "${profileId}_${type}_$streamId",
                                profileId = profileId,
                                streamId = streamId,
                                type = type,
                                name = name.trim(),
                                categoryId = categoryId,
                                streamUrl = streamUrl,
                                logoUrl = logo,
                                backdropUrl = backdrop.ifBlank { logo },
                                containerExtension = ext,
                                epgChannelId = epgId,
                                rating = rating,
                                year = year,
                                plot = plot,
                                addedTimestamp = added,
                                channelNumber = num,
                                isAdult = isAdult
                            )
                        )
                        total++
                        if (batch.size >= AppConfig.DB_INSERT_BATCH_SIZE) {
                            onBatch(batch.toList())
                            batch.clear()
                        }
                    }
                }
                reader.endArray()
            }
        }
        if (batch.isNotEmpty()) {
            onBatch(batch.toList())
            batch.clear()
        }
        total
    }

    suspend fun fetchSeriesDetails(
        serverUrl: String,
        username: String,
        password: String,
        seriesId: String
    ): SeriesDetails? = withContext(Dispatchers.IO) {
        try {
            val base = normalizeServerUrl(serverUrl)
            val url = buildApiUrl(base, username, password, "get_series_info", mapOf("series_id" to seriesId))
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                val root = JsonParser.parseString(body).asJsonObject
                val info = root.getAsJsonObject("info")
                val episodesObj = root.getAsJsonObject("episodes")

                val seasonsMap = sortedMapOf<Int, List<SeriesEpisode>>()
                episodesObj?.entrySet()?.forEach { (seasonKey, element) ->
                    val seasonNum = seasonKey.toIntOrNull() ?: 1
                    val list = mutableListOf<SeriesEpisode>()
                    if (element.isJsonArray) {
                        element.asJsonArray.forEach { epEl ->
                            if (epEl.isJsonObject) {
                                val ep = epEl.asJsonObject
                                val epId = ep.get("id")?.safeString().orEmpty()
                                val epNum = ep.get("episode_num")?.safeString()?.toIntOrNull() ?: (list.size + 1)
                                val epTitle = ep.get("title")?.safeString().orEmpty().ifBlank { "Episode $epNum" }
                                val ext = ep.get("container_extension")?.safeString().orEmpty().ifBlank { "mp4" }
                                val epInfo = ep.get("info")?.takeIf { it.isJsonObject }?.asJsonObject
                                val plot = epInfo?.get("plot")?.safeString().orEmpty()
                                val duration = epInfo?.get("duration")?.safeString().orEmpty()
                                val thumb = epInfo?.get("movie_image")?.safeString().orEmpty()
                                if (epId.isNotBlank()) {
                                    list.add(
                                        SeriesEpisode(
                                            id = epId,
                                            seasonNumber = seasonNum,
                                            episodeNumber = epNum,
                                            title = epTitle,
                                            plot = plot,
                                            duration = duration,
                                            thumbnailUrl = thumb,
                                            streamUrl = "$base/series/$username/$password/$epId.$ext",
                                            containerExtension = ext
                                        )
                                    )
                                }
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        seasonsMap[seasonNum] = list.sortedBy { it.episodeNumber }
                    }
                }

                val backdrop = info?.get("backdrop_path")?.let { bp ->
                    when {
                        bp.isJsonArray && bp.asJsonArray.size() > 0 -> bp.asJsonArray[0].safeString()
                        bp.isJsonPrimitive -> bp.safeString()
                        else -> ""
                    }
                }.orEmpty()

                SeriesDetails(
                    title = info?.get("name")?.safeString().orEmpty(),
                    plot = info?.get("plot")?.safeString().orEmpty(),
                    cast = info?.get("cast")?.safeString().orEmpty(),
                    director = info?.get("director")?.safeString().orEmpty(),
                    genre = info?.get("genre")?.safeString().orEmpty(),
                    releaseDate = info?.get("releaseDate")?.safeString().orEmpty(),
                    rating = info?.get("rating")?.safeString().orEmpty(),
                    coverUrl = info?.get("cover")?.safeString().orEmpty(),
                    backdropUrl = backdrop,
                    seasons = seasonsMap
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchShortEpg(
        profileId: Long,
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        channelKey: String
    ): List<EpgProgramEntity> = withContext(Dispatchers.IO) {
        try {
            val url = buildApiUrl(
                serverUrl,
                username,
                password,
                "get_short_epg",
                mapOf("stream_id" to streamId, "limit" to "6")
            )
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                val root = JsonParser.parseString(body).asJsonObject
                val listings = root.getAsJsonArray("epg_listings") ?: return@withContext emptyList()
                val out = mutableListOf<EpgProgramEntity>()
                listings.forEach { el ->
                    if (el.isJsonObject) {
                        val obj = el.asJsonObject
                        val title = decodeBase64Safe(obj.get("title")?.safeString().orEmpty())
                        val desc = decodeBase64Safe(obj.get("description")?.safeString().orEmpty())
                        val startSec = obj.get("start_timestamp")?.safeString()?.toLongOrNull() ?: 0L
                        val stopSec = obj.get("stop_timestamp")?.safeString()?.toLongOrNull() ?: 0L
                        if (title.isNotBlank() && startSec > 0L && stopSec > startSec) {
                            val startMs = startSec * 1000L
                            val endMs = stopSec * 1000L
                            out.add(
                                EpgProgramEntity(
                                    id = "${profileId}_${channelKey}_$startMs",
                                    profileId = profileId,
                                    channelId = channelKey,
                                    title = title,
                                    description = desc,
                                    startMs = startMs,
                                    endMs = endMs
                                )
                            )
                        }
                    }
                }
                out
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeBase64Safe(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            val decoded = Base64.decode(raw, Base64.DEFAULT)
            String(decoded, Charsets.UTF_8).trim()
        } catch (_: Exception) {
            raw.trim()
        }
    }

    private fun isAdultName(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return AppConfig.ADULT_CATEGORY_KEYWORDS.any { lower.contains(it) }
    }

    private fun com.google.gson.JsonElement.safeString(): String =
        if (isJsonNull) "" else try { asString } catch (_: Exception) { "" }

    private fun JsonReader.nextFlexibleString(): String {
        return when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                ""
            }
            JsonToken.STRING -> nextString()
            JsonToken.NUMBER -> nextString()
            JsonToken.BOOLEAN -> nextBoolean().toString()
            else -> {
                skipValue()
                ""
            }
        }
    }

    private fun JsonReader.nextFlexibleLong(): Long =
        nextFlexibleString().toLongOrNull() ?: 0L

    private fun JsonReader.nextFlexibleDouble(): Double =
        nextFlexibleString().replace(",", ".").toDoubleOrNull() ?: 0.0

    private fun JsonReader.nextFirstBackdrop(): String {
        return when (peek()) {
            JsonToken.STRING -> nextString()
            JsonToken.BEGIN_ARRAY -> {
                var first = ""
                beginArray()
                while (hasNext()) {
                    val v = nextFlexibleString()
                    if (first.isBlank() && v.isNotBlank()) first = v
                }
                endArray()
                first
            }
            else -> {
                skipValue()
                ""
            }
        }
    }
}
