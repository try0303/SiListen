package com.silisten.app.data.repository

import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.PlaylistCommentBundle
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.Song
import com.silisten.app.data.source.MusicSource
import com.silisten.app.data.source.MusicSourceRegistry
import com.silisten.app.data.source.NeteaseActionResult
import com.silisten.app.data.source.NeteaseMusicSource

class MusicRepository(
    val registry: MusicSourceRegistry
) {
    fun sources(): List<MusicSource> = registry.all()

    fun source(sourceId: String): MusicSource = registry.byId(sourceId)

    fun defaultSourceId(): String = registry.default().info.id

    suspend fun featured(sourceId: String): List<MusicPlaylist> =
        source(sourceId).featured()

    suspend fun search(sourceId: String, query: String): List<Song> =
        source(sourceId).search(query)

    suspend fun lyrics(song: Song): List<LyricLine> =
        source(song.sourceId).lyrics(song)

    suspend fun neteaseDailyDiscovery(): MusicPlaylist =
        netease().dailyDiscovery()

    suspend fun neteasePersonalFm(): MusicPlaylist =
        netease().personalFm()

    suspend fun neteaseRecommendedPlaylists(): List<MusicPlaylist> =
        netease().recommendedPlaylists()

    suspend fun neteaseUserPlaylists(userId: Long): List<MusicPlaylist> =
        netease().userPlaylists(userId)

    suspend fun neteasePlaylistDetail(playlist: MusicPlaylist): MusicPlaylist =
        netease().playlistDetail(playlist)

    suspend fun neteaseLikedSongs(userId: Long): MusicPlaylist =
        netease().likedSongs(userId)

    suspend fun neteaseLikedSongIds(userId: Long): Set<String> =
        netease().likedSongIds(userId)

    suspend fun neteaseCloudSongs(): MusicPlaylist =
        netease().cloudSongs()

    suspend fun neteasePodcasts(): MusicPlaylist =
        netease().podcasts()

    suspend fun neteasePlaylistComments(
        playlist: MusicPlaylist,
        sort: PlaylistCommentSort
    ): PlaylistCommentBundle = when (sort) {
        PlaylistCommentSort.Hot -> netease().hotPlaylistComments(playlist)
        PlaylistCommentSort.Latest -> netease().playlistComments(playlist)
    }

    suspend fun neteaseSongComments(
        song: Song,
        sort: PlaylistCommentSort
    ): PlaylistCommentBundle = when (sort) {
        PlaylistCommentSort.Hot -> netease().hotSongComments(song)
        PlaylistCommentSort.Latest -> netease().songComments(song)
    }

    suspend fun neteaseTogglePlaylistSubscription(
        playlist: MusicPlaylist,
        subscribe: Boolean
    ): NeteaseActionResult = netease().togglePlaylistSubscription(playlist, subscribe)

    suspend fun neteaseToggleSongLike(
        song: Song,
        like: Boolean
    ): NeteaseActionResult = netease().toggleSongLike(song, like)

    private fun netease(): NeteaseMusicSource =
        registry.byId("netease") as? NeteaseMusicSource
            ?: error("网易云音源未注册")
}
