package com.silisten.app.data.source

import com.silisten.app.data.model.PlaybackQuality

class MusicSourceRegistry(private val sources: List<MusicSource>) {
    fun all(): List<MusicSource> = sources

    fun default(): MusicSource = sources.first()

    fun byId(id: String): MusicSource = sources.firstOrNull { it.info.id == id } ?: default()

    companion object {
        fun create(
            apiClient: NeteaseApiClient,
            qualityProvider: () -> PlaybackQuality = { PlaybackQuality.ExHigh }
        ): MusicSourceRegistry = MusicSourceRegistry(
            listOf(
                NeteaseMusicSource(apiClient, qualityProvider),
                LocalMusicSource(),
                DemoMusicSource()
            )
        )
    }
}
