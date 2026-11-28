package com.tiendatv.player.data.network

import com.tiendatv.player.config.AppConfig
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

object NetworkModule {
    /** Client tuned for API requests and large streaming JSON/M3U catalog downloads. */
    val apiClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", AppConfig.USER_AGENT)
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    /**
     * Client tuned specifically for Media3 ExoPlayer live/VOD IPTV streaming:
     * - Forces HTTP/1.1 (avoids HTTP/2 stalling/throttling on Xtream/nginx IPTV edge servers)
     * - Disables transparent gzip on binary video streams
     * - Uses clean short-lived connection pool so single-connection IPTV accounts never hit connection-limit throttling
     */
    val streamClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(2, 10, TimeUnit.SECONDS))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", AppConfig.STREAM_USER_AGENT)
                    .header("Accept", "*/*")
                    .build()
                chain.proceed(req)
            }
            .build()
    }
}
