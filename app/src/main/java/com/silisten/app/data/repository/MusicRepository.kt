package com.silisten.app.data.repository

import android.content.Context
import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.CustomSourceConfig
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.PlaylistCommentBundle
import com.silisten.app.data.model.PlaylistCommentReply
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.Song
import com.silisten.app.data.model.SourcePlatformIds
import com.silisten.app.data.model.SourceSettingsState
import com.silisten.app.data.model.neteaseIdentityId
import com.silisten.app.data.model.normalizeBuiltInPlatformId
import com.silisten.app.data.model.withCanonicalIdentity
import com.silisten.app.data.model.withPlaybackSource
import com.silisten.app.data.source.CustomPlaybackSourceClient
import com.silisten.app.data.source.CustomSourceInspectResult
import com.silisten.app.data.source.CollectionDetailSource
import com.silisten.app.data.source.CollectionSearchSource
import com.silisten.app.data.source.MusicSource
import com.silisten.app.data.source.MusicSourceRegistry
import com.silisten.app.data.source.NeteaseActionResult
import com.silisten.app.data.source.NeteaseMusicSource
import com.silisten.app.data.source.PagedMusicSearchSource
import com.silisten.app.data.source.SongCommentSource
import com.silisten.app.data.source.SongCommentReplySource
import com.silisten.app.playback.SongFileCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

class MusicRepository(
    val registry: MusicSourceRegistry,
    private val customPlaybackSourceClient: CustomPlaybackSourceClient,
    private val appContext: Context,
    private val playbackQualityProvider: () -> PlaybackQuality = { PlaybackQuality.ExHigh }
) {
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sources(): List<MusicSource> = registry.all()

    fun source(sourceId: String): MusicSource = registry.byId(sourceId)

    fun defaultSourceId(): String = registry.default().info.id

    suspend fun inspectCustomSource(config: CustomSourceConfig): CustomSourceInspectResult =
        customPlaybackSourceClient.inspect(config)

    suspend fun inspectCustomSourceScript(script: String): CustomSourceInspectResult =
        customPlaybackSourceClient.inspectScript(script)

    suspend fun importCustomSourceScript(url: String) =
        customPlaybackSourceClient.importLxScriptFromUrl(url)

    fun clearNeteaseLibraryCache() {
        netease().clearLibraryCache()
    }

    suspend fun featured(sourceId: String): List<MusicPlaylist> =
        if (sourceId == SourcePlatformIds.ALL) source(defaultSourceId()).featured() else source(sourceId).featured()

    suspend fun search(
        sourceId: String,
        query: String,
        sourceSettings: SourceSettingsState? = null
    ): List<Song> {
        val songs = if (sourceId == SourcePlatformIds.ALL) {
            searchSongsAcrossEnabledSources(query, 30, 0, sourceSettings)
        } else {
            source(sourceId).search(query)
        }
        return rankSongsForQuery(query, songs)
    }

    suspend fun searchSongs(
        sourceId: String,
        query: String,
        limit: Int,
        offset: Int,
        sourceSettings: SourceSettingsState? = null
    ): List<Song> {
        if (sourceId == SourcePlatformIds.ALL) {
            return searchSongsAcrossEnabledSources(query, limit, offset, sourceSettings)
        }
        val source = source(sourceId)
        val songs = if (source is PagedMusicSearchSource) {
            withTimeoutOrNull(SINGLE_SOURCE_SEARCH_TIMEOUT_MS) {
                source.searchSongs(query, limit, offset)
            }.orEmpty()
        } else {
            if (offset == 0) source.search(query).take(limit) else emptyList()
        }
        // Keep page stability for offset > 0; still prefer original cuts within the page.
        return rankSongsForQuery(query, songs)
    }

    suspend fun matchNeteaseIdentity(song: Song): Song? {
        val existingId = song.neteaseIdentityId()
        if (!existingId.isNullOrBlank()) {
            return song.withCanonicalIdentity(SourcePlatformIds.NETEASE, existingId)
        }
        if (song.title.isBlank()) return null
        val query = "${song.title} ${song.artist}".trim()
        val candidates = runCatching { netease().searchSongs(query, limit = 8, offset = 0) }
            .getOrDefault(emptyList())
        val best = candidates
            .map { candidate -> candidate to scoreSongMatch(song, candidate) }
            .filter { (_, score) -> score >= 76 }
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: return null
        return song.withCanonicalIdentity(SourcePlatformIds.NETEASE, best.id)
            .withMatchedNeteaseMetadata(best)
    }

    suspend fun searchPlaylists(
        sourceId: String,
        query: String,
        limit: Int,
        offset: Int,
        sourceSettings: SourceSettingsState? = null
    ): List<MusicPlaylist> {
        if (sourceId == SourcePlatformIds.ALL) {
            return searchCollectionsAcrossEnabledSources(query, limit, offset, sourceSettings) { source, keyword, pageLimit, pageOffset ->
                source.searchPlaylists(keyword, pageLimit, pageOffset)
            }
        }
        val source = source(sourceId)
        return if (source is CollectionSearchSource) {
            withTimeoutOrNull(COLLECTION_SEARCH_TIMEOUT_MS) {
                source.searchPlaylists(query, limit, offset)
            }.orEmpty()
        } else {
            emptyList()
        }
    }

    suspend fun searchAlbums(
        sourceId: String,
        query: String,
        limit: Int,
        offset: Int,
        sourceSettings: SourceSettingsState? = null
    ): List<MusicPlaylist> {
        if (sourceId == SourcePlatformIds.ALL) {
            return searchCollectionsAcrossEnabledSources(query, limit, offset, sourceSettings) { source, keyword, pageLimit, pageOffset ->
                source.searchAlbums(keyword, pageLimit, pageOffset)
            }
        }
        val source = source(sourceId)
        return if (source is CollectionSearchSource) {
            withTimeoutOrNull(COLLECTION_SEARCH_TIMEOUT_MS) {
                source.searchAlbums(query, limit, offset)
            }.orEmpty()
        } else {
            emptyList()
        }
    }

    suspend fun searchArtists(
        sourceId: String,
        query: String,
        limit: Int,
        offset: Int,
        sourceSettings: SourceSettingsState? = null
    ): List<MusicPlaylist> {
        if (sourceId == SourcePlatformIds.ALL) {
            return searchCollectionsAcrossEnabledSources(query, limit, offset, sourceSettings) { source, keyword, pageLimit, pageOffset ->
                source.searchArtists(keyword, pageLimit, pageOffset)
            }
        }
        val source = source(sourceId)
        return if (source is CollectionSearchSource) {
            withTimeoutOrNull(COLLECTION_SEARCH_TIMEOUT_MS) {
                source.searchArtists(query, limit, offset)
            }.orEmpty()
        } else {
            emptyList()
        }
    }

    suspend fun lyrics(song: Song): List<LyricLine> {
        val primary = registry.findById(song.sourceId)?.lyrics(song).orEmpty()
        if (primary.hasUsableLyrics()) return primary
        return findFallbackLyrics(song).takeIf { it.hasUsableLyrics() } ?: primary
    }

    suspend fun resolvePlayable(
        song: Song,
        sourceSettings: SourceSettingsState
    ): Pair<Song, String>? {
        val quality = playbackQualityProvider()

        // 1) True complete song-file cache: play offline without re-resolving remote URLs.
        SongFileCache.findFile(appContext, song, quality)?.let { file ->
            return song to SongFileCache.playbackUri(file)
        }

        // 2) Local / explicit stream hints (file, content, or ready HTTP).
        song.streamHint
            ?.takeIf { it.isPlayableStreamUrl() }
            ?.let { hint ->
                persistResolvedStream(song, quality, hint)
                return song to hint
            }

        if (song.sourceId == SourcePlatformIds.LOCAL) {
            return null
        }

        val shouldPreferCustomSource =
            sourceSettings.autoSourceFallbackEnabled &&
                sourceSettings.customSources.any { it.enabled }
        if (shouldPreferCustomSource) {
            resolveByCustomSources(song, sourceSettings)?.let { resolved ->
                persistResolvedStream(resolved.first, quality, resolved.second)
                return resolved
            }
        }

        // 3) Official / built-in source stream.
        val directUrl = resolveDirectStream(song)
        if (!directUrl.isNullOrBlank()) {
            val resolvedIdentitySong = if (song.neteaseIdentityId().isNullOrBlank()) {
                runCatching { matchNeteaseIdentity(song) }.getOrNull() ?: song
            } else {
                song
            }
            persistResolvedStream(resolvedIdentitySong, quality, directUrl)
            return resolvedIdentitySong to directUrl
        }

        if (!sourceSettings.autoSourceFallbackEnabled) {
            return null
        }

        // 4) Custom playback sources as fallback.
        resolveByCustomSources(song, sourceSettings)?.let { resolved ->
            persistResolvedStream(resolved.first, quality, resolved.second)
            return resolved
        }

        // 5) Cross-platform title/artist match (was dead code before).
        for (candidateSourceId in PLATFORM_FALLBACK_ORDER) {
            if (candidateSourceId == song.sourceId) continue
            resolveByPlatformMatch(song, candidateSourceId)?.let { resolved ->
                persistResolvedStream(resolved.first, quality, resolved.second)
                return resolved
            }
        }

        return null
    }

    private fun persistResolvedStream(song: Song, quality: PlaybackQuality, streamUrl: String) {
        if (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://")) return
        cacheScope.launch {
            runCatching {
                SongFileCache.ensureDownloaded(appContext, song, quality, streamUrl)
            }
        }
    }

    private suspend fun resolveByCustomSources(
        song: Song,
        sourceSettings: SourceSettingsState
    ): Pair<Song, String>? {
        for (customSource in sourceSettings.customSources.filter { it.enabled }.distinctBy { it.endpoint.trim() }) {
            val url = customPlaybackSourceClient.resolvePlayUrl(song, customSource, playbackQualityProvider())
                ?.takeIf { it.isPlayableStreamUrl() }
            if (!url.isNullOrBlank()) {
                val identitySong = if (song.neteaseIdentityId().isNullOrBlank()) {
                    runCatching { matchNeteaseIdentity(song) }.getOrNull() ?: song
                } else {
                    song
                }
                return identitySong.withPlaybackSource(customSource.id, customSource.id) to url
            }
        }
        return null
    }

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

    suspend fun playlistDetail(playlist: MusicPlaylist): MusicPlaylist {
        val sourceId = playlist.collectionSourceId()
        val source = registry.findById(sourceId)
        val loaded = if (source is CollectionDetailSource) {
            withTimeoutOrNull(COLLECTION_DETAIL_TIMEOUT_MS) {
                runCatching { source.playlistDetail(playlist) }.getOrNull()
            }
        } else {
            null
        }
        val detail = loaded ?: playlist
        if (detail.songs.isNotEmpty()) return detail.withPlayableSummary()

        val fallbackSongs = searchSongs(
            sourceId = sourceId,
            query = detail.collectionFallbackQuery(),
            limit = 50,
            offset = 0,
            sourceSettings = null
        )
        return detail.copy(
            subtitle = if (fallbackSongs.isEmpty()) detail.subtitle else "${fallbackSongs.size} 首歌曲",
            coverUrl = detail.coverUrl.ifBlank { fallbackSongs.firstOrNull()?.coverUrl.orEmpty() },
            songs = fallbackSongs
        )
    }

    suspend fun neteaseArtistSongs(
        artist: MusicPlaylist,
        limit: Int,
        offset: Int
    ): List<Song> = netease().artistSongs(artist, limit, offset)

    suspend fun artistSongs(
        artist: MusicPlaylist,
        limit: Int,
        offset: Int
    ): List<Song> {
        val sourceId = artist.collectionSourceId()
        val source = registry.findById(sourceId)
        val songs = if (source is CollectionDetailSource) {
            withTimeoutOrNull(COLLECTION_DETAIL_TIMEOUT_MS) {
                runCatching { source.artistSongs(artist, limit, offset) }.getOrDefault(emptyList())
            }.orEmpty()
        } else {
            emptyList()
        }
        if (songs.isNotEmpty()) return songs
        return searchSongs(sourceId, artist.title, limit, offset, null)
    }

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
        sort: PlaylistCommentSort,
        limit: Int = 30,
        offset: Int = 0
    ): PlaylistCommentBundle = when (sort) {
        PlaylistCommentSort.Hot -> netease().hotSongComments(song, limit, offset)
        PlaylistCommentSort.Latest -> netease().songComments(song, limit, offset)
    }

    suspend fun songComments(
        song: Song,
        sort: PlaylistCommentSort,
        sourceSettings: SourceSettingsState,
        limit: Int = 30,
        offset: Int = 0
    ): PlaylistCommentBundle? {
        val commentSourceId = song.commentSourceId(sourceSettings.enabledCommentPlatformIds)
            ?: return null
        val commentSong = song.withCommentSourceIdentity(commentSourceId) ?: return null
        val source = registry.findById(commentSourceId) as? SongCommentSource ?: return null
        return source.commentsForSong(commentSong, sort, limit, offset)
    }

    suspend fun songCommentReplies(
        song: Song,
        commentId: String,
        sourceSettings: SourceSettingsState,
        limit: Int = 50
    ): List<PlaylistCommentReply> {
        val commentSourceId = song.commentSourceId(sourceSettings.enabledCommentPlatformIds)
            ?: return emptyList()
        val commentSong = song.withCommentSourceIdentity(commentSourceId) ?: return emptyList()
        val source = registry.findById(commentSourceId) as? SongCommentReplySource ?: return emptyList()
        return source.repliesForSongComment(commentSong, commentId, limit)
    }

    suspend fun neteaseTogglePlaylistSubscription(
        playlist: MusicPlaylist,
        subscribe: Boolean
    ): NeteaseActionResult = netease().togglePlaylistSubscription(playlist, subscribe)

    suspend fun neteaseToggleSongLike(
        song: Song,
        like: Boolean
    ): NeteaseActionResult = netease().toggleSongLike(song, like)

    suspend fun neteaseAddSongToPlaylist(
        song: Song,
        playlist: MusicPlaylist
    ): NeteaseActionResult = netease().addSongToPlaylist(song, playlist)

    private fun netease(): NeteaseMusicSource =
        registry.byId(SourcePlatformIds.NETEASE) as? NeteaseMusicSource
            ?: error("网易云音乐源未注册")

    private suspend fun searchSongsAcrossEnabledSources(
        query: String,
        limit: Int,
        offset: Int,
        sourceSettings: SourceSettingsState? = null
    ): List<Song> {
        val searchableIds = sourceSettings.searchPlatformIds()
        val perSourceOffset = offset
        return coroutineScope {
            val searchJobs: List<Deferred<List<Song>>> = searchableIds.mapNotNull { sourceId ->
                registry.findById(sourceId)?.let { source ->
                    async<List<Song>> {
                        runCatching {
                            withTimeoutOrNull(MULTI_SOURCE_SEARCH_TIMEOUT_MS) {
                                if (source is PagedMusicSearchSource) {
                                    source.searchSongs(query, limit.coerceAtMost(30), perSourceOffset)
                                } else {
                                    if (offset == 0) source.search(query).take(limit.coerceAtMost(30)) else emptyList()
                                }
                            }.orEmpty()
                        }.getOrDefault(emptyList())
                    }
                }
            }
            val merged = searchJobs.awaitAll()
                .interleave()
                .distinctBy { "${it.sourceId}:${it.id}" }
            rankSongsForQuery(query, merged).take(limit)
        }
    }

    private fun List<List<Song>>.interleave(): List<Song> = buildList {
        val maxSize = this@interleave.maxOfOrNull { it.size } ?: 0
        for (index in 0 until maxSize) {
            for (songs in this@interleave) {
                songs.getOrNull(index)?.let(::add)
            }
        }
    }

    private suspend fun searchCollectionsAcrossEnabledSources(
        query: String,
        limit: Int,
        offset: Int,
        sourceSettings: SourceSettingsState? = null,
        search: suspend (CollectionSearchSource, String, Int, Int) -> List<MusicPlaylist>
    ): List<MusicPlaylist> {
        val searchableIds = sourceSettings.searchPlatformIds()
        val perSourceLimit = collectionPerSourceLimit(limit, searchableIds.size)
        val perSourceOffset = collectionPerSourceOffset(offset, searchableIds.size)
        return coroutineScope {
            val searchJobs: List<Deferred<List<MusicPlaylist>>> = searchableIds.mapNotNull { sourceId ->
                (registry.findById(sourceId) as? CollectionSearchSource)?.let { source ->
                    async<List<MusicPlaylist>> {
                        runCatching {
                            withTimeoutOrNull(COLLECTION_SEARCH_TIMEOUT_MS) {
                                search(source, query, perSourceLimit, perSourceOffset)
                            }.orEmpty()
                        }.getOrDefault(emptyList())
                    }
                }
            }
            searchJobs.awaitAll()
                .interleavePlaylists()
                .distinctBy { it.id }
                .take(limit)
        }
    }

    private fun collectionPerSourceLimit(limit: Int, sourceCount: Int): Int {
        if (sourceCount <= 1) return limit.coerceAtMost(30)
        val balancedLimit = (limit.coerceAtLeast(1) + sourceCount - 1) / sourceCount + 2
        return balancedLimit.coerceIn(3, 8)
    }

    private fun collectionPerSourceOffset(offset: Int, sourceCount: Int): Int =
        if (sourceCount <= 1) {
            offset.coerceAtLeast(0)
        } else {
            (offset.coerceAtLeast(0) / sourceCount).coerceAtLeast(0)
        }

    private fun List<List<MusicPlaylist>>.interleavePlaylists(): List<MusicPlaylist> = buildList {
        val maxSize = this@interleavePlaylists.maxOfOrNull { it.size } ?: 0
        for (index in 0 until maxSize) {
            for (playlists in this@interleavePlaylists) {
                playlists.getOrNull(index)?.let(::add)
            }
        }
    }

    private fun List<LyricLine>.hasUsableLyrics(): Boolean =
        any { line ->
            val text = line.text.trim()
            text.isNotBlank() &&
                text != "歌词加载中..." &&
                text != "暂时没有歌词"
        }

    private fun SourceSettingsState?.searchPlatformIds(): List<String> {
        val enabled = this?.enabledSearchPlatformIds
            ?.map(::normalizeBuiltInPlatformId)
            ?.toSet()
            ?: SourceSettingsState().enabledSearchPlatformIds
        return listOf(
            SourcePlatformIds.NETEASE,
            SourcePlatformIds.KUWO,
            SourcePlatformIds.KUGOU,
            SourcePlatformIds.QQ,
            SourcePlatformIds.MIGU
        ).filter { it in enabled && registry.findById(it) != null }
    }

    private fun MusicPlaylist.collectionSourceId(): String =
        id.substringBefore('-', missingDelimiterValue = "")
            .takeIf { it in collectionSourceIds }
            ?: songs.firstOrNull()?.sourceId
            ?: defaultSourceId()

    private fun MusicPlaylist.collectionFallbackQuery(): String =
        when {
            title.isNotBlank() && subtitle.isNotBlank() -> "$title $subtitle"
            title.isNotBlank() -> title
            else -> subtitle
        }.trim().ifBlank { "华语流行" }

    private fun MusicPlaylist.withPlayableSummary(): MusicPlaylist =
        copy(
            subtitle = when {
                songs.isEmpty() -> subtitle
                kind == com.silisten.app.data.model.PlaylistKind.Artist -> {
                    val total = songCount.coerceAtLeast(songs.size)
                    if (total > 0) "$total 首单曲" else subtitle
                }
                else -> "${songs.size} 首歌曲"
            },
            coverUrl = coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() }
        )

    private suspend fun findFallbackLyrics(song: Song): List<LyricLine> {
        for (candidate in song.lyricIdentityCandidates()) {
            val lines = runCatching { registry.findById(candidate.sourceId)?.lyrics(candidate).orEmpty() }
                .getOrDefault(emptyList())
            if (lines.hasUsableLyrics()) return lines
        }

        val query = "${song.title} ${song.artist}".trim()
        if (query.isBlank()) return emptyList()
        for (sourceId in lyricFallbackSourceIds(song.sourceId)) {
            val source = registry.findById(sourceId) as? PagedMusicSearchSource ?: continue
            val best = withTimeoutOrNull(LYRIC_MATCH_SEARCH_TIMEOUT_MS) {
                runCatching { source.searchSongs(query, limit = 8, offset = 0) }
                    .getOrDefault(emptyList())
                    .map { candidate -> candidate to scoreSongMatch(song, candidate) }
                    .filter { (_, score) -> score >= LYRIC_MATCH_MIN_SCORE }
                    .maxByOrNull { (_, score) -> score }
                    ?.first
            } ?: continue
            val lines = runCatching { registry.findById(best.sourceId)?.lyrics(best).orEmpty() }
                .getOrDefault(emptyList())
            if (lines.hasUsableLyrics()) return lines
        }
        return emptyList()
    }

    private fun Song.lyricIdentityCandidates(): List<Song> =
        buildList {
            fun addCandidate(sourceId: String, songId: String?) {
                val normalized = normalizeBuiltInPlatformId(sourceId)
                val id = songId?.takeIf { it.isNotBlank() } ?: return
                if (registry.findById(normalized) == null) return
                add(copy(id = id, sourceId = normalized))
            }
            addCandidate(sourceId, id)
            canonicalSourceId?.let { addCandidate(it, canonicalSongId) }
            providerIds.forEach { (key, value) -> addCandidate(key, value) }
            neteaseIdentityId()?.let { addCandidate(SourcePlatformIds.NETEASE, it) }
        }.distinctBy { "${it.sourceId}:${it.id}" }

    private fun lyricFallbackSourceIds(currentSourceId: String): List<String> =
        listOf(
            SourcePlatformIds.NETEASE,
            SourcePlatformIds.KUWO,
            SourcePlatformIds.KUGOU,
            SourcePlatformIds.QQ,
            SourcePlatformIds.MIGU
        )
            .map(::normalizeBuiltInPlatformId)
            .filter { it != normalizeBuiltInPlatformId(currentSourceId) }
            .filter { registry.findById(it) != null }

    private fun Song.commentSourceId(enabledIds: Set<String>): String? {
        val enabled = enabledIds.map(::normalizeBuiltInPlatformId).toSet()
        val candidates = buildList {
            add(sourceId)
            canonicalSourceId?.let(::add)
            playbackSourceId?.let(::add)
            providerIds.keys.forEach(::add)
            if (!neteaseIdentityId().isNullOrBlank()) add(SourcePlatformIds.NETEASE)
        }.map(::normalizeBuiltInPlatformId)
        return candidates.firstOrNull { it in enabled && registry.findById(it) is SongCommentSource }
    }

    private fun Song.withCommentSourceIdentity(sourceId: String): Song? {
        val normalized = normalizeBuiltInPlatformId(sourceId)
        val songId = when (normalized) {
            SourcePlatformIds.NETEASE -> neteaseIdentityId()
            this.sourceId -> id
            canonicalSourceId -> canonicalSongId
            else -> providerIds[normalized] ?: providerIds[normalized.toLxKey()]
        }?.takeIf { it.isNotBlank() } ?: return null
        return copy(id = songId, sourceId = normalized)
    }

    private fun String.toLxKey(): String = when (normalizeBuiltInPlatformId(this)) {
        SourcePlatformIds.NETEASE -> "wy"
        SourcePlatformIds.KUWO -> "kw"
        SourcePlatformIds.KUGOU -> "kg"
        SourcePlatformIds.QQ -> "tx"
        SourcePlatformIds.MIGU -> "mg"
        else -> this
    }

    private fun Song.withMatchedNeteaseMetadata(match: Song): Song =
        copy(
            coverUrl = if (coverUrl.shouldUseMatchedCover()) match.coverUrl else coverUrl,
            album = if (album.isBlank() || album == "未知专辑") match.album else album,
            durationMs = if (durationMs <= 0L) match.durationMs else durationMs
        )

    private fun String.shouldUseMatchedCover(): Boolean =
        isBlank() ||
            contains("images.unsplash.com/photo-1516280440614-37939bbacd81", ignoreCase = true)

    private suspend fun resolveDirectStream(song: Song): String? {
        val source = registry.findById(song.sourceId) ?: return null
        return runCatching { source.streamUrl(song) }
            .getOrNull()
            ?.takeIf { it.isPlayableStreamUrl() }
    }

    private suspend fun resolveByPlatformMatch(
        original: Song,
        candidateSourceId: String
    ): Pair<Song, String>? {
        val source = registry.findById(candidateSourceId) ?: return null
        val query = "${original.title} ${original.artist}".trim()
        val candidates = runCatching { source.search(query).take(12) }.getOrDefault(emptyList())
        val best = candidates
            .map { candidate -> candidate to scoreSongMatch(original, candidate) }
            .filter { (_, score) -> score >= 70 }
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: return null
        val url = runCatching { source.streamUrl(best) }
            .getOrNull()
            ?.takeIf { it.isPlayableStreamUrl() }
            ?: return null
        return original.withPlaybackSource(best.sourceId, best.id) to url
    }

    private fun scoreSongMatch(original: Song, candidate: Song): Int {
        val originalTitle = original.title.normalizedMusicText()
        val candidateTitle = candidate.title.normalizedMusicText()
        val originalArtist = original.artist.normalizedMusicText()
        val candidateArtist = candidate.artist.normalizedMusicText()
        val originalAlbum = original.album.normalizedMusicText()
        val candidateAlbum = candidate.album.normalizedMusicText()

        var score = 0
        if (originalTitle == candidateTitle) {
            score += 50
        } else if (originalTitle.contains(candidateTitle) || candidateTitle.contains(originalTitle)) {
            score += 32
        }

        if (originalArtist.isNotBlank() && candidateArtist.isNotBlank()) {
            val originalArtists = originalArtist.splitArtistTokens()
            val candidateArtists = candidateArtist.splitArtistTokens()
            if (originalArtists.any { it in candidateArtists }) {
                score += 28
            } else if (originalArtists.any { token -> candidateArtists.any { it.contains(token) || token.contains(it) } }) {
                score += 18
            }
        }

        if (originalAlbum.isNotBlank() && candidateAlbum.isNotBlank() && originalAlbum == candidateAlbum) {
            score += 8
        }

        if (original.durationMs > 0L && candidate.durationMs > 0L) {
            val diffSeconds = abs(original.durationMs - candidate.durationMs) / 1000L
            score += when {
                diffSeconds <= 3L -> 18
                diffSeconds <= 8L -> 10
                diffSeconds <= 15L -> 2
                else -> -24
            }
        }

        score += coverVersionPenalty(candidate)
        return score.coerceIn(0, 100)
    }

    private fun rankSongsForQuery(query: String, songs: List<Song>): List<Song> {
        if (songs.size <= 1) return songs
        return songs
            .mapIndexed { index, song -> Triple(song, searchRelevanceScore(query, song), index) }
            .sortedWith(
                compareByDescending<Triple<Song, Int, Int>> { it.second }
                    .thenBy { it.third }
            )
            .map { it.first }
    }

    private fun searchRelevanceScore(query: String, song: Song): Int {
        val rawQuery = query.trim()
        if (rawQuery.isBlank()) return coverVersionPenalty(song)

        val q = rawQuery.normalizedMusicText()
        val title = song.title.normalizedMusicText()
        val artist = song.artist.normalizedMusicText()
        val album = song.album.normalizedMusicText()
        val queryTokens = rawQuery.lowercase()
            .split(Regex("[\\s/&、,，;；]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .map { it.normalizedMusicText() }
            .filter { it.isNotBlank() }
            .distinct()

        var score = 0
        when {
            title == q -> score += 100
            title.startsWith(q) || q.startsWith(title) -> score += 82
            title.contains(q) || q.contains(title) -> score += 60
            queryTokens.any { it.isNotBlank() && title.contains(it) } -> score += 36
        }

        if (artist.isNotBlank()) {
            when {
                artist == q || q.contains(artist) -> score += 48
                artist.contains(q) -> score += 28
                queryTokens.any { token ->
                    token.isNotBlank() && (
                        artist == token ||
                            artist.contains(token) ||
                            token.contains(artist)
                        )
                } -> score += 40
            }
        }

        // Prefer "title + original artist" style matches over loose partial hits.
        if (title.isNotBlank() && artist.isNotBlank()) {
            val titleHit = title == q || q.contains(title) || title.contains(q)
            val artistHit = queryTokens.any { token ->
                artist == token || artist.contains(token) || token.contains(artist)
            } || q.contains(artist)
            if (titleHit && artistHit) score += 55
        }

        if (album.isNotBlank() && (album == q || q.contains(album) || album.contains(q))) {
            score += 8
        }

        score += coverVersionPenalty(song)
        score += when (song.sourceId) {
            SourcePlatformIds.NETEASE -> 4
            SourcePlatformIds.QQ -> 3
            SourcePlatformIds.KUGOU -> 2
            SourcePlatformIds.KUWO -> 2
            SourcePlatformIds.MIGU -> 1
            else -> 0
        }
        return score
    }

    private fun coverVersionPenalty(song: Song): Int {
        val blob = listOf(song.title, song.artist, song.album)
            .joinToString(" ")
            .lowercase()
        var penalty = 0
        if (Regex("翻唱|cover|翻自|翻版|翻录").containsMatchIn(blob)) penalty -= 45
        if (Regex("伴奏|instrumental|karaoke|消音|off vocal").containsMatchIn(blob)) penalty -= 40
        if (Regex("\\blive\\b|现场|演唱会|音乐会").containsMatchIn(blob)) penalty -= 28
        if (Regex("remix|混音|dj|夜店|加速|剪辑|片段").containsMatchIn(blob)) penalty -= 22
        if (Regex("纯音乐|钢琴版|吉他版|小提琴").containsMatchIn(blob) &&
            !song.title.normalizedMusicText().contains("纯音乐")
        ) {
            penalty -= 12
        }
        return penalty
    }

    private fun String.isPlayableStreamUrl(): Boolean {
        if (isBlank()) return false
        if (startsWith("file:", ignoreCase = true) || startsWith("content:", ignoreCase = true)) {
            return true
        }
        return (startsWith("http://") || startsWith("https://")) &&
            !contains("music.163.com/song/media/outer/url", ignoreCase = true)
    }

    private fun String.normalizedMusicText(): String =
        lowercase()
            .replace(Regex("\\s+"), "")
            .replace("（", "(")
            .replace("）", ")")
            .replace(Regex("[\\[\\]【】《》〈〉'\"“”‘’,，.。!！?？:：;；_\\-]"), "")
            .replace(Regex("\\((live|伴奏|纯音乐|片段|cover|翻自|完整版|新版|旧版|remix).*?\\)"), "")
            .trim()

    private fun String.splitArtistTokens(): Set<String> =
        split("/", "&", "、", ",", "，", ";", "；", "和", "feat", "ft")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    private companion object {
        const val SINGLE_SOURCE_SEARCH_TIMEOUT_MS = 3_800L
        const val MULTI_SOURCE_SEARCH_TIMEOUT_MS = 2_800L
        const val COLLECTION_SEARCH_TIMEOUT_MS = 2_600L
        const val COLLECTION_DETAIL_TIMEOUT_MS = 15_000L
        const val LYRIC_MATCH_SEARCH_TIMEOUT_MS = 3_000L
        const val LYRIC_MATCH_MIN_SCORE = 74
        val collectionSourceIds = setOf(
            SourcePlatformIds.NETEASE,
            SourcePlatformIds.KUWO,
            SourcePlatformIds.KUGOU,
            SourcePlatformIds.QQ,
            SourcePlatformIds.MIGU
        )
        val PLATFORM_FALLBACK_ORDER = listOf(
            SourcePlatformIds.NETEASE,
            SourcePlatformIds.QQ,
            SourcePlatformIds.KUGOU,
            SourcePlatformIds.KUWO,
            SourcePlatformIds.MIGU
        )
    }
}
