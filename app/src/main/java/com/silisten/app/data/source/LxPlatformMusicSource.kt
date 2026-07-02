package com.silisten.app.data.source

import android.util.Base64
import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.MusicSourceInfo
import com.silisten.app.data.model.PlaylistComment
import com.silisten.app.data.model.PlaylistCommentBundle
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.SourcePlatformIds
import com.silisten.app.data.model.Song
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
) : MusicSource, PagedMusicSearchSource, SongCommentSource {
    override val info = MusicSourceInfo(
        id = platform.id,
        name = platform.label,
        description = platform.description,
        badge = platform.lxId,
        accentHex = platform.accentHex
    )

    override suspend fun featured(): List<MusicPlaylist> = emptyList()

    override suspend fun search(keyword: String): List<Song> = searchSongs(keyword, 30, 0)

    override suspend fun searchSongs(keyword: String, limit: Int, offset: Int): List<Song> =
        withContext(Dispatchers.IO) {
            val query = keyword.trim()
            if (query.isBlank()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, 50)
            val page = offset.coerceAtLeast(0) / safeLimit + 1
            runCatching {
                when (platform.id) {
                    SourcePlatformIds.KUWO -> searchKuwo(query, page, safeLimit)
                    SourcePlatformIds.KUGOU -> searchKugou(query, page, safeLimit)
                    SourcePlatformIds.QQ -> searchQq(query, page, safeLimit)
                    SourcePlatformIds.MIGU -> searchMigu(query, page, safeLimit)
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
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
        limit: Int
    ): PlaylistCommentBundle =
        withContext(Dispatchers.IO) {
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
        return songs.mapObjects { item ->
            val songMid = item.optString("mid").ifBlank { item.optString("songmid") }.ifBlank { return@mapObjects null }
            val songId = item.optLong("id", item.optLong("songid", 0L)).takeIf { it > 0L }?.toString().orEmpty()
            val album = item.optJSONObject("album")
            val albumMid = album?.optString("mid").orEmpty()
            Song(
                id = songMid,
                title = item.optString("title").ifBlank { item.optString("songname") }.clean("未知歌曲"),
                artist = item.optJSONArray("singer").names("name").clean("未知歌手"),
                album = album?.optString("name").orEmpty().clean("未知专辑"),
                coverUrl = if (albumMid.isNotBlank()) {
                    "https://y.gtimg.cn/music/photo_new/T002R500x500M000$albumMid.jpg"
                } else {
                    defaultCover
                },
                durationMs = item.optLong("interval", 0L).secondsToMillis(),
                sourceId = platform.id,
                providerIds = platformIdentity(songMid) + mapOf(
                    "tx_song_id" to songId,
                    "tx_media_mid" to (item.optJSONObject("file")?.optString("media_mid").orEmpty()),
                    "tx_album_mid" to albumMid
                )
            )
        }
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
                authorName = item.optString("u_name").clean("酷我用户"),
                authorAvatarUrl = item.optString("u_pic").clean(),
                content = item.optString("msg").clean("这条评论暂时没有内容"),
                timeLabel = item.optLong("time", 0L).secondsToCommentTime(),
                likedCount = item.optInt("like_num", 0),
                replyCount = item.optJSONArray("child_comments")?.length() ?: 0
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
                authorName = item.optString("user_name").clean("酷狗用户"),
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
                authorName = item.optString("rootcommentnick").removePrefix("@").clean("QQ 音乐用户"),
                authorAvatarUrl = item.optString("avatarurl").clean(),
                content = content,
                timeLabel = item.optString("time").toQqCommentTime(),
                likedCount = item.optInt("praisenum", 0),
                replyCount = item.optJSONArray("middlecommentcontent")?.length() ?: 0
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
                authorName = item.optString("Nick").clean("QQ 音乐用户"),
                authorAvatarUrl = item.optString("Avatar").clean(),
                content = item.optString("Content").replace("\\n", "\n").clean("这条评论暂时没有内容"),
                timeLabel = item.optString("PubTime").toQqCommentTime(),
                likedCount = item.optInt("PraiseNum", 0),
                replyCount = item.optJSONArray("SubComments")?.length() ?: 0
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
                authorName = user?.optString("nickName").clean("咪咕用户"),
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
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
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

        fun kuwo(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.KUWO, "kw", "酷我音乐", "内置酷我搜索与评论模块，播放交给自定义音源解析。", 0xFFFFA726)
        )

        fun kugou(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.KUGOU, "kg", "酷狗音乐", "内置酷狗搜索与评论模块，播放交给自定义音源解析。", 0xFF42A5F5)
        )

        fun qq(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.QQ, "tx", "QQ 音乐", "内置 QQ 搜索与评论模块，播放交给自定义音源解析。", 0xFF66BB6A)
        )

        fun migu(): LxPlatformMusicSource = LxPlatformMusicSource(
            LxPlatform(SourcePlatformIds.MIGU, "mg", "咪咕音乐", "内置咪咕搜索与评论模块，播放交给自定义音源解析。", 0xFFFF7043)
        )
    }
}

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
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

private fun String.toMiguCover(): String =
    clean().let { cover ->
        when {
            cover.isBlank() -> "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900"
            cover.startsWith("http://") || cover.startsWith("https://") -> cover
            else -> "http://d.musicapp.migu.cn$cover"
        }
    }

private fun JSONObject.kuwoCoverUrl(): String {
    val direct = optString("hts_MVPIC").clean()
        .ifBlank { optString("MVPIC").clean() }
    if (direct.startsWith("http://") || direct.startsWith("https://")) return direct

    val albumShort = optString("web_albumpic_short").clean()
    if (albumShort.isNotBlank()) {
        return "http://img1.kwcdn.kuwo.cn/star/albumcover/500/${albumShort.substringAfter('/')}"
    }

    val artistShort = optString("web_artistpic_short").clean()
    if (artistShort.isNotBlank()) {
        return "http://img1.kwcdn.kuwo.cn/star/starheads/500/${artistShort.substringAfter('/')}"
    }

    return "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900"
}

private fun String.toKugouCover(): String {
    val cover = clean()
    if (cover.isBlank()) return "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=900"
    return cover.replace("{size}", "480")
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
