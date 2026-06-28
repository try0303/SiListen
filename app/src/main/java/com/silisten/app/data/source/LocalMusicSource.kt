package com.silisten.app.data.source

import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.Song

class LocalMusicSource : MusicSource {
    override val info = MusicSourceInfo(
        id = "local",
        name = "本地音乐",
        description = "读取设备 MediaStore 中的本地音频，播放时使用系统 content 地址。",
        badge = "本地",
        accentHex = 0xFF8BD3FF
    )

    override suspend fun featured(): List<MusicPlaylist> = emptyList()

    override suspend fun search(keyword: String): List<Song> = emptyList()

    override suspend fun streamUrl(song: Song): String = song.streamHint.orEmpty()
}
