package com.silisten.app.playback

import android.content.Context
import android.net.Uri
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.Song
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * True song-file cache: complete audio files keyed by song identity + quality.
 * Unlike Media3 progressive byte cache, a hit can play without re-resolving remote URLs.
 */
object SongFileCache {
    private const val CACHE_DIR_NAME = "silisten_song_files"
    private const val PARTIAL_SUFFIX = ".part"
    private const val MAX_CACHE_BYTES = 2L * 1024L * 1024L * 1024L
    private const val MIN_VALID_BYTES = 32L * 1024L

    private val downloadLocks = ConcurrentHashMap<String, Mutex>()
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun cacheKey(song: Song, quality: PlaybackQuality): String {
        val source = song.playbackSourceId?.takeIf { it.isNotBlank() }
            ?: song.sourceId.ifBlank { "unknown" }
        val identity = song.canonicalSongId?.takeIf { it.isNotBlank() }
            ?: song.id
        val canonical = song.canonicalSourceId.orEmpty()
        val raw = listOf(source, identity, canonical, quality.neteaseLevel, quality.bitrate.toString())
            .joinToString("|")
        return sha1(raw)
    }

    fun findFile(context: Context, song: Song, quality: PlaybackQuality): File? {
        val key = cacheKey(song, quality)
        val file = fileForKey(context, key)
        return file.takeIf { it.isFile && it.length() >= MIN_VALID_BYTES }
    }

    fun playbackUri(file: File): String = Uri.fromFile(file).toString()

    fun sizeBytes(context: Context): Long {
        val dir = cacheDir(context)
        if (!dir.exists()) return 0L
        return dir.listFiles().orEmpty()
            .filter { it.isFile && !it.name.endsWith(PARTIAL_SUFFIX) }
            .sumOf { it.length().coerceAtLeast(0L) }
    }

    fun clear(context: Context) {
        val dir = cacheDir(context)
        if (!dir.exists()) return
        dir.listFiles().orEmpty().forEach { child ->
            runCatching { child.deleteRecursively() }
        }
    }

    suspend fun ensureDownloaded(
        context: Context,
        song: Song,
        quality: PlaybackQuality,
        streamUrl: String
    ): File? = withContext(Dispatchers.IO) {
        if (streamUrl.isBlank() || streamUrl.startsWith("file:", ignoreCase = true)) {
            return@withContext findFile(context, song, quality)
        }
        if (!(streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
            return@withContext findFile(context, song, quality)
        }

        val key = cacheKey(song, quality)
        findFile(context, song, quality)?.let { return@withContext it }

        val lock = downloadLocks.getOrPut(key) { Mutex() }
        lock.withLock {
            findFile(context, song, quality)?.let { return@withLock it }
            val target = fileForKey(context, key)
            val partial = File(target.parentFile, target.name + PARTIAL_SUFFIX)
            runCatching { partial.delete() }
            val downloaded = downloadTo(partial, streamUrl)
            if (!downloaded || partial.length() < MIN_VALID_BYTES) {
                runCatching { partial.delete() }
                return@withLock null
            }
            runCatching { target.delete() }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            trimIfNeeded(context)
            target.takeIf { it.isFile && it.length() >= MIN_VALID_BYTES }
        }
    }

    private fun downloadTo(target: File, streamUrl: String): Boolean {
        target.parentFile?.mkdirs()
        val request = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", "Mozilla/5.0 SiListen/0.1.3 Android")
            .header("Referer", "https://music.163.com/")
            .get()
            .build()
        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                target.outputStream().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
                true
            }
        }.getOrDefault(false)
    }

    private fun trimIfNeeded(context: Context) {
        val dir = cacheDir(context)
        val files = dir.listFiles().orEmpty()
            .filter { it.isFile && !it.name.endsWith(PARTIAL_SUFFIX) }
            .sortedBy { it.lastModified() }
            .toMutableList()
        var total = files.sumOf { it.length() }
        while (total > MAX_CACHE_BYTES && files.isNotEmpty()) {
            val oldest = files.removeAt(0)
            total -= oldest.length()
            runCatching { oldest.delete() }
        }
    }

    private fun cacheDir(context: Context): File =
        File(context.applicationContext.filesDir, CACHE_DIR_NAME).also { it.mkdirs() }

    private fun fileForKey(context: Context, key: String): File =
        File(cacheDir(context), "$key.bin")

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
