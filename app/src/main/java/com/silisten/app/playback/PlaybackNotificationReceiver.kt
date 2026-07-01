package com.silisten.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.silisten.app.MainActivity

class PlaybackNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = PlaybackCenter.controller(context)
        when (intent.action) {
            PlaybackNotificationController.ACTION_LIKE -> {
                if (!PlaybackNotificationBridge.like()) {
                    openAppForLikeFallback(context)
                }
            }
            PlaybackNotificationController.ACTION_TOGGLE -> controller.toggle()
            PlaybackNotificationController.ACTION_PREVIOUS -> controller.previous()
            PlaybackNotificationController.ACTION_NEXT -> controller.next()
            PlaybackNotificationController.ACTION_DISMISS -> controller.dismissNotification()
        }
    }

    private fun openAppForLikeFallback(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TOGGLE_CURRENT_LIKE, true)
        }
        context.startActivity(launchIntent)
    }
}

internal object PlaybackNotificationBridge {
    private var controller: PlayerController? = null
    private var likeHandler: (() -> Unit)? = null
    private var likeStateProvider: ((com.silisten.app.data.model.Song) -> Boolean)? = null

    fun attach(controller: PlayerController) {
        this.controller = controller
    }

    fun attachLikeHandler(handler: () -> Unit) {
        likeHandler = handler
    }

    fun attachLikeStateProvider(provider: (com.silisten.app.data.model.Song) -> Boolean) {
        likeStateProvider = provider
    }

    fun detachLikeHandler(handler: () -> Unit) {
        if (likeHandler === handler) {
            likeHandler = null
        }
    }

    fun detachLikeStateProvider(provider: (com.silisten.app.data.model.Song) -> Boolean) {
        if (likeStateProvider === provider) {
            likeStateProvider = null
        }
    }

    fun detach(controller: PlayerController) {
        if (this.controller === controller) {
            this.controller = null
        }
    }

    fun toggle() {
        controller?.toggle()
    }

    fun like(): Boolean {
        val handler = likeHandler ?: return false
        handler.invoke()
        return true
    }

    fun isLiked(song: com.silisten.app.data.model.Song): Boolean =
        likeStateProvider?.invoke(song) == true

    fun previous() {
        controller?.previous()
    }

    fun next() {
        controller?.next()
    }

    fun dismissNotification() {
        controller?.dismissNotification()
    }
}
