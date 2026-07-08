package com.silisten.app.data.repository

import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.PlaylistKind
import com.silisten.app.data.model.SourcePlatformIds
import java.net.URLDecoder
import java.util.Locale

data class PlaylistImportResult(
    val playlist: MusicPlaylist?,
    val message: String
)

class PlaylistImportRepository(
    private val musicRepository: MusicRepository
) {
    suspend fun import(input: String): PlaylistImportResult {
        val candidate = PlaylistLinkCandidate.parse(input)
            ?: return PlaylistImportResult(null, "没有识别到支持的歌单链接")
        if (candidate.sourceId == SourcePlatformIds.KUWO) {
            return PlaylistImportResult(null, "已识别酷我歌单链接，但当前版本暂不支持按链接精确导入酷我歌单")
        }
        val shell = candidate.toPlaylistShell()
        val detail = runCatching { musicRepository.playlistDetail(shell) }.getOrNull()
            ?: return PlaylistImportResult(null, "歌单读取失败，请确认链接公开可访问")
        if (detail.songs.isEmpty()) {
            return PlaylistImportResult(null, "已识别${candidate.platformName}链接，但没有读取到可导入的歌曲")
        }
        return PlaylistImportResult(
            playlist = detail.copy(
                title = detail.title.ifBlank { candidate.platformName },
                subtitle = detail.subtitle.ifBlank { "${candidate.platformName} · 链接导入" }
            ),
            message = "已导入「${detail.title.ifBlank { candidate.platformName }}」"
        )
    }
}

private data class PlaylistLinkCandidate(
    val sourceId: String,
    val id: String,
    val platformName: String
) {
    fun toPlaylistShell(): MusicPlaylist =
        MusicPlaylist(
            id = "$sourceId-${PlaylistKind.Playlist.name.lowercase()}-$id",
            title = "$platformName 歌单",
            subtitle = "$platformName · 链接导入",
            coverUrl = "",
            songs = emptyList(),
            kind = PlaylistKind.Playlist
        )

    companion object {
        fun parse(input: String): PlaylistLinkCandidate? {
            val text = input.trim()
            if (text.isBlank()) return null
            val url = Regex("""https?://\S+""")
                .find(text)
                ?.value
                ?.trimEnd(')', ']', '}', '。', '，', ',', ';')
                ?: text
            val decoded = runCatching { URLDecoder.decode(url, Charsets.UTF_8.name()) }.getOrDefault(url)
            val lower = decoded.lowercase(Locale.ROOT)
            return when {
                "music.163.com" in lower || "y.music.163.com" in lower -> {
                    val id = decoded.queryValue("id") ?: decoded.firstNumberAfter("/playlist/")
                    id?.let { PlaylistLinkCandidate(SourcePlatformIds.NETEASE, it, "网易云") }
                }
                "y.qq.com" in lower || "i.y.qq.com" in lower || "c.y.qq.com" in lower || "qqmusic" in lower -> {
                    val id = decoded.queryValue("disstid")
                        ?: decoded.queryValue("dissid")
                        ?: decoded.firstNumberAfter("/playlist/")
                        ?: decoded.firstNumberAfter("/n/ryqq/playlist/")
                    id?.let { PlaylistLinkCandidate(SourcePlatformIds.QQ, it, "QQ 音乐") }
                }
                "kugou.com" in lower -> {
                    val id = decoded.queryValue("specialid")
                        ?: decoded.queryValue("global_collection_id")
                        ?: decoded.firstNumberAfter("/plist/list/")
                        ?: decoded.firstNumberAfter("/special/single/")
                    id?.let { PlaylistLinkCandidate(SourcePlatformIds.KUGOU, it, "酷狗") }
                }
                "migu.cn" in lower -> {
                    val id = decoded.queryValue("playlistId")
                        ?: decoded.queryValue("playlistid")
                        ?: decoded.queryValue("songListId")
                        ?: decoded.firstNumberAfter("/playlist/")
                    id?.let { PlaylistLinkCandidate(SourcePlatformIds.MIGU, it, "咪咕") }
                }
                "kuwo.cn" in lower -> {
                    val id = decoded.queryValue("pid")
                        ?: decoded.queryValue("playlistid")
                        ?: decoded.firstNumberAfter("/playlist/")
                    id?.let { PlaylistLinkCandidate(SourcePlatformIds.KUWO, it, "酷我") }
                }
                else -> null
            }
        }
    }
}

private fun String.queryValue(name: String): String? {
    val pattern = Regex("""[?&#]${Regex.escape(name)}=([^&#\s]+)""", RegexOption.IGNORE_CASE)
    return pattern.find(this)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}

private fun String.firstNumberAfter(prefix: String): String? {
    val index = lowercase(Locale.ROOT).indexOf(prefix.lowercase(Locale.ROOT))
    if (index < 0) return null
    val tail = substring(index + prefix.length)
    return Regex("""\d{4,}""").find(tail)?.value
}
