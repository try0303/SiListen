package com.silisten.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
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
        mediaSession: MediaSession
    ) {
        val song = playbackState.currentSong ?: run {
            cancel()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(song.title)
            .setContentText(song.artist.ifBlank { "SiListen" })
            .setSubText(song.album.ifBlank { if (playbackState.isPlaying) "正在播放" else "已暂停" })
            .setContentIntent(contentIntent())
            .setDeleteIntent(actionIntent(ACTION_DISMISS))
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(playbackState.isPlaying)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "上一首",
                    actionIntent(ACTION_PREVIOUS)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (playbackState.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (playbackState.isPlaying) "暂停" else "播放",
                    actionIntent(ACTION_TOGGLE)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "下一首",
                    actionIntent(ACTION_NEXT)
                )
            )
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SiListen Playback",
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
        const val ACTION_TOGGLE = "com.silisten.app.action.PLAYBACK_TOGGLE"
        const val ACTION_PREVIOUS = "com.silisten.app.action.PLAYBACK_PREVIOUS"
        const val ACTION_NEXT = "com.silisten.app.action.PLAYBACK_NEXT"
        const val ACTION_DISMISS = "com.silisten.app.action.PLAYBACK_DISMISS"

        private const val CHANNEL_ID = "silisten_playback"
        private const val NOTIFICATION_ID = 1036
    }
}
