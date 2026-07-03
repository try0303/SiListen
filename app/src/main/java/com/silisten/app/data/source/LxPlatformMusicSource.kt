package com.silisten.app.data.source

import android.util.Base64
import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.PlaylistComment
import com.silisten.app.data.model.PlaylistCommentBundle
import com.silisten.app.data.model.PlaylistCommentReply
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.PlaylistKind
import com.silisten.app.data.model.SourcePlatformIds
import com.silisten.app.data.model.Song
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class LxPlatformMusicSource(
    private val platform: LxPlatform,
    private val client: OkHttpClient = defaultClient
) : MusicSource, PagedMusicSearchSource, CollectionSearchSource, CollectionDetailSource, SongCommentSource {
    override val info = MusicSourceInfo(
        id = platform.id,
        name = platform.label,
        description = platform.description,
        badge = platform.lxId,
        accentHex = platform.accentHex
    )
    private val songSearchCache = ConcurrentHashMap<String, CachedSongs>()
    private val collectionSearchCache = ConcurrentHashMap<String, CachedCollections>()
    private val playlistDetailCache = ConcurrentHashMap<String, CachedCollectionDetail>()
    private val artistSongsCache = ConcurrentHashMap<String, CachedSongs>()

    override suspend fun featured(): List<MusicPlaylist> = emptyList()

    override suspend fun search(keyword: String): List<Song> = searchSongs(keyword, 30, 0)

    override suspend fun searchSongs(keyword: String, limit: Int, offset: Int): List<Song> =
        withContext(Dispatchers.IO) {
            val query = keyword.trim()
            if (query.isBlank()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, 50)
            val page = offset.coerceAtLeast(0) / safeLimit + 1
            val cacheKey = "song:${query.cacheKey()}:$safeLimit:$page"
            songSearchCache[cacheKey]?.takeIfFresh()?.let { return@withContext it }
            val songs = runCatching {
                when (platform.id) {
                    SourcePlatformIds.KUWO -> searchKuwo(query, page, safeLimit)
                    SourcePlatformIds.KUGOU -> searchKugou(query, page, safeLimit)
                    SourcePlatformIds.QQ -> searchQq(query, page, safeLimit)
                    SourcePlatformIds.MIGU -> searchMigu(query, page, safeLimit)
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
            songSearchCache[cacheKey] = CachedSongs(songs, cacheTtl(songs))
            songs
        }

    override suspend fun searchPlaylists(keyword: String, limit: Int, offset: Int): List<MusicPlaylist> =
        searchCollections("playlist", keyword, limit, offset) { query, page, safeLimit ->
            when (platform.id) {
                SourcePlatformIds.KUWO -> searchKuwoPlaylists(query, page, safeLimit)
                SourcePlatformIds.KUGOU -> searchKugouPlaylists(query, page, safeLimit)
                SourcePlatformIds.QQ -> searchQqPlaylists(query, page, safeLimit)
                SourcePlatformIds.MIGU -> searchMiguPlaylists(query, page, safeLimit)
                else -> emptyList()
            }
        }

    override suspend fun searchAlbums(keyword: String, limit: Int, offset: Int): List<MusicPlaylist> =
        searchCollections("album", keyword, limit, offset) { query, page, safeLimit ->
            when (platform.id) {
                SourcePlatformIds.KUWO -> searchKuwoAlbums(query, page, safeLimit)
                SourcePlatformIds.KUGOU -> searchKugouAlbums(query, page, safeLimit)
                SourcePlatformIds.QQ -> searchQqAlbums(query, page, safeLimit)
                SourcePlatformIds.MIGU -> searchMiguAlbums(query, page, safeLimit)
                else -> emptyList()
            }
        }

    override suspend fun searchArtists(keyword: String, limit: Int, offset: Int): List<MusicPlaylist> =
        searchCollections("artist", keyword, limit, offset) { query, page, safeLimit ->
            when (platform.id) {
                SourcePlatformIds.KUWO -> searchKuwoArtists(query, page, safeLimit)
                SourcePlatformIds.KUGOU -> searchKugouArtists(query, page, safeLimit)
                SourcePlatformIds.QQ -> searchQqArtists(query, page, safeLimit)
                SourcePlatformIds.MIGU -> searchMiguArtists(query, page, safeLimit)
                else -> emptyList()
            }
        }

    override suspend fun streamUrl(song: Song): String = song.streamHint.orEmpty()

    override suspend fun lyrics(song: Song): List<LyricLine> =
        withContext(Dispatchers.IO) {
            runCatching {
                when (platform.id) {
                    SourcePlatformIds.KUGOU -> kugouLyrics(song)
                    SourcePlatformIds.QQ -> qqLyrics(song)
                    SourcePlatformIds.MIGU -> miguLyrics(song)
                    SourcePlatformIds.KUWO -> kuwoLyrics(song)
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
        }

    override suspend fun commentsForSong(
        song: Song,
        sort: PlaylistCommentSort,
        limit: Int,
        offset: Int
    ): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
            if (offset > 0) return@withContext PlaylistCommentBundle(emptyList(), 0)
            runCatching {
                when (platform.id) {
                    SourcePlatformIds.KUWO -> kuwoComments(song, sort, limit)
                    SourcePlatformIds.KUGOU -> kugouComments(song, sort, limit)
                    SourcePlatformIds.QQ -> qqComments(song, sort, limit)
                    SourcePlatformIds.MIGU -> miguComments(song, sort, limit)
                    else -> PlaylistCommentBundle(emptyList(), 0)
                }
            }.getOrElse {
                PlaylistCommentBundle(emptyList(), 0)
            }
        }

    override suspend fun playlistDetail(playlist: MusicPlaylist): MusicPlaylist =
        withContext(Dispatchers.IO) {
            val cacheKey = "detail:${playlist.id}"
            playlistDetailCache[cacheKey]?.takeIfFresh()?.let { return@withContext it }
            val detail = runCatching {
                when (platform.id) {
                    SourcePlatformIds.KUWO -> kuwoCollectionDetail(playlist)
                    SourcePlatformIds.KUGOU -> kugouCollectionDetail(playlist)
                    SourcePlatformIds.QQ -> qqCollectionDetail(playlist)
                    SourcePlatformIds.MIGU -> miguCollectionDetail(playlist)
                    else -> null
                }
            }.getOrNull()
            val loaded = detail?.takeIf { it.hasCollectionDetailPayload() } ?: playlist.withFallbackSongs()
            loaded.copy(
                subtitle = loaded.collectionSubtitleAfterDetail(),
                coverUrl = loaded.bestCover()
            ).also { result ->
                playlistDetailCache[cacheKey] = CachedCollectionDetail(result, cacheTtl(result.songs + result.albums))
            }
        }

    override suspend fun artistSongs(artist: MusicPlaylist, limit: Int, offset: Int): List<Song> =
        withContext(Dispatchers.IO) {
            val safeLimit = limit.coerceIn(1, 80)
            val page = offset.coerceAtLeast(0) / safeLimit + 1
            val cacheKey = "artist:${artist.id}:$safeLimit:$page"
            artistSongsCache[cacheKey]?.takeIfFresh()?.let { return@withContext it }
            val songs = runCatching {
                when (platform.id) {
                    SourcePlatformIds.KUGOU -> kugouArtistSongs(artist.rawCollectionId(), page, safeLimit)
                    SourcePlatformIds.QQ -> qqArtistSongs(artist.rawCollectionId(), offset, safeLimit).first
                    SourcePlatformIds.MIGU -> searchMigu("${artist.title} ${artist.subtitle}".trim(), page, safeLimit)
                    SourcePlatformIds.KUWO -> searchKuwo(artist.title, page, safeLimit)
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
            songs.ifEmpty { fallbackSongsFor(artist, safeLimit, page) }.also { result ->
                artistSongsCache[cacheKey] = CachedSongs(result, cacheTtl(result))
            }
        }

    private fun searchKuwo(query: String, page: Int, limit: Int): List<Song> {
        val json = getJson(
            "http://search.kuwo.cn/r.s?client=kt&all=${query.urlEncode()}" +
                "&pn=${page - 1}&rn=$limit&uid=794762570&ver=kwplayer_ar_9.2.2.1" +
                "&vipver=1&show_copyright_off=1&newver=1&ft=music&cluster=0" +
                "&strategy=2012&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1"
        ) ?: return emptyList()
        return json.optJSONArray("abslist").orEmpty().mapObjects { item ->
            val id = item.optString("MUSICRID").removePrefix("MUSIC_").ifBlank { return@mapObjects null }
            Song(
                id = id,
                title = item.optString("SONGNAME").htmlEntityDecode().clean("未知歌曲"),
                artist = item.optString("ARTIST").htmlEntityDecode().clean("未知歌手"),
                album = item.optString("ALBUM").htmlEntityDecode().clean("未知专辑"),
                coverUrl = item.kuwoCoverUrl(),
                durationMs = item.optLong("DURATION", 0L).secondsToMillis(),
                sourceId = platform.id,
                providerIds = platformIdentity(id)
            )
        }
    }

    private fun searchKugou(query: String, page: Int, limit: Int): List<Song> {
        val json = getJson(
            "https://songsearch.kugou.com/song_search_v2?keyword=${query.urlEncode()}" +
                "&page=$page&pagesize=$limit&userid=0&clientver=&platform=WebFilter" +
                "&filter=2&iscorrection=1&privilege_filter=0&area_code=1"
        ) ?: return emptyList()
        val lists = json.optJSONObject("data")?.optJSONArray("lists").orEmpty()
        val seen = linkedSetOf<String>()
        return buildList {
            lists.forEachObject { item ->
                addKugouSong(item, seen)?.let(::add)
                item.optJSONArray("Grp").orEmpty().forEachObject { child ->
                    addKugouSong(child, seen)?.let(::add)
                }
            }
        }
    }

    private fun addKugouSong(item: JSONObject, seen: MutableSet<String>): Song? {
        val id = item.optString("Audioid").ifBlank { return null }
        val hash = item.optString("FileHash")
        val key = "$id:$hash"
        if (!seen.add(key)) return null
        return Song(
            id = id,
            title = item.optString("SongName").clean("未知歌曲"),
            artist = item.optString("SingerName").ifBlank { item.optJSONArray("Singers").names("name") }.clean("未知歌手"),
            album = item.optString("AlbumName").clean("未知专辑"),
            coverUrl = item.optString("Image").toKugouCover(),
            durationMs = item.optLong("Duration", 0L).secondsToMillis(),
            sourceId = platform.id,
            providerIds = platformIdentity(id) + mapOf(
                "kg_hash" to hash,
                "kg_album_id" to item.optString("AlbumID")
            )
        )
    }

    private fun searchQq(query: String, page: Int, limit: Int): List<Song> {
        val json = getJson(
            "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?ct=24&qqmusic_ver=1298" +
                "&new_json=1&remoteplace=txt.yqq.song&searchid=${System.currentTimeMillis()}" +
                "&t=0&aggr=1&cr=1&catZhida=1&lossless=0&flag_qc=0&p=$page&n=$limit" +
                "&w=${query.urlEncode()}&format=json",
            headers = mapOf("Referer" to "https://y.qq.com/")
        ) ?: return emptyList()
        val songs = json.optJSONObject("data")?.optJSONObject("song")?.optJSONArray("list").orEmpty()
        return songs.mapObjects { item -> item.toQqSong() }
    }

    private fun searchMigu(query: String, page: Int, limit: Int): List<Song> {
        val time = System.currentTimeMillis().toString()
        val sign = miguSignature(time, query)
        val json = getJson(
            "https://jadeite.migu.cn/music_search/v3/search/searchAll?isCorrect=0&isCopyright=1" +
                "&searchSwitch=%7B%22song%22%3A1%2C%22album%22%3A0%2C%22singer%22%3A0%2C%22tagSong%22%3A1%2C%22mvSong%22%3A0%2C%22bestShow%22%3A1%2C%22songlist%22%3A0%2C%22lyricSong%22%3A0%7D" +
                "&pageSize=$limit&text=${query.urlEncode()}&pageNo=$page&sort=0&sid=USS",
            headers = mapOf(
                "uiVersion" to "A_music_3.6.1",
                "deviceId" to miguDeviceId,
                "timestamp" to time,
                "sign" to sign,
                "channel" to "0146921",
                "User-Agent" to mobileUserAgent
            )
        ) ?: return emptyList()
        val resultList = json.optJSONObject("songResultData")?.optJSONArray("resultList").orEmpty()
        return buildList {
            for (index in 0 until resultList.length()) {
                val group = resultList.optJSONArray(index) ?: continue
                group.forEachObject { item ->
                    val id = item.optString("songId").ifBlank { return@forEachObject }
                    val copyrightId = item.optString("copyrightId")
                    if (copyrightId.isBlank()) return@forEachObject
                    add(
                        Song(
                            id = id,
                            title = item.optString("name").clean("未知歌曲"),
                            artist = item.optJSONArray("singerList").names("name").clean("未知歌手"),
                            album = item.optString("album").clean("未知专辑"),
                            coverUrl = item.optString("img3")
                                .ifBlank { item.optString("img2") }
                                .ifBlank { item.optString("img1") }
                                .toMiguCover(),
                            durationMs = item.optLong("duration", 0L).secondsToMillis(),
                            sourceId = platform.id,
                            providerIds = platformIdentity(id) + mapOf(
                                "mg_copyright_id" to copyrightId,
                                "mg_album_id" to item.optString("albumId"),
                                "mg_lrc_url" to item.optString("lrcUrl"),
                                "mg_mrc_url" to item.optString("mrcurl"),
                                "mg_trc_url" to item.optString("trcUrl")
                            )
                        )
                    )
                }
            }
        }.distinctBy { it.providerIds["mg_copyright_id"] ?: it.id }
    }

    private suspend fun searchCollections(
        category: String,
        keyword: String,
        limit: Int,
        offset: Int,
        block: (String, Int, Int) -> List<MusicPlaylist>
    ): List<MusicPlaylist> = withContext(Dispatchers.IO) {
        val query = keyword.trim()
        if (query.isBlank()) return@withContext emptyList()
        val safeLimit = limit.coerceIn(1, 50)
        val page = offset.coerceAtLeast(0) / safeLimit + 1
        val cacheKey = "artwork-v3:$category:${query.cacheKey()}:$safeLimit:$page"
        collectionSearchCache[cacheKey]?.takeIfFresh()?.let { return@withContext it }
        val collections = runCatching { block(query, page, safeLimit) }.getOrDefault(emptyList())
        collectionSearchCache[cacheKey] = CachedCollections(collections, cacheTtl(collections))
        collections
    }

    private fun searchKuwoPlaylists(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchKuwoCollections(query, page, limit, ft = "playlist", kind = PlaylistKind.Playlist)

    private fun searchKuwoAlbums(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchKuwoCollections(query, page, limit, ft = "album", kind = PlaylistKind.Album)

    private fun searchKuwoArtists(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchKuwoCollections(query, page, limit, ft = "artist", kind = PlaylistKind.Artist)

    private fun searchKuwoCollections(
        query: String,
        page: Int,
        limit: Int,
        ft: String,
        kind: PlaylistKind
    ): List<MusicPlaylist> {
        val json = getJson(
            "http://search.kuwo.cn/r.s?client=kt&all=${query.urlEncode()}" +
                "&pn=${page - 1}&rn=$limit&uid=794762570&ver=kwplayer_ar_9.2.2.1" +
                "&vipver=1&newver=1&ft=$ft&encoding=utf8&rformat=json&mobi=1"
        ) ?: return emptyList()
        return json.optJSONArray("abslist").orEmpty().mapObjects { item ->
            val id = item.optString("id")
                .ifBlank { item.optString("playlistid") }
                .ifBlank { item.optString("albumid") }
                .ifBlank { item.optString("artistid") }
                .ifBlank { item.optString("ARTISTID") }
                .ifBlank { return@mapObjects null }
            val title = item.optString("name")
                .ifBlank { item.optString("NAME") }
                .ifBlank { item.optString("album") }
                .ifBlank { item.optString("ALBUM") }
                .ifBlank { item.optString("artist") }
                .ifBlank { item.optString("ARTIST") }
                .htmlEntityDecode()
                .clean(defaultCollectionTitle(kind))
            val subtitle = item.optString("info")
                .ifBlank { item.optString("desc") }
                .ifBlank { item.optString("artist") }
                .ifBlank { item.optString("ARTIST") }
                .htmlEntityDecode()
                .clean(platform.label)
            MusicPlaylist(
                id = "${platform.id}-${kind.name.lowercase()}-$id",
                title = title,
                subtitle = subtitle,
                coverUrl = item.kuwoCoverUrl(kind = kind, fallback = ""),
                songs = emptyList(),
                kind = kind,
                albumCount = item.firstInt(listOf("albumNum", "ALBUMNUM", "album_count", "albumCount")),
                songCount = item.firstInt(listOf("songnum", "SONGNUM", "musicNum", "MUSICNUM", "song_count", "songCount")),
                mvCount = item.firstInt(listOf("mvNum", "MVNUM", "videoCount", "VideoCount"))
            )
        }
    }

    private fun searchKugouPlaylists(query: String, page: Int, limit: Int): List<MusicPlaylist> {
        val json = getJson(
            "https://songsearch.kugou.com/special_search?keyword=${query.urlEncode()}" +
                "&page=$page&pagesize=$limit&platform=WebFilter"
        ) ?: return emptyList()
        return json.optJSONObject("data")?.optJSONArray("lists").orEmpty().mapObjects { item ->
            val id = item.optString("specialid")
                .ifBlank { item.optString("specialId") }
                .ifBlank { item.optString("SpecialId") }
                .ifBlank { return@mapObjects null }
            MusicPlaylist(
                id = "${platform.id}-playlist-$id",
                title = item.optString("specialname")
                    .ifBlank { item.optString("SpecialName") }
                    .clean(defaultCollectionTitle(PlaylistKind.Playlist)),
                subtitle = item.optString("intro").ifBlank { item.optString("Intro") }.clean(platform.label),
                coverUrl = item.firstArtworkUrl(
                    listOf("img", "Img", "imgurl", "ImgUrl", "Image", "image", "Avatar", "cover", "pic"),
                    fallback = ""
                ),
                songs = emptyList(),
                kind = PlaylistKind.Playlist,
                songCount = item.firstInt(listOf("song_count", "songcount", "SongCount", "AudioCount", "audioCount"))
            )
        }
    }

    private fun searchKugouAlbums(query: String, page: Int, limit: Int): List<MusicPlaylist> {
        val json = getJson(
            "https://songsearch.kugou.com/album_search?keyword=${query.urlEncode()}" +
                "&page=$page&pagesize=$limit&platform=WebFilter"
        ) ?: return emptyList()
        return json.optJSONObject("data")?.optJSONArray("lists").orEmpty().mapObjects { item ->
            val id = item.optString("albumid")
                .ifBlank { item.optString("albumId") }
                .ifBlank { item.optString("AlbumID") }
                .ifBlank { item.optString("AlbumId") }
                .ifBlank { return@mapObjects null }
            MusicPlaylist(
                id = "${platform.id}-album-$id",
                title = item.optString("albumname")
                    .ifBlank { item.optString("AlbumName") }
                    .clean(defaultCollectionTitle(PlaylistKind.Album)),
                subtitle = item.optString("singer")
                    .ifBlank { item.optString("singername") }
                    .ifBlank { item.optString("SingerName") }
                    .clean(platform.label),
                coverUrl = item.firstArtworkUrl(
                    listOf("img", "Img", "imgurl", "ImgUrl", "Image", "image", "Avatar", "cover", "pic"),
                    fallback = ""
                ),
                songs = emptyList(),
                kind = PlaylistKind.Album,
                songCount = item.firstInt(listOf("song_count", "songcount", "SongCount", "AudioCount", "audioCount"))
            )
        }
    }

    private fun searchKugouArtists(query: String, page: Int, limit: Int): List<MusicPlaylist> {
        val json = getJson(
            "https://songsearch.kugou.com/author_search?keyword=${query.urlEncode()}" +
                "&page=$page&pagesize=$limit&platform=WebFilter"
        ) ?: return emptyList()
        return json.optJSONObject("data")?.optJSONArray("lists").orEmpty().mapObjects { item ->
            val id = item.optString("singerid")
                .ifBlank { item.optString("singerId") }
                .ifBlank { item.optString("AuthorId") }
                .ifBlank { item.optString("authorId") }
                .ifBlank { return@mapObjects null }
            MusicPlaylist(
                id = "${platform.id}-artist-$id",
                title = item.optString("singername")
                    .ifBlank { item.optString("singerName") }
                    .ifBlank { item.optString("AuthorName") }
                    .ifBlank { item.optString("authorName") }
                    .clean(defaultCollectionTitle(PlaylistKind.Artist)),
                subtitle = item.optString("intro")
                    .ifBlank { item.optString("Intro") }
                    .ifBlank { item.optString("Auxiliary") }
                    .clean(platform.label),
                coverUrl = item.firstArtworkUrl(
                    listOf("Avatar", "avatar", "FirstFrameImage", "firstFrameImage", "imgurl", "ImgUrl", "Image", "image"),
                    fallback = ""
                ),
                songs = emptyList(),
                kind = PlaylistKind.Artist,
                albumCount = item.firstInt(listOf("AlbumCount", "albumCount", "albumcount")),
                songCount = item.firstInt(listOf("AudioCount", "audioCount", "songcount", "SongCount", "songCount")),
                mvCount = item.firstInt(listOf("VideoCount", "videoCount", "MVCount", "mvCount"))
            )
        }
    }

    private fun searchQqPlaylists(query: String, page: Int, limit: Int): List<MusicPlaylist> {
        val json = getJson(
            "http://c.y.qq.com/soso/fcgi-bin/client_music_search_songlist?page_no=${page - 1}" +
                "&num_per_page=$limit&format=json&query=${query.urlEncode()}" +
                "&remoteplace=txt.yqq.playlist&inCharset=utf8&outCharset=utf-8",
            headers = mapOf(
                "Referer" to "http://y.qq.com/portal/search.html",
                "User-Agent" to desktopUserAgent
            )
        ) ?: return emptyList()
        return json.optJSONObject("data")?.optJSONArray("list").orEmpty().mapObjects { item ->
            val id = item.optString("dissid").ifBlank { item.optString("docid") }.ifBlank { return@mapObjects null }
            val creator = item.optJSONObject("creator")
            MusicPlaylist(
                id = "${platform.id}-playlist-$id",
                title = item.optString("dissname").clean(defaultCollectionTitle(PlaylistKind.Playlist)),
                subtitle = creator?.optString("name").clean(platform.label),
                coverUrl = item.optString("imgurl").toArtworkUrl(fallback = ""),
                songs = emptyList(),
                kind = PlaylistKind.Playlist,
                description = item.optString("introduction").htmlEntityDecode(),
                songCount = item.optInt("song_count", item.optInt("copyrightnum", 0))
            )
        }
    }

    private fun searchQqAlbums(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchQqCollections(query, page, limit, type = 8, kind = PlaylistKind.Album)
            .ifEmpty { searchQqSmartCollections(query, page, limit, PlaylistKind.Album) }

    private fun searchQqArtists(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchQqCollections(query, page, limit, type = 9, kind = PlaylistKind.Artist)
            .ifEmpty { searchQqSmartCollections(query, page, limit, PlaylistKind.Artist) }

    private fun searchQqCollections(query: String, page: Int, limit: Int, type: Int, kind: PlaylistKind): List<MusicPlaylist> {
        val json = getJson(
            "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?ct=24&qqmusic_ver=1298" +
                "&remoteplace=txt.yqq.collection&t=$type&p=$page&n=$limit&w=${query.urlEncode()}&format=json",
            headers = mapOf("Referer" to "https://y.qq.com/")
        ) ?: return emptyList()
        val container = when (kind) {
            PlaylistKind.Playlist -> json.optJSONObject("data")?.optJSONObject("songlist")
            PlaylistKind.Album -> json.optJSONObject("data")?.optJSONObject("album")
            PlaylistKind.Artist -> json.optJSONObject("data")?.optJSONObject("singer")
            else -> null
        }
        return container?.optJSONArray("list").orEmpty().mapObjects { item ->
            val id = item.optString("dissid")
                .ifBlank { item.optString("albumMID") }
                .ifBlank { item.optString("album_mid") }
                .ifBlank { item.optString("singermid") }
                .ifBlank { item.optString("singerMID") }
                .ifBlank { item.optString("id") }
                .ifBlank { return@mapObjects null }
            val title = item.optString("dissname")
                .ifBlank { item.optString("albumName") }
                .ifBlank { item.optString("albumname") }
                .ifBlank { item.optString("singerName") }
                .ifBlank { item.optString("singername") }
                .clean(defaultCollectionTitle(kind))
            val cover = item.optString("imgurl")
                .ifBlank { item.optString("albumPic") }
                .ifBlank { item.optString("singerPic") }
                .ifBlank { item.optString("pic") }
                .ifBlank { item.optString("logo") }
                .ifBlank { item.optString("cover") }
            val normalizedCover = when {
                cover.isNotBlank() -> cover.toArtworkUrl(fallback = "")
                kind == PlaylistKind.Album -> qqAlbumCover(id)
                kind == PlaylistKind.Artist -> qqSingerAvatar(id)
                else -> ""
            }
            MusicPlaylist(
                id = "${platform.id}-${kind.name.lowercase()}-$id",
                title = title,
                subtitle = item.optString("creator").ifBlank { item.optString("singerName") }.clean(platform.label),
                coverUrl = normalizedCover,
                songs = emptyList(),
                kind = kind,
                albumCount = item.firstInt(listOf("album_count", "albumCount", "album_num", "albumNum")),
                songCount = item.firstInt(listOf("song_count", "song_count_new", "songNum", "songnum", "musicNum", "total_song")),
                mvCount = item.firstInt(listOf("mv_count", "mvCount", "total_mv"))
            )
        }
    }

    private fun searchQqSmartCollections(
        query: String,
        page: Int,
        limit: Int,
        kind: PlaylistKind
    ): List<MusicPlaylist> {
        if (page > 1) return emptyList()
        val json = getJson(
            "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?format=json&key=${query.urlEncode()}" +
                "&g_tk=5381&loginUin=0&hostUin=0&inCharset=utf8&outCharset=utf-8" +
                "&notice=0&platform=yqq.json&needNewCode=0",
            headers = mapOf("Referer" to "https://y.qq.com/", "User-Agent" to desktopUserAgent)
        ) ?: return emptyList()
        val key = if (kind == PlaylistKind.Artist) "singer" else "album"
        return json.optJSONObject("data")?.optJSONObject(key)?.optJSONArray("itemlist").orEmpty()
            .mapObjects { item ->
                val mid = item.optString("mid").ifBlank { item.optString("id") }.ifBlank { return@mapObjects null }
                MusicPlaylist(
                    id = "${platform.id}-${kind.name.lowercase()}-$mid",
                    title = item.optString("name").clean(defaultCollectionTitle(kind)),
                    subtitle = item.optString("singer").clean(platform.label),
                    coverUrl = item.optString("pic").toArtworkUrl(fallback = "").ifBlank {
                        if (kind == PlaylistKind.Artist) qqSingerAvatar(mid) else qqAlbumCover(mid)
                    },
                    songs = emptyList(),
                    kind = kind,
                    albumCount = item.firstInt(listOf("album_count", "albumCount", "albumNum")),
                    songCount = item.firstInt(listOf("song_count", "songCount", "songnum", "songNum", "musicNum")),
                    mvCount = item.firstInt(listOf("mv_count", "mvCount"))
                )
            }
            .take(limit)
    }

    private fun searchMiguPlaylists(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchMiguCollections(query, page, limit, switch = "%22songlist%22%3A1", key = "songListResultData", kind = PlaylistKind.Playlist)

    private fun searchMiguAlbums(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchMiguCollections(query, page, limit, switch = "%22album%22%3A1", key = "albumResultData", kind = PlaylistKind.Album)

    private fun searchMiguArtists(query: String, page: Int, limit: Int): List<MusicPlaylist> =
        searchMiguCollections(query, page, limit, switch = "%22singer%22%3A1", key = "singerResultData", kind = PlaylistKind.Artist)

    private fun searchMiguCollections(
        query: String,
        page: Int,
        limit: Int,
        switch: String,
        key: String,
        kind: PlaylistKind
    ): List<MusicPlaylist> {
        val time = System.currentTimeMillis().toString()
        val sign = miguSignature(time, query)
        val json = getJson(
            "https://jadeite.migu.cn/music_search/v3/search/searchAll?isCorrect=0&isCopyright=1" +
                "&searchSwitch=%7B$switch%7D&pageSize=$limit&text=${query.urlEncode()}&pageNo=$page&sort=0&sid=USS",
            headers = mapOf(
                "uiVersion" to "A_music_3.6.1",
                "deviceId" to miguDeviceId,
                "timestamp" to time,
                "sign" to sign,
                "channel" to "0146921",
                "User-Agent" to mobileUserAgent
            )
        ) ?: return emptyList()
        val resultList = json.optJSONObject(key)?.optJSONArray("resultList").orEmpty()
        return resultList.mapObjects { item ->
            val id = item.optString("id")
                .ifBlank { item.optString("songListId") }
                .ifBlank { item.optString("albumId") }
                .ifBlank { item.optString("singerId") }
                .ifBlank { return@mapObjects null }
            MusicPlaylist(
                id = "${platform.id}-${kind.name.lowercase()}-$id",
                title = item.optString("name").clean(defaultCollectionTitle(kind)),
                subtitle = item.optString("singer").ifBlank { item.optString("summary") }.clean(platform.label),
                coverUrl = item.firstArtworkUrl(
                    listOf("img3", "img2", "img1", "img", "pic", "picUrl", "cover", "coverUrl", "singerPic", "albumPic", "songListPic"),
                    fallback = ""
                ),
                songs = emptyList(),
                kind = kind,
                albumCount = item.firstInt(listOf("albumNum", "albumCount")),
                songCount = item.firstInt(listOf("musicNum", "songNum", "songCount", "total", "count")),
                mvCount = item.firstInt(listOf("mvNum", "mvCount"))
            )
        }
    }

    private fun kuwoCollectionDetail(playlist: MusicPlaylist): MusicPlaylist? {
        val id = playlist.rawCollectionId()
        if (id.isBlank()) return null
        return when (playlist.kind) {
            PlaylistKind.Artist -> {
                val songs = searchKuwo(playlist.title, page = 1, limit = 50)
                val albums = kuwoArtistAlbums(id, limit = 40)
                playlist.copy(
                    coverUrl = playlist.coverUrl.ifBlank { albums.firstOrNull()?.coverUrl.orEmpty() },
                    songs = songs,
                    songCount = playlist.songCount.coerceAtLeast(songs.size),
                    albumCount = playlist.albumCount.coerceAtLeast(albums.size),
                    albums = albums
                )
            }
            else -> null
        }
    }

    private fun kuwoArtistAlbums(id: String, limit: Int): List<MusicPlaylist> {
        val json = getJson(
            "http://search.kuwo.cn/r.s?stype=albumlist&artistid=${id.urlEncode()}" +
                "&pn=0&rn=${limit.coerceIn(1, 80)}&encoding=utf8&rformat=json&vipver=1&newver=1"
        ) ?: return emptyList()
        return json.optJSONArray("albumlist").orEmpty()
            .mapObjects { item ->
                val albumId = item.optString("id")
                    .ifBlank { item.optString("albumid") }
                    .ifBlank { return@mapObjects null }
                MusicPlaylist(
                    id = "${platform.id}-album-$albumId",
                    title = item.optString("name")
                        .ifBlank { item.optString("album") }
                        .clean(defaultCollectionTitle(PlaylistKind.Album)),
                    subtitle = item.optString("artist").clean(platform.label),
                    coverUrl = item.kuwoCoverUrl(kind = PlaylistKind.Album, fallback = ""),
                    songs = emptyList(),
                    kind = PlaylistKind.Album,
                    description = item.optString("info").clean(),
                    songCount = item.firstInt(listOf("musiccnt", "musicNum", "songCount", "SONGNUM"))
                )
            }
            .distinctBy { it.id }
    }

    private fun kugouCollectionDetail(playlist: MusicPlaylist): MusicPlaylist? {
        val id = playlist.rawCollectionId()
        if (id.isBlank()) return null
        return when (playlist.kind) {
            PlaylistKind.Album -> {
                val songs = kugouAlbumSongs(id, page = 1, limit = 80)
                playlist.copy(songs = songs, coverUrl = playlist.coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() })
            }
            PlaylistKind.Artist -> {
                val songs = kugouArtistSongs(id, page = 1, limit = 80)
                val albums = kugouArtistAlbums(id, page = 1, limit = 80)
                val info = getJson("http://mobiles.kugou.com/api/v5/singer/info?singerid=${id.urlEncode()}")
                    ?.optJSONObject("data")
                playlist.copy(
                    title = info?.optString("singername").clean().ifBlank { playlist.title },
                    description = info?.optString("profile").clean().ifBlank { playlist.description },
                    coverUrl = info?.optString("imgurl").orEmpty().toKugouCover().ifBlank { playlist.coverUrl },
                    songs = songs,
                    songCount = listOf(
                        info?.optInt("songcount", 0) ?: 0,
                        playlist.songCount,
                        songs.size
                    ).maxOrNull() ?: songs.size,
                    albumCount = listOf(
                        info?.optInt("albumcount", 0) ?: 0,
                        info?.optInt("albumCount", 0) ?: 0,
                        playlist.albumCount,
                        albums.size
                    ).maxOrNull() ?: albums.size,
                    albums = albums.ifEmpty { songs.albumShellsFromSongs(playlist, limit = 40) }
                )
            }
            else -> null
        }
    }

    private fun kugouAlbumSongs(id: String, page: Int, limit: Int): List<Song> {
        val json = getJson(
            "http://mobiles.kugou.com/api/v3/album/song?version=9108&albumid=${id.urlEncode()}" +
                "&plat=0&pagesize=${limit.coerceIn(1, 100)}&area_code=0&page=$page&with_res_tag=0"
        ) ?: return emptyList()
        return json.optJSONObject("data")?.optJSONArray("info").orEmpty()
            .mapObjects { item -> item.toKugouDetailSong() }
    }

    private fun kugouArtistSongs(id: String, page: Int, limit: Int): List<Song> {
        if (id == "0" || id.isBlank()) return emptyList()
        val json = getJson(
            "http://mobiles.kugou.com/api/v5/singer/song?singerid=${id.urlEncode()}" +
                "&page=$page&pagesize=${limit.coerceIn(1, 100)}"
        ) ?: return emptyList()
        return json.optJSONObject("data")?.optJSONArray("info").orEmpty()
            .mapObjects { item -> item.toKugouDetailSong() }
    }

    private fun kugouArtistAlbums(id: String, page: Int, limit: Int): List<MusicPlaylist> {
        if (id == "0" || id.isBlank()) return emptyList()
        val json = getJson(
            "http://mobiles.kugou.com/api/v5/singer/song?singerid=${id.urlEncode()}" +
                "&page=$page&pagesize=${limit.coerceIn(1, 100)}"
        ) ?: return emptyList()
        return json.optJSONObject("data")?.optJSONArray("info").orEmpty()
            .mapObjects { item ->
                val albumId = item.optString("AlbumID")
                    .ifBlank { item.optString("album_id") }
                    .ifBlank { item.optString("albumid") }
                    .ifBlank { return@mapObjects null }
                MusicPlaylist(
                    id = "${platform.id}-album-$albumId",
                    title = item.optString("AlbumName")
                        .ifBlank { item.optString("album_name") }
                        .ifBlank { item.optString("albumname") }
                        .clean(defaultCollectionTitle(PlaylistKind.Album)),
                    subtitle = item.optString("SingerName")
                        .ifBlank { item.optString("singername") }
                        .clean(platform.label),
                    coverUrl = item.optString("Image")
                        .ifBlank { item.optString("imgurl") }
                        .ifBlank { item.optString("img") }
                        .ifBlank { item.optString("image") }
                        .ifBlank { item.optJSONObject("trans_param")?.optString("union_cover").orEmpty() }
                        .toKugouCover(fallback = ""),
                    songs = emptyList(),
                    kind = PlaylistKind.Album,
                    songCount = item.firstInt(listOf("song_count", "songcount", "SongCount", "AudioCount", "audioCount"))
                )
            }
            .distinctBy { it.id }
    }

    private fun qqCollectionDetail(playlist: MusicPlaylist): MusicPlaylist? {
        val id = playlist.rawCollectionId()
        if (id.isBlank()) return null
        return when (playlist.kind) {
            PlaylistKind.Playlist -> {
                val json = getJson(
                    "https://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg" +
                        "?type=1&json=1&utf8=1&onlysong=0&new_format=1&disstid=${id.urlEncode()}" +
                        "&loginUin=0&hostUin=0&format=json&inCharset=utf8&outCharset=utf-8" +
                        "&notice=0&platform=yqq.json&needNewCode=0",
                    headers = mapOf("Origin" to "https://y.qq.com", "Referer" to "https://y.qq.com/")
                ) ?: return null
                val detail = json.optJSONArray("cdlist")?.optJSONObject(0) ?: return null
                val songs = detail.optJSONArray("songlist").orEmpty().mapObjects { item -> item.toQqSong() }
                playlist.copy(
                    title = detail.optString("dissname").clean().ifBlank { playlist.title },
                    subtitle = detail.optString("nickname").clean().ifBlank { playlist.subtitle },
                    description = detail.optString("desc").htmlEntityDecode(),
                    coverUrl = detail.optString("logo").clean().ifBlank { playlist.coverUrl },
                    songs = songs,
                    songCount = songs.size
                )
            }
            PlaylistKind.Artist -> {
                val (songs, data) = qqArtistSongs(id, offset = 0, limit = 80)
                val info = data?.optJSONObject("singer_info") ?: data?.optJSONObject("singerInfo")
                val albums = artistAlbumFallbacks(playlist, songs, limit = 40)
                playlist.copy(
                    title = info?.optString("name").clean().ifBlank { playlist.title },
                    coverUrl = info?.optString("pic").clean().ifBlank { playlist.coverUrl.ifBlank { qqSingerAvatar(id) } },
                    songs = songs,
                    songCount = listOf(
                        data?.optInt("total_song", 0) ?: 0,
                        playlist.songCount,
                        songs.size
                    ).maxOrNull() ?: songs.size,
                    albumCount = (data?.optInt("total_album", playlist.albumCount) ?: playlist.albumCount)
                        .coerceAtLeast(albums.size),
                    mvCount = data?.optInt("total_mv", playlist.mvCount) ?: playlist.mvCount,
                    albums = albums
                )
            }
            else -> null
        }
    }

    private fun qqArtistSongs(id: String, offset: Int, limit: Int): Pair<List<Song>, JSONObject?> {
        val payload = JSONObject()
            .put("comm", JSONObject().put("ct", 24).put("cv", 0))
            .put(
                "singer",
                JSONObject()
                    .put("module", "music.web_singer_info_svr")
                    .put("method", "get_singer_detail_info")
                    .put(
                        "param",
                        JSONObject()
                            .put("sort", 5)
                            .put("singermid", id)
                            .put("sin", offset.coerceAtLeast(0))
                            .put("num", limit.coerceIn(1, 80))
                    )
            )
        val json = postJson(
            "https://u.y.qq.com/cgi-bin/musicu.fcg",
            payload.toString().toRequestBody(jsonMediaType),
            headers = mapOf("Referer" to "https://y.qq.com/", "Origin" to "https://y.qq.com", "User-Agent" to desktopUserAgent)
        ) ?: return emptyList<Song>() to null
        val block = json.optJSONObject("singer")
        val data = block?.optJSONObject("data")
        val songs = data?.optJSONArray("songlist").orEmpty().mapObjects { raw ->
            val item = raw.optJSONObject("track_info")
                ?: raw.optJSONObject("songInfo")
                ?: raw.optJSONObject("songinfo")
                ?: raw.optJSONObject("song")
                ?: raw
            item.toQqSong()
        }
        return songs to data
    }

    private fun miguCollectionDetail(playlist: MusicPlaylist): MusicPlaylist? {
        val id = playlist.rawCollectionId()
        if (id.isBlank()) return null
        return when (playlist.kind) {
            PlaylistKind.Playlist -> {
                val listJson = getJson(
                    "https://app.c.nf.migu.cn/MIGUM3.0/resource/playlist/song/v2.0" +
                        "?pageNo=1&pageSize=80&playlistId=${id.urlEncode()}",
                    headers = miguDetailHeaders
                )
                val infoJson = getJson(
                    "https://app.c.nf.migu.cn/MIGUM3.0/resource/playlist/v2.0?playlistId=${id.urlEncode()}",
                    headers = miguDetailHeaders
                )?.optJSONObject("data")
                val songs = listJson?.optJSONObject("data")?.optJSONArray("songList").orEmpty()
                    .mapObjects { item -> item.toMiguSong() }
                playlist.copy(
                    title = infoJson?.optString("title").clean().ifBlank { playlist.title },
                    subtitle = infoJson?.optString("ownerName").clean().ifBlank { playlist.subtitle },
                    description = infoJson?.optString("summary").clean().ifBlank { playlist.description },
                    coverUrl = infoJson?.optJSONObject("imgItem")?.optString("img").orEmpty().toMiguCover().ifBlank { playlist.coverUrl },
                    songs = songs,
                    songCount = infoJson?.optInt("musicNum", playlist.songCount) ?: playlist.songCount
                )
            }
            PlaylistKind.Album -> {
                val json = getJson(
                    "http://app.c.nf.migu.cn/MIGUM2.0/v1.0/content/queryAlbumSong?albumId=${id.urlEncode()}&pageNo=1",
                    headers = miguDetailHeaders
                )
                val songs = json?.optJSONObject("data")?.optJSONArray("songList").orEmpty()
                    .mapObjects { item -> item.toMiguSong() }
                playlist.copy(songs = songs, coverUrl = playlist.coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() })
            }
            PlaylistKind.Artist -> {
                val songs = searchMigu(playlist.title, page = 1, limit = 50)
                val albums = artistAlbumFallbacks(playlist, songs, limit = 40)
                playlist.copy(
                    coverUrl = playlist.coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() },
                    songs = songs,
                    songCount = playlist.songCount.coerceAtLeast(songs.size),
                    albumCount = playlist.albumCount.coerceAtLeast(albums.size),
                    albums = albums
                )
            }
            else -> null
        }
    }

    private fun MusicPlaylist.withFallbackSongs(): MusicPlaylist {
        val songs = fallbackSongsFor(this, limit = 50, page = 1)
        return copy(songs = songs, coverUrl = coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() })
    }

    private fun fallbackSongsFor(collection: MusicPlaylist, limit: Int, page: Int): List<Song> {
        val subtitle = collection.subtitle.takeIf {
            it.isNotBlank() && it != platform.label && !it.contains("首")
        }.orEmpty()
        val query = listOf(collection.title, subtitle).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
            collection.title.ifBlank { "华语流行" }
        }
        return when (platform.id) {
            SourcePlatformIds.KUWO -> searchKuwo(query, page, limit)
            SourcePlatformIds.KUGOU -> searchKugou(query, page, limit)
            SourcePlatformIds.QQ -> searchQq(query, page, limit)
            SourcePlatformIds.MIGU -> searchMigu(query, page, limit)
            else -> emptyList()
        }
    }

    private fun artistAlbumFallbacks(
        artist: MusicPlaylist,
        songs: List<Song>,
        limit: Int
    ): List<MusicPlaylist> {
        val fromSongs = songs.albumShellsFromSongs(artist, limit)
        val searched = when (platform.id) {
            SourcePlatformIds.KUWO -> kuwoArtistAlbums(artist.rawCollectionId(), limit)
                .ifEmpty { searchKuwoAlbums(artist.title, page = 1, limit = limit) }
            SourcePlatformIds.KUGOU -> kugouArtistAlbums(artist.rawCollectionId(), page = 1, limit = limit)
                .ifEmpty { searchKugouAlbums(artist.title, page = 1, limit = limit) }
            SourcePlatformIds.QQ -> searchQqAlbums(artist.title, page = 1, limit = limit)
            SourcePlatformIds.MIGU -> searchMiguAlbums(artist.title, page = 1, limit = limit)
            else -> emptyList()
        }.filterForArtist(artist.title)
        return (fromSongs + searched)
            .distinctBy { it.id }
            .take(limit.coerceAtLeast(1))
    }

    private fun List<Song>.albumShellsFromSongs(
        artist: MusicPlaylist,
        limit: Int
    ): List<MusicPlaylist> =
        filter { it.album.isNotBlank() && it.album != defaultCollectionTitle(PlaylistKind.Album) }
            .groupBy { "${it.sourceId}:${it.album.cacheKey()}" }
            .values
            .mapNotNull { albumSongs ->
                val first = albumSongs.firstOrNull() ?: return@mapNotNull null
                MusicPlaylist(
                    id = "${first.sourceId}-album-${first.album.cacheKey()}",
                    title = first.album,
                    subtitle = artist.title.ifBlank { first.artist },
                    coverUrl = first.coverUrl,
                    songs = emptyList(),
                    kind = PlaylistKind.Album,
                    songCount = albumSongs.size
                )
            }
            .take(limit.coerceAtLeast(1))

    private fun List<MusicPlaylist>.filterForArtist(artistName: String): List<MusicPlaylist> {
        val token = artistName.cacheKey()
        if (token.isBlank()) return this
        val matched = filter { playlist ->
            playlist.subtitle.cacheKey().contains(token) ||
                playlist.description.cacheKey().contains(token) ||
                playlist.title.cacheKey().contains(token)
        }
        return matched.ifEmpty { this }
    }

    private fun MusicPlaylist.rawCollectionId(): String =
        id.substringAfterLast('-', missingDelimiterValue = id).trim()

    private fun MusicPlaylist.hasCollectionDetailPayload(): Boolean =
        songs.isNotEmpty() || albums.isNotEmpty() || coverUrl.isNotBlank() || description.isNotBlank()

    private fun MusicPlaylist.bestCover(): String =
        coverUrl.takeIf { it.isNotBlank() && it != defaultCover }
            ?: songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl != defaultCover }?.coverUrl
            ?: albums.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl != defaultCover }?.coverUrl
            ?: coverUrl.ifBlank { defaultCover }

    private fun MusicPlaylist.collectionSubtitleAfterDetail(): String =
        when {
            songs.isEmpty() -> subtitle
            kind == PlaylistKind.Artist -> {
                val total = songCount.coerceAtLeast(songs.size)
                if (total > 0) "${platform.label} · $total 首单曲" else platform.label
            }
            else -> "${songs.size} 首歌曲"
        }

    private fun JSONObject.toKugouDetailSong(): Song? {
        val id = optString("Audioid")
            .ifBlank { optString("audio_id") }
            .ifBlank { optString("album_audio_id") }
            .ifBlank { return null }
        val hash = optString("FileHash").ifBlank { optString("hash") }.ifBlank { optString("Hash") }
        val filename = optString("filename").clean()
        val title = optString("SongName")
            .ifBlank { optString("songname") }
            .ifBlank { optString("song_name") }
            .ifBlank { filename.substringAfter(" - ", filename) }
            .clean("未知歌曲")
        val artist = optString("SingerName")
            .ifBlank { optString("singername") }
            .ifBlank { optString("author_name") }
            .ifBlank { filename.substringBefore(" - ", "") }
            .clean("未知歌手")
        val cover = optString("Image")
            .ifBlank { optString("imgurl") }
            .ifBlank { optString("image") }
            .ifBlank { optJSONObject("trans_param")?.optString("union_cover").orEmpty() }
            .toKugouCover()
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = optString("AlbumName").ifBlank { optString("album_name") }.clean("未知专辑"),
            coverUrl = cover,
            durationMs = optLong("Duration", optLong("duration", 0L)).secondsToMillis(),
            sourceId = platform.id,
            providerIds = platformIdentity(id) + mapOf(
                "kg_hash" to hash,
                "kg_album_id" to optString("AlbumID").ifBlank { optString("album_id") }
            )
        )
    }

    private fun JSONObject.toQqSong(): Song? {
        val songMid = optString("mid").ifBlank { optString("songmid") }.ifBlank { return null }
        val album = optJSONObject("album")
        val albumMid = album?.optString("mid").orEmpty()
            .ifBlank { album?.optString("pmid").orEmpty().substringBefore('_') }
        val singer = optJSONArray("singer").names("name").clean("未知歌手")
        val firstSingerMid = optJSONArray("singer")?.optJSONObject(0)?.optString("mid").orEmpty()
        val songId = optLong("id", optLong("songid", 0L)).takeIf { it > 0L }?.toString().orEmpty()
        return Song(
            id = songMid,
            title = optString("title").ifBlank { optString("name") }.ifBlank { optString("songname") }.clean("未知歌曲"),
            artist = singer,
            album = album?.optString("name").orEmpty().ifBlank { album?.optString("title").orEmpty() }.clean("未知专辑"),
            coverUrl = if (albumMid.isNotBlank()) qqAlbumCover(albumMid) else firstSingerMid.takeIf { it.isNotBlank() }?.let(::qqSingerAvatar) ?: defaultCover,
            durationMs = optLong("interval", 0L).secondsToMillis(),
            sourceId = platform.id,
            providerIds = platformIdentity(songMid) + mapOf(
                "tx_song_id" to songId,
                "tx_media_mid" to (optJSONObject("file")?.optString("media_mid").orEmpty()),
                "tx_album_mid" to albumMid,
                "tx_singer_mid" to firstSingerMid
            )
        )
    }

    private fun JSONObject.toMiguSong(): Song? {
        val id = optString("songId")
            .ifBlank { optString("contentId") }
            .ifBlank { optString("copyrightId") }
            .ifBlank { return null }
        val copyrightId = optString("copyrightId")
        val cover = optString("img3")
            .ifBlank { optString("img2") }
            .ifBlank { optString("img1") }
            .ifBlank { optJSONArray("albumImgs")?.optJSONObject(0)?.optString("img").orEmpty() }
            .toMiguCover()
        return Song(
            id = id,
            title = optString("songName").ifBlank { optString("name") }.clean("未知歌曲"),
            artist = optJSONArray("singerList").names("name").ifBlank { optString("singer") }.clean("未知歌手"),
            album = optString("album").clean("未知专辑"),
            coverUrl = cover,
            durationMs = optLong("duration", 0L).secondsToMillis(),
            sourceId = platform.id,
            providerIds = platformIdentity(id) + mapOf(
                "mg_copyright_id" to copyrightId,
                "mg_album_id" to optString("albumId"),
                "mg_lrc_url" to optString("lrcUrl"),
                "mg_mrc_url" to optString("mrcUrl").ifBlank { optString("mrcurl") },
                "mg_trc_url" to optString("trcUrl")
            )
        )
    }

    private fun defaultCollectionTitle(kind: PlaylistKind): String = when (kind) {
        PlaylistKind.Album -> "未知专辑"
        PlaylistKind.Artist -> "未知歌手"
        else -> "未知歌单"
    }

    private fun kuwoComments(song: Song, sort: PlaylistCommentSort, limit: Int): PlaylistCommentBundle {
        val sid = song.providerIds[platform.lxId] ?: song.providerIds[platform.id] ?: song.id
        val type = if (sort == PlaylistCommentSort.Hot) "get_rec_comment" else "get_comment"
        val json = getJson(
            "http://ncomment.kuwo.cn/com.s?f=web&type=$type&aapiver=1" +
                "&prod=kwplayer_ar_10.5.2.0&digest=15&sid=${sid.urlEncode()}" +
                "&start=0&msgflag=1&count=${limit.coerceIn(1, 100)}&newver=3&uid=0",
            headers = mapOf("User-Agent" to "Dalvik/2.1.0 (Linux; U; Android 9;)")
        ) ?: return PlaylistCommentBundle(emptyList(), 0)
        val comments = if (sort == PlaylistCommentSort.Hot) {
            json.optJSONArray("hot_comments").orEmpty()
        } else {
            json.optJSONArray("comments").orEmpty()
        }.mapObjects { item ->
            PlaylistComment(
                id = item.optString("id").ifBlank { item.optString("u_id") },
                authorName = item.optString("u_name").clean("酷窝用户"),
                authorAvatarUrl = item.optString("u_pic").clean(),
                content = item.optString("msg").clean("这条评论暂时没有内容"),
                timeLabel = item.optLong("time", 0L).secondsToCommentTime(),
                likedCount = item.optInt("like_num", 0),
                replyCount = item.optJSONArray("child_comments")?.length() ?: 0,
                replies = item.optJSONArray("child_comments").toLxCommentReplies(
                    authorKeys = listOf("u_name", "nickname", "name"),
                    contentKeys = listOf("msg", "content"),
                    timeKeys = listOf("time"),
                    likeKeys = listOf("like_num")
                )
            )
        }
        val total = if (sort == PlaylistCommentSort.Hot) {
            json.optInt("hot_comments_counts", comments.size)
        } else {
            json.optInt("comments_counts", comments.size)
        }
        return PlaylistCommentBundle(comments, total)
    }

    private fun kugouComments(song: Song, sort: PlaylistCommentSort, limit: Int): PlaylistCommentBundle {
        val hash = song.providerIds["kg_hash"].orEmpty().ifBlank { song.id.takeIf { it.length == 32 }.orEmpty() }
        if (hash.isBlank()) return PlaylistCommentBundle(emptyList(), 0)
        val timestamp = System.currentTimeMillis()
        val params = "dfid=0&mid=16249512204336365674023395779019&clienttime=$timestamp" +
            "&uuid=0&extdata=$hash&appid=1005&code=fc4be23b4e972707f36b8a828a93ba8a" +
            "&schash=$hash&clientver=11409&p=1&clienttoken=&pagesize=${limit.coerceIn(1, 100)}&ver=10&kugouid=0"
        val path = if (sort == PlaylistCommentSort.Hot) "topliked" else "newest"
        val json = getJson(
            "http://m.comment.service.kugou.com/r/v1/rank/$path?$params&signature=${kugouSignature(params)}",
            headers = mapOf("User-Agent" to desktopUserAgent)
        ) ?: return PlaylistCommentBundle(emptyList(), 0)
        val comments = json.optJSONArray("list").orEmpty().mapObjects { item ->
            PlaylistComment(
                id = item.optString("id").ifBlank { item.optString("user_id") },
                authorName = item.optString("user_name").clean("酷构用户"),
                authorAvatarUrl = item.optString("user_pic").clean(),
                content = item.optString("content").clean("这条评论暂时没有内容"),
                timeLabel = item.optString("addtime").clean(),
                likedCount = item.optJSONObject("like")?.optInt("likenum", 0) ?: 0,
                replyCount = item.optInt("reply_num", 0)
            )
        }
        return PlaylistCommentBundle(comments, json.optInt("count", comments.size))
    }

    private fun qqComments(song: Song, sort: PlaylistCommentSort, limit: Int): PlaylistCommentBundle {
        val songId = song.providerIds["tx_song_id"].orEmpty()
            .ifBlank { song.id.takeIf { it.all(Char::isDigit) }.orEmpty() }
        if (songId.isBlank()) return PlaylistCommentBundle(emptyList(), 0)
        return if (sort == PlaylistCommentSort.Hot) {
            qqHotComments(songId, limit)
        } else {
            qqLatestComments(songId, limit)
        }
    }

    private fun qqLatestComments(songId: String, limit: Int): PlaylistCommentBundle {
        val body = FormBody.Builder()
            .add("uin", "0")
            .add("format", "json")
            .add("cid", "205360772")
            .add("reqtype", "2")
            .add("biztype", "1")
            .add("topid", songId)
            .add("cmd", "8")
            .add("needmusiccrit", "1")
            .add("pagenum", "0")
            .add("pagesize", limit.coerceIn(1, 50).toString())
            .build()
        val json = postJson(
            "http://c.y.qq.com/base/fcgi-bin/fcg_global_comment_h5.fcg",
            body,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)")
        ) ?: return PlaylistCommentBundle(emptyList(), 0)
        val comment = json.optJSONObject("comment") ?: return PlaylistCommentBundle(emptyList(), 0)
        val comments = comment.optJSONArray("commentlist").orEmpty().mapObjects { item ->
            val content = item.optString("rootcommentcontent").replace("\\n", "\n").clean("这条评论暂时没有内容")
            PlaylistComment(
                id = "${item.optString("rootcommentid")}_${item.optString("commentid")}",
                authorName = item.optString("rootcommentnick").removePrefix("@").clean("轻雀用户"),
                authorAvatarUrl = item.optString("avatarurl").clean(),
                content = content,
                timeLabel = item.optString("time").toQqCommentTime(),
                likedCount = item.optInt("praisenum", 0),
                replyCount = item.optJSONArray("middlecommentcontent")?.length() ?: 0,
                replies = item.optJSONArray("middlecommentcontent").toLxCommentReplies(
                    authorKeys = listOf("subcommentnick", "nick", "name"),
                    contentKeys = listOf("subcommentcontent", "content"),
                    timeKeys = listOf("time"),
                    likeKeys = listOf("praisenum")
                )
            )
        }
        return PlaylistCommentBundle(comments, comment.optInt("commenttotal", comments.size))
    }

    private fun qqHotComments(songId: String, limit: Int): PlaylistCommentBundle {
        val payload = JSONObject()
            .put(
                "comm",
                JSONObject()
                    .put("cv", 4747474)
                    .put("ct", 24)
                    .put("format", "json")
                    .put("inCharset", "utf-8")
                    .put("outCharset", "utf-8")
                    .put("notice", 0)
                    .put("platform", "yqq.json")
                    .put("needNewCode", 1)
                    .put("uin", 0)
            )
            .put(
                "req",
                JSONObject()
                    .put("module", "music.globalComment.CommentRead")
                    .put("method", "GetHotCommentList")
                    .put(
                        "param",
                        JSONObject()
                            .put("BizType", 1)
                            .put("BizId", songId)
                            .put("LastCommentSeqNo", "")
                            .put("PageSize", limit.coerceIn(1, 50))
                            .put("PageNum", 0)
                            .put("HotType", 1)
                            .put("WithAirborne", 0)
                            .put("PicEnable", 1)
                    )
            )
        val json = postJson(
            "https://u.y.qq.com/cgi-bin/musicu.fcg",
            payload.toString().toRequestBody(jsonMediaType),
            headers = mapOf(
                "User-Agent" to desktopUserAgent,
                "Referer" to "https://y.qq.com/",
                "Origin" to "https://y.qq.com"
            )
        ) ?: return PlaylistCommentBundle(emptyList(), 0)
        val data = json.optJSONObject("req")?.optJSONObject("data")?.optJSONObject("CommentList")
            ?: return PlaylistCommentBundle(emptyList(), 0)
        val comments = data.optJSONArray("Comments").orEmpty().mapObjects { item ->
            PlaylistComment(
                id = "${item.optString("SeqNo")}_${item.optString("CmId")}",
                authorName = item.optString("Nick").clean("轻雀用户"),
                authorAvatarUrl = item.optString("Avatar").clean(),
                content = item.optString("Content").replace("\\n", "\n").clean("这条评论暂时没有内容"),
                timeLabel = item.optString("PubTime").toQqCommentTime(),
                likedCount = item.optInt("PraiseNum", 0),
                replyCount = item.optJSONArray("SubComments")?.length() ?: 0,
                replies = item.optJSONArray("SubComments").toLxCommentReplies(
                    authorKeys = listOf("Nick", "nick", "name"),
                    contentKeys = listOf("Content", "content"),
                    timeKeys = listOf("PubTime", "time"),
                    likeKeys = listOf("PraiseNum", "praisenum")
                )
            )
        }
        return PlaylistCommentBundle(comments, data.optInt("Total", comments.size))
    }

    private fun miguComments(song: Song, sort: PlaylistCommentSort, limit: Int): PlaylistCommentBundle {
        val songId = song.providerIds[platform.lxId] ?: song.providerIds[platform.id] ?: song.id
        val safeLimit = limit.coerceIn(1, 50)
        val url = if (sort == PlaylistCommentSort.Hot) {
            "https://app.c.nf.migu.cn/MIGUM3.0/user/comment/stack/v1.0?pageSize=$safeLimit" +
                "&queryType=2&resourceId=${songId.urlEncode()}&resourceType=2&hotCommentStart=0"
        } else {
            "https://app.c.nf.migu.cn/MIGUM3.0/user/comment/stack/v1.0?pageSize=$safeLimit" +
                "&queryType=1&resourceId=${songId.urlEncode()}&resourceType=2&commentId="
        }
        val json = getJson(url, headers = mapOf("User-Agent" to mobileUserAgent))
            ?: return PlaylistCommentBundle(emptyList(), 0)
        val data = json.optJSONObject("data") ?: return PlaylistCommentBundle(emptyList(), 0)
        val rawComments = if (sort == PlaylistCommentSort.Hot) {
            data.optJSONArray("hotComments").orEmpty()
        } else {
            data.optJSONArray("comments").orEmpty()
        }
        val comments = rawComments.mapObjects { item ->
            val user = item.optJSONObject("user")
            PlaylistComment(
                id = item.optString("commentId").ifBlank { item.optString("replyId") },
                authorName = user?.optString("nickName").clean("米谷用户"),
                authorAvatarUrl = user?.optString("middleIcon").clean()
                    .ifBlank { user?.optString("bigIcon").clean() }
                    .ifBlank { user?.optString("smallIcon").clean() },
                content = item.optString("commentInfo").clean("这条评论暂时没有内容"),
                timeLabel = item.optString("commentTime").clean(),
                likedCount = item.optJSONObject("opNumItem")?.optInt("thumbNum", 0) ?: 0,
                replyCount = item.optInt("replyTotalCount", 0)
            )
        }
        val total = if (sort == PlaylistCommentSort.Hot) {
            data.optInt("cfgHotCount", comments.size)
        } else {
            data.optInt("commentNums", comments.size)
        }
        return PlaylistCommentBundle(comments, total)
    }

    private fun kuwoLyrics(song: Song): List<LyricLine> {
        val songId = song.providerIds[platform.lxId] ?: song.providerIds[platform.id] ?: song.id
        val lyricxLines = runCatching { kuwoNewLyrics(songId, isGetLyricx = true) }.getOrDefault(emptyList())
        if (lyricxLines.isNotEmpty()) return lyricxLines

        val json = getJson(
            "http://m.kuwo.cn/newh5/singles/songinfoandlrc?musicId=${songId.urlEncode()}",
            headers = mapOf(
                "Referer" to "http://www.kuwo.cn/",
                "User-Agent" to mobileUserAgent
            )
        ) ?: return emptyList()
        val list = json.optJSONObject("data")?.optJSONArray("lrclist").orEmpty()
        return list.mapObjects { item ->
            val seconds = item.optDouble("time", -1.0).takeIf { it >= 0.0 } ?: return@mapObjects null
            val text = item.optString("lineLyric").clean()
            if (text.isBlank()) return@mapObjects null
            LyricLine((seconds * 1000.0).toLong(), text)
        }
    }

    private fun kuwoNewLyrics(songId: String, isGetLyricx: Boolean): List<LyricLine> {
        val encodedParams = kuwoLyricParams(songId, isGetLyricx)
        val bytes = getBytes(
            "http://newlyric.kuwo.cn/newlyric.lrc?$encodedParams",
            headers = mapOf("User-Agent" to mobileUserAgent)
        ) ?: return emptyList()
        val decodedText = decodeKuwoLyric(bytes, isGetLyricx)
        return decodedText
            .replace(Regex("<-?\\d+,-?\\d+(?:,-?\\d+)?>"), "")
            .parseLrc()
            .mergeSameTimestampTranslations()
    }

    private fun kugouLyrics(song: Song): List<LyricLine> {
        val hash = song.providerIds["kg_hash"].orEmpty()
        if (hash.isBlank()) return emptyList()
        val seconds = (song.durationMs / 1000L).coerceAtLeast(0L)
        val searchJson = getJson(
            "http://lyrics.kugou.com/search?ver=1&man=yes&client=pc" +
                "&keyword=${song.title.urlEncode()}&hash=${hash.urlEncode()}" +
                "&timelength=$seconds&lrctxt=1",
            headers = kugouLyricHeaders
        ) ?: return emptyList()
        val candidate = searchJson.optJSONArray("candidates")?.optJSONObject(0) ?: return emptyList()
        val id = candidate.optString("id").ifBlank { return emptyList() }
        val accessKey = candidate.optString("accesskey").ifBlank { return emptyList() }
        val lyricJson = getJson(
            "http://lyrics.kugou.com/download?ver=1&client=pc&id=${id.urlEncode()}" +
                "&accesskey=${accessKey.urlEncode()}&fmt=lrc&charset=utf8",
            headers = kugouLyricHeaders
        ) ?: return emptyList()
        val encoded = lyricJson.optString("content").ifBlank { return emptyList() }
        return encoded.decodeBase64Text().parseLrc()
    }

    private fun qqLyrics(song: Song): List<LyricLine> {
        val songMid = song.providerIds[platform.lxId] ?: song.providerIds[platform.id] ?: song.id
        if (songMid.isBlank()) return emptyList()
        val json = getJson(
            "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=${songMid.urlEncode()}" +
                "&g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=utf8&outCharset=utf-8&platform=yqq",
            headers = mapOf("Referer" to "https://y.qq.com/portal/player.html")
        ) ?: return emptyList()
        if (json.optInt("code", -1) != 0) return emptyList()
        val lyric = json.optString("lyric").decodeBase64Text()
        val translation = json.optString("trans").decodeBase64Text()
        return mergeLyrics(lyric.parseLrc(), translation.parseLrc())
    }

    private fun miguLyrics(song: Song): List<LyricLine> {
        val lyricUrl = song.providerIds["mg_lrc_url"].orEmpty()
        val translationUrl = song.providerIds["mg_trc_url"].orEmpty()
        if (lyricUrl.isBlank()) return emptyList()
        val lyric = getText(lyricUrl, headers = miguLyricHeaders).orEmpty()
        if (lyric.isBlank()) return emptyList()
        val translation = translationUrl
            .takeIf { it.isNotBlank() }
            ?.let { getText(it, headers = miguLyricHeaders).orEmpty() }
            .orEmpty()
        return mergeLyrics(lyric.parseLrc(), translation.parseLrc())
    }

    private fun platformIdentity(songId: String): Map<String, String> =
        mapOf(platform.id to songId, platform.lxId to songId)

    private fun getJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string().orEmpty().toJsonObjectOrNull()
        }
    }

    private fun getText(url: String, headers: Map<String, String> = emptyMap()): String? {
        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string()
        }
    }

    private fun getBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.bytes()
        }
    }

    private fun postJson(url: String, body: okhttp3.RequestBody, headers: Map<String, String>): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .post(body)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string().orEmpty().toJsonObjectOrNull()
        }
    }

        data class LxPlatform(
        val id: String,
        val lxId: String,
        val label: String,
        val description: String,
        val accentHex: Long
    )

    companion object {
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        private const val defaultCover = "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900"
        private const val mobileUserAgent = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Mobile Safari/537.36"
        private const val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/113 Safari/537.36"
        private const val miguDeviceId = "963B7AA0D21511ED807EE5846EC87D20"
        private val kugouLyricHeaders = mapOf(
            "KG-RC" to "1",
            "KG-THash" to "expand_search_manager.cpp:852736169:451",
            "User-Agent" to "KuGou2012-9020-ExpandSearchManager"
        )
        private val miguLyricHeaders = mapOf(
            "Referer" to "https://app.c.nf.migu.cn/",
            "User-Agent" to mobileUserAgent,
            "channel" to "0146921"
        )
        private val miguDetailHeaders = mapOf(
            "Referer" to "https://m.music.migu.cn/",
            "User-Agent" to mobileUserAgent,
            "channel" to "0146921"
        )

        fun kuwo(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.KUWO, "kw", "酷窝曲库", "内置搜索与评论模块，播放交给自定义源解析。", 0xFFFFA726)
        )

        fun kugou(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.KUGOU, "kg", "酷构曲库", "内置搜索与评论模块，播放交给自定义源解析。", 0xFF42A5F5)
        )

        fun qq(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.QQ, "tx", "轻雀曲库", "内置搜索与评论模块，播放交给自定义源解析。", 0xFF66BB6A)
        )

        fun migu(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.MIGU, "mg", "米谷曲库", "内置搜索与评论模块，播放交给自定义源解析。", 0xFFFF7043)
        )
    }
}

private data class CachedSongs(
    val items: List<Song>,
    val expiresAt: Long
)

private data class CachedCollections(
    val items: List<MusicPlaylist>,
    val expiresAt: Long
)

private data class CachedCollectionDetail(
    val item: MusicPlaylist,
    val expiresAt: Long
)

private fun CachedSongs.takeIfFresh(): List<Song>? =
    items.takeIf { expiresAt > System.currentTimeMillis() }

private fun CachedCollections.takeIfFresh(): List<MusicPlaylist>? =
    items.takeIf { expiresAt > System.currentTimeMillis() }

private fun CachedCollectionDetail.takeIfFresh(): MusicPlaylist? =
    item.takeIf { expiresAt > System.currentTimeMillis() }

private fun cacheTtl(items: Collection<*>): Long =
    System.currentTimeMillis() + if (items.isEmpty()) 30_000L else 3 * 60_000L

private fun String.cacheKey(): String =
    trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

private fun Request.Builder.headers(headers: Map<String, String>): Request.Builder = apply {
    if ("User-Agent" !in headers) header("User-Agent", "Mozilla/5.0 SiListen/0.3 Android")
    headers.forEach { (key, value) -> header(key, value) }
}

private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (index in 0 until length()) {
        optJSONObject(index)?.let(block)
    }
}

private inline fun <T : Any> JSONArray.mapObjects(block: (JSONObject) -> T?): List<T> =
    buildList {
        forEachObject { item ->
            block(item)?.let(::add)
        }
    }

private fun JSONArray?.names(key: String): String {
    if (this == null) return ""
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.optString(key)?.takeIf { it.isNotBlank() }?.let(::add)
        }
    }.joinToString(" / ")
}

private fun String.toJsonObjectOrNull(): JSONObject? {
    val text = trim()
        .removePrefix("callback(")
        .removeSuffix(")")
    return runCatching { JSONTokener(text).nextValue() as? JSONObject }.getOrNull()
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

private fun String?.clean(fallback: String = ""): String {
    val value = this.orEmpty().trim()
    return when {
        value.isBlank() -> fallback
        value.equals("null", ignoreCase = true) -> fallback
        else -> value.htmlEntityDecode()
    }
}

private fun String.htmlEntityDecode(): String =
    replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

private const val fallbackArtworkUrl = "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900"

private fun String?.toArtworkUrl(fallback: String = fallbackArtworkUrl): String {
    val cover = clean()
        .replace("\\/", "/")
        .replace("{size}", "500")
        .replace("{0}", "500")
        .replace("!400", "")
    if (cover.isBlank()) return fallback
    return when {
        cover.startsWith("https://") || cover.startsWith("http://") -> cover
            .preferHttpsForStableArtworkHosts()
        cover.startsWith("//") -> "https:$cover"
        cover.startsWith("/") -> "https://d.musicapp.migu.cn$cover"
        cover.startsWith("albumcover/") -> "https://img1.kwcdn.kuwo.cn/star/${cover.toKuwoSizedPath("albumcover")}"
        cover.startsWith("starheads/") -> "https://img1.kwcdn.kuwo.cn/star/${cover.toKuwoSizedPath("starheads")}"
        else -> fallback
    }
}

private fun String.preferHttpsForStableArtworkHosts(): String =
    when {
        startsWith("http://imge.kugou.com/", ignoreCase = true) -> replaceFirst("http://", "https://")
        startsWith("http://singerimg.kugou.com/", ignoreCase = true) -> replaceFirst("http://", "https://")
        startsWith("http://imgecnt.kugou.com/", ignoreCase = true) -> replaceFirst("http://", "https://")
        startsWith("http://y.gtimg.cn/", ignoreCase = true) -> replaceFirst("http://", "https://")
        else -> this
    }

private fun String.toMiguCover(fallback: String = fallbackArtworkUrl): String =
    clean().let { cover ->
        if (cover.isBlank()) fallback else cover.toArtworkUrl(fallback = fallback)
    }

private fun JSONObject.kuwoCoverUrl(
    kind: PlaylistKind = PlaylistKind.Album,
    fallback: String = fallbackArtworkUrl
): String {
    val direct = optString("hts_MVPIC").clean()
        .ifBlank { optString("MVPIC").clean() }
        .ifBlank { optString("hts_pic").clean() }
        .ifBlank { optString("hts_img").clean() }
        .ifBlank { optString("hts_PICPATH").clean() }
        .ifBlank { optString("pic").clean() }
        .ifBlank { optString("img").clean() }
        .ifBlank { optString("image").clean() }
        .ifBlank { optString("cover").clean() }
        .ifBlank { optString("coverUrl").clean() }
        .ifBlank { optString("web_albumpic").clean() }
        .ifBlank { optString("web_artistpic").clean() }
    direct.toArtworkUrl(fallback = "").takeIf { it.isNotBlank() }?.let { return it }

    val folder = if (kind == PlaylistKind.Artist) "starheads" else "albumcover"
    val picPath = optString("PICPATH").clean()
        .ifBlank { optString("picpath").clean() }
        .ifBlank { optString("picPath").clean() }
    if (picPath.isNotBlank()) {
        return "https://img1.kwcdn.kuwo.cn/star/${picPath.toKuwoSizedPath(folder)}"
    }

    val albumShort = optString("web_albumpic_short").clean()
    if (albumShort.isNotBlank()) {
        return "https://img1.kwcdn.kuwo.cn/star/${albumShort.toKuwoSizedPath("albumcover")}"
    }

    val artistShort = optString("web_artistpic_short").clean()
    if (artistShort.isNotBlank()) {
        return "https://img1.kwcdn.kuwo.cn/star/${artistShort.toKuwoSizedPath("starheads")}"
    }

    return fallback
}

private fun String.toKugouCover(fallback: String = fallbackArtworkUrl): String {
    val cover = clean()
    if (cover.isBlank()) return fallback
    return cover
        .replace("{size}", "480")
        .replace("{0}", "480")
        .toArtworkUrl(fallback = fallback)
}

private fun JSONObject.firstArtworkUrl(keys: List<String>, fallback: String = fallbackArtworkUrl): String =
    keys.firstNotNullOfOrNull { key ->
        optString(key).toArtworkUrl(fallback = "").takeIf { it.isNotBlank() }
    } ?: fallback

private fun qqAlbumCover(albumMid: String): String =
    if (albumMid.isBlank()) {
        fallbackArtworkUrl
    } else {
        "https://y.gtimg.cn/music/photo_new/T002R500x500M000$albumMid.jpg"
    }

private fun qqSingerAvatar(singerMid: String): String =
    if (singerMid.isBlank()) {
        fallbackArtworkUrl
    } else {
        "https://y.gtimg.cn/music/photo_new/T001R500x500M000$singerMid.jpg"
    }

private fun String.toKuwoSizedPath(folder: String): String {
    val path = trim().trimStart('/')
        .removePrefix("$folder/")
        .let { value ->
            val firstSegment = value.substringBefore('/')
            if (firstSegment.all(Char::isDigit) && '/' in value) value.substringAfter('/') else value
        }
    return "$folder/500/$path"
}

private fun String.decodeBase64Text(): String {
    if (isBlank()) return ""
    return runCatching {
        String(Base64.decode(this, Base64.DEFAULT), Charsets.UTF_8)
    }.getOrDefault("")
}

private fun kuwoLyricParams(songId: String, isGetLyricx: Boolean): String {
    val raw = buildString {
        append("user=12345,web,web,web&requester=localhost&req=1&rid=MUSIC_")
        append(songId)
        if (isGetLyricx) append("&lrcx=1")
    }.toByteArray(Charsets.UTF_8)
    val key = "yeelion".toByteArray(Charsets.UTF_8)
    val output = ByteArray(raw.size) { index ->
        (raw[index].toInt() xor key[index % key.size].toInt()).toByte()
    }
    return Base64.encodeToString(output, Base64.NO_WRAP)
}

private fun decodeKuwoLyric(bytes: ByteArray, isGetLyricx: Boolean): String {
    if (bytes.size < 10 || !String(bytes, 0, 10, Charsets.UTF_8).startsWith("tp=content")) {
        return ""
    }
    val separator = "\r\n\r\n".toByteArray(Charsets.UTF_8)
    val start = bytes.indexOf(separator).takeIf { it >= 0 }?.plus(separator.size) ?: return ""
    val inflated = InflaterInputStream(bytes.copyOfRange(start, bytes.size).inputStream()).use { input ->
        input.readBytes()
    }
    if (!isGetLyricx) {
        return String(inflated, charset("GB18030"))
    }
    val encrypted = runCatching {
        Base64.decode(String(inflated, Charsets.UTF_8), Base64.DEFAULT)
    }.getOrDefault(ByteArray(0))
    if (encrypted.isEmpty()) return ""
    val key = "yeelion".toByteArray(Charsets.UTF_8)
    val output = ByteArray(encrypted.size) { index ->
        (encrypted[index].toInt() xor key[index % key.size].toInt()).toByte()
    }
    return String(output, charset("GB18030"))
}

private fun String.parseLrc(): List<LyricLine> {
    val timePattern = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?]")
    return lineSequence().flatMap { rawLine ->
        val cleanLine = rawLine.trim()
        val matches = timePattern.findAll(cleanLine).toList()
        val text = cleanLine.replace(timePattern, "").trim()
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

private fun List<LyricLine>.mergeSameTimestampTranslations(): List<LyricLine> {
    if (isEmpty()) return emptyList()
    return groupBy { it.timeMs }
        .toSortedMap()
        .mapNotNull { (_, lines) ->
            val main = lines.firstOrNull { it.text.isNotBlank() } ?: return@mapNotNull null
            val translation = lines
                .drop(1)
                .firstOrNull { it.text.isNotBlank() && it.text != main.text }
                ?.text
            main.copy(translation = main.translation ?: translation)
        }
}

private fun ByteArray.indexOf(pattern: ByteArray): Int {
    if (pattern.isEmpty() || size < pattern.size) return -1
    for (start in 0..(size - pattern.size)) {
        var matched = true
        for (offset in pattern.indices) {
            if (this[start + offset] != pattern[offset]) {
                matched = false
                break
            }
        }
        if (matched) return start
    }
    return -1
}

private fun mergeLyrics(
    mainLines: List<LyricLine>,
    translationLines: List<LyricLine>
): List<LyricLine> {
    if (mainLines.isEmpty()) return translationLines
    if (translationLines.isEmpty()) return mainLines
    return mainLines.map { line ->
        val translation = translationLines
            .minByOrNull { kotlin.math.abs(it.timeMs - line.timeMs) }
            ?.takeIf { kotlin.math.abs(it.timeMs - line.timeMs) <= 600L }
            ?.text
            ?.takeIf { it.isNotBlank() && it != line.text }
        line.copy(translation = translation)
    }
}

private fun Long.secondsToMillis(): Long =
    if (this <= 0L) 0L else this * 1000L

private fun Long.secondsToCommentTime(): String =
    if (this <= 0L) "刚刚" else (this * 1000L).toCommentTime()

private fun Long.toCommentTime(): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(this))

private fun JSONArray?.toLxCommentReplies(
    authorKeys: List<String>,
    contentKeys: List<String>,
    timeKeys: List<String>,
    likeKeys: List<String>
): List<PlaylistCommentReply> {
    if (this == null) return emptyList()
    return mapObjects { item ->
        val author = item.firstClean(authorKeys, "用户")
        val content = item.firstClean(contentKeys, "")
        if (content.isBlank()) {
            null
        } else {
            PlaylistCommentReply(
                authorName = author,
                content = content,
                timeLabel = item.firstClean(timeKeys, ""),
                likedCount = item.firstInt(likeKeys)
            )
        }
    }
}

private fun JSONObject.firstClean(keys: List<String>, fallback: String): String =
    keys.firstNotNullOfOrNull { key -> optString(key).clean().takeIf { it.isNotBlank() } } ?: fallback

private fun JSONObject.firstInt(keys: List<String>): Int =
    keys.firstNotNullOfOrNull { key -> optInt(key, 0).takeIf { it > 0 } } ?: 0

private fun String.toQqCommentTime(): String {
    val raw = trim()
    val value = raw.toLongOrNull() ?: return raw
    return when {
        raw.length >= 13 -> value.toCommentTime()
        raw.length >= 10 -> (value * 1000L).toCommentTime()
        else -> raw
    }
}

private fun kugouSignature(params: String): String {
    val key = "OIlwieks28dk2k092lksi2UIkp"
    val sorted = params.split('&').sorted().joinToString("")
    return "$key$sorted$key".md5()
}

private fun miguSignature(time: String, query: String): String {
    val signatureMd5 = "6cdc72a439cef99a3418d2a78aa28c73"
    return "$query${signatureMd5}yyapp2d16148780a1dcc7408e06336b98cfd50${miguDeviceIdForSign()}$time".md5()
}

private fun miguDeviceIdForSign(): String = "963B7AA0D21511ED807EE5846EC87D20"

private fun String.md5(): String =
    MessageDigest.getInstance("MD5")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
