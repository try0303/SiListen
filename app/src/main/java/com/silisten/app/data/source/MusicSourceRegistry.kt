package com.silisten.app.data.source

import com.silisten.app.data.model.PlaybackQuality

class MusicSourceRegistry(private val sources: List<MusicSource>) {
    fun all(): List<MusicSource> = sources

    fun default(): MusicSource = sources.first()

    fun findById(id: String): MusicSource? = sources.firstOrNull { it.info.id == id }

    fun byId(id: String): MusicSource = sources.firstOrNull { it.info.id == id } ?: default()

    companion object {
        fun create(
            apiClient: NeteaseApiClient,
            qualityProvider: () -> PlaybackQuality = { PlaybackQuality.ExHigh }
        ): MusicSourceRegistry = MusicSourceRegistry(
            listOf(
                NeteaseMusicSource(apiClient, qualityProvider),
                LxPlatformMusicSource.kuwo(),
                LxPlatformMusicSource.kugou(),
                LxPlatformMusicSource.qq(),
                LxPlatformMusicSource.migu(),
                LocalMusicSource()
                // DemoMusicSource is offline sample audio only; keep it out of production registry
                // so search / default fallback never surface fake catalog rows.
            )
        )
    }
}
