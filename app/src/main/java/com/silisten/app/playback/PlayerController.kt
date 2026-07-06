package com.silisten.app.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata as PlatformMediaMetadata
import android.media.session.MediaSession as PlatformMediaSession
import android.media.session.PlaybackState as PlatformPlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.silisten.app.PlayerSheetPanel
import com.silisten.app.R
import com.silisten.app.data.model.Song
import com.silisten.app.data.source.MusicSourceRegistry
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class PlaybackMode(
    val label: String,
    val description: String
) {
    Order("顺序播放", "按当前列表顺序播放，播到末尾后从头继续"),
    RepeatOne("单曲循环", "一直重复当前歌曲"),
    Shuffle("随机播放", "从当前队列里随机播放歌曲"),
    StopAtEnd("禁用歌曲切换", "当前歌曲播完后暂停，不自动进入下一首")
}

data class PlaybackState(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val currentSong: Song? = null,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.Order,
    val errorMessage: String? = null
)

typealias PlaybackStreamResolver = suspend (Song) -> Pair<Song, String>?

class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 SiListen/0.3 Android")
        .setDefaultRequestProperties(
            mapOf(
                "Referer" to "https://music.163.com/",
                "Origin" to "https://music.163.com"
            )
        )
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(5_000)
        .setReadTimeoutMs(15_000)
    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
        )
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    12_000,
                    48_000,
                    900,
                    1_500
                )
                .build()
        )
        .build()
    private val systemMediaSession = PlatformMediaSession(appContext, "SiListen").apply {
        setSessionActivity(playerContentIntent())
        setCallback(
            object : PlatformMediaSession.Callback() {
                override fun onPlay() {
                    notificationDismissedByUser = false
                    player.play()
                    state = state.copy(isPlaying = player.isPlaying)
                    syncNotification()
                }

                override fun onPause() {
                    player.pause()
                    state = state.copy(isPlaying = false, isPreparing = false)
                    syncNotification()
                }

                override fun onStop() {
                    dismissNotification()
                }

                override fun onSkipToPrevious() {
                    previous()
                }

                override fun onSkipToNext() {
                    next()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        PlaybackNotificationController.ACTION_LIKE -> PlaybackNotificationBridge.like()
                        PlaybackNotificationController.ACTION_DESKTOP_LYRIC -> PlaybackNotificationBridge.toggleDesktopLyric(appContext)
                    }
                }
            },
            Handler(Looper.getMainLooper())
        )
    }
    private val notificationController = PlaybackNotificationController(appContext)
    private var playSession = 0
    private var notificationDismissedByUser = false
    private var notificationLyricEnabled = false
    private var notificationLyricText: String? = null
    private var stopAfterCurrentSongForTimer = false
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val artworkImageLoader = ImageLoader(appContext)
    private var notificationArtworkJob: Job? = null
    private var notificationArtworkKey: String? = null
    private var notificationArtworkBitmap: Bitmap? = null
    private var lastNotificationSignature: NotificationSignature? = null
    private var restoredWithoutMedia = false
    private var playableQueueIndexes: List<Int> = emptyList()
    private var playableQueueSongs: List<Song> = emptyList()
    private var activeResolver: PlaybackStreamResolver? = null
    private var queuePrefetchJob: Job? = null
    private var priorityResolveJob: Job? = null

    private data class NotificationSignature(
        val songKey: String,
        val isPlaying: Boolean,
        val isPreparing: Boolean,
        val isLiked: Boolean,
        val desktopLyricEnabled: Boolean,
        val contentTextOverride: String?,
        val artworkKey: String?
    )

    var state by mutableStateOf(PlaybackState())
        private set

    fun hasRestoredStateWithoutMedia(): Boolean =
        restoredWithoutMedia && state.currentSong != null && state.queue.isNotEmpty() && player.mediaItemCount == 0

    fun restoreSnapshot(
        queue: List<Song>,
        currentIndex: Int,
        positionMs: Long,
        durationMs: Long,
        playbackMode: PlaybackMode
    ) {
        if (queue.isEmpty() || state.currentSong != null || player.mediaItemCount > 0) return
        val safeIndex = currentIndex.coerceIn(0, queue.lastIndex)
        val currentSong = queue[safeIndex]
        playSession++
        restoredWithoutMedia = true
        playableQueueIndexes = emptyList()
        playableQueueSongs = emptyList()
        activeResolver = null
        queuePrefetchJob?.cancel()
        priorityResolveJob?.cancel()
        notificationDismissedByUser = true
        state = PlaybackState(
            queue = queue,
            currentIndex = safeIndex,
            currentSong = currentSong,
            isPreparing = false,
            isPlaying = false,
            durationMs = durationMs.takeIf { it > 0L } ?: currentSong.durationMs,
            positionMs = positionMs.coerceAtLeast(0L),
            bufferedMs = positionMs.coerceAtLeast(0L),
            playbackMode = playbackMode,
            errorMessage = null
        )
        applyPlaybackMode()
    }

    fun selectRestoredQueueIndex(index: Int) {
        if (!hasRestoredStateWithoutMedia()) return
        val safeIndex = index.coerceIn(0, state.queue.lastIndex)
        val song = state.queue[safeIndex]
        state = state.copy(
            currentIndex = safeIndex,
            currentSong = song,
            durationMs = song.durationMs,
            positionMs = 0L,
            bufferedMs = 0L,
            errorMessage = null
        )
    }

    init {
        PlaybackNotificationBridge.attach(this)
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    state = state.copy(
                        isPlaying = isPlaying,
                        isPreparing = if (isPlaying) false else state.isPreparing
                    )
                    syncNotification()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    updateProgress()
                }

                override fun onPlayerError(error: PlaybackException) {
                    state = state.copy(
                        isPlaying = false,
                        errorMessage = "播放失败，已尝试切换播放地址，请换一首或稍后重试"
                    )
                    syncNotification()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val mediaIndex = player.currentMediaItemIndex
                    val index = player.currentQueueIndex()
                    state = state.copy(
                        currentIndex = index,
                        currentSong = playableQueueSongs.getOrNull(mediaIndex)
                            ?: state.queue.getOrNull(index),
                        isPreparing = false
                    )
                    updateProgress()
                }
            }
        )
    }

    suspend fun playQueue(
        songs: List<Song>,
        startIndex: Int,
        registry: MusicSourceRegistry
    ) {
        playQueue(
            songs = songs,
            startIndex = startIndex,
            resolver = { song -> song.resolveWithRegistry(registry) }
        )
    }

    suspend fun playQueue(
        songs: List<Song>,
        startIndex: Int,
        resolver: PlaybackStreamResolver,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true
    ) {
        if (songs.isEmpty()) return
        val session = ++playSession
        restoredWithoutMedia = false
        playableQueueIndexes = emptyList()
        playableQueueSongs = emptyList()
        activeResolver = resolver
        queuePrefetchJob?.cancel()
        priorityResolveJob?.cancel()
        val safeStartIndex = startIndex.coerceIn(0, songs.lastIndex)
        val safeStartPositionMs = startPositionMs.coerceAtLeast(0L)
        val orderedSongs = buildList {
            add(songs[safeStartIndex])
            addAll(songs.filterIndexed { index, _ -> index != safeStartIndex })
        }
        state = state.copy(
            queue = orderedSongs,
            currentIndex = 0,
            currentSong = orderedSongs.first(),
            isPreparing = true,
            isPlaying = false,
            durationMs = orderedSongs.first().durationMs,
            positionMs = safeStartPositionMs,
            bufferedMs = safeStartPositionMs,
            errorMessage = null
        )
        notificationDismissedByUser = false
        player.stop()
        player.clearMediaItems()

        val firstPlayable = orderedSongs.withIndex().firstNotNullOfOrNull { (index, song) ->
            song.toPlayable(resolver)?.let { index to it }
        }
            ?: run {
                state = state.copy(
                    isPreparing = false,
                    errorMessage = "没有拿到可播放地址，请换一首或稍后重试"
                )
                return
            }
        val firstPlayableIndex = firstPlayable.first
        val firstPlayableSong = firstPlayable.second.first
        val firstPlayableItem = firstPlayable.second.second
        if (session != playSession) return
        state = state.copy(
            queue = orderedSongs,
            currentIndex = firstPlayableIndex,
            currentSong = firstPlayableSong,
            isPreparing = false,
            errorMessage = null
        )
        playableQueueIndexes = listOf(firstPlayableIndex)
        playableQueueSongs = listOf(firstPlayableSong)
        player.setMediaItem(firstPlayableItem)
        applyPlaybackMode()
        player.prepare()
        if (safeStartPositionMs > 0L) {
            player.seekTo(safeStartPositionMs)
        }
        if (autoPlay) {
            player.play()
        }
        syncNotification()

        queuePrefetchJob = notificationScope.launch {
            for ((index, song) in orderedSongs.withIndex()) {
                if (index == firstPlayableIndex) continue
                if (session != playSession) return@launch
                if (playableQueueIndexes.contains(index)) continue
                val playable = song.toPlayable(resolver) ?: continue
                if (session != playSession) return@launch
                appendPlayableItem(index, playable, keepPlaybackOrder = true)
            }
        }
    }

    private suspend fun Song.toPlayable(resolver: PlaybackStreamResolver): Pair<Song, MediaItem>? {
        val (resolvedSong, streamUrl) = resolver(this) ?: return null
        if (streamUrl.isBlank()) return null
        return resolvedSong to MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(resolvedSong.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(resolvedSong.title)
                    .setArtist(resolvedSong.artist)
                    .setAlbumTitle(resolvedSong.album)
                    .setArtworkUri(resolvedSong.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                    .build()
            )
            .build()
    }

    private suspend fun Song.resolveWithRegistry(registry: MusicSourceRegistry): Pair<Song, String>? {
        val streamUrl = streamHint?.takeIf { it.isNotBlank() }
            ?: runCatching { registry.byId(sourceId).streamUrl(this) }.getOrNull()
            ?: return null
        if (streamUrl.isBlank()) return null
        return this to streamUrl
    }

    suspend fun queueNext(song: Song, registry: MusicSourceRegistry): Boolean {
        return queueNext(song) { item -> item.resolveWithRegistry(registry) }
    }

    suspend fun queueNext(song: Song, resolver: PlaybackStreamResolver): Boolean {
        if (state.currentSong == null || player.mediaItemCount == 0) {
            playQueue(listOf(song), 0, resolver)
            return state.currentSong?.id == song.id
        }
        val playable = song.toPlayable(resolver) ?: return false
        val mediaInsertIndex = (player.currentMediaItemIndex + 1)
            .coerceIn(0, player.mediaItemCount)
        val queueInsertIndex = (state.currentIndex + 1)
            .coerceIn(0, state.queue.size)
        val nextQueue = state.queue.toMutableList().apply {
            add(queueInsertIndex, playable.first)
        }
        state = state.copy(queue = nextQueue, errorMessage = null)
        playableQueueIndexes = playableQueueIndexes
            .map { if (it >= queueInsertIndex) it + 1 else it }
            .toMutableList()
            .apply { add(mediaInsertIndex, queueInsertIndex) }
        playableQueueSongs = playableQueueSongs
            .toMutableList()
            .apply { add(mediaInsertIndex, playable.first) }
        player.addMediaItem(mediaInsertIndex, playable.second)
        syncNotification()
        return true
    }

    fun toggle() {
        if (player.isPlaying) {
            player.pause()
        } else {
            notificationDismissedByUser = false
            player.play()
        }
        state = state.copy(isPlaying = player.isPlaying)
        syncNotification()
    }

    fun pause() {
        player.pause()
        state = state.copy(isPlaying = false, isPreparing = false)
        syncNotification()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        if (state.playbackMode == mode) return
        state = state.copy(playbackMode = mode)
        applyPlaybackMode()
        syncNotification()
    }

    fun setStopAfterCurrentSongForTimer(enabled: Boolean) {
        if (stopAfterCurrentSongForTimer == enabled) return
        stopAfterCurrentSongForTimer = enabled
        applyPlaybackMode()
    }

    fun next() {
        notificationDismissedByUser = false
        if (state.playbackMode == PlaybackMode.Shuffle) {
            randomNextIndex()?.let { targetIndex ->
                seekToQueueIndex(targetIndex, startPlayback = player.isPlaying)
                return
            }
        }
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            nextUnresolvedQueueIndex(direction = 1)?.let { targetIndex ->
                seekToQueueIndex(targetIndex, startPlayback = player.isPlaying)
            }
        }
        updateProgress()
    }

    fun previous() {
        notificationDismissedByUser = false
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            nextUnresolvedQueueIndex(direction = -1)?.let { targetIndex ->
                seekToQueueIndex(targetIndex, startPlayback = player.isPlaying)
                updateProgress()
                return
            }
            player.seekTo(0L)
        }
        updateProgress()
    }

    fun playAt(index: Int) {
        notificationDismissedByUser = false
        seekToQueueIndex(index, startPlayback = true)
    }

    fun seekTo(positionMs: Long) {
        if (restoredWithoutMedia && player.mediaItemCount == 0) {
            state = state.copy(positionMs = positionMs.coerceAtLeast(0L), bufferedMs = positionMs.coerceAtLeast(0L))
            return
        }
        player.seekTo(positionMs)
        updateProgress()
        syncNotification(force = true)
    }

    fun updateProgress() {
        if (restoredWithoutMedia && player.mediaItemCount == 0) {
            val nextState = state.copy(isPlaying = false, isPreparing = false)
            if (nextState != state) {
                state = nextState
            }
            return
        }
        val previousState = state
        val nextState = state.copy(
            durationMs = player.duration.takeIf { it > 0 } ?: state.currentSong?.durationMs ?: 0L,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
            isPlaying = player.isPlaying,
            isPreparing = if (player.isPlaying) false else state.isPreparing
        )
        if (nextState != previousState) {
            state = nextState
        }
        if (nextState.hasNotificationMeaningfulChangeFrom(previousState)) {
            syncNotification()
        }
    }

    fun dismissNotification() {
        notificationDismissedByUser = true
        if (player.isPlaying) {
            player.pause()
        }
        state = state.copy(
            isPlaying = false,
            isPreparing = false
        )
        updateSystemMediaSession(isLiked = false)
        notificationController.cancel()
        PlaybackService.stop()
    }

    fun setNotificationLyric(enabled: Boolean, lyricText: String?) {
        val normalizedText = lyricText?.trim()?.takeIf { it.isNotBlank() }
        if (notificationLyricEnabled == enabled && notificationLyricText == normalizedText) return
        notificationLyricEnabled = enabled
        notificationLyricText = normalizedText
        syncNotification()
    }

    fun refreshNotification() {
        syncNotification(force = true)
    }

    fun release() {
        PlaybackNotificationBridge.detach(this)
        queuePrefetchJob?.cancel()
        priorityResolveJob?.cancel()
        notificationArtworkJob?.cancel()
        notificationScope.cancel()
        notificationController.cancel()
        PlaybackService.stop()
        systemMediaSession.release()
        player.release()
    }

    private fun syncNotification(force: Boolean = false) {
        if (state.currentSong == null) {
            lastNotificationSignature = null
            updateSystemMediaSession(isLiked = false)
            notificationController.cancel()
            PlaybackService.stop()
        } else if (notificationDismissedByUser && !state.isPlaying) {
            lastNotificationSignature = null
            updateSystemMediaSession(isLiked = false)
            notificationController.cancel()
            PlaybackService.stop()
        } else {
            if (state.isPlaying) {
                notificationDismissedByUser = false
            }
            val song = state.currentSong
            if (song != null) {
                ensureNotificationArtwork(song)
            }
            val artwork = song?.notificationArtworkKey()
                ?.takeIf { it == notificationArtworkKey }
                ?.let { notificationArtworkBitmap }
            val isLiked = song?.let(PlaybackNotificationBridge::isLiked) == true
            val desktopLyricEnabled = isDesktopLyricEnabled()
            val contentTextOverride = if (notificationLyricEnabled) notificationLyricText else null
            updateSystemMediaSession(isLiked, artwork)
            val signature = song?.let {
                NotificationSignature(
                    songKey = it.notificationContentKey(),
                    isPlaying = state.isPlaying,
                    isPreparing = state.isPreparing,
                    isLiked = isLiked,
                    desktopLyricEnabled = desktopLyricEnabled,
                    contentTextOverride = contentTextOverride,
                    artworkKey = artwork?.let { notificationArtworkKey }
                )
            }
            if (!force && signature == lastNotificationSignature) return
            lastNotificationSignature = signature
            val notification = notificationController.show(
                playbackState = state,
                mediaSessionToken = systemMediaSession.sessionToken,
                isCurrentSongLiked = isLiked,
                desktopLyricEnabled = desktopLyricEnabled,
                artwork = artwork,
                contentTextOverride = contentTextOverride
            )
            if (notification != null) {
                PlaybackService.update(
                    context = appContext,
                    notification = notification,
                    foreground = state.isPlaying || state.isPreparing
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun applyPlaybackMode() {
        player.repeatMode = when (state.playbackMode) {
            PlaybackMode.RepeatOne -> Player.REPEAT_MODE_ONE
            PlaybackMode.Shuffle -> Player.REPEAT_MODE_ALL
            PlaybackMode.Order -> Player.REPEAT_MODE_ALL
            PlaybackMode.StopAtEnd -> Player.REPEAT_MODE_OFF
        }
        player.shuffleModeEnabled = state.playbackMode == PlaybackMode.Shuffle
        player.setPauseAtEndOfMediaItems(
            state.playbackMode == PlaybackMode.StopAtEnd || stopAfterCurrentSongForTimer
        )
    }

    private fun randomNextIndex(): Int? {
        val playableCount = minOf(player.mediaItemCount, playableQueueIndexes.size)
        if (playableCount <= 1) return null
        val currentMediaIndex = player.currentMediaItemIndex
            .takeIf { it in 0 until playableCount }
            ?: playableQueueIndexes.indexOf(state.currentIndex).takeIf { it >= 0 }
            ?: 0
        var targetIndex = Random.nextInt(playableCount - 1)
        if (targetIndex >= currentMediaIndex) targetIndex += 1
        return playableQueueIndexes.getOrNull(targetIndex)
    }

    private fun seekToQueueIndex(index: Int, startPlayback: Boolean) {
        if (state.queue.isEmpty() || player.mediaItemCount == 0) return
        val targetIndex = index.coerceIn(0, state.queue.lastIndex)
        val mediaIndex = playableQueueIndexes.indexOf(targetIndex)
        if (mediaIndex < 0) {
            resolveAndSeekToQueueIndex(targetIndex, startPlayback)
            return
        }
        seekToPlayableMediaIndex(targetIndex, mediaIndex, startPlayback)
    }

    private fun resolveAndSeekToQueueIndex(targetIndex: Int, startPlayback: Boolean) {
        val resolver = activeResolver
        val targetSong = state.queue.getOrNull(targetIndex)
        if (resolver == null || targetSong == null) {
            state = state.copy(
                currentIndex = targetIndex,
                currentSong = targetSong,
                isPreparing = false,
                errorMessage = "\u5f53\u524d\u6b4c\u66f2\u6682\u65f6\u65e0\u6cd5\u64ad\u653e"
            )
            syncNotification()
            return
        }
        priorityResolveJob?.cancel()
        state = state.copy(
            currentIndex = targetIndex,
            currentSong = targetSong,
            isPreparing = true,
            errorMessage = null
        )
        syncNotification(force = true)
        priorityResolveJob = notificationScope.launch {
            val session = playSession
            val playable = targetSong.toPlayable(resolver)
            if (session != playSession) return@launch
            if (playable == null) {
                state = state.copy(
                    currentIndex = targetIndex,
                    currentSong = targetSong,
                    isPreparing = false,
                    isPlaying = false,
                    errorMessage = "\u5f53\u524d\u6b4c\u66f2\u6682\u65f6\u65e0\u6cd5\u64ad\u653e"
                )
                syncNotification(force = true)
                return@launch
            }
            val mediaIndex = appendPlayableItem(targetIndex, playable, keepPlaybackOrder = true)
            seekToPlayableMediaIndex(targetIndex, mediaIndex, startPlayback)
        }
    }

    private fun seekToPlayableMediaIndex(targetIndex: Int, mediaIndex: Int, startPlayback: Boolean) {
        player.seekTo(mediaIndex, 0L)
        if (startPlayback) {
            player.play()
        }
        state = state.copy(
            currentIndex = targetIndex,
            currentSong = playableQueueSongs.getOrNull(mediaIndex)
                ?: state.queue.getOrNull(targetIndex),
            isPreparing = false,
            isPlaying = if (startPlayback) true else player.isPlaying,
            errorMessage = null
        )
        updateProgress()
    }

    private fun appendPlayableItem(
        queueIndex: Int,
        playable: Pair<Song, MediaItem>,
        keepPlaybackOrder: Boolean = false
    ): Int {
        val existingMediaIndex = playableQueueIndexes.indexOf(queueIndex)
        if (existingMediaIndex >= 0) return existingMediaIndex
        val insertIndex = if (keepPlaybackOrder) {
            playableQueueIndexes.size
        } else {
            playableQueueIndexes.indexOfFirst { it > queueIndex }
                .takeIf { it >= 0 }
                ?: playableQueueIndexes.size
        }
        playableQueueIndexes = playableQueueIndexes.toMutableList().apply { add(insertIndex, queueIndex) }
        playableQueueSongs = playableQueueSongs.toMutableList().apply { add(insertIndex, playable.first) }
        player.addMediaItem(insertIndex, playable.second)
        applyPlaybackMode()
        return insertIndex
    }

    private fun nextUnresolvedQueueIndex(direction: Int): Int? {
        if (state.queue.isEmpty()) return null
        var index = state.currentIndex + direction
        while (index in state.queue.indices) {
            if (!playableQueueIndexes.contains(index)) return index
            index += direction
        }
        if (state.playbackMode != PlaybackMode.Order) return null
        index = if (direction > 0) 0 else state.queue.lastIndex
        while (index in state.queue.indices && index != state.currentIndex) {
            if (!playableQueueIndexes.contains(index)) return index
            index += direction
        }
        return null
    }

    private fun Player.currentQueueIndex(): Int {
        val mediaIndex = currentMediaItemIndex
        return playableQueueIndexes.getOrNull(mediaIndex)
            ?: state.currentIndex.coerceIn(0, state.queue.lastIndex.coerceAtLeast(0))
    }

    private fun ensureNotificationArtwork(song: Song) {
        val key = song.notificationArtworkKey()
        if (notificationArtworkKey == key) return
        notificationArtworkKey = key
        notificationArtworkBitmap = null
        notificationArtworkJob?.cancel()
        val coverUrl = song.coverUrl.takeIf { it.isNotBlank() } ?: return
        notificationArtworkJob = notificationScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val request = ImageRequest.Builder(appContext)
                        .data(coverUrl)
                        .allowHardware(false)
                        .size(512)
                        .build()
                    val result = artworkImageLoader.execute(request) as? SuccessResult
                    result?.drawable?.toBitmap(width = 512, height = 512)
                }.getOrNull()
            }
            if (notificationArtworkKey == key) {
                notificationArtworkBitmap = bitmap
                syncNotification()
            }
        }
    }

    private fun Song.notificationArtworkKey(): String = "$sourceId:$id:$coverUrl"

    private fun Song.notificationContentKey(): String = "$sourceId:$id:$title:$artist:$album:$coverUrl"

    private fun PlaybackState.hasNotificationMeaningfulChangeFrom(previous: PlaybackState): Boolean =
        currentSong?.notificationContentKey() != previous.currentSong?.notificationContentKey() ||
            isPlaying != previous.isPlaying ||
            isPreparing != previous.isPreparing ||
            playbackMode != previous.playbackMode ||
            durationMs != previous.durationMs ||
            currentIndex != previous.currentIndex

    private fun updateSystemMediaSession(isLiked: Boolean, artwork: Bitmap? = null) {
        val song = state.currentSong
        if (song == null) {
            systemMediaSession.setPlaybackState(
                PlatformPlaybackState.Builder()
                    .setState(PlatformPlaybackState.STATE_NONE, 0L, 1f)
                    .build()
            )
            systemMediaSession.isActive = false
            return
        }

        systemMediaSession.isActive = true
        val metadata = PlatformMediaMetadata.Builder()
            .putString(PlatformMediaMetadata.METADATA_KEY_TITLE, song.title)
            .putString(PlatformMediaMetadata.METADATA_KEY_ARTIST, song.artist)
            .putString(PlatformMediaMetadata.METADATA_KEY_ALBUM, song.album)
            .putLong(
                PlatformMediaMetadata.METADATA_KEY_DURATION,
                state.durationMs.takeIf { it > 0L } ?: song.durationMs
            )
            .apply {
                artwork?.let {
                    putBitmap(PlatformMediaMetadata.METADATA_KEY_ART, it)
                    putBitmap(PlatformMediaMetadata.METADATA_KEY_ALBUM_ART, it)
                }
            }
            .build()
        systemMediaSession.setMetadata(metadata)

        val platformState = when {
            state.isPreparing -> PlatformPlaybackState.STATE_BUFFERING
            state.isPlaying -> PlatformPlaybackState.STATE_PLAYING
            else -> PlatformPlaybackState.STATE_PAUSED
        }
        val actions = PlatformPlaybackState.ACTION_PLAY or
            PlatformPlaybackState.ACTION_PAUSE or
            PlatformPlaybackState.ACTION_PLAY_PAUSE or
            PlatformPlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlatformPlaybackState.ACTION_SKIP_TO_NEXT or
            PlatformPlaybackState.ACTION_SEEK_TO or
            PlatformPlaybackState.ACTION_STOP

        systemMediaSession.setPlaybackState(
            PlatformPlaybackState.Builder()
                .setState(platformState, state.positionMs.coerceAtLeast(0L), if (state.isPlaying) 1f else 0f)
                .setBufferedPosition(state.bufferedMs.coerceAtLeast(0L))
                .setActions(actions)
                .addCustomAction(
                    PlatformPlaybackState.CustomAction.Builder(
                        PlaybackNotificationController.ACTION_LIKE,
                        if (isLiked) "取消喜欢" else "喜欢",
                        if (isLiked) R.drawable.ic_favorite_24 else R.drawable.ic_favorite_border_24
                    ).build()
                )
                .addCustomAction(
                    PlatformPlaybackState.CustomAction.Builder(
                        PlaybackNotificationController.ACTION_DESKTOP_LYRIC,
                        if (isDesktopLyricEnabled()) "关闭桌面歌词" else "桌面歌词",
                        if (isDesktopLyricEnabled()) R.drawable.ic_desktop_lyric_checked_24 else R.drawable.ic_desktop_lyric_24
                    ).build()
                )
                .build()
        )
    }

    private fun isDesktopLyricEnabled(): Boolean =
        appContext.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
            .getBoolean("desktop_lyric", false)

    private fun playerContentIntent(): PendingIntent {
        val intent = Intent(appContext, com.silisten.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_player", true)
            putExtra("player_panel", PlayerSheetPanel.Detail.name)
        }
        return PendingIntent.getActivity(
            appContext,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}
