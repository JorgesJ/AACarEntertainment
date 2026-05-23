package com.jorso.carapp.auto

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MusicCarActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val songButtons = mutableListOf<LinearLayout>()

    private lateinit var tvNowPlaying: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnPlayPause: TextView
    private lateinit var songList: LinearLayout

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUI())
        startService(Intent(this, MusicPlaybackService::class.java))
        handler.postDelayed({
            MusicPlaybackService.instance?.onStateChanged = {
                runOnUiThread { refreshSongList() }
            }
            refreshSongList()
        }, 800)
    }

    private fun buildUI(): View {
        // Layout horizontal: lista izquierda + player derecha
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Panel izquierdo — lista canciones
        val leftPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A1A.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Header izquierdo
        val leftHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val btnBack = TextView(this).apply {
            text = "←"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0)
            setOnClickListener { finish() }
        }

        val tvTitle = TextView(this).apply {
            text = "Música"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        leftHeader.addView(btnBack)
        leftHeader.addView(tvTitle)
        leftPanel.addView(leftHeader)

        leftPanel.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        songList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        val tvLoading = TextView(this).apply {
            text = "Cargando canciones..."
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        }
        songList.addView(tvLoading)

        scrollView.addView(songList)
        leftPanel.addView(scrollView)

        // Panel derecho — player
        val rightPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val tvIcon = TextView(this).apply {
            text = "🎵"
            textSize = 48f
            gravity = Gravity.CENTER
        }

        val tvLabel = TextView(this).apply {
            text = "REPRODUCIENDO"
            textSize = 10f
            setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.15f
            setPadding(0, dp(16), 0, dp(8))
        }

        tvNowPlaying = TextView(this).apply {
            text = "Selecciona\nuna canción"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, 0, 0, dp(8))
        }

        tvStatus = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(0xFF4FC3F7.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }

        // Controles
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnPrev = buildControlButton("⏮") {
            MusicPlaybackService.instance?.skipToPrevious()
        }

        btnPlayPause = buildControlButton("▶") {
            val svc = MusicPlaybackService.instance ?: return@buildControlButton
            if (svc.isPlaying) svc.pausePlayback() else svc.resumePlayback()
        }

        val btnNext = buildControlButton("⏭") {
            MusicPlaybackService.instance?.skipToNext()
        }

        controls.addView(btnPrev)
        controls.addView(btnPlayPause)
        controls.addView(btnNext)

        rightPanel.addView(tvIcon)
        rightPanel.addView(tvLabel)
        rightPanel.addView(tvNowPlaying)
        rightPanel.addView(tvStatus)
        rightPanel.addView(controls)

        root.addView(leftPanel)
        root.addView(rightPanel)
        return root
    }

    private fun buildControlButton(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(20), dp(8), dp(20), dp(8))
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                null, null
            )
            setOnClickListener { onClick() }
        }
    }

    private fun refreshSongList() {
        val svc = MusicPlaybackService.instance ?: return
        songList.removeAllViews()
        songButtons.clear()

        if (svc.songs.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No se encontraron canciones\nConcede permisos de audio"
                textSize = 13f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(24), dp(8), 0)
            }
            songList.addView(tv)
            return
        }

        svc.songs.forEachIndexed { index, song ->
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                isClickable = true
                isFocusable = true
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                    android.graphics.drawable.ColorDrawable(0xFF222222.toInt()), null
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(2) }
            }

            val tvNum = TextView(this).apply {
                text = "${index + 1}"
                textSize = 11f
                setTextColor(0xFF555555.toInt())
                setPadding(0, 0, dp(10), 0)
                minWidth = dp(28)
            }

            val tvName = TextView(this).apply {
                text = song.title
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            btn.addView(tvNum)
            btn.addView(tvName)
            btn.setOnClickListener { svc.playSong(index) }

            songButtons.add(btn)
            songList.addView(btn)
        }
        updateUI()
    }

    private fun updateUI() {
        val svc = MusicPlaybackService.instance ?: return
        val song = svc.songs.getOrNull(svc.currentIndex)

        tvNowPlaying.text = song?.title ?: "Selecciona\nuna canción"
        tvStatus.text = when {
            svc.isPlaying -> "▶ Reproduciendo"
            svc.currentIndex >= 0 -> "⏸ Pausado"
            else -> ""
        }
        btnPlayPause.text = if (svc.isPlaying) "⏸" else "▶"

        songButtons.forEachIndexed { index, btn ->
            btn.setBackgroundColor(
                if (index == svc.currentIndex) 0xFF1A2A3A.toInt() else 0xFF222222.toInt()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
        MusicPlaybackService.instance?.onStateChanged = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

