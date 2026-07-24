package com.silisten.app.data.repository

import android.content.Context
import com.silisten.app.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

class RecentPlaybackStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "library_state",
        Context.MODE_PRIVATE
    )

    fun load(): List<Song> {
        val raw = preferences.getString(KEY_RECENT_PLAYED, null) ?: return emptyList()
        val loaded = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toSongOrNull()?.let(::add)
                }
            }.distinctBy { it.id }.take(MAX_ITEMS)
        }.getOrDefault(emptyList())
        // Old Netease search used to inject 4 hardcoded seed songs with a Unsplash placeholder
        // cover when offline. Scrub them so "最近播放" no longer looks polluted.
        val cleaned = loaded.filterNot { it.isLegacyFakeSeedSong() }
        if (cleaned.size != loaded.size) {
            save(cleaned)
        }
        return cleaned
    }

    fun save(songs: List<Song>) {
        val array = JSONArray()
        songs.filterNot { it.isLegacyFakeSeedSong() }.take(MAX_ITEMS).forEach { song ->
            array.put(
                JSONObject()
                    .put("id", song.id)
                    .put("title", song.title)
                    .put("artist", song.artist)
                    .put("album", song.album)
                    .put("coverUrl", song.coverUrl)
                    .put("durationMs", song.durationMs)
                    .put("sourceId", song.sourceId)
                    .put("streamHint", song.streamHint)
            )
        }
        preferences.edit()
            .putString(KEY_RECENT_PLAYED, array.toString())
            .apply()
    }

    private fun JSONObject.toSongOrNull(): Song? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = optString("title").takeIf { it.isNotBlank() } ?: return null
        return Song(
            id = id,
            title = title,
            artist = optString("artist").ifBlank { "未知歌手" },
            album = optString("album").ifBlank { "未知专辑" },
            coverUrl = optString("coverUrl"),
            durationMs = optLong("durationMs", 0L),
            sourceId = optString("sourceId").ifBlank { "netease" },
            streamHint = optString("streamHint").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    companion object {
        const val MAX_ITEMS = 50
        private const val KEY_RECENT_PLAYED = "recent_played_songs"

        // Hardcoded IDs previously returned by NeteaseMusicSource.seedSongs().
        private val LEGACY_FAKE_SEED_IDS = setOf(
            "33894312",   // 起风了
            "1901371647", // 哪里都是你
            "1827600686", // 删了吧
            "26259003"    // 海阔天空
        )
        private const val LEGACY_SEED_PLACEHOLDER_MARKER = "photo-1516280440614-37939bbacd81"

        private fun Song.isLegacyFakeSeedSong(): Boolean {
            if (id !in LEGACY_FAKE_SEED_IDS) return false
            // Real plays of the same song IDs keep a music.126.net cover; seed rows used Unsplash.
            return coverUrl.isBlank() || coverUrl.contains(LEGACY_SEED_PLACEHOLDER_MARKER)
        }
    }
}
