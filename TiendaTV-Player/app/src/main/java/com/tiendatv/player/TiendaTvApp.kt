package com.tiendatv.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.tiendatv.player.data.local.SettingsDataStore
import com.tiendatv.player.data.local.TiendaTvDatabase
import com.tiendatv.player.data.network.NetworkModule
import com.tiendatv.player.data.repository.CatalogRepository

class TiendaTvApp : Application(), ImageLoaderFactory {

    lateinit var database: TiendaTvDatabase
        private set

    lateinit var settingsStore: SettingsDataStore
        private set

    lateinit var repository: CatalogRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = TiendaTvDatabase.getInstance(this)
        settingsStore = SettingsDataStore(this)
        repository = CatalogRepository(database)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkModule.apiClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("poster_cache"))
                    .maxSizeBytes(120L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
