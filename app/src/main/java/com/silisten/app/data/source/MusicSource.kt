package com.silisten.app.data.source

import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.PlaylistCommentBundle
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.Song

interface MusicSource {
    val info: MusicSourceInfo

    suspend fun featured(): List<MusicPlaylist>

    suspend fun search(keyword: String): List<Song>

    suspend fun streamUrl(song: Song): String

    suspend fun lyrics(song: Song): List<LyricLine> = emptyList()
}

interface PagedMusicSearchSource {
    suspend fun searchSongs(keyword: String, limit: Int, offset: Int): List<Song>
}

interface SongCommentSource {
    suspend fun commentsForSong(
        song: Song,
        sort: PlaylistCommentSort,
        limit: Int = 30,
        offset: Int = 0
    ): PlaylistCommentBundle
}
