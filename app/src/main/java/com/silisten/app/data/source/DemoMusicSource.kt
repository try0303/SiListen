package com.silisten.app.data.source

import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.Song

class DemoMusicSource : MusicSource {
    override val info = MusicSourceInfo(
        id = "demo",
        name = "公开示例",
        description = "用于验证播放器队列的公开示例音频，接入新音源时也可按这个类的结构实现。",
        badge = "备用",
        accentHex = 0xFFFF6B6B
    )

    private val songs = listOf(
        Song(
            id = "soundhelix-1",
            title = "晨光草图",
            artist = "SiListen 实验室",
            album = "公开试听",
            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=900",
            durationMs = 372_000,
            sourceId = info.id,
            streamHint = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ),
        Song(
            id = "soundhelix-2",
            title = "城市夜行",
            artist = "SiListen 实验室",
            album = "公开试听",
            coverUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=900",
            durationMs = 295_000,
            sourceId = info.id,
            streamHint = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        ),
        Song(
            id = "soundhelix-3",
            title = "霓虹房间",
            artist = "SiListen 实验室",
            album = "公开试听",
            coverUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900",
            durationMs = 256_000,
            sourceId = info.id,
            streamHint = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        )
    )

    override suspend fun featured(): List<MusicPlaylist> = listOf(
        MusicPlaylist(
            id = "demo-featured",
            title = "试听队列",
            subtitle = "公开音频播放验证",
            coverUrl = songs.first().coverUrl,
            songs = songs
        )
    )

    override suspend fun search(keyword: String): List<Song> {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return songs
        return songs.filter {
            it.title.contains(normalized, ignoreCase = true) ||
                it.artist.contains(normalized, ignoreCase = true)
        }
    }

    override suspend fun streamUrl(song: Song): String = song.streamHint.orEmpty()
}
