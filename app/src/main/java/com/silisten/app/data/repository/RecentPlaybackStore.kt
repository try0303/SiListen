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
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toSongOrNull()?.let(::add)
                }
            }.distinctBy { it.id }.take(MAX_ITEMS)
        }.getOrDefault(emptyList())
    }

    fun save(songs: List<Song>) {
        val array = JSONArray()
        songs.take(MAX_ITEMS).forEach { song ->
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
    }
}
