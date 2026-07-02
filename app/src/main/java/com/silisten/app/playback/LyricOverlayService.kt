package com.silisten.app.playback

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.silisten.app.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class LyricOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var statusView: StatusLyricView? = null
    private var desktopView: View? = null
    private var desktopLyricView: DesktopLyricTextView? = null
    private var desktopTitleView: TextView? = null
    private var desktopPlayPauseButton: DesktopIconButton? = null
    private var desktopLayoutParams: WindowManager.LayoutParams? = null
    private var desktopExpanded = false
    private var desktopLocked = false
    private var desktopDismissedByUser = false
    private var desktopSettingsVisible = false
    private var desktopLyricTextColor = Color.rgb(155, 91, 217)
    private var desktopLyricTextSizeSp = 17f
    private var desktopSavedX = 0
    private var desktopSavedY = Int.MIN_VALUE
    private val desktopLyricColorOptions = intArrayOf(
        Color.rgb(231, 76, 60),
        Color.rgb(64, 196, 218),
        Color.rgb(72, 210, 143),
        Color.rgb(218, 190, 62),
        Color.rgb(155, 91, 217)
    )

    private var lastTitle = "SiListen"
    private var lastArtist = ""
    private var lastText = ""
    private var lastIsPlaying = false
    private var lastLyricProgress = 0f
    private var statusOffsetDp = 0
    private var statusHorizontalPercent = 0f
    private var statusWidthPercent = 1f
    private var statusTextColorArgb = 0xFFFFFFFF.toInt()
    private var statusFrameVisible = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        loadDesktopLyricStyle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE || !canDrawOverlays(this)) {
            removeAllViews()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val statusEnabled = intent?.getBooleanExtra(EXTRA_STATUS_ENABLED, false) == true
        val requestedDesktopEnabled = intent?.getBooleanExtra(EXTRA_DESKTOP_ENABLED, false) == true
        val desktopPreferenceEnabled = getSharedPreferences(PLAYBACK_SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DESKTOP_LYRIC, requestedDesktopEnabled)
        desktopDismissedByUser = desktopDismissedByUser && !desktopPreferenceEnabled
        val desktopEnabled = requestedDesktopEnabled && !desktopDismissedByUser && desktopPreferenceEnabled
        val statusText = intent?.getStringExtra(EXTRA_TEXT).orEmpty().trim()
        val desktopText = intent?.getStringExtra(EXTRA_DESKTOP_TEXT).orEmpty().ifBlank { statusText }.trim()
        loadDesktopLyricStyle()
        lastTitle = intent?.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "SiListen" }
        lastArtist = intent?.getStringExtra(EXTRA_ARTIST).orEmpty()
        lastIsPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, false) == true
        lastLyricProgress = intent?.getFloatExtra(EXTRA_LYRIC_PROGRESS, 0f)?.coerceIn(0f, 1f) ?: 0f
        lastText = desktopText
        statusOffsetDp = intent?.getIntExtra(EXTRA_STATUS_OFFSET_DP, 0)?.coerceIn(0, 120) ?: 0
        statusHorizontalPercent = intent?.getFloatExtra(EXTRA_STATUS_HORIZONTAL_PERCENT, 0f)?.coerceIn(-1f, 1f) ?: 0f
        statusWidthPercent = intent?.getFloatExtra(EXTRA_STATUS_WIDTH_PERCENT, 1f)?.coerceIn(0.35f, 1f) ?: 1f
        statusTextColorArgb = (intent?.getLongExtra(EXTRA_STATUS_TEXT_COLOR_ARGB, 0xFFFFFFFF) ?: 0xFFFFFFFF).toInt()
        statusFrameVisible = intent?.getBooleanExtra(EXTRA_STATUS_FRAME_VISIBLE, false) == true

        if ((!statusEnabled && !desktopEnabled) || (statusText.isBlank() && desktopText.isBlank())) {
            removeAllViews()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        renderStatusLyric(statusEnabled, statusText.ifBlank { desktopText.lineSequence().firstOrNull()?.trim().orEmpty() })
        renderDesktopLyric(desktopEnabled && lastIsPlaying)
        return START_STICKY
    }

    override fun onDestroy() {
        removeAllViews()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun renderStatusLyric(enabled: Boolean, text: String) {
        if (!enabled) {
            statusView?.let { runCatching { windowManager.removeView(it) } }
            statusView = null
            return
        }
        val view = statusView ?: StatusLyricView(this).apply {
            statusView = this
            background = statusBackground()
            windowManager.addView(this, statusLayoutParams())
        }
        view.background = statusBackground()
        view.setLyric(text, statusTextColorArgb)
        runCatching { windowManager.updateViewLayout(view, statusLayoutParams()) }
    }

    private class StatusLyricView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private val horizontalPadding = dp(14)
        private val verticalPadding = dp(0)
        private val repeatGap = dp(56).toFloat()
        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = sp(12.5f)
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
            setShadowLayer(dp(5).toFloat(), 0f, dp(1).toFloat(), Color.argb(190, 0, 0, 0))
        }
        private var lyric = ""
        private var lyricWidth = 0f
        private var textChangedAt = SystemClock.uptimeMillis()

        fun setLyric(text: String, color: Int) {
            val nextText = text.ifBlank { " " }
            if (lyric != nextText) {
                lyric = nextText
                lyricWidth = textPaint.measureText(lyric)
                textChangedAt = SystemClock.uptimeMillis()
            }
            if (textPaint.color != color) {
                textPaint.color = color
            }
            invalidate()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val metrics = textPaint.fontMetrics
            val desiredHeight = ((metrics.descent - metrics.ascent) + verticalPadding * 2).roundToInt()
            val height = resolveSize(desiredHeight, heightMeasureSpec)
            setMeasuredDimension(width, height)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (lyric.isBlank() || width <= horizontalPadding * 2) return

            val contentLeft = horizontalPadding.toFloat()
            val contentRight = (width - horizontalPadding).toFloat()
            val contentWidth = contentRight - contentLeft
            val textWidth = lyricWidth.takeIf { it > 0f } ?: textPaint.measureText(lyric)
            val metrics = textPaint.fontMetrics
            val baseline = height / 2f - (metrics.ascent + metrics.descent) / 2f
            if (textWidth <= contentWidth) {
                val x = contentLeft + (contentWidth - textWidth) / 2f
                canvas.drawText(lyric, x, baseline, textPaint)
                return
            }

            val cycleDistance = textWidth + repeatGap
            val offset = seamlessScrollOffset(cycleDistance)
            val firstX = contentLeft - offset
            val secondX = firstX + cycleDistance
            val checkpoint = canvas.save()
            canvas.clipRect(contentLeft, 0f, contentRight, height.toFloat())
            drawTextIfVisible(canvas, firstX, baseline, textWidth, contentLeft, contentRight)
            drawTextIfVisible(canvas, secondX, baseline, textWidth, contentLeft, contentRight)
            canvas.restoreToCount(checkpoint)
            postInvalidateOnAnimation()
        }

        private fun seamlessScrollOffset(distance: Float): Float {
            val startPauseMs = 900L
            val scrollDurationMs = ((distance / (32f * density)) * 1000f).roundToInt().coerceAtLeast(2200).toLong()
            val cycleMs = startPauseMs + scrollDurationMs
            val elapsed = (SystemClock.uptimeMillis() - textChangedAt).floorMod(cycleMs)
            return when {
                elapsed < startPauseMs -> 0f
                else -> {
                    val progress = (elapsed - startPauseMs).toFloat() / scrollDurationMs
                    distance * progress
                }
            }
        }

        private fun drawTextIfVisible(
            canvas: Canvas,
            x: Float,
            baseline: Float,
            textWidth: Float,
            visibleLeft: Float,
            visibleRight: Float
        ) {
            if (x + textWidth >= visibleLeft && x <= visibleRight) {
                canvas.drawText(lyric, x, baseline, textPaint)
            }
        }

        private fun dp(value: Int): Int = (value * density).roundToInt()

        private fun sp(value: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

        private fun Long.floorMod(other: Long): Long = ((this % other) + other) % other
    }

    private class DesktopLyricTextView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private var primaryLyric = " "
        private var secondaryLyric = " "
        private var progress = 0f
        private var highlightColor = Color.rgb(155, 91, 217)
        private var textSizeSp = 19f
        private var cachedWidth = -1
        private var primaryBaseLayout: StaticLayout? = null
        private var primaryHighlightLayout: StaticLayout? = null
        private var secondaryLayout: StaticLayout? = null

        private val basePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.01f
        }
        private val highlightPaint = TextPaint(basePaint).apply {
            color = highlightColor
        }
        private val secondaryPaint = TextPaint(basePaint).apply {
            color = Color.argb(170, 255, 255, 255)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        init {
            setPadding(dp(18), 0, dp(18), 0)
        }

        fun setLyric(text: String, progress: Float, highlightColor: Int, textSizeSp: Float) {
            val lines = text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val nextPrimary = lines.getOrNull(0) ?: " "
            val nextSecondary = lines.getOrNull(1) ?: " "
            val nextProgress = progress.coerceIn(0f, 1f)
            val nextSize = textSizeSp.coerceIn(16f, 34f)
            val changed = primaryLyric != nextPrimary ||
                secondaryLyric != nextSecondary ||
                this.highlightColor != highlightColor ||
                this.textSizeSp != nextSize
            primaryLyric = nextPrimary
            secondaryLyric = nextSecondary
            this.progress = nextProgress
            this.highlightColor = highlightColor
            this.textSizeSp = nextSize
            if (changed) {
                cachedWidth = -1
                requestLayout()
            }
            invalidate()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            ensureLayouts((width - paddingLeft - paddingRight).coerceAtLeast(1))
            val primaryHeight = primaryBaseLayout?.height ?: dp(24)
            val secondaryHeight = secondaryLayout?.height ?: dp(20)
            val desiredHeight = primaryHeight + dp(5) + secondaryHeight + paddingTop + paddingBottom
            setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec))
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val contentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1)
            ensureLayouts(contentWidth)
            val primary = primaryBaseLayout ?: return
            val highlight = primaryHighlightLayout ?: return
            val secondary = secondaryLayout ?: return
            val gap = dp(5).toFloat()
            val totalHeight = primary.height + gap + secondary.height
            val top = ((height - totalHeight) / 2f).coerceAtLeast(0f)
            val checkpoint = canvas.save()
            canvas.translate(paddingLeft.toFloat(), top)
            primary.draw(canvas)
            drawHighlight(canvas, highlight)
            canvas.translate(0f, primary.height + gap)
            secondary.draw(canvas)
            canvas.restoreToCount(checkpoint)
        }

        private fun drawHighlight(canvas: Canvas, layout: StaticLayout) {
            if (progress <= 0f) return
            if (layout.lineCount == 0) return
            val line = 0
            val lineWidth = (layout.getLineRight(line) - layout.getLineLeft(line)).coerceAtLeast(1f)
            val fillWidth = lineWidth * progress.coerceIn(0f, 1f)
            val left = layout.getLineLeft(line)
            val top = layout.getLineTop(line).toFloat()
            val bottom = layout.getLineBottom(line).toFloat()
            val checkpoint = canvas.save()
            canvas.clipRect(left, top, left + fillWidth, bottom)
            layout.draw(canvas)
            canvas.restoreToCount(checkpoint)
        }

        private fun ensureLayouts(contentWidth: Int) {
            if (cachedWidth == contentWidth && primaryBaseLayout != null && primaryHighlightLayout != null && secondaryLayout != null) return
            cachedWidth = contentWidth
            val primarySizePx = sp(textSizeSp)
            basePaint.textSize = primarySizePx
            highlightPaint.textSize = primarySizePx
            highlightPaint.color = highlightColor
            secondaryPaint.textSize = sp(textSizeSp * 0.82f)
            primaryBaseLayout = buildSingleLineLayout(primaryLyric, basePaint, contentWidth)
            primaryHighlightLayout = buildSingleLineLayout(primaryLyric, highlightPaint, contentWidth)
            secondaryLayout = buildSingleLineLayout(secondaryLyric, secondaryPaint, contentWidth)
        }

        private fun buildSingleLineLayout(text: String, paint: TextPaint, contentWidth: Int): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(1)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .build()

        private fun dp(value: Int): Int = (value * density).roundToInt()

        private fun sp(value: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }

    private fun renderDesktopLyric(enabled: Boolean) {
        if (!enabled) {
            saveCurrentDesktopLyricPosition()
            desktopView?.let { runCatching { windowManager.removeView(it) } }
            desktopView = null
            desktopLyricView = null
            desktopTitleView = null
            desktopPlayPauseButton = null
            return
        }
        val view = desktopView ?: buildDesktopView().also { nextView ->
            desktopView = nextView
            val params = desktopLayoutParams ?: desktopLayoutParams()
            desktopLayoutParams = params
            windowManager.addView(nextView, params)
        }
        desktopTitleView?.text = desktopTitleText()
        desktopLyricView?.setLyric(
            text = lastText,
            progress = lastLyricProgress,
            highlightColor = desktopLyricTextColor,
            textSizeSp = desktopLyricTextSizeSp + if (desktopExpanded) 1.5f else 0f
        )
        desktopPlayPauseButton?.icon = if (lastIsPlaying) DesktopIcon.Pause else DesktopIcon.Play
        runCatching { windowManager.updateViewLayout(view, desktopLayoutParams ?: desktopLayoutParams()) }
    }

    private fun rebuildDesktopView() {
        val params = desktopLayoutParams ?: desktopLayoutParams()
        params.width = desktopWidth()
        desktopView?.let { runCatching { windowManager.removeView(it) } }
        desktopLayoutParams = params
        desktopView = buildDesktopView().also { windowManager.addView(it, params) }
        renderDesktopLyric(enabled = true)
    }

    private fun buildDesktopView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                dp(if (desktopExpanded) 18 else 8),
                dp(if (desktopExpanded) 14 else 0),
                dp(if (desktopExpanded) 18 else 8),
                dp(if (desktopExpanded) 16 else 0)
            )
            background = if (desktopExpanded) desktopBackground() else null
            setOnTouchListener(DesktopTouchListener())
        }
        desktopTitleView = TextView(this).apply {
            text = desktopTitleText()
            setTextColor(Color.argb(142, 255, 255, 255))
            textSize = 12.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.02f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Color.argb(120, 0, 0, 0))
        }
        desktopLyricView = DesktopLyricTextView(this).apply {
            setLyric(
                text = lastText,
                progress = lastLyricProgress,
                highlightColor = desktopLyricTextColor,
                textSizeSp = desktopLyricTextSizeSp + if (desktopExpanded) 1.5f else 0f
            )
        }

        if (desktopExpanded) {
            root.addView(expandedHeader())
            root.addView(desktopLyricView, matchWrapLayout(top = 12))
            root.addView(desktopTitleView, matchWrapLayout(top = 7))
            root.addView(expandedControls(), matchWrapLayout(top = 20))
            if (desktopSettingsVisible) {
                root.addView(desktopSettingsPanel(), matchWrapLayout(top = 18))
            }
        } else {
            root.addView(desktopLyricView, matchWrapLayout())
        }
        return root
    }

    private fun expandedHeader(): View =
        FrameLayout(this).apply {
            addView(appIconButton(), FrameLayout.LayoutParams(dp(42), dp(42), Gravity.START or Gravity.CENTER_VERTICAL))
            addView(desktopIconButton(DesktopIcon.Close) {
                closeDesktopLyrics()
            }, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.END or Gravity.CENTER_VERTICAL))
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
        }

    private fun expandedControls(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val controller = PlaybackCenter.controller(this@LyricOverlayService)
            addView(desktopIconButton(if (desktopLocked) DesktopIcon.Locked else DesktopIcon.Unlocked) {
                desktopLocked = !desktopLocked
                rebuildDesktopView()
            }, controlButtonLayout())
            addView(desktopIconButton(DesktopIcon.Previous) { controller.previous() }, controlButtonLayout())
            val playPauseButton = desktopIconButton(if (lastIsPlaying) DesktopIcon.Pause else DesktopIcon.Play) {
                controller.toggle()
                lastIsPlaying = !lastIsPlaying
                desktopPlayPauseButton?.icon = if (lastIsPlaying) DesktopIcon.Pause else DesktopIcon.Play
            }
            desktopPlayPauseButton = playPauseButton
            addView(playPauseButton, controlButtonLayout(width = 60))
            addView(desktopIconButton(DesktopIcon.Next) { controller.next() }, controlButtonLayout())
            addView(desktopIconButton(DesktopIcon.Settings) {
                desktopSettingsVisible = !desktopSettingsVisible
                rebuildDesktopView()
            }, controlButtonLayout())
        }

    private fun desktopSettingsPanel(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(2), dp(4), 0)
            desktopLyricColorOptions.forEach { color ->
                addView(colorOptionButton(color), colorButtonLayout())
            }
            addView(textSizeButton("T+") {
                changeDesktopLyricTextSize(1f)
            }, textSizeButtonLayout())
            addView(textSizeButton("T-") {
                changeDesktopLyricTextSize(-1f)
            }, textSizeButtonLayout())
        }

    private fun colorOptionButton(color: Int): View =
        DesktopColorButton(this, color, color == desktopLyricTextColor).apply {
            setOnClickListener {
                desktopLyricTextColor = color
                saveDesktopLyricStyle()
                updateDesktopLyricStyle()
                rebuildDesktopView()
            }
        }

    private fun textSizeButton(text: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(Color.argb(198, 255, 255, 255))
            background = roundButtonBackground(alpha = 22)
            setOnClickListener { onClick() }
        }

    private fun changeDesktopLyricTextSize(delta: Float) {
        desktopLyricTextSizeSp = (desktopLyricTextSizeSp + delta).coerceIn(16f, 32f)
        saveDesktopLyricStyle()
        updateDesktopLyricStyle()
    }

    private fun updateDesktopLyricStyle() {
        desktopLyricView?.setLyric(
            text = lastText,
            progress = lastLyricProgress,
            highlightColor = desktopLyricTextColor,
            textSizeSp = desktopLyricTextSizeSp + if (desktopExpanded) 1.5f else 0f
        )
    }

    private fun loadDesktopLyricStyle() {
        val preferences = getSharedPreferences(PLAYBACK_SETTINGS_PREFS, Context.MODE_PRIVATE)
        desktopLyricTextColor = preferences.getInt(KEY_DESKTOP_LYRIC_COLOR, Color.rgb(155, 91, 217))
        desktopLyricTextSizeSp = preferences.getFloat(KEY_DESKTOP_LYRIC_SIZE_SP, 17f).coerceIn(14f, 32f)
        desktopSavedX = preferences.getInt(KEY_DESKTOP_LYRIC_X, 0)
        desktopSavedY = preferences.getInt(KEY_DESKTOP_LYRIC_Y, Int.MIN_VALUE)
    }

    private fun saveDesktopLyricStyle() {
        getSharedPreferences(PLAYBACK_SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DESKTOP_LYRIC_COLOR, desktopLyricTextColor)
            .putFloat(KEY_DESKTOP_LYRIC_SIZE_SP, desktopLyricTextSizeSp)
            .apply()
    }

    private fun saveCurrentDesktopLyricPosition() {
        val params = desktopLayoutParams ?: return
        saveDesktopLyricPosition(params.x, params.y)
    }

    private fun saveDesktopLyricPosition(x: Int, y: Int) {
        desktopSavedX = x
        desktopSavedY = y
        getSharedPreferences(PLAYBACK_SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DESKTOP_LYRIC_X, x)
            .putInt(KEY_DESKTOP_LYRIC_Y, y)
            .apply()
    }

    private fun appIconButton(): View =
        ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(9), dp(9), dp(9), dp(9))
            background = roundButtonBackground(alpha = 30)
            clipToOutline = true
            contentDescription = "打开 SiListen"
            isClickable = true
            setOnClickListener { openAppSettings() }
        }

    private fun desktopIconButton(icon: DesktopIcon, onClick: () -> Unit): DesktopIconButton =
        DesktopIconButton(this).apply {
            this.icon = icon
            contentDescription = icon.description
            setOnClickListener { onClick() }
        }

    private fun roundButtonBackground(alpha: Int = 30): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(alpha, 255, 255, 255))
        }

    private fun closeDesktopLyrics() {
        saveCurrentDesktopLyricPosition()
        PlaybackNotificationBridge.setDesktopLyric(this, enabled = false)
        desktopDismissedByUser = true
        desktopView?.let { runCatching { windowManager.removeView(it) } }
        desktopView = null
        desktopLyricView = null
        desktopTitleView = null
        desktopPlayPauseButton = null
        desktopLayoutParams = null
        if (statusView == null) stopSelf()
    }

    private enum class DesktopIcon(val description: String) {
        Close("关闭桌面歌词"),
        Locked("解锁拖动"),
        Unlocked("锁定位置"),
        Previous("上一首"),
        Play("播放"),
        Pause("暂停"),
        Next("下一首"),
        Settings("调节桌面歌词")
    }

    private class DesktopIconButton(context: Context) : View(context) {
        var icon: DesktopIcon = DesktopIcon.Play
            set(value) {
                field = value
                invalidate()
            }

        private val density = resources.displayMetrics.density
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(232, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = dp(1.85f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(238, 255, 255, 255)
            style = Paint.Style.FILL
        }
        private val path = Path()
        private val rect = RectF()

        init {
            isClickable = true
        }

        override fun drawableStateChanged() {
            super.drawableStateChanged()
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = min(width, height).toFloat()
            val cx = width / 2f
            val cy = height / 2f
            bgPaint.color = Color.argb(if (isPressed) 62 else 24, 255, 255, 255)
            canvas.drawCircle(cx, cy, size * 0.36f, bgPaint)
            when (icon) {
                DesktopIcon.Close -> drawClose(canvas, cx, cy)
                DesktopIcon.Locked -> drawLock(canvas, cx, cy, locked = true)
                DesktopIcon.Unlocked -> drawLock(canvas, cx, cy, locked = false)
                DesktopIcon.Previous -> drawPrevious(canvas, cx, cy)
                DesktopIcon.Play -> drawPlay(canvas, cx, cy)
                DesktopIcon.Pause -> drawPause(canvas, cx, cy)
                DesktopIcon.Next -> drawNext(canvas, cx, cy)
                DesktopIcon.Settings -> drawSettings(canvas, cx, cy)
            }
        }

        private fun drawClose(canvas: Canvas, cx: Float, cy: Float) {
            val r = dp(7f)
            canvas.drawLine(cx - r, cy - r, cx + r, cy + r, strokePaint)
            canvas.drawLine(cx + r, cy - r, cx - r, cy + r, strokePaint)
        }

        private fun drawLock(canvas: Canvas, cx: Float, cy: Float, locked: Boolean) {
            rect.set(cx - dp(8f), cy - dp(1f), cx + dp(8f), cy + dp(12f))
            canvas.drawRoundRect(rect, dp(3f), dp(3f), strokePaint)
            if (locked) {
                rect.set(cx - dp(7f), cy - dp(13f), cx + dp(7f), cy + dp(5f))
                canvas.drawArc(rect, 205f, 130f, false, strokePaint)
            } else {
                rect.set(cx - dp(3f), cy - dp(13f), cx + dp(11f), cy + dp(5f))
                canvas.drawArc(rect, 205f, 100f, false, strokePaint)
            }
        }

        private fun drawPrevious(canvas: Canvas, cx: Float, cy: Float) {
            canvas.drawLine(cx - dp(9f), cy - dp(10f), cx - dp(9f), cy + dp(10f), strokePaint)
            path.reset()
            path.moveTo(cx + dp(8f), cy - dp(11f))
            path.lineTo(cx - dp(6f), cy)
            path.lineTo(cx + dp(8f), cy + dp(11f))
            path.close()
            canvas.drawPath(path, fillPaint)
        }

        private fun drawPlay(canvas: Canvas, cx: Float, cy: Float) {
            path.reset()
            path.moveTo(cx - dp(5f), cy - dp(11f))
            path.lineTo(cx + dp(10f), cy)
            path.lineTo(cx - dp(5f), cy + dp(11f))
            path.close()
            canvas.drawPath(path, fillPaint)
        }

        private fun drawPause(canvas: Canvas, cx: Float, cy: Float) {
            val w = dp(4.2f)
            val h = dp(20f)
            rect.set(cx - dp(8f), cy - h / 2f, cx - dp(8f) + w, cy + h / 2f)
            canvas.drawRoundRect(rect, dp(1.8f), dp(1.8f), fillPaint)
            rect.set(cx + dp(4f), cy - h / 2f, cx + dp(4f) + w, cy + h / 2f)
            canvas.drawRoundRect(rect, dp(1.8f), dp(1.8f), fillPaint)
        }

        private fun drawNext(canvas: Canvas, cx: Float, cy: Float) {
            path.reset()
            path.moveTo(cx - dp(8f), cy - dp(11f))
            path.lineTo(cx + dp(6f), cy)
            path.lineTo(cx - dp(8f), cy + dp(11f))
            path.close()
            canvas.drawPath(path, fillPaint)
            canvas.drawLine(cx + dp(9f), cy - dp(10f), cx + dp(9f), cy + dp(10f), strokePaint)
        }

        private fun drawSettings(canvas: Canvas, cx: Float, cy: Float) {
            val left = cx - dp(10f)
            val right = cx + dp(10f)
            val y1 = cy - dp(7f)
            val y2 = cy
            val y3 = cy + dp(7f)
            canvas.drawLine(left, y1, right, y1, strokePaint)
            canvas.drawLine(left, y2, right, y2, strokePaint)
            canvas.drawLine(left, y3, right, y3, strokePaint)
            canvas.drawCircle(cx - dp(3f), y1, dp(2.5f), fillPaint)
            canvas.drawCircle(cx + dp(5f), y2, dp(2.5f), fillPaint)
            canvas.drawCircle(cx - dp(6f), y3, dp(2.5f), fillPaint)
        }

        private fun dp(value: Float): Float = value * density
    }

    private class DesktopColorButton(
        context: Context,
        private val color: Int,
        private val selected: Boolean
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
        }
        private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (color == Color.WHITE || color == Color.rgb(218, 190, 62)) Color.argb(210, 0, 0, 0) else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dp(2.4f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val path = Path()

        init {
            isClickable = true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = min(width, height) * 0.34f
            val cx = width / 2f
            val cy = height / 2f
            circlePaint.color = color
            canvas.drawCircle(cx, cy, radius, circlePaint)
            if (selected) {
                canvas.drawCircle(cx, cy, radius + dp(3.5f), ringPaint)
                path.reset()
                path.moveTo(cx - dp(5.5f), cy)
                path.lineTo(cx - dp(1f), cy + dp(4.5f))
                path.lineTo(cx + dp(7f), cy - dp(6f))
                canvas.drawPath(path, checkPaint)
            }
        }

        private fun dp(value: Float): Float = value * density
    }

    private inner class DesktopTouchListener : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = desktopLayoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (desktopLocked) return true
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                    params.x = boundedDesktopX(startX + dx.roundToInt(), params)
                    params.y = (startY + dy.roundToInt()).coerceAtLeast(0)
                    windowManager.updateViewLayout(view, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    saveDesktopLyricPosition(params.x, params.y)
                    if (!moved) {
                        desktopExpanded = !desktopExpanded
                        rebuildDesktopView()
                    }
                    return true
                }
            }
            return true
        }
    }

    private fun boundedDesktopX(targetX: Int, params: WindowManager.LayoutParams): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val viewWidth = when {
            params.width > 0 -> params.width
            desktopView?.width?.let { it > 0 } == true -> desktopView?.width ?: desktopWidth()
            else -> desktopWidth()
        }
        val maxOffset = ((screenWidth - viewWidth).coerceAtLeast(0) / 2f).roundToInt()
        return targetX.coerceIn(-maxOffset, maxOffset)
    }

    private fun statusLayoutParams(): WindowManager.LayoutParams {
        val width = if (statusWidthPercent >= 0.99f) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            (resources.displayMetrics.widthPixels * statusWidthPercent).roundToInt()
        }
        val horizontalOffset = if (width == WindowManager.LayoutParams.MATCH_PARENT) {
            0
        } else {
            val available = (resources.displayMetrics.widthPixels - width).coerceAtLeast(0) / 2f
            (available * statusHorizontalPercent).roundToInt()
        }
        return baseLayoutParams(
            width = width,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = horizontalOffset
            y = dp(statusOffsetDp)
        }
    }

    private fun statusBackground(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            if (statusFrameVisible) {
                intArrayOf(Color.argb(18, 255, 255, 255), Color.argb(92, 0, 0, 0), Color.argb(18, 255, 255, 255))
            } else {
                intArrayOf(Color.TRANSPARENT, Color.argb(54, 0, 0, 0), Color.TRANSPARENT)
            }
        ).apply {
            cornerRadius = dp(999).toFloat()
            if (statusFrameVisible) {
                setStroke(dp(1), Color.argb(150, 255, 255, 255))
            }
        }

    private fun desktopLayoutParams(): WindowManager.LayoutParams =
        baseLayoutParams(
            width = desktopWidth(),
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = boundedDesktopX(desktopSavedX, this)
            y = desktopSavedY.takeIf { it != Int.MIN_VALUE } ?: dp(520)
        }

    private fun desktopWidth(): Int =
        resources.displayMetrics.widthPixels

    private fun baseLayoutParams(width: Int, height: Int, flags: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            overlayWindowType(),
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    private fun desktopBackground(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb(146, 18, 20, 24),
                Color.argb(118, 34, 38, 44),
                Color.argb(102, 10, 12, 16)
            )
        ).apply {
            cornerRadius = dp(28).toFloat()
            setStroke(dp(1), Color.argb(34, 255, 255, 255))
        }

    private fun desktopTitleText(): String =
        listOf(lastTitle, lastArtist).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "SiListen" }

    private fun matchWrapLayout(top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
        }

    private fun controlButtonLayout(width: Int = 52): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(width), dp(48)).apply {
            leftMargin = dp(3)
            rightMargin = dp(3)
        }

    private fun colorButtonLayout(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(38), dp(38)).apply {
            leftMargin = dp(1)
            rightMargin = dp(1)
        }

    private fun textSizeButtonLayout(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(44), dp(36)).apply {
            leftMargin = dp(5)
            rightMargin = dp(1)
        }

    private fun openAppSettings() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
    }

    private fun removeAllViews() {
        statusView?.let { runCatching { windowManager.removeView(it) } }
        desktopView?.let { runCatching { windowManager.removeView(it) } }
        statusView = null
        desktopView = null
        desktopLyricView = null
        desktopTitleView = null
        desktopPlayPauseButton = null
        desktopLayoutParams = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val ACTION_UPDATE = "com.silisten.app.playback.action.UPDATE_LYRIC_OVERLAY"
        private const val ACTION_HIDE = "com.silisten.app.playback.action.HIDE_LYRIC_OVERLAY"
        private const val EXTRA_STATUS_ENABLED = "status_enabled"
        private const val EXTRA_DESKTOP_ENABLED = "desktop_enabled"
        private const val EXTRA_STATUS_OFFSET_DP = "status_offset_dp"
        private const val EXTRA_STATUS_HORIZONTAL_PERCENT = "status_horizontal_percent"
        private const val EXTRA_STATUS_WIDTH_PERCENT = "status_width_percent"
        private const val EXTRA_STATUS_TEXT_COLOR_ARGB = "status_text_color_argb"
        private const val EXTRA_STATUS_FRAME_VISIBLE = "status_frame_visible"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_DESKTOP_TEXT = "desktop_text"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ARTIST = "artist"
        private const val EXTRA_IS_PLAYING = "is_playing"
        private const val EXTRA_LYRIC_PROGRESS = "lyric_progress"
        private const val PLAYBACK_SETTINGS_PREFS = "playback_settings"
        private const val KEY_DESKTOP_LYRIC = "desktop_lyric"
        private const val KEY_DESKTOP_LYRIC_COLOR = "desktop_lyric_color_argb"
        private const val KEY_DESKTOP_LYRIC_SIZE_SP = "desktop_lyric_size_sp"
        private const val KEY_DESKTOP_LYRIC_X = "desktop_lyric_x"
        private const val KEY_DESKTOP_LYRIC_Y = "desktop_lyric_y"

        fun update(
            context: Context,
            statusEnabled: Boolean,
            desktopEnabled: Boolean,
            statusOffsetDp: Int,
            statusHorizontalPercent: Float,
            statusWidthPercent: Float,
            statusTextColorArgb: Long,
            statusFrameVisible: Boolean,
            text: String,
            desktopText: String = text,
            title: String = "",
            artist: String = "",
            isPlaying: Boolean = false,
            lyricProgress: Float = 0f
        ) {
            val appContext = context.applicationContext
            if ((!statusEnabled && !desktopEnabled) || (text.isBlank() && desktopText.isBlank()) || !canDrawOverlays(appContext)) {
                hide(appContext)
                return
            }
            val intent = Intent(appContext, LyricOverlayService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_STATUS_ENABLED, statusEnabled)
                putExtra(EXTRA_DESKTOP_ENABLED, desktopEnabled)
                putExtra(EXTRA_STATUS_OFFSET_DP, statusOffsetDp)
                putExtra(EXTRA_STATUS_HORIZONTAL_PERCENT, statusHorizontalPercent)
                putExtra(EXTRA_STATUS_WIDTH_PERCENT, statusWidthPercent)
                putExtra(EXTRA_STATUS_TEXT_COLOR_ARGB, statusTextColorArgb)
                putExtra(EXTRA_STATUS_FRAME_VISIBLE, statusFrameVisible)
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_DESKTOP_TEXT, desktopText)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_LYRIC_PROGRESS, lyricProgress.coerceIn(0f, 1f))
            }
            runCatching { appContext.startService(intent) }
        }

        fun hide(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, LyricOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            runCatching { appContext.startService(intent) }
        }

        fun canDrawOverlays(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        fun openOverlayPermissionSettings(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }

        @Suppress("DEPRECATION")
        private fun overlayWindowType(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
    }
}
