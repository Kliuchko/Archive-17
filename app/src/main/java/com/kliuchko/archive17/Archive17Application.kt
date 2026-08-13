package com.kliuchko.archive17

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kliuchko.archive17.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Archive17Application : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@Archive17Application)
            modules(appModule)
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.15)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("cover-cache"))
                .maxSizeBytes(MAX_COVER_CACHE_BYTES)
                .build()
        }
        .respectCacheHeaders(false)
        .build()

    private companion object {
        const val MAX_COVER_CACHE_BYTES = 40L * 1024L * 1024L
    }
}
