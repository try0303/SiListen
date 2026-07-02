package com.silisten.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaSession.Token as MediaSessionToken
import android.os.Build
import com.silisten.app.MainActivity
import com.silisten.app.PlayerSheetPanel
import com.silisten.app.R

internal class PlaybackNotificationController(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    fun show(
        playbackState: PlaybackState,
        mediaSessionToken: MediaSessionToken,
        isCurrentSongLiked: Boolean,
        artwork: Bitmap? = null,
        contentTextOverride: String? = null
    ): Notification? {
        val song = playbackState.currentSong ?: run {
            cancel()
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(song.title)
            .setContentText(contentTextOverride?.takeIf { it.isNotBlank() } ?: song.artist.ifBlank { "SiListen" })
            .setSubText(song.album.ifBlank { if (playbackState.isPlaying) "正在播放" else "已暂停" })
            .setContentIntent(contentIntent())
            .setDeleteIntent(actionIntent(ACTION_DISMISS))
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(playbackState.isPlaying)
            .setLargeIcon(artwork)
            .addAction(appAction(
                iconRes = if (isCurrentSongLiked) R.drawable.ic_favorite_24 else R.drawable.ic_favorite_border_24,
                title = if (isCurrentSongLiked) "取消喜欢" else "喜欢",
                action = ACTION_LIKE
            ))
            .addAction(systemAction(android.R.drawable.ic_media_previous, "上一首", ACTION_PREVIOUS))
            .addAction(systemAction(
                iconRes = if (playbackState.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                title = if (playbackState.isPlaying) "暂停" else "播放",
                action = ACTION_TOGGLE
            ))
            .addAction(systemAction(android.R.drawable.ic_media_next, "下一首", ACTION_NEXT))
            .addAction(systemAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", ACTION_DISMISS))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSessionToken)
                    .setShowActionsInCompactView(0, 2, 3)
            )

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        return notification
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun appAction(
        iconRes: Int,
        title: String,
        action: String
    ): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(context, iconRes),
            title,
            actionIntent(action)
        ).build()

    private fun systemAction(
        iconRes: Int,
        title: String,
        action: String
    ): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource("android", iconRes),
            title,
            actionIntent(action)
        ).build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示 SiListen 的音乐播放控制通知"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_player", true)
            putExtra("player_panel", PlayerSheetPanel.Lyrics.name)
        }
        return PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(context, PlaybackNotificationReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_LIKE = "com.silisten.app.action.PLAYBACK_LIKE"
        const val ACTION_TOGGLE = "com.silisten.app.action.PLAYBACK_TOGGLE"
        const val ACTION_PREVIOUS = "com.silisten.app.action.PLAYBACK_PREVIOUS"
        const val ACTION_NEXT = "com.silisten.app.action.PLAYBACK_NEXT"
        const val ACTION_DISMISS = "com.silisten.app.action.PLAYBACK_DISMISS"

        private const val CHANNEL_ID = "silisten_playback"
        internal const val NOTIFICATION_ID = 1036
    }
}
