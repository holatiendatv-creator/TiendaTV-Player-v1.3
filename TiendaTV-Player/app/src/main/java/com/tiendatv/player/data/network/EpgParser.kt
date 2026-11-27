package com.tiendatv.player.data.network

import android.util.Xml
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.EpgProgramEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

/**
 * Streaming XMLTV EPG parser (supports plain XML and .xml.gz) that imports only
 * relevant programs within a rolling window (-2h to +24h) to keep Firestick storage lean.
 */
class EpgParser(
    private val client: OkHttpClient = NetworkModule.apiClient
) {
    private val xmltvFormatWithTz = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val xmltvFormatNoTz = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun importXmltv(
        profileId: Long,
        epgUrl: String,
        onBatch: suspend (List<EpgProgramEntity>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        if (epgUrl.isBlank()) return@withContext 0
        val now = System.currentTimeMillis()
        val minWindow = now - 2L * 3600L * 1000L
        val maxWindow = now + 24L * 3600L * 1000L

        val request = Request.Builder().url(epgUrl.trim()).get().build()
        var imported = 0
        val batch = ArrayList<EpgProgramEntity>(AppConfig.DB_INSERT_BATCH_SIZE)

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext 0
            val rawStream = response.body?.byteStream() ?: return@withContext 0
            val buffered = BufferedInputStream(rawStream, 64 * 1024)
            buffered.mark(2)
            val b1 = buffered.read()
            val b2 = buffered.read()
            buffered.reset()
            val isGzip = (b1 == 0x1f && b2 == 0x8b)
            val input = if (isGzip) GZIPInputStream(buffered) else buffered

            input.use { stream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(stream, "UTF-8")

                var event = parser.eventType
                var channelId = ""
                var startMs = 0L
                var endMs = 0L
                var title = ""
                var desc = ""
                var inProgramme = false

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "programme" -> {
                                    inProgramme = true
                                    channelId = parser.getAttributeValue(null, "channel").orEmpty()
                                    startMs = parseXmltvTime(parser.getAttributeValue(null, "start"))
                                    endMs = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                                    title = ""
                                    desc = ""
                                }
                                "title" -> if (inProgramme) {
                                    title = parser.nextText().trim()
                                }
                                "desc" -> if (inProgramme) {
                                    desc = parser.nextText().trim()
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "programme" && inProgramme) {
                                inProgramme = false
                                if (channelId.isNotBlank() && title.isNotBlank() &&
                                    endMs > minWindow && startMs < maxWindow && endMs > startMs
                                ) {
                                    batch.add(
                                        EpgProgramEntity(
                                            id = "${profileId}_${channelId}_$startMs",
                                            profileId = profileId,
                                            channelId = channelId,
                                            title = title,
                                            description = desc,
                                            startMs = startMs,
                                            endMs = endMs
                                        )
                                    )
                                    imported++
                                    if (batch.size >= AppConfig.DB_INSERT_BATCH_SIZE) {
                                        onBatch(batch.toList())
                                        batch.clear()
                                    }
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        }

        if (batch.isNotEmpty()) {
            onBatch(batch.toList())
            batch.clear()
        }
        imported
    }

    private fun parseXmltvTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val clean = raw.trim()
        return try {
            synchronized(xmltvFormatWithTz) {
                xmltvFormatWithTz.parse(clean)?.time
            } ?: synchronized(xmltvFormatNoTz) {
                xmltvFormatNoTz.parse(clean.take(14))?.time
            } ?: 0L
        } catch (_: Exception) {
            try {
                synchronized(xmltvFormatNoTz) {
                    xmltvFormatNoTz.parse(clean.take(14))?.time ?: 0L
                }
            } catch (_: Exception) {
                0L
            }
        }
    }
}
