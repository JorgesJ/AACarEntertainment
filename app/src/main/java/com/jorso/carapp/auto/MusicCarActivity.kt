package com.jorso.carapp.auto

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MusicCarActivity : AppCompatActivity() {

    companion object {
        const val PREFS_CONFIG = "app_config"
        const val KEY_MUSIC_FOLDER = "music_folder"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val songButtons = mutableListOf<LinearLayout>()

    private lateinit var tvNowPlaying: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnPlayPause: TextView
    private lateinit var songList: LinearLayout
    private lateinit var container: android.widget.FrameLayout
    private lateinit var ivArtwork: ImageView
    private lateinit var tvDefaultIcon: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView

    private var userSeeking = false

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            saveFolder(uri.toString())
            MusicPlaybackService.instance?.reloadSongs()
            refreshSongList()
            showToast("Carpeta guardada ✓")
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateUI()
            updateProgress()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = android.widget.FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            fitsSystemWindows = true
        }
        setContentView(container)
        container.addView(buildUI())
        startService(Intent(this, MusicPlaybackService::class.java))

        handler.postDelayed({
            MusicPlaybackService.instance?.onStateChanged = {
                runOnUiThread { refreshSongList() }
            }
            val folder = getFolder()
            if (folder.isEmpty()) {
                showFolderPrompt()
            } else {
                refreshSongList()
            }
        }, 800)
    }

    private fun getFolder(): String =
        getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE).getString(KEY_MUSIC_FOLDER, "") ?: ""

    private fun saveFolder(uri: String) =
        getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE).edit().putString(KEY_MUSIC_FOLDER, uri).apply()

    private fun isPhone(): Boolean {
        val dm = resources.displayMetrics
        return (dm.widthPixels / dm.density) < 600f
    }

    private fun showFolderPrompt() {
        val overlay = android.widget.FrameLayout(this).apply {
            setBackgroundColor(0xEE111111.toInt())
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0xFF1E1E2E.toInt())
            setPadding(dp(32), dp(32), dp(32), dp(32))
            layoutParams = android.widget.FrameLayout.LayoutParams(
                if (isPhone()) ViewGroup.LayoutParams.MATCH_PARENT else dp(420),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        }
        card.addView(TextView(this).apply {
            text = "🎵"; textSize = 48f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        })
        card.addView(TextView(this).apply {
            text = "Selecciona la carpeta con la música que deseas cargar en la aplicación"
            textSize = 16f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        })
        card.addView(buildButton("📁  Seleccionar carpeta", 0xFF1A237E.toInt()) {
            container.removeView(overlay)
            folderPicker.launch(null)
        })
        overlay.addView(card)
        container.addView(overlay)
    }

    private fun buildUI(): View {
        return if (isPhone()) buildPhoneUI() else buildCarUI()
    }

    // ---------- MÓVIL: vertical ----------
    private fun buildPhoneUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(buildHeader())

        // Player compacto
        val player = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0xFF1A1A2A.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        player.addView(buildArtworkView(dp(120)))
        tvNowPlaying = TextView(this).apply {
            text = "Selecciona una canción"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(12), 0, 0)
        }
        tvStatus = TextView(this).apply {
            text = ""; textSize = 12f; setTextColor(0xFF4FC3F7.toInt()); gravity = Gravity.CENTER
        }
        player.addView(tvNowPlaying)
        player.addView(tvStatus)
        player.addView(buildProgressBar())
        player.addView(buildControls())
        root.addView(player)

        root.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        songList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        songList.addView(loadingText())
        scrollView.addView(songList)
        root.addView(scrollView)
        return root
    }

    // ---------- COCHE: horizontal ----------
    private fun buildCarUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val leftPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A1A.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        leftPanel.addView(buildHeader())
        leftPanel.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        songList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        songList.addView(loadingText())
        scrollView.addView(songList)
        leftPanel.addView(scrollView)

        // Panel derecho dentro de un ScrollView para que nunca se corten los controles
        val rightScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            isFillViewport = true
        }
        val rightPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }
        rightPanel.addView(buildArtworkView(dp(90)))
        val tvLabel = TextView(this).apply {
            text = "REPRODUCIENDO"; textSize = 9f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            letterSpacing = 0.15f; setPadding(0, dp(8), 0, dp(4))
        }
        tvNowPlaying = TextView(this).apply {
            text = "Selecciona\nuna canción"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(0, 0, 0, dp(4))
        }
        tvStatus = TextView(this).apply {
            text = ""; textSize = 12f; setTextColor(0xFF4FC3F7.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(6))
        }
        rightPanel.addView(tvLabel)
        rightPanel.addView(tvNowPlaying)
        rightPanel.addView(tvStatus)
        rightPanel.addView(buildProgressBar())
        rightPanel.addView(buildControls())
        rightScroll.addView(rightPanel)

        root.addView(leftPanel)
        root.addView(rightScroll)
        return root
    }

    // ---------- Componentes reutilizables ----------
    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0); setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Música"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "📁"; textSize = 18f; setPadding(dp(8), 0, 0, 0)
            setOnClickListener { folderPicker.launch(null) }
        })
        return header
    }

    private fun buildArtworkView(size: Int): View {
        val frame = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setBackgroundColor(0xFF2A2A3A.toInt())
        }
        tvDefaultIcon = TextView(this).apply {
            text = "🎵"; textSize = (size / 3f) / resources.displayMetrics.density
            gravity = Gravity.CENTER
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        ivArtwork = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        frame.addView(tvDefaultIcon)
        frame.addView(ivArtwork)
        return frame
    }

    private fun buildProgressBar(): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4); bottomMargin = dp(8) }
        }
        seekBar = SeekBar(this).apply {
            max = 1000
            progress = 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(sb: SeekBar?) { userSeeking = true }
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    val svc = MusicPlaybackService.instance
                    val dur = svc?.getDuration() ?: 0L
                    if (dur > 0) {
                        val newPos = dur * (sb?.progress ?: 0) / 1000
                        svc?.seekTo(newPos)
                    }
                    userSeeking = false
                }
            })
        }
        val times = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        tvCurrentTime = TextView(this).apply {
            text = "0:00"; textSize = 11f; setTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvTotalTime = TextView(this).apply {
            text = "0:00"; textSize = 11f; setTextColor(0xFF888888.toInt())
            gravity = Gravity.END
        }
        times.addView(tvCurrentTime)
        times.addView(tvTotalTime)
        wrap.addView(seekBar)
        wrap.addView(times)
        return wrap
    }

    private fun buildControls(): View {
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        }
        controls.addView(buildControlButton("⏮") { MusicPlaybackService.instance?.skipToPrevious() })
        btnPlayPause = buildControlButton("▶") {
            val svc = MusicPlaybackService.instance ?: return@buildControlButton
            if (svc.isPlaying) svc.pausePlayback() else svc.resumePlayback()
        }
        controls.addView(btnPlayPause)
        controls.addView(buildControlButton("⏭") { MusicPlaybackService.instance?.skipToNext() })
        return controls
    }

    private fun loadingText() = TextView(this).apply {
        text = "Cargando canciones..."; textSize = 13f; setTextColor(0xFF888888.toInt())
        gravity = Gravity.CENTER; setPadding(0, dp(24), 0, 0)
    }

    private fun buildControlButton(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 28f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(20), dp(8), dp(20), dp(8))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF), null, null
            )
            setOnClickListener { onClick() }
        }
    }

    private fun buildButton(text: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 15f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            setPadding(dp(24), dp(16), dp(24), dp(16)); isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.ColorDrawable(color), null
            )
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener { onClick() }
        }
    }

    private fun refreshSongList() {
        val svc = MusicPlaybackService.instance ?: return
        songList.removeAllViews()
        songButtons.clear()

        if (svc.songs.isEmpty()) {
            val folder = getFolder()
            songList.addView(TextView(this).apply {
                text = if (folder.isEmpty())
                    "Pulsa 📁 para seleccionar la carpeta de música"
                else
                    "No se encontraron canciones en la carpeta seleccionada.\nPulsa 📁 para cambiar la carpeta."
                textSize = 13f; setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER; setPadding(dp(8), dp(24), dp(8), 0)
            })
            return
        }

        svc.songs.forEachIndexed { index, song ->
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                isClickable = true; isFocusable = true
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                    android.graphics.drawable.ColorDrawable(0xFF222222.toInt()), null
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(2) }
            }
            btn.addView(TextView(this).apply {
                text = "${index + 1}"; textSize = 11f; setTextColor(0xFF555555.toInt())
                setPadding(0, 0, dp(10), 0); minWidth = dp(28)
            })
            btn.addView(TextView(this).apply {
                text = song.title; textSize = 13f; setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            })
            btn.setOnClickListener { svc.playSong(index) }
            songButtons.add(btn)
            songList.addView(btn)
        }
        updateUI()
    }

    private fun updateUI() {
        val svc = MusicPlaybackService.instance ?: return
        val song = svc.songs.getOrNull(svc.currentIndex)

        tvNowPlaying.text = song?.title ?: if (isPhone()) "Selecciona una canción" else "Selecciona\nuna canción"
        tvStatus.text = when {
            svc.isPlaying -> "▶ Reproduciendo"
            svc.currentIndex >= 0 -> "⏸ Pausado"
            else -> ""
        }
        btnPlayPause.text = if (svc.isPlaying) "⏸" else "▶"

        // Artwork
        if (svc.currentIndex >= 0) {
            val art: Bitmap? = svc.getCurrentArtwork()
            if (art != null) {
                ivArtwork.setImageBitmap(art)
                ivArtwork.visibility = View.VISIBLE
                tvDefaultIcon.visibility = View.GONE
            } else {
                ivArtwork.visibility = View.GONE
                tvDefaultIcon.visibility = View.VISIBLE
            }
        } else {
            ivArtwork.visibility = View.GONE
            tvDefaultIcon.visibility = View.VISIBLE
        }

        songButtons.forEachIndexed { index, btn ->
            btn.setBackgroundColor(
                if (index == svc.currentIndex) 0xFF1A2A3A.toInt() else 0xFF222222.toInt()
            )
        }
    }

    private fun updateProgress() {
        if (userSeeking) return
        val svc = MusicPlaybackService.instance ?: return
        val pos = svc.getCurrentPosition()
        val dur = svc.getDuration()
        if (dur > 0) {
            seekBar.progress = (pos * 1000 / dur).toInt()
            tvCurrentTime.text = formatTime(pos)
            tvTotalTime.text = formatTime(dur)
        } else {
            seekBar.progress = 0
            tvCurrentTime.text = "0:00"
            tvTotalTime.text = "0:00"
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%d:%02d", min, sec)
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Recargar la lista por si se añadieron/quitaron canciones de la carpeta
        if (getFolder().isNotEmpty()) {
            MusicPlaybackService.instance?.let {
                it.reloadSongs()
                refreshSongList()
            }
        }
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
