package com.silisten.app.data.source

import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.LyricWord
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.PlaylistComment
import com.silisten.app.data.model.PlaylistCommentBundle
import com.silisten.app.data.model.PlaylistCommentImage
import com.silisten.app.data.model.PlaylistCommentReply
import com.silisten.app.data.model.PlaylistCommentSort
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
) : MusicSource, PagedMusicSearchSource, SongCommentSource {
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
        if (playlist.id.startsWith("netease-album-")) {
            return@withContext albumDetail(playlist).also {
                playlistCache[playlist.id] = CachedPlaylist(it, System.currentTimeMillis() + 5 * 60 * 1000)
            }
        }
        if (playlist.id.startsWith("netease-artist-")) {
            return@withContext artistDetail(playlist).also {
                playlistCache[playlist.id] = CachedPlaylist(it, System.currentTimeMillis() + 5 * 60 * 1000)
            }
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

    private suspend fun albumDetail(playlist: MusicPlaylist): MusicPlaylist {
        val numericId = extractPlaylistNumericId(playlist)
        val json = runCatching {
            apiClient.getJson("/album?id=${numericId.encode()}&timestamp=${System.currentTimeMillis()}")
        }.getOrNull()
        val albumJson = json?.optJSONObject("album")
        val songs = json?.optJSONArray("songs").orEmpty().toSongs()
        return playlist.copy(
            title = albumJson?.optString("name").cleanText().ifBlank { playlist.title },
            subtitle = if (songs.isEmpty()) playlist.subtitle else "${songs.size} 首歌曲",
            coverUrl = albumJson?.optString("picUrl").cleanText()
                .ifBlank { playlist.coverUrl }
                .ifBlank { songs.firstOrNull()?.coverUrl ?: defaultCover },
            songs = songs,
            kind = PlaylistKind.Album
        )
    }

    private suspend fun artistDetail(playlist: MusicPlaylist): MusicPlaylist {
        val numericId = extractPlaylistNumericId(playlist)
        val json = runCatching {
            apiClient.getJson("/artists?id=${numericId.encode()}&timestamp=${System.currentTimeMillis()}")
        }.getOrNull()
        val albumsJson = runCatching {
            apiClient.getJson(
                "/artist/album?id=${numericId.encode()}&limit=40&offset=0&timestamp=${System.currentTimeMillis()}"
            )
        }.getOrNull()
        val artistJson = json?.optJSONObject("artist")
        val songs = json?.optJSONArray("hotSongs").orEmpty().toSongs()
        val albumCount = artistJson?.optInt("albumSize", playlist.albumCount) ?: playlist.albumCount
        val songCount = artistJson?.optInt("musicSize", playlist.songCount) ?: playlist.songCount
        val mvCount = artistJson?.optInt("mvSize", playlist.mvCount) ?: playlist.mvCount
        val albums = albumsJson
            ?.optJSONArray("hotAlbums")
            .orEmpty()
            .toAlbumShells(40)
        return playlist.copy(
            title = artistJson?.optString("name").cleanText().ifBlank { playlist.title },
            subtitle = if (songs.isEmpty()) playlist.subtitle else "${songs.size} 首热门歌曲",
            coverUrl = artistJson?.optString("picUrl").cleanText()
                .ifBlank { artistJson?.optString("img1v1Url").cleanText() }
                .ifBlank { playlist.coverUrl }
                .ifBlank { songs.firstOrNull()?.coverUrl ?: defaultCover },
            songs = songs,
            kind = PlaylistKind.Artist,
            description = artistJson?.optString("briefDesc").cleanText().ifBlank { playlist.description },
            albumCount = albumCount,
            songCount = songCount,
            mvCount = mvCount,
            albums = albums
        )
    }

    suspend fun artistSongs(
        artist: MusicPlaylist,
        limit: Int = 50,
        offset: Int = 0
    ): List<Song> = withContext(Dispatchers.IO) {
        val numericId = extractPlaylistNumericId(artist)
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val json = apiClient.getJson(
            "/artist/songs?id=${numericId.encode()}&limit=$safeLimit&offset=$safeOffset&order=time&timestamp=${System.currentTimeMillis()}"
        )
        json.optJSONArray("songs")
            ?: json.optJSONObject("data")?.optJSONArray("songs")
            ?: json.optJSONObject("result")?.optJSONArray("songs")
            ?: JSONArray()
    }.toSongs()

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

    suspend fun songComments(song: Song, limit: Int = 30, offset: Int = 0): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
            val pageNo = (offset / limit.coerceAtLeast(1)) + 1
            val json = apiClient.getJson(
                "/comment/music?id=${song.id.encode()}&limit=$limit&offset=$offset&pageNo=$pageNo&sortType=2&timestamp=${System.currentTimeMillis()}"
            )
            val comments = json.optJSONArray("comments").orEmpty().toPlaylistComments()
            PlaylistCommentBundle(
                comments = comments,
                totalCount = json.optInt("total", comments.size)
            )
        }

    suspend fun hotSongComments(song: Song, limit: Int = 30, offset: Int = 0): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
            val json = apiClient.getJson(
                "/comment/hot?id=${song.id.encode()}&type=0&limit=$limit&offset=$offset&timestamp=${System.currentTimeMillis()}"
            )
            val comments = json.optJSONArray("hotComments").orEmpty().toPlaylistComments()
            PlaylistCommentBundle(
                comments = comments,
                totalCount = json.optInt("total", comments.size)
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
                if (like) "已添加到我喜欢的音乐" else "已从我喜欢的音乐移除"
            } else {
                json.optString("message").cleanText("歌曲红心操作失败")
            }
        )
    }

    suspend fun addSongToPlaylist(song: Song, playlist: MusicPlaylist): NeteaseActionResult = withContext(Dispatchers.IO) {
        val numericId = extractPlaylistNumericId(playlist)
        val json = apiClient.getJson(
            "/playlist/tracks?op=add&pid=${numericId.encode()}&tracks=${song.id.encode()}&timestamp=${System.currentTimeMillis()}"
        )
        val success = json.optInt("code") == 200
        NeteaseActionResult(
            success = success,
            message = if (success) {
                "已添加到${playlist.title}"
            } else {
                json.optString("message").cleanText("加入歌单失败")
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

    override suspend fun search(keyword: String): List<Song> = searchSongs(keyword, 30, 0)

    override suspend fun searchSongs(
        keyword: String,
        limit: Int,
        offset: Int
    ): List<Song> = withContext(Dispatchers.IO) {
        val query = keyword.trim().ifEmpty { "华语流行" }
        val safeLimit = limit.coerceIn(1, 60)
        val safeOffset = offset.coerceAtLeast(0)
        val cacheKey = "songs:$query:$safeLimit:$safeOffset"
        searchCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.songs?.let {
            return@withContext it
        }

        val songs = searchNeteaseGateway(
            query = query,
            limit = safeLimit,
            offset = safeOffset
        )
        val ttl = if (songs.isEmpty()) 60_000L else 3 * 60 * 1000L
        searchCache[cacheKey] = CachedSearch(songs, System.currentTimeMillis() + ttl)
        songs
    }

    suspend fun searchPlaylists(
        keyword: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<MusicPlaylist> = withContext(Dispatchers.IO) {
        searchCollectionShells(
            keyword = keyword,
            limit = limit,
            offset = offset,
            type = "1000",
            cachePrefix = "search-playlists"
        ) { json ->
            json.optJSONObject("result")
                ?.optJSONArray("playlists")
                .orEmpty()
                .toPlaylistShells(limit.coerceIn(1, 50))
        }
    }

    suspend fun searchAlbums(
        keyword: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<MusicPlaylist> = withContext(Dispatchers.IO) {
        searchCollectionShells(
            keyword = keyword,
            limit = limit,
            offset = offset,
            type = "10",
            cachePrefix = "search-albums"
        ) { json ->
            json.optJSONObject("result")
                ?.optJSONArray("albums")
                .orEmpty()
                .toAlbumShells(limit.coerceIn(1, 50))
        }
    }

    suspend fun searchArtists(
        keyword: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<MusicPlaylist> = withContext(Dispatchers.IO) {
        searchCollectionShells(
            keyword = keyword,
            limit = limit,
            offset = offset,
            type = "100",
            cachePrefix = "search-artists"
        ) { json ->
            json.optJSONObject("result")
                ?.optJSONArray("artists")
                .orEmpty()
                .toArtistShells(limit.coerceIn(1, 50))
        }
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
            val yrcText = json.optJSONObject("yrc")?.optString("lyric").orEmpty()
            val yrcLines = yrcText.parseYrc()
            val lrcText = json.optJSONObject("lrc")?.optString("lyric").orEmpty()
            val tlyricText = json.optJSONObject("tlyric")?.optString("lyric").orEmpty()
            val romalrcText = json.optJSONObject("romalrc")?.optString("lyric").orEmpty()
            val baseLines = if (yrcLines.isNotEmpty()) yrcLines else lrcText.parseLrc()
            mergeLyrics(
                mainLines = baseLines,
                translationLines = tlyricText.parseLrc(),
                romanizationLines = romalrcText.parseLrc()
            )
        }.getOrDefault(emptyList())
        if (lines.isNotEmpty()) {
            lyricCache[song.id] = CachedLyrics(lines, System.currentTimeMillis() + 30 * 60 * 1000L)
            return@withContext lines
        }
        listOf(LyricLine(0L, "暂时没有歌词"))
    }

    private suspend fun searchNeteaseGateway(
        query: String,
        limit: Int,
        offset: Int
    ): List<Song> {
        val encodedQuery = query.encode()
        val json = runCatching {
            apiClient.getJson(
                "/search?keywords=$encodedQuery&type=1&limit=$limit&offset=$offset&timestamp=${System.currentTimeMillis()}"
            )
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
        }.ifEmpty {
            if (offset == 0) seedSongs().filterBy(query) else emptyList()
        }
    }

    private suspend fun searchCollectionShells(
        keyword: String,
        limit: Int,
        offset: Int,
        type: String,
        cachePrefix: String,
        parser: (JSONObject) -> List<MusicPlaylist>
    ): List<MusicPlaylist> {
        val query = keyword.trim().ifEmpty { "华语流行" }
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val cacheKey = "$cachePrefix:$query:$safeLimit:$safeOffset"
        playlistListCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.items?.let {
            return it
        }
        val encodedQuery = query.encode()
        val items = runCatching {
            val json = apiClient.getJson(
                "/search?keywords=$encodedQuery&type=$type&limit=$safeLimit&offset=$safeOffset&timestamp=${System.currentTimeMillis()}"
            )
            parser(json)
        }.getOrDefault(emptyList())
        playlistListCache[cacheKey] = CachedPlaylistList(
            items,
            System.currentTimeMillis() + if (items.isEmpty()) 60_000L else 3 * 60 * 1000L
        )
        return items
    }

    override suspend fun commentsForSong(
        song: Song,
        sort: PlaylistCommentSort,
        limit: Int,
        offset: Int
    ): PlaylistCommentBundle =
        when (sort) {
            PlaylistCommentSort.Hot -> hotSongComments(song, limit, offset)
            PlaylistCommentSort.Latest -> songComments(song, limit, offset)
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
        return downloadUrl
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

    private fun JSONArray.toAlbumShells(limit: Int): List<MusicPlaylist> {
        return buildList {
            for (index in 0 until length().coerceAtMost(limit)) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                val artistName = item.optJSONObject("artist")?.optString("name").cleanText()
                    .ifBlank { item.optJSONArray("artists").artistNames() }
                val size = item.optInt("size", 0)
                add(
                    MusicPlaylist(
                        id = "netease-album-$id",
                        title = item.optString("name").cleanText("网易云专辑"),
                        subtitle = listOfNotNull(
                            artistName.takeIf { it.isNotBlank() },
                            size.takeIf { it > 0 }?.let { "$it 首歌曲" }
                        ).joinToString(" · ").ifBlank { "专辑" },
                        coverUrl = item.optString("picUrl").cleanText()
                            .ifBlank { item.optString("blurPicUrl").cleanText() }
                            .ifBlank { defaultCover },
                        songs = emptyList(),
                        kind = PlaylistKind.Album,
                        songCount = size
                    )
                )
            }
        }
    }

    private fun JSONArray.toArtistShells(limit: Int): List<MusicPlaylist> {
        return buildList {
            for (index in 0 until length().coerceAtMost(limit)) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                val musicSize = item.optInt("musicSize", item.optInt("songSize", 0))
                val albumSize = item.optInt("albumSize", 0)
                val subtitle = buildList {
                    if (musicSize > 0) add("$musicSize 首歌曲")
                    if (albumSize > 0) add("$albumSize 张专辑")
                }.joinToString(" · ").ifBlank { "歌手" }
                add(
                    MusicPlaylist(
                        id = "netease-artist-$id",
                        title = item.optString("name").cleanText("网易云歌手"),
                        subtitle = subtitle,
                        coverUrl = item.optString("img1v1Url").cleanText()
                            .ifBlank { item.optString("picUrl").cleanText() }
                            .ifBlank { defaultCover },
                        songs = emptyList(),
                        kind = PlaylistKind.Artist,
                        albumCount = albumSize,
                        songCount = musicSize,
                        mvCount = item.optInt("mvSize", 0)
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
                val replies = item.commentReplies()
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
                        replyCount = maxOf(item.optInt("replyCount", 0), item.floorReplyCount(), replies.size),
                        images = item.commentImages(),
                        replies = replies
                    )
                )
            }
        }
    }

    private fun JSONObject.commentImages(): List<PlaylistCommentImage> {
        val arrays = listOf("picList", "pics", "images", "imageList")
            .mapNotNull { key -> optJSONArray(key) }
        val directObjects = listOf("picture", "image", "pic")
            .mapNotNull { key -> optJSONObject(key) }
        return buildList {
            arrays.forEach { array ->
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index)
                    val url = item?.commentImageUrl().orEmpty()
                        .ifBlank { array.optString(index).cleanText() }
                    if (url.isNotBlank()) {
                        add(
                            PlaylistCommentImage(
                                url = url,
                                width = item?.optInt("width", 0) ?: 0,
                                height = item?.optInt("height", 0) ?: 0
                            )
                        )
                    }
                }
            }
            directObjects.forEach { item ->
                val url = item.commentImageUrl()
                if (url.isNotBlank()) {
                    add(
                        PlaylistCommentImage(
                            url = url,
                            width = item.optInt("width", 0),
                            height = item.optInt("height", 0)
                        )
                    )
                }
            }
            listOf("imageUrl", "picUrl", "resourceUrl").forEach { key ->
                val url = optString(key).cleanText()
                if (url.isNotBlank()) add(PlaylistCommentImage(url))
            }
        }.distinctBy { it.url }
    }

    private fun JSONObject.commentImageUrl(): String =
        listOf("originUrl", "rectangleUrl", "squareUrl", "url", "picUrl", "imageUrl", "resourceUrl")
            .firstNotNullOfOrNull { key -> optString(key).cleanText().takeIf { it.isNotBlank() } }
            .orEmpty()

    private fun JSONObject.commentReplies(): List<PlaylistCommentReply> =
        buildList {
            addAll(optJSONArray("beReplied").toCommentReplies())
            addAll(optJSONArray("replies").toCommentReplies())
            addAll(optJSONObject("showFloorComment")?.optJSONArray("comments").toCommentReplies())
            addAll(optJSONObject("showFloorComment")?.optJSONArray("replies").toCommentReplies())
            addAll(optJSONObject("floorComment")?.optJSONArray("comments").toCommentReplies())
        }.distinctBy { "${it.authorName}:${it.content}" }

    private fun JSONObject.floorReplyCount(): Int =
        listOfNotNull(optJSONObject("showFloorComment"), optJSONObject("floorComment"))
            .maxOfOrNull { floor ->
                maxOf(
                    floor.optInt("replyCount", 0),
                    floor.optInt("totalCount", 0),
                    floor.optInt("count", 0),
                    floor.optJSONArray("comments")?.length() ?: 0,
                    floor.optJSONArray("replies")?.length() ?: 0
                )
            }
            ?: 0

    private fun JSONArray?.toCommentReplies(): List<PlaylistCommentReply> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val user = item.optJSONObject("user")
                    ?: item.optJSONObject("beRepliedUser")
                    ?: item.optJSONObject("replyUser")
                val content = item.optString("content").cleanText()
                    .ifBlank { item.optJSONObject("beRepliedComment")?.optString("content").cleanText() }
                    .ifBlank { item.optString("replyContent").cleanText() }
                if (content.isBlank()) continue
                add(
                    PlaylistCommentReply(
                        authorName = user?.optString("nickname").cleanText("用户"),
                        content = content,
                        timeLabel = item.optString("timeStr").cleanText().ifBlank {
                            item.optLong("time", 0L).takeIf { value -> value > 0L }?.toCommentTimeLabel().orEmpty()
                        },
                        likedCount = item.optInt("likedCount", 0)
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

    private fun String.parseYrc(): List<LyricLine> {
        // YRC format: [lineStartMs,lineDurationMs] then (wordOffsetMs,wordDurationMs,0)text per word
        // wordOffsetMs may be absolute (relative to song start) or relative (relative to line start).
        // We normalize to relative-to-line-start on parse so downstream always uses relative.
        val linePattern = Regex("\\[(\\d+),(\\d+)](.+)")
        val wordPattern = Regex("\\((\\d+),(\\d+),\\d+\\)([^(]*)")
        return lineSequence().mapNotNull { rawLine ->
            val lineMatch = linePattern.find(rawLine.trim()) ?: return@mapNotNull null
            val lineStartMs = lineMatch.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val wordSection = lineMatch.groupValues[3]
            val rawWords = wordPattern.findAll(wordSection).mapNotNull { wm ->
                val offsetMs = wm.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val durationMs = wm.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val text = wm.groupValues[3]
                if (text.isBlank()) return@mapNotNull null
                Triple(offsetMs, durationMs, text)
            }.toList()
            if (rawWords.isEmpty()) return@mapNotNull null
            val firstOffset = rawWords.first().first
            val isAbsolute = firstOffset >= lineStartMs && lineStartMs > 0L
            val words = rawWords.map { (offset, duration, text) ->
                val relativeOffset = if (isAbsolute) (offset - lineStartMs).coerceAtLeast(0L) else offset
                LyricWord(offsetMs = relativeOffset, durationMs = duration, text = text)
            }
            val fullText = words.joinToString("") { it.text }.trim()
            if (fullText.isBlank()) return@mapNotNull null
            LyricLine(timeMs = lineStartMs, text = fullText, words = words)
        }.sortedBy { it.timeMs }.toList()
    }

    private fun mergeLyrics(
        mainLines: List<LyricLine>,
        translationLines: List<LyricLine>,
        romanizationLines: List<LyricLine>
    ): List<LyricLine> {
        val baseLines = mainLines.ifEmpty { translationLines }.ifEmpty { romanizationLines }
        if (baseLines.isEmpty()) return emptyList()
        return baseLines.map { line ->
            val translation = translationLines.closestTextTo(line.timeMs)
                ?.takeIf { it != line.text }
            val romanization = romanizationLines.closestTextTo(line.timeMs)
                ?.takeIf { it != line.text && it != translation }
            line.copy(
                translation = translation,
                romanization = romanization
            )
        }.sortedBy { it.timeMs }
    }

    private fun List<LyricLine>.closestTextTo(timeMs: Long): String? {
        if (isEmpty()) return null
        val nearest = minByOrNull { kotlin.math.abs(it.timeMs - timeMs) } ?: return null
        return nearest.text.takeIf {
            it.isNotBlank() && kotlin.math.abs(nearest.timeMs - timeMs) <= 500L
        }
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
