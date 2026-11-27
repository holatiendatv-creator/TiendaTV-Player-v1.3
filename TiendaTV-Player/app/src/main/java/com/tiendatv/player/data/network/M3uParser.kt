package com.tiendatv.player.data.network

import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.CategoryEntity
import com.tiendatv.player.data.local.MediaItemEntity
import com.tiendatv.player.data.local.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class M3uParseSummary(
    val liveCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val detectedEpgUrl: String
)

/**
 * Streaming line-by-line M3U / M3U8 parser designed to handle massive playlists
 * (100,000+ entries) without loading the entire file into memory.
 */
class M3uParser(
    private val client: OkHttpClient = NetworkModule.apiClient
) {
    private val attrRegex = Regex("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")

    suspend fun parseUrl(
        profileId: Long,
        playlistUrl: String,
        onCategoriesReady: suspend (List<CategoryEntity>) -> Unit,
        onMediaBatch: suspend (List<MediaItemEntity>) -> Unit
    ): M3uParseSummary = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(playlistUrl.trim()).get().build()
        val seenCategories = LinkedHashMap<String, CategoryEntity>()
        val batch = ArrayList<MediaItemEntity>(AppConfig.DB_INSERT_BATCH_SIZE)
        var liveCount = 0
        var movieCount = 0
        var seriesCount = 0
        var detectedEpgUrl = ""

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: ${response.message}")
            }
            val stream = response.body?.byteStream()
                ?: throw IllegalStateException("Empty M3U response body")

            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 64 * 1024).use { reader ->
                var pendingName = ""
                var pendingLogo = ""
                var pendingGroup = "General"
                var pendingTvgId = ""
                var lineIndex = 0
                var itemIndex = 0

                while (true) {
                    val rawLine = reader.readLine() ?: break
                    val line = rawLine.trim()
                    if (line.isEmpty()) continue

                    if (lineIndex == 0 && line.startsWith("#EXTM3U", ignoreCase = true)) {
                        val attrs = parseAttrs(line)
                        detectedEpgUrl = attrs["url-tvg"]
                            ?: attrs["x-tvg-url"]
                            ?: ""
                    } else if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                        val attrs = parseAttrs(line)
                        pendingTvgId = attrs["tvg-id"].orEmpty()
                        pendingLogo = attrs["tvg-logo"].orEmpty()
                        pendingGroup = attrs["group-title"].orEmpty().ifBlank { "General" }
                        val commaIdx = line.lastIndexOf(',')
                        pendingName = if (commaIdx >= 0 && commaIdx + 1 < line.length) {
                            line.substring(commaIdx + 1).trim()
                        } else {
                            attrs["tvg-name"].orEmpty().ifBlank { "Stream ${itemIndex + 1}" }
                        }
                    } else if (!line.startsWith("#")) {
                        val url = line
                        val type = detectMediaType(url, pendingGroup, pendingName)
                        val catId = pendingGroup.lowercase(Locale.ROOT).hashCode().toString()
                        val catKey = "${profileId}_${type}_$catId"
                        val isAdult = isAdultText(pendingGroup) || isAdultText(pendingName)

                        if (!seenCategories.containsKey(catKey)) {
                            seenCategories[catKey] = CategoryEntity(
                                uid = catKey,
                                profileId = profileId,
                                categoryId = catId,
                                name = pendingGroup,
                                type = type,
                                isAdult = isAdult,
                                sortOrder = seenCategories.size
                            )
                        }

                        itemIndex++
                        val streamId = itemIndex.toString()
                        batch.add(
                            MediaItemEntity(
                                uid = "${profileId}_${type}_m3u_$streamId",
                                profileId = profileId,
                                streamId = streamId,
                                type = type,
                                name = pendingName.ifBlank { "Channel $itemIndex" },
                                categoryId = catId,
                                streamUrl = url,
                                logoUrl = pendingLogo,
                                backdropUrl = pendingLogo,
                                epgChannelId = pendingTvgId,
                                channelNumber = itemIndex,
                                addedTimestamp = System.currentTimeMillis() - itemIndex,
                                isAdult = isAdult
                            )
                        )

                        when (type) {
                            MediaType.LIVE -> liveCount++
                            MediaType.MOVIE -> movieCount++
                            MediaType.SERIES -> seriesCount++
                        }

                        if (batch.size >= AppConfig.DB_INSERT_BATCH_SIZE) {
                            onMediaBatch(batch.toList())
                            batch.clear()
                        }

                        pendingName = ""
                        pendingLogo = ""
                        pendingGroup = "General"
                        pendingTvgId = ""
                    }
                    lineIndex++
                }
            }
        }

        if (batch.isNotEmpty()) {
            onMediaBatch(batch.toList())
            batch.clear()
        }
        if (seenCategories.isNotEmpty()) {
            onCategoriesReady(seenCategories.values.toList())
        }

        M3uParseSummary(
            liveCount = liveCount,
            movieCount = movieCount,
            seriesCount = seriesCount,
            detectedEpgUrl = detectedEpgUrl
        )
    }

    private fun parseAttrs(line: String): Map<String, String> {
        val map = HashMap<String, String>()
        attrRegex.findAll(line).forEach { match ->
            map[match.groupValues[1].lowercase(Locale.ROOT)] = match.groupValues[2].trim()
        }
        return map
    }

    private fun detectMediaType(url: String, group: String, name: String): String {
        val lowerUrl = url.lowercase(Locale.ROOT)
        val lowerGroup = group.lowercase(Locale.ROOT)
        val lowerName = name.lowercase(Locale.ROOT)
        return when {
            lowerUrl.contains("/series/") ||
                lowerGroup.contains("series") ||
                Regex("s\\d{1,2}\\s*e\\d{1,2}").containsMatchIn(lowerName) -> MediaType.SERIES
            lowerUrl.contains("/movie/") ||
                lowerUrl.endsWith(".mp4") ||
                lowerUrl.endsWith(".mkv") ||
                lowerUrl.endsWith(".avi") ||
                lowerGroup.contains("vod") ||
                lowerGroup.contains("movie") ||
                lowerGroup.contains("pelicula") ||
                lowerGroup.contains("película") -> MediaType.MOVIE
            else -> MediaType.LIVE
        }
    }

    private fun isAdultText(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return AppConfig.ADULT_CATEGORY_KEYWORDS.any { lower.contains(it) }
    }
}
