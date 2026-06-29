package com.silisten.app.data.source

import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.PlaylistComment
import com.silisten.app.data.model.PlaylistCommentBundle
import com.silisten.app.data.model.PlaylistKind
import com.silisten.app.data.model.Song
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class NeteaseMusicSource(
    private val apiClient: NeteaseApiClient,
    private val qualityProvider: () -> PlaybackQuality = { PlaybackQuality.ExHigh }
) : MusicSource {
    override val info = MusicSourceInfo(
        id = "netease",
        name = "网易云音乐",
        description = "默认音源。优先直接连接网易云音乐，网络不稳定时会自动尝试备用方式。",
        badge = "默认",
        accentHex = 0xFF1ED760
    )

    private val streamCache = ConcurrentHashMap<String, CachedStreamUrl>()
    private val searchCache = ConcurrentHashMap<String, CachedSearch>()
    private val lyricCache = ConcurrentHashMap<String, CachedLyrics>()
    private val playlistCache = ConcurrentHashMap<String, CachedPlaylist>()
    private val playlistListCache = ConcurrentHashMap<String, CachedPlaylistList>()

    override suspend fun featured(): List<MusicPlaylist> {
        playlistListCache["featured"]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.items?.let { return it }
        val songs = search("华语流行").take(12).ifEmpty { seedSongs() }
        return listOf(
            MusicPlaylist(
                id = "netease-daily",
                title = "网易云今日灵感",
                subtitle = "默认音源推荐",
                coverUrl = songs.firstOrNull()?.coverUrl.orEmpty(),
                songs = songs,
                kind = PlaylistKind.DailyDiscovery
            ),
            MusicPlaylist(
                id = "netease-night",
                title = "深夜漫游",
                subtitle = "iOS 质感加一点 Spotify 氛围",
                coverUrl = songs.getOrNull(1)?.coverUrl ?: songs.firstOrNull()?.coverUrl.orEmpty(),
                songs = songs.shuffled().take(8),
                kind = PlaylistKind.Playlist
            )
        ).also {
            playlistListCache["featured"] = CachedPlaylistList(it, System.currentTimeMillis() + 2 * 60 * 1000)
        }
    }

    suspend fun dailyDiscovery(): MusicPlaylist = withContext(Dispatchers.IO) {
        playlistCache["daily-discovery"]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.item?.let {
            return@withContext it
        }
        val songs = runCatching {
            val json = apiClient.getJson("/recommend/songs?timestamp=${System.currentTimeMillis()}")
            json.optJSONObject("data")?.optJSONArray("dailySongs").orEmpty().toSongs()
        }.getOrDefault(emptyList()).ifEmpty { search("每日推荐").take(20) }
        MusicPlaylist(
            id = "netease-daily-discovery",
            title = "每日发现",
            subtitle = if (songs.isEmpty()) "登录后可获取网易云每日推荐" else "根据你的网易云听歌偏好推荐",
            coverUrl = songs.firstOrNull()?.coverUrl ?: defaultCover,
            songs = songs,
            kind = PlaylistKind.DailyDiscovery
        ).also {
            playlistCache["daily-discovery"] = CachedPlaylist(it, System.currentTimeMillis() + 90_000L)
        }
    }

    suspend fun personalFm(): MusicPlaylist = withContext(Dispatchers.IO) {
        playlistCache["personal-fm"]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.item?.let {
            return@withContext it
        }
        val songs = runCatching {
            val json = apiClient.getJson("/personal_fm?timestamp=${System.currentTimeMillis()}")
            json.optJSONArray("data").orEmpty().toSongs()
        }.getOrDefault(emptyList()).ifEmpty { search("私人漫游").take(8) }
        MusicPlaylist(
            id = "netease-personal-fm",
            title = "每日 FM",
            subtitle = "网易云私人 FM",
            coverUrl = songs.firstOrNull()?.coverUrl ?: defaultCover,
            songs = songs,
            kind = PlaylistKind.PersonalFm
        ).also {
            playlistCache["personal-fm"] = CachedPlaylist(it, System.currentTimeMillis() + 90_000L)
        }
    }

    suspend fun recommendedPlaylists(limit: Int = 12): List<MusicPlaylist> = withContext(Dispatchers.IO) {
        val cacheKey = "recommended-$limit"
        playlistListCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.items?.let {
            return@withContext it
        }
        runCatching {
            val json = apiClient.getJson("/recommend/resource?timestamp=${System.currentTimeMillis()}")
            json.optJSONArray("recommend").orEmpty().toPlaylistShells(limit)
        }.getOrDefault(emptyList()).ifEmpty { featured() }.also {
            playlistListCache[cacheKey] = CachedPlaylistList(it, System.currentTimeMillis() + 2 * 60 * 1000)
        }
    }

    suspend fun userPlaylists(userId: Long): List<MusicPlaylist> = withContext(Dispatchers.IO) {
        if (userId == 0L) return@withContext emptyList()
        val cacheKey = "user-playlists-$userId"
        playlistListCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.items?.let {
            return@withContext it
        }
        runCatching {
            val json = apiClient.getJson(
                "/user/playlist?uid=$userId&limit=40&timestamp=${System.currentTimeMillis()}"
            )
            json.optJSONArray("playlist").orEmpty().toPlaylistShells(40, PlaylistKind.UserPlaylist)
        }.getOrDefault(emptyList()).also {
            playlistListCache[cacheKey] = CachedPlaylistList(it, System.currentTimeMillis() + 2 * 60 * 1000)
        }
    }

    suspend fun playlistDetail(playlist: MusicPlaylist): MusicPlaylist = withContext(Dispatchers.IO) {
        playlistCache[playlist.id]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.item?.let {
            return@withContext it
        }
        val numericId = extractPlaylistNumericId(playlist)
        val songs = runCatching {
            val json = apiClient.getJson(
                "/playlist/detail?id=${numericId.encode()}&timestamp=${System.currentTimeMillis()}"
            )
            val playlistJson = json.optJSONObject("playlist")
            val tracks = playlistJson?.optJSONArray("tracks").orEmpty().toSongs()
            tracks.ifEmpty {
                val ids = playlistJson?.optJSONArray("trackIds").orEmpty().ids()
                songsByIds(ids.take(50))
            }
        }.getOrDefault(emptyList())
        playlist.copy(
            subtitle = if (songs.isEmpty()) playlist.subtitle else "${songs.size} 首歌曲",
            coverUrl = playlist.coverUrl.ifBlank { songs.firstOrNull()?.coverUrl ?: defaultCover },
            songs = songs
        ).also {
            playlistCache[playlist.id] = CachedPlaylist(it, System.currentTimeMillis() + 5 * 60 * 1000)
        }
    }

    suspend fun likedSongs(userId: Long): MusicPlaylist = withContext(Dispatchers.IO) {
        val cacheKey = "liked-$userId"
        playlistCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.item?.let {
            return@withContext it
        }
        val songs = runCatching {
            val json = apiClient.getJson("/likelist?uid=$userId&timestamp=${System.currentTimeMillis()}")
            songsByIds(json.optJSONArray("ids").orEmpty().longValues().take(50))
        }.getOrDefault(emptyList())
        MusicPlaylist(
            id = "netease-liked-$userId",
            title = "喜欢的音乐",
            subtitle = if (songs.isEmpty()) "登录后同步网易云喜欢列表" else "${songs.size} 首喜欢的歌",
            coverUrl = songs.firstOrNull()?.coverUrl ?: defaultCover,
            songs = songs,
            kind = PlaylistKind.LikedSongs
        ).also {
            playlistCache[cacheKey] = CachedPlaylist(it, System.currentTimeMillis() + 2 * 60 * 1000)
        }
    }

    suspend fun likedSongIds(userId: Long): Set<String> = withContext(Dispatchers.IO) {
        if (userId == 0L) return@withContext emptySet()
        val json = apiClient.getJson("/likelist?uid=$userId&timestamp=${System.currentTimeMillis()}")
        json.optJSONArray("ids").orEmpty().longValues().mapTo(linkedSetOf()) { it.toString() }
    }

    suspend fun playlistComments(playlist: MusicPlaylist, limit: Int = 30): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
            val numericId = extractPlaylistNumericId(playlist)
            val json = apiClient.getJson(
                "/comment/playlist?id=${numericId.encode()}&limit=$limit&sortType=2&timestamp=${System.currentTimeMillis()}"
            )
            val comments = json.optJSONArray("comments").orEmpty().toPlaylistComments()
            PlaylistCommentBundle(
                comments = comments,
                totalCount = json.optInt("total", comments.size)
            )
        }

    suspend fun hotPlaylistComments(playlist: MusicPlaylist, limit: Int = 30): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
            val numericId = extractPlaylistNumericId(playlist)
            val json = apiClient.getJson(
                "/comment/hot?id=${numericId.encode()}&type=2&limit=$limit&timestamp=${System.currentTimeMillis()}"
            )
            val comments = json.optJSONArray("hotComments").orEmpty().toPlaylistComments()
            PlaylistCommentBundle(
                comments = comments,
                totalCount = comments.size
            )
        }

    suspend fun songComments(song: Song, limit: Int = 30): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
            val json = apiClient.getJson(
                "/comment/music?id=${song.id.encode()}&limit=$limit&sortType=2&timestamp=${System.currentTimeMillis()}"
            )
            val comments = json.optJSONArray("comments").orEmpty().toPlaylistComments()
            PlaylistCommentBundle(
                comments = comments,
                totalCount = json.optInt("total", comments.size)
            )
        }

    suspend fun hotSongComments(song: Song, limit: Int = 30): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
            val json = apiClient.getJson(
                "/comment/hot?id=${song.id.encode()}&type=0&limit=$limit&timestamp=${System.currentTimeMillis()}"
            )
            val comments = json.optJSONArray("hotComments").orEmpty().toPlaylistComments()
            PlaylistCommentBundle(
                comments = comments,
                totalCount = comments.size
            )
        }

    suspend fun togglePlaylistSubscription(
        playlist: MusicPlaylist,
        subscribe: Boolean
    ): NeteaseActionResult = withContext(Dispatchers.IO) {
        val numericId = extractPlaylistNumericId(playlist)
        val json = apiClient.getJson(
            "/playlist/subscribe?t=${if (subscribe) 1 else 2}&id=${numericId.encode()}&timestamp=${System.currentTimeMillis()}"
        )
        val success = json.optInt("code") == 200
        NeteaseActionResult(
            success = success,
            message = if (success) {
                if (subscribe) "已收藏歌单" else "已取消收藏"
            } else {
                json.optString("message").cleanText("歌单收藏操作失败")
            }
        )
    }

    suspend fun toggleSongLike(song: Song, like: Boolean): NeteaseActionResult = withContext(Dispatchers.IO) {
        val json = apiClient.getJson(
            "/like?id=${song.id.encode()}&like=$like&timestamp=${System.currentTimeMillis()}"
        )
        val success = json.optInt("code") == 200
        NeteaseActionResult(
            success = success,
            message = if (success) {
                if (like) "已加入红心歌曲" else "已取消红心"
            } else {
                json.optString("message").cleanText("歌曲红心操作失败")
            }
        )
    }

    suspend fun cloudSongs(): MusicPlaylist = withContext(Dispatchers.IO) {
        playlistCache["cloud"]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.item?.let {
            return@withContext it
        }
        val songs = runCatching {
            val json = apiClient.getJson("/user/cloud?limit=50&timestamp=${System.currentTimeMillis()}")
            json.optJSONArray("data").orEmpty().toCloudSongs()
        }.getOrDefault(emptyList())
        MusicPlaylist(
            id = "netease-cloud-drive",
            title = "云盘",
            subtitle = if (songs.isEmpty()) "登录后读取网易云音乐云盘" else "${songs.size} 首云盘歌曲",
            coverUrl = songs.firstOrNull()?.coverUrl ?: defaultCover,
            songs = songs,
            kind = PlaylistKind.CloudDrive
        ).also {
            playlistCache["cloud"] = CachedPlaylist(it, System.currentTimeMillis() + 2 * 60 * 1000)
        }
    }

    suspend fun podcasts(): MusicPlaylist = withContext(Dispatchers.IO) {
        playlistCache["podcasts"]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.item?.let {
            return@withContext it
        }
        val songs = runCatching {
            val json = apiClient.getJson("/dj/recommend?timestamp=${System.currentTimeMillis()}")
            json.optJSONArray("data").orEmpty().toDjSongs()
        }.getOrDefault(emptyList())
        MusicPlaylist(
            id = "netease-podcasts",
            title = "播客",
            subtitle = if (songs.isEmpty()) "网易云播客推荐暂时不可用" else "网易云播客推荐",
            coverUrl = songs.firstOrNull()?.coverUrl ?: defaultCover,
            songs = songs,
            kind = PlaylistKind.Podcast
        ).also {
            playlistCache["podcasts"] = CachedPlaylist(it, System.currentTimeMillis() + 2 * 60 * 1000)
        }
    }

    override suspend fun search(keyword: String): List<Song> = withContext(Dispatchers.IO) {
        val query = keyword.trim().ifEmpty { "华语流行" }
        searchCache[query]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.songs?.let {
            return@withContext it
        }

        val songs = searchNeteaseGateway(query)
        val ttl = if (songs.isEmpty()) 60_000L else 3 * 60 * 1000L
        searchCache[query] = CachedSearch(songs, System.currentTimeMillis() + ttl)
        songs
    }

    override suspend fun streamUrl(song: Song): String = withContext(Dispatchers.IO) {
        val quality = qualityProvider()
        val cacheKey = "${song.id}:${quality.neteaseLevel}:${quality.bitrate}"
        streamCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.url?.let {
            return@withContext it
        }

        val playableUrl = officialStreamUrl(song, quality)
        streamCache[cacheKey] = CachedStreamUrl(
            url = playableUrl,
            expiresAt = System.currentTimeMillis() + 12 * 60 * 1000
        )
        playableUrl
    }

    override suspend fun lyrics(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        lyricCache[song.id]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.lines?.let {
            return@withContext it
        }
        val lines = runCatching {
            val json = apiClient.getJson("/lyric?id=${song.id.encode()}&timestamp=${System.currentTimeMillis()}")
            json.optJSONObject("lrc")?.optString("lyric").orEmpty().parseLrc()
        }.getOrDefault(emptyList())
        val playableLines = lines.ifEmpty {
            listOf(LyricLine(0L, "暂时没有歌词"))
        }
        lyricCache[song.id] = CachedLyrics(playableLines, System.currentTimeMillis() + 30 * 60 * 1000)
        playableLines
    }

    private suspend fun searchNeteaseGateway(query: String): List<Song> {
        val encodedQuery = query.encode()
        val json = runCatching {
            apiClient.getJson("/search?keywords=$encodedQuery&limit=30&timestamp=${System.currentTimeMillis()}")
        }.getOrNull() ?: return seedSongs().filterBy(query)

        val songs = json.optJSONObject("result")?.optJSONArray("songs").orEmpty()
        return buildList {
            for (index in 0 until songs.length()) {
                val item = songs.optJSONObject(index) ?: continue
                add(
                    Song(
                        id = item.optString("id"),
                        title = item.optString("name").cleanText("未知歌曲"),
                        artist = item.optJSONArray("artists").artistNames()
                            .ifBlank { item.optJSONArray("ar").artistNames() }
                            .ifBlank { "未知歌手" },
                        album = item.optJSONObject("album")?.optString("name")?.cleanText()
                            ?: item.optJSONObject("al")?.optString("name")?.cleanText()
                            ?: "未知专辑",
                        coverUrl = item.optJSONObject("album")?.optString("picUrl")?.cleanText()
                            ?: item.optJSONObject("al")?.optString("picUrl")?.cleanText()
                            ?: defaultCover,
                        durationMs = item.optLong("duration", item.optLong("dt", 0L)),
                        sourceId = info.id
                    )
                )
            }
        }.ifEmpty { seedSongs().filterBy(query) }
    }

    private suspend fun officialStreamUrl(song: Song, quality: PlaybackQuality): String {
        val streamJson = runCatching {
            apiClient.getJson("/song/url/v1?id=${song.id}&level=${quality.neteaseLevel}&timestamp=${System.currentTimeMillis()}")
        }.getOrNull()
        val streamData = streamJson?.optJSONArray("data")?.optJSONObject(0)
        val streamUrl = streamData?.optString("url").orEmpty()
        if (streamUrl.isNotBlank()) {
            return streamUrl
        }

        val downloadJson = runCatching {
            apiClient.getJson("/song/download/url?id=${song.id}&br=${quality.bitrate}&timestamp=${System.currentTimeMillis()}")
        }.getOrNull()
        val downloadUrl = downloadJson?.optJSONObject("data")?.optString("url").orEmpty()
        return downloadUrl.ifBlank { "https://music.163.com/song/media/outer/url?id=${song.id}.mp3" }
    }

    private suspend fun songsByIds(ids: List<Long>): List<Song> {
        if (ids.isEmpty()) return emptyList()
        val joined = ids.joinToString(",")
        val json = apiClient.getJson("/song/detail?ids=$joined&timestamp=${System.currentTimeMillis()}")
        return json.optJSONArray("songs").orEmpty().toSongs()
    }

    private fun seedSongs(): List<Song> = listOf(
        Song("33894312", "起风了", "买辣椒也用券", "起风了", defaultCover, 325_000, info.id),
        Song("1901371647", "哪里都是你", "队长", "哪里都是你", defaultCover, 222_000, info.id),
        Song("1827600686", "删了吧", "烟", "删了吧", defaultCover, 221_000, info.id),
        Song("26259003", "海阔天空", "Beyond", "乐与怒", defaultCover, 326_000, info.id)
    )

    private fun List<Song>.filterBy(keyword: String): List<Song> {
        val value = keyword.trim()
        if (value.isEmpty()) return this
        return filter {
            it.title.contains(value, ignoreCase = true) ||
                it.artist.contains(value, ignoreCase = true)
        }.ifEmpty { this }
    }

    private fun JSONArray.toSongs(): List<Song> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                item.toSong()?.let(::add)
            }
        }
    }

    private fun JSONArray.toCloudSongs(): List<Song> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                (item.optJSONObject("simpleSong") ?: item.optJSONObject("song") ?: item).toSong()?.let(::add)
            }
        }
    }

    private fun JSONArray.toDjSongs(): List<Song> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id").ifBlank { item.optString("mainTrackId") }
                val title = item.optString("name").ifBlank { item.optString("rcmdtext") }
                if (id.isBlank() || title.isBlank()) continue
                add(
                    Song(
                        id = id,
                        title = title,
                        artist = item.optJSONObject("dj")?.optString("nickname").orEmpty().ifBlank { "网易云播客" },
                        album = item.optString("category").ifBlank { "播客" },
                        coverUrl = item.optString("picUrl").ifBlank { item.optString("coverUrl") }.ifBlank { defaultCover },
                        durationMs = item.optLong("duration", 0L),
                        sourceId = info.id
                    )
                )
            }
        }
    }

    private fun JSONArray.toPlaylistShells(limit: Int, kind: PlaylistKind = PlaylistKind.Playlist): List<MusicPlaylist> {
        return buildList {
            for (index in 0 until length().coerceAtMost(limit)) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                    .ifBlank { item.optString("playlistId") }
                    .ifBlank { item.optString("resourceId") }
                if (id.isBlank()) continue
                add(
                    MusicPlaylist(
                        id = "netease-playlist-$id",
                        title = item.optString("name").cleanText()
                            .ifBlank { item.optString("title").cleanText() }
                            .ifBlank { "网易云歌单" },
                        subtitle = item.optString("copywriter").cleanText()
                            .ifBlank { item.optString("description").cleanText() }
                            .ifBlank {
                                item.optLong("trackCount", 0L).takeIf { it > 0 }?.let { "$it 首歌曲" }.orEmpty()
                            },
                        coverUrl = item.optString("picUrl").cleanText()
                            .ifBlank { item.optString("coverImgUrl").cleanText() }
                            .ifBlank { defaultCover },
                        songs = emptyList(),
                        kind = kind
                    )
                )
            }
        }
    }

    private fun JSONArray.toPlaylistComments(): List<PlaylistComment> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val user = item.optJSONObject("user")
                add(
                    PlaylistComment(
                        id = item.optString("commentId").ifBlank { "comment-$index" },
                        authorName = user?.optString("nickname").cleanText("网易云用户"),
                        authorAvatarUrl = user?.optString("avatarUrl").cleanText(),
                        content = item.optString("content").cleanText("这条评论暂时没有内容"),
                        timeLabel = item.optString("timeStr").cleanText().ifBlank {
                            item.optLong("time", 0L).takeIf { value -> value > 0L }?.toCommentTimeLabel().orEmpty()
                        }.ifBlank { "刚刚" },
                        likedCount = item.optInt("likedCount", 0),
                        replyCount = item.optInt("replyCount", 0)
                    )
                )
            }
        }
    }

    private fun JSONObject.toSong(): Song? {
        val id = optString("id").ifBlank { optString("songId") }
        if (id.isBlank() || id == "0") return null
        val albumJson = optJSONObject("al") ?: optJSONObject("album")
        return Song(
            id = id,
            title = optString("name").cleanText("未知歌曲"),
            artist = optJSONArray("ar").artistNames()
                .ifBlank { optJSONArray("artists").artistNames() }
                .ifBlank { optJSONArray("artist").stringValues() }
                .ifBlank { "未知歌手" },
            album = albumJson?.optString("name").orEmpty().cleanText().ifBlank { "未知专辑" },
            coverUrl = albumJson?.optString("picUrl").orEmpty().cleanText()
                .ifBlank { optString("picUrl").cleanText() }
                .ifBlank { optString("coverUrl").cleanText() }
                .ifBlank { defaultCover },
            durationMs = optLong("dt", optLong("duration", 0L)),
            sourceId = info.id
        )
    }

    private fun JSONArray.ids(): List<Long> {
        return buildList {
            for (index in 0 until length()) {
                val id = optJSONObject(index)?.optLong("id", 0L) ?: optLong(index, 0L)
                if (id > 0) add(id)
            }
        }
    }

    private fun JSONArray.longValues(): List<Long> {
        return buildList {
            for (index in 0 until length()) {
                val id = optLong(index, 0L)
                if (id > 0) add(id)
            }
        }
    }

    private fun JSONArray?.artistNames(): String {
        if (this == null) return ""
        return buildList {
            for (index in 0 until length()) {
                val name = optJSONObject(index)?.optString("name").orEmpty()
                if (name.isNotBlank()) add(name)
            }
        }.joinToString(" / ")
    }

    private fun JSONArray?.stringValues(): String {
        if (this == null) return ""
        return buildList {
            for (index in 0 until length()) {
                val name = optString(index).cleanText()
                if (name.isNotBlank()) add(name)
            }
        }.joinToString(" / ")
    }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")

    private fun String?.cleanText(fallback: String = ""): String {
        val value = this.orEmpty().trim()
        return when {
            value.isBlank() -> fallback
            value.equals("null", ignoreCase = true) -> fallback
            else -> value
        }
    }

    private fun Long.toCommentTimeLabel(): String =
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(this))

    private fun extractPlaylistNumericId(playlist: MusicPlaylist): String {
        return playlist.id.substringAfter("netease-playlist-", missingDelimiterValue = playlist.id)
            .substringAfter("netease-liked-", missingDelimiterValue = playlist.id)
            .substringAfterLast('-')
    }

    private fun String.parseLrc(): List<LyricLine> {
        val timePattern = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?]")
        return lineSequence().flatMap { rawLine ->
            val matches = timePattern.findAll(rawLine).toList()
            val text = rawLine.replace(timePattern, "").trim()
            if (matches.isEmpty() || text.isBlank()) {
                emptySequence()
            } else {
                matches.asSequence().mapNotNull { match ->
                    val minute = match.groupValues.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                    val second = match.groupValues.getOrNull(2)?.toLongOrNull() ?: return@mapNotNull null
                    val fraction = match.groupValues.getOrNull(3).orEmpty()
                    val millis = when (fraction.length) {
                        0 -> 0L
                        1 -> fraction.toLong() * 100
                        2 -> fraction.toLong() * 10
                        else -> fraction.take(3).toLong()
                    }
                    LyricLine((minute * 60 + second) * 1000 + millis, text)
                }
            }
        }.sortedBy { it.timeMs }.toList()
    }

    private data class CachedStreamUrl(
        val url: String,
        val expiresAt: Long
    )

    private data class CachedSearch(
        val songs: List<Song>,
        val expiresAt: Long
    )

    private data class CachedLyrics(
        val lines: List<LyricLine>,
        val expiresAt: Long
    )

    private data class CachedPlaylist(
        val item: MusicPlaylist,
        val expiresAt: Long
    )

    private data class CachedPlaylistList(
        val items: List<MusicPlaylist>,
        val expiresAt: Long
    )

    private companion object {
        const val defaultCover = "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900"
    }
}
