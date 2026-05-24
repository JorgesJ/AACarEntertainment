package com.jorso.carapp.auto

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var container: FrameLayout

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            saveFolder(uri.toString())
            // Recargar canciones con la nueva carpeta
            MusicPlaybackService.instance?.reloadSongs()
            refreshSongList()
            showToast("Carpeta guardada ✓")
        }
        // Si cancela, continuar con lo que había
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this).apply {
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
            // Si no hay carpeta configurada, mostrar selector
            val folder = getFolder()
            if (folder.isEmpty()) {
                showFolderPrompt()
            } else {
                refreshSongList()
            }
        }, 800)
    }

    private fun getFolder(): String {
        return getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
            .getString(KEY_MUSIC_FOLDER, "") ?: ""
    }

    private fun saveFolder(uri: String) {
        getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
            .edit().putString(KEY_MUSIC_FOLDER, uri).apply()
    }

    private fun showFolderPrompt() {
        // Overlay que pide seleccionar carpeta
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0xEE111111.toInt())
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF1E1E2E.toInt())
            setPadding(dp(32), dp(32), dp(32), dp(32))
            layoutParams = FrameLayout.LayoutParams(dp(420), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }
        card.addView(TextView(this).apply {
            text = "🎵"
            textSize = 48f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        })
        card.addView(TextView(this).apply {
            text = "Selecciona la carpeta con la música que deseas cargar en la aplicación"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
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

    private fun isPhone(): Boolean {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        return widthDp < 600f
    }

    private fun buildUI(): View {
        return if (isPhone()) buildPhoneUI() else buildCarUI()
    }

    private fun buildPhoneUI(): View {
        // Layout vertical para móvil: header + player compacto + lista canciones
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(16), 0); setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Música"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "📁"; textSize = 22f; setPadding(dp(8), 0, 0, 0)
            setOnClickListener { folderPicker.launch(null) }
        })
        root.addView(header)

        // Player compacto
        val player = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0xFF1A1A2A.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        tvNowPlaying = TextView(this).apply {
            text = "Selecciona una canción"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }
        tvStatus = TextView(this).apply {
            text = ""; textSize = 12f; setTextColor(0xFF4FC3F7.toInt()); gravity = Gravity.CENTER
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        val btnPrev = buildControlButton("⏮") { MusicPlaybackService.instance?.skipToPrevious() }
        btnPlayPause = buildControlButton("▶") {
            val svc = MusicPlaybackService.instance ?: return@buildControlButton
            if (svc.isPlaying) svc.pausePlayback() else svc.resumePlayback()
        }
        val btnNext = buildControlButton("⏭") { MusicPlaybackService.instance?.skipToNext() }
        controls.addView(btnPrev); controls.addView(btnPlayPause); controls.addView(btnNext)
        player.addView(tvNowPlaying); player.addView(tvStatus); player.addView(controls)
        root.addView(player)

        root.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        // Lista canciones
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        songList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        songList.addView(TextView(this).apply {
            text = "Cargando canciones..."; textSize = 13f; setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER; setPadding(0, dp(24), 0, 0)
        })
        scrollView.addView(songList); root.addView(scrollView)
        return root
    }

    private fun buildCarUI(): View {
        // Layout horizontal para coche: lista izquierda + player derecha
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

        val leftHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt()); setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        leftHeader.addView(TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0); setOnClickListener { finish() }
        })
        leftHeader.addView(TextView(this).apply {
            text = "Música"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        leftHeader.addView(TextView(this).apply {
            text = "📁"; textSize = 18f; setPadding(dp(8), 0, 0, 0)
            setOnClickListener { folderPicker.launch(null) }
        })

        leftPanel.addView(leftHeader)
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
        songList.addView(TextView(this).apply {
            text = "Cargando canciones..."; textSize = 13f; setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER; setPadding(0, dp(24), 0, 0)
        })
        scrollView.addView(songList); leftPanel.addView(scrollView)

        val rightPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val tvIcon = TextView(this).apply { text = "🎵"; textSize = 48f; gravity = Gravity.CENTER }
        val tvLabel = TextView(this).apply {
            text = "REPRODUCIENDO"; textSize = 10f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            letterSpacing = 0.15f; setPadding(0, dp(16), 0, dp(8))
        }
        tvNowPlaying = TextView(this).apply {
            text = "Selecciona\nuna canción"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(0, 0, 0, dp(8))
        }
        tvStatus = TextView(this).apply {
            text = ""; textSize = 13f; setTextColor(0xFF4FC3F7.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(16))
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        }
        val btnPrev = buildControlButton("⏮") { MusicPlaybackService.instance?.skipToPrevious() }
        btnPlayPause = buildControlButton("▶") {
            val svc = MusicPlaybackService.instance ?: return@buildControlButton
            if (svc.isPlaying) svc.pausePlayback() else svc.resumePlayback()
        }
        val btnNext = buildControlButton("⏭") { MusicPlaybackService.instance?.skipToNext() }
        controls.addView(btnPrev); controls.addView(btnPlayPause); controls.addView(btnNext)

        rightPanel.addView(tvIcon); rightPanel.addView(tvLabel)
        rightPanel.addView(tvNowPlaying); rightPanel.addView(tvStatus); rightPanel.addView(controls)

        root.addView(leftPanel); root.addView(rightPanel)
        return root
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
            setPadding(dp(24), dp(16), dp(24), dp(16))
            isClickable = true; isFocusable = true
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
        songList.removeAllViews(); songButtons.clear()

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
            songButtons.add(btn); songList.addView(btn)
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

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
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
