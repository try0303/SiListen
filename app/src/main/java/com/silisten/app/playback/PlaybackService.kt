package com.silisten.app.playback

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class PlaybackService : Service() {
    override fun onCreate() {
        super.onCreate()
        activeService = this
        PlaybackCenter.controller(this)
        pendingNotification?.let { notification ->
            if (pendingForeground) {
                promoteToForeground(notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                leaveForeground(removeNotification = false)
                stopSelf(startId)
                return START_NOT_STICKY
            }
            else -> {
                pendingForeground = intent?.getBooleanExtra(EXTRA_FOREGROUND, pendingForeground) ?: pendingForeground
                pendingNotification?.let { notification ->
                    if (pendingForeground) {
                        promoteToForeground(notification)
                    } else {
                        leaveForeground(removeNotification = false)
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val controller = PlaybackCenter.controller(this)
        if (!controller.state.isPlaying) {
            leaveForeground(removeNotification = false)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                PlaybackNotificationController.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(PlaybackNotificationController.NOTIFICATION_ID, notification)
        }
    }

    private fun leaveForeground(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(
                if (removeNotification) {
                    STOP_FOREGROUND_REMOVE
                } else {
                    STOP_FOREGROUND_DETACH
                }
            )
        } else {
            @Suppress("DEPRECATION")
            stopForeground(removeNotification)
        }
    }

    companion object {
        private const val ACTION_UPDATE = "com.silisten.app.playback.action.UPDATE_FOREGROUND"
        private const val ACTION_STOP = "com.silisten.app.playback.action.STOP_FOREGROUND"
        private const val EXTRA_FOREGROUND = "foreground"

        @Volatile
        private var activeService: PlaybackService? = null

        @Volatile
        private var pendingNotification: Notification? = null

        @Volatile
        private var pendingForeground: Boolean = false

        fun update(context: Context, notification: Notification, foreground: Boolean) {
            val appContext = context.applicationContext
            pendingNotification = notification
            pendingForeground = foreground
            val service = activeService
            if (service != null) {
                if (foreground) {
                    service.promoteToForeground(notification)
                } else {
                    service.leaveForeground(removeNotification = false)
                }
                return
            }
            if (foreground) {
                val intent = Intent(appContext, PlaybackService::class.java).apply {
                    action = ACTION_UPDATE
                    putExtra(EXTRA_FOREGROUND, foreground)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            }
        }

        fun stop() {
            pendingNotification = null
            pendingForeground = false
            val service = activeService ?: return
            service.leaveForeground(removeNotification = false)
            service.stopSelf()
        }
    }
}
