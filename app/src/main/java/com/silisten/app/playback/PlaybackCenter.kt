package com.silisten.app.playback

import android.content.Context

object PlaybackCenter {
    @Volatile
    private var sharedController: PlayerController? = null

    fun controller(context: Context): PlayerController {
        return sharedController ?: synchronized(this) {
            sharedController ?: PlayerController(context.applicationContext).also { controller ->
                sharedController = controller
            }
        }
    }

    fun release() {
        synchronized(this) {
            sharedController?.release()
            sharedController = null
        }
    }
}
