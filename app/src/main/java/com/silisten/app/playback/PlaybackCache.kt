package com.silisten.app.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object PlaybackCache {
    private const val CACHE_DIR_NAME = "silisten_media_cache"
    private const val MAX_CACHE_BYTES = 512L * 1024L * 1024L

    @Volatile
    private var sharedCache: SimpleCache? = null

    fun cache(context: Context): SimpleCache {
        val appContext = context.applicationContext
        return sharedCache ?: synchronized(this) {
            sharedCache ?: SimpleCache(
                File(appContext.cacheDir, CACHE_DIR_NAME),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                StandaloneDatabaseProvider(appContext)
            ).also { sharedCache = it }
        }
    }

    fun sizeBytes(context: Context): Long {
        sharedCache?.let { return it.cacheSpace.coerceAtLeast(0L) }
        return File(context.applicationContext.cacheDir, CACHE_DIR_NAME).sizeBytes()
    }

    fun clear(context: Context) {
        val cache = cache(context)
        cache.keys.toList().forEach { key ->
            runCatching { cache.removeResource(key) }
        }
    }
}

private fun File.sizeBytes(): Long {
    if (!exists()) return 0L
    if (isFile) return length()
    return listFiles().orEmpty().sumOf { it.sizeBytes() }
}
