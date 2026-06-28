package com.silisten.app.data.source

import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.Song

interface MusicSource {
    val info: MusicSourceInfo

    suspend fun featured(): List<MusicPlaylist>

    suspend fun search(keyword: String): List<Song>

    suspend fun streamUrl(song: Song): String

    suspend fun lyrics(song: Song): List<LyricLine> = emptyList()
}
