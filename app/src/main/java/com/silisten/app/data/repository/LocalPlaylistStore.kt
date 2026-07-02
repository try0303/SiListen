package com.silisten.app.data.repository

import android.content.Context
import com.silisten.app.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LocalPlaylistRecord(
    val id: String,
    val title: String,
    val songs: List<Song>
)

class LocalPlaylistStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "local_playlist_state",
        Context.MODE_PRIVATE
    )

    fun load(): List<LocalPlaylistRecord> {
        val raw = preferences.getString(KEY_LOCAL_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toLocalPlaylistRecordOrNull()?.let(::add)
                }
            }.distinctBy { it.id }
        }.getOrDefault(emptyList())
    }

    fun save(records: List<LocalPlaylistRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("title", record.title)
                    .put("songs", record.songs.toJsonArray())
            )
        }
        preferences.edit()
            .putString(KEY_LOCAL_PLAYLISTS, array.toString())
            .apply()
    }

    companion object {
        private const val KEY_LOCAL_PLAYLISTS = "local_playlists"

        fun newLocalPlaylistId(): String =
            "local-playlist-${UUID.randomUUID().toString().replace("-", "")}"
    }
}

private fun JSONObject.toLocalPlaylistRecordOrNull(): LocalPlaylistRecord? {
    val id = optString("id").takeIf { it.isNotBlank() } ?: return null
    val title = optString("title").takeIf { it.isNotBlank() } ?: return null
    return LocalPlaylistRecord(
        id = id,
        title = title,
        songs = optJSONArray("songs").toSongs()
    )
}

private fun JSONArray?.toSongs(): List<Song> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.toSongOrNull()?.let(::add)
        }
    }.distinctBy { "${it.sourceId}:${it.id}" }
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
        sourceId = optString("sourceId").ifBlank { "local" },
        streamHint = optString("streamHint").takeIf { it.isNotBlank() && it != "null" },
        canonicalSourceId = optString("canonicalSourceId").takeIf { it.isNotBlank() && it != "null" },
        canonicalSongId = optString("canonicalSongId").takeIf { it.isNotBlank() && it != "null" },
        playbackSourceId = optString("playbackSourceId").takeIf { it.isNotBlank() && it != "null" },
        providerIds = optJSONObject("providerIds").toStringMap()
    )
}

private fun List<Song>.toJsonArray(): JSONArray =
    JSONArray().also { array ->
        forEach { song ->
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
                    .put("canonicalSourceId", song.canonicalSourceId)
                    .put("canonicalSongId", song.canonicalSongId)
                    .put("playbackSourceId", song.playbackSourceId)
                    .put("providerIds", song.providerIds.toJsonObject())
            )
        }
    }

private fun JSONObject?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    return buildMap {
        keys().forEach { key ->
            val value = optString(key)
            if (key.isNotBlank() && value.isNotBlank()) put(key, value)
        }
    }
}

private fun Map<String, String>.toJsonObject(): JSONObject =
    JSONObject().also { json ->
        forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) json.put(key, value)
        }
    }
