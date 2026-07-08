package com.silisten.app.data.repository

import android.content.Context
import java.io.File

class ImageCacheRepository(context: Context) {
    private val appContext = context.applicationContext

    fun sizeBytes(): Long =
        cacheDirectories().distinctBy { it.absolutePath }.sumOf { it.sizeBytes() }

    fun clear() {
        cacheDirectories()
            .distinctBy { it.absolutePath }
            .forEach { directory ->
                if (directory.exists() && directory.isDirectory) {
                    directory.listFiles().orEmpty().forEach { child ->
                        child.deleteRecursively()
                    }
                }
            }
    }

    private fun cacheDirectories(): List<File> {
        val cacheDir = appContext.cacheDir
        val direct = listOf(
            File(cacheDir, "image_cache"),
            File(cacheDir, "coil"),
            File(cacheDir, "coil_image_cache"),
            File(cacheDir, "comment_image_cache")
        )
        val discovered = cacheDir.listFiles().orEmpty().filter { file ->
            file.isDirectory &&
                file.name.contains(Regex("coil|image|picture|comment", RegexOption.IGNORE_CASE)) &&
                !file.name.equals("silisten_media_cache", ignoreCase = true)
        }
        return direct + discovered
    }
}

private fun File.sizeBytes(): Long {
    if (!exists()) return 0L
    if (isFile) return length()
    return listFiles().orEmpty().sumOf { it.sizeBytes() }
}
