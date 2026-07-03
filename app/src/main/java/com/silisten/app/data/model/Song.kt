package com.silisten.app.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val durationMs: Long = 0L,
    val sourceId: String,
    val streamHint: String? = null,
    val canonicalSourceId: String? = null,
    val canonicalSongId: String? = null,
    val playbackSourceId: String? = null,
    val providerIds: Map<String, String> = emptyMap()
)

fun Song.neteaseIdentityId(): String? = when {
    sourceId == SourcePlatformIds.NETEASE -> id
    canonicalSourceId == SourcePlatformIds.NETEASE && !canonicalSongId.isNullOrBlank() -> canonicalSongId
    else -> providerIds[SourcePlatformIds.NETEASE]
}

fun Song.withPlaybackSource(sourceId: String, matchedSongId: String): Song =
    copy(
        playbackSourceId = sourceId,
        providerIds = providerIds + (sourceId to matchedSongId)
    )

fun Song.withCanonicalIdentity(sourceId: String, songId: String): Song =
    copy(
        canonicalSourceId = sourceId,
        canonicalSongId = songId,
        providerIds = providerIds + (sourceId to songId)
    )

data class MusicPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val songs: List<Song>,
    val kind: PlaylistKind = PlaylistKind.Playlist,
    val description: String = "",
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val mvCount: Int = 0,
    val creatorUserId: Long = 0L,
    val subscribed: Boolean = false,
    val albums: List<MusicPlaylist> = emptyList()
)

enum class PlaylistKind {
    Playlist,
    DailyDiscovery,
    PersonalFm,
    Podcast,
    CloudDrive,
    LikedSongs,
    UserPlaylist,
    Album,
    Artist,
    LocalMusic,
    LocalPlaylist,
    Donation
}

data class PlaylistComment(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val timeLabel: String,
    val likedCount: Int = 0,
    val replyCount: Int = 0,
    val images: List<PlaylistCommentImage> = emptyList(),
    val replies: List<PlaylistCommentReply> = emptyList()
)

data class PlaylistCommentImage(
    val url: String,
    val width: Int = 0,
    val height: Int = 0
)

data class PlaylistCommentReply(
    val authorName: String,
    val content: String,
    val timeLabel: String = "",
    val likedCount: Int = 0
)

data class PlaylistCommentBundle(
    val comments: List<PlaylistComment>,
    val totalCount: Int
)

enum class PlaylistRoute {
    Overview,
    Comments
}

enum class PlaylistCommentSort {
    Hot,
    Latest
}

data class MusicSourceInfo(
    val id: String,
    val name: String,
    val description: String,
    val badge: String,
    val accentHex: Long
)

data class LyricWord(
    val offsetMs: Long,
    val durationMs: Long,
    val text: String
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null,
    val romanization: String? = null,
    val words: List<LyricWord>? = null
)

enum class PlaybackQuality(
    val label: String,
    val description: String,
    val neteaseLevel: String,
    val bitrate: Int
) {
    Standard("标准", "128 kbps，加载最快", "standard", 128),
    Higher("较高", "192 kbps，兼顾速度和音质", "higher", 192),
    ExHigh("极高", "320 kbps，默认推荐", "exhigh", 320),
    Lossless("无损", "优先请求 lossless，可能需要会员或登录态", "lossless", 999)
}
