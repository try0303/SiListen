package com.silisten.app.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.silisten.app.PlayerSheetPanel
import com.silisten.app.R
import com.silisten.app.data.model.Song
import com.silisten.app.data.source.MusicSourceRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class PlaybackState(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val currentSong: Song? = null,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val errorMessage: String? = null
)

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
                    if (action == PlaybackNotificationController.ACTION_LIKE) {
                        PlaybackNotificationBridge.like()
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

    var state by mutableStateOf(PlaybackState())
        private set

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
                    val index = player.currentMediaItemIndex.coerceAtLeast(0)
                    state = state.copy(
                        currentIndex = index,
                        currentSong = state.queue.getOrNull(index),
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
        if (songs.isEmpty()) return
        val session = ++playSession
        val safeStartIndex = startIndex.coerceIn(0, songs.lastIndex)
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
            positionMs = 0L,
            bufferedMs = 0L,
            errorMessage = null
        )
        notificationDismissedByUser = false

        val firstPlayable = orderedSongs.firstNotNullOfOrNull { song -> song.toPlayable(registry) }
            ?: run {
                state = state.copy(
                    isPreparing = false,
                    errorMessage = "没有拿到可播放地址，请换一首或稍后重试"
                )
                return
            }
        if (session != playSession) return
        state = state.copy(
            queue = orderedSongs,
            currentIndex = 0,
            currentSong = firstPlayable.first,
            isPreparing = false,
            errorMessage = null
        )
        player.setMediaItem(firstPlayable.second)
        player.prepare()
        player.play()
        syncNotification()

        val remainingPlayable = coroutineScope {
            orderedSongs.drop(1)
                .take(24)
                .map { song -> async { song.toPlayable(registry) } }
                .awaitAll()
                .filterNotNull()
        }
        if (session != playSession) return
        if (remainingPlayable.isEmpty()) return
        val playable = listOf(firstPlayable) + remainingPlayable
        val playableSongs = playable.map { it.first }
        val remainingItems = remainingPlayable.map { it.second }
        state = state.copy(queue = playableSongs, currentIndex = 0, currentSong = playableSongs.first())
        player.addMediaItems(remainingItems)
    }

    private suspend fun Song.toPlayable(registry: MusicSourceRegistry): Pair<Song, MediaItem>? {
        val streamUrl = streamHint?.takeIf { it.isNotBlank() }
            ?: runCatching { registry.byId(sourceId).streamUrl(this) }.getOrNull()
            ?: return null
        if (streamUrl.isBlank()) return null
        return this to MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                    .build()
            )
            .build()
    }

    suspend fun queueNext(song: Song, registry: MusicSourceRegistry): Boolean {
        if (state.currentSong == null || player.mediaItemCount == 0) {
            playQueue(listOf(song), 0, registry)
            return state.currentSong?.id == song.id
        }
        val playable = song.toPlayable(registry) ?: return false
        val insertIndex = (player.currentMediaItemIndex + 1)
            .coerceIn(0, state.queue.size)
        val nextQueue = state.queue.toMutableList().apply {
            add(insertIndex, playable.first)
        }
        state = state.copy(queue = nextQueue, errorMessage = null)
        player.addMediaItem(insertIndex, playable.second)
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

    fun next() {
        notificationDismissedByUser = false
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
        updateProgress()
    }

    fun previous() {
        notificationDismissedByUser = false
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0L)
        updateProgress()
    }

    fun playAt(index: Int) {
        val targetIndex = index.coerceIn(0, state.queue.lastIndex)
        if (state.queue.isEmpty()) return
        notificationDismissedByUser = false
        player.seekTo(targetIndex, 0L)
        player.play()
        state = state.copy(
            currentIndex = targetIndex,
            currentSong = state.queue.getOrNull(targetIndex),
            isPreparing = false,
            isPlaying = true,
            errorMessage = null
        )
        updateProgress()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        updateProgress()
    }

    fun updateProgress() {
        state = state.copy(
            durationMs = player.duration.takeIf { it > 0 } ?: state.currentSong?.durationMs ?: 0L,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
            isPlaying = player.isPlaying,
            isPreparing = if (player.isPlaying) false else state.isPreparing
        )
        syncNotification()
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

    fun release() {
        PlaybackNotificationBridge.detach(this)
        notificationController.cancel()
        PlaybackService.stop()
        systemMediaSession.release()
        player.release()
    }

    private fun syncNotification() {
        if (state.currentSong == null) {
            updateSystemMediaSession(isLiked = false)
            notificationController.cancel()
            PlaybackService.stop()
        } else if (notificationDismissedByUser && !state.isPlaying) {
            updateSystemMediaSession(isLiked = false)
            notificationController.cancel()
            PlaybackService.stop()
        } else {
            if (state.isPlaying) {
                notificationDismissedByUser = false
            }
            val isLiked = state.currentSong?.let(PlaybackNotificationBridge::isLiked) == true
            updateSystemMediaSession(isLiked)
            val notification = notificationController.show(
                playbackState = state,
                mediaSessionToken = systemMediaSession.sessionToken,
                isCurrentSongLiked = isLiked,
                contentTextOverride = if (notificationLyricEnabled) notificationLyricText else null
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

    private fun updateSystemMediaSession(isLiked: Boolean) {
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
        systemMediaSession.setMetadata(
            PlatformMediaMetadata.Builder()
                .putString(PlatformMediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(PlatformMediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(PlatformMediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(
                    PlatformMediaMetadata.METADATA_KEY_DURATION,
                    state.durationMs.takeIf { it > 0L } ?: song.durationMs
                )
                .build()
        )

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
                .build()
        )
    }

    private fun playerContentIntent(): PendingIntent {
        val intent = Intent(appContext, com.silisten.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_player", true)
            putExtra("player_panel", PlayerSheetPanel.Lyrics.name)
        }
        return PendingIntent.getActivity(
            appContext,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}
