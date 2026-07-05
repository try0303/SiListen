package com.silisten.app.data.repository

import android.content.Context
import com.silisten.app.data.model.Song
import com.silisten.app.playback.PlaybackMode
import com.silisten.app.playback.PlaybackState
import org.json.JSONArray
import org.json.JSONObject

data class LastPlaybackSnapshot(
    val queue: List<Song>,
    val currentIndex: Int,
    val positionMs: Long,
    val durationMs: Long,
    val playbackMode: PlaybackMode,
    val wasPlaying: Boolean,
    val updatedAtMs: Long
)

class LastPlaybackStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "last_playback_state",
        Context.MODE_PRIVATE
    )

    fun load(): LastPlaybackSnapshot? {
        val raw = preferences.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val queue = json.optJSONArray("queue").toSongs()
            if (queue.isEmpty()) return@runCatching null
            val currentIndex = json.optInt("currentIndex", 0).coerceIn(0, queue.lastIndex)
            LastPlaybackSnapshot(
                queue = queue,
                currentIndex = currentIndex,
                positionMs = json.optLong("positionMs", 0L).coerceAtLeast(0L),
                durationMs = json.optLong("durationMs", queue[currentIndex].durationMs).coerceAtLeast(0L),
                playbackMode = runCatching {
                    PlaybackMode.valueOf(json.optString("playbackMode", PlaybackMode.Order.name))
                }.getOrDefault(PlaybackMode.Order),
                wasPlaying = json.optBoolean("wasPlaying", false),
                updatedAtMs = json.optLong("updatedAtMs", 0L)
            )
        }.getOrNull()
    }

    fun save(state: PlaybackState) {
        val queue = state.queue.take(MAX_QUEUE_ITEMS)
        val currentSong = state.currentSong ?: return
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.sourceId == currentSong.sourceId && it.id == currentSong.id }
            .takeIf { it >= 0 }
            ?: state.currentIndex.coerceIn(0, queue.lastIndex)
        val durationMs = state.durationMs.takeIf { it > 0L } ?: queue[currentIndex].durationMs
        val safePositionMs = state.positionMs
            .coerceAtLeast(0L)
            .let { position -> durationMs.takeIf { it > 0L }?.let { position.coerceAtMost(it) } ?: position }

        val json = JSONObject()
            .put("queue", queue.toJsonArray())
            .put("currentIndex", currentIndex)
            .put("positionMs", safePositionMs)
            .put("durationMs", durationMs)
            .put("playbackMode", state.playbackMode.name)
            .put("wasPlaying", state.isPlaying)
            .put("updatedAtMs", System.currentTimeMillis())
        preferences.edit()
            .putString(KEY_SNAPSHOT, json.toString())
            .apply()
    }

    companion object {
        private const val KEY_SNAPSHOT = "snapshot"
        private const val MAX_QUEUE_ITEMS = 100
    }
}

private fun JSONArray?.toSongs(): List<Song> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.toSongOrNull()?.let(::add)
        }
    }
}

private fun JSONObject.toSongOrNull(): Song? {
    val id = optString("id").takeIf { it.isNotBlank() } ?: return null
    val title = optString("title").takeIf { it.isNotBlank() } ?: return null
    return Song(
        id = id,
        title = title,
        artist = optString("artist").ifBlank { "Unknown Artist" },
        album = optString("album").ifBlank { "Unknown Album" },
        coverUrl = optString("coverUrl"),
        durationMs = optLong("durationMs", 0L),
        sourceId = optString("sourceId").ifBlank { "netease" },
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
