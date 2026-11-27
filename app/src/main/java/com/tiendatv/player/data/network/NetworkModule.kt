package com.tiendatv.player.data.network

import com.tiendatv.player.config.AppConfig
import okhttp3.OkHttpClient
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

    /** Client tuned specifically for Media3 ExoPlayer live/VOD streaming. */
    val streamClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
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
}
