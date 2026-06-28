package com.silisten.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlaybackNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PlaybackNotificationController.ACTION_TOGGLE -> PlaybackNotificationBridge.toggle()
            PlaybackNotificationController.ACTION_PREVIOUS -> PlaybackNotificationBridge.previous()
            PlaybackNotificationController.ACTION_NEXT -> PlaybackNotificationBridge.next()
            PlaybackNotificationController.ACTION_DISMISS -> PlaybackNotificationBridge.dismissNotification()
        }
    }
}

internal object PlaybackNotificationBridge {
    private var controller: PlayerController? = null

    fun attach(controller: PlayerController) {
        this.controller = controller
    }

    fun detach(controller: PlayerController) {
        if (this.controller === controller) {
            this.controller = null
        }
    }

    fun toggle() {
        controller?.toggle()
    }

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
