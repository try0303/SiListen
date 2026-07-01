package com.silisten.app

import android.view.ViewGroup
import android.widget.FrameLayout
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silisten.app.ui.SiListenApp
import com.silisten.app.ui.theme.SiListenTheme

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val contentView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(contentView)
        setContentView(root)

        contentView.setContent {
            val viewModel: SiListenViewModel = viewModel()
            LaunchedEffect(viewModel) {
                handlePlaybackIntent(viewModel)
            }
            SiListenTheme(viewModel.uiState.themeSettings) {
                SiListenApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        contentViewModel()?.let(::handlePlaybackIntent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun contentViewModel(): SiListenViewModel? {
        return runCatching {
            androidx.lifecycle.ViewModelProvider(this)[SiListenViewModel::class.java]
        }.getOrNull()
    }

    private fun handlePlaybackIntent(viewModel: SiListenViewModel) {
        val currentIntent = intent ?: return
        if (currentIntent.getBooleanExtra(EXTRA_TOGGLE_CURRENT_LIKE, false)) {
            viewModel.toggleCurrentSongLike()
            currentIntent.removeExtra(EXTRA_TOGGLE_CURRENT_LIKE)
        }
        if (currentIntent.getBooleanExtra("open_player", false)) {
            val panel = currentIntent.getStringExtra("player_panel")
                ?.let { runCatching { PlayerSheetPanel.valueOf(it) }.getOrNull() }
                ?: PlayerSheetPanel.Lyrics
            viewModel.openPlayerSheet(panel)
            currentIntent.removeExtra("open_player")
            currentIntent.removeExtra("player_panel")
        }
    }

    companion object {
        const val EXTRA_TOGGLE_CURRENT_LIKE = "com.silisten.app.extra.TOGGLE_CURRENT_LIKE"
    }
}
