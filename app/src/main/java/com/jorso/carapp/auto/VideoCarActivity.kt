package com.jorso.carapp.auto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

class VideoCarActivity : AppCompatActivity() {

    data class VideoItem(val title: String, val uri: Uri, val path: String)

    private var player: ExoPlayer? = null
    private var audioManager: android.media.AudioManager? = null
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private var videos = mutableListOf<VideoItem>()
    private var currentIndex = -1
    private val handler = Handler(Looper.getMainLooper())
    private val videoButtons = mutableListOf<LinearLayout>()

    private lateinit var surfaceView: SurfaceView
    private lateinit var tvNowPlaying: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnPlayPause: TextView
    private lateinit var videoList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUI())
        initPlayer()
        loadVideos()
    }

    private fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF000000.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Panel izquierdo — lista videos
        val leftPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A1A.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val leftHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        leftHeader.addView(TextView(this).apply {
            text = "←"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0)
            setOnClickListener { finish() }
        })

        leftHeader.addView(TextView(this).apply {
            text = "Video"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

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

        videoList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        videoList.addView(TextView(this).apply {
            text = "Cargando videos..."
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })

        scrollView.addView(videoList)
        leftPanel.addView(scrollView)

        // Panel derecho — reproductor
        val rightPanel = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setBackgroundColor(0xFF000000.toInt())
        }

        surfaceView = SurfaceView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                player?.setVideoSurface(holder.surface)
            }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                player?.setVideoSurface(null)
            }
        })

        // Info overlay
        val infoOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        tvNowPlaying = TextView(this).apply {
            text = ""
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        tvStatus = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                topMargin = dp(12)
            }
        }

        infoOverlay.addView(progressBar)
        infoOverlay.addView(tvNowPlaying)
        infoOverlay.addView(tvStatus)

        // Controles
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xCC000000.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        }

        val btnPrev = buildControlButton("⏮") {
            if (currentIndex > 0) playVideo(currentIndex - 1)
        }

        btnPlayPause = buildControlButton("⏸") {
            if (player?.isPlaying == true) player?.pause() else player?.play()
        }

        val btnNext = buildControlButton("⏭") {
            if (currentIndex < videos.size - 1) playVideo(currentIndex + 1)
        }

        val tvCurrentVideo = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(8), 0, 0, 0)
        }
        tvNowPlaying = tvCurrentVideo

        controls.addView(btnPrev)
        controls.addView(btnPlayPause)
        controls.addView(btnNext)
        controls.addView(tvCurrentVideo)

        rightPanel.addView(surfaceView)
        rightPanel.addView(infoOverlay)
        rightPanel.addView(controls)

        root.addView(leftPanel)
        root.addView(rightPanel)
        return root
    }

    private fun buildControlButton(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(16), dp(8), dp(16), dp(8))
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                null, null
            )
            setOnClickListener { onClick() }
        }
    }

    private fun initPlayer() {
        audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val attrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        audioFocusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { focus ->
                if (focus == android.media.AudioManager.AUDIOFOCUS_LOSS) player?.pause()
            }
            .build()
        audioManager?.requestAudioFocus(audioFocusRequest!!)
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                runOnUiThread {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            tvStatus.text = "Cargando..."
                            progressBar.visibility = View.VISIBLE
                        }
                        Player.STATE_READY -> {
                            tvStatus.text = ""
                            progressBar.visibility = View.GONE
                            btnPlayPause.text = "⏸"
                        }
                        Player.STATE_ENDED -> {
                            if (currentIndex < videos.size - 1) playVideo(currentIndex + 1)
                        }
                        else -> {
                            tvStatus.text = ""
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                runOnUiThread { btnPlayPause.text = if (isPlaying) "⏸" else "▶" }
            }
            override fun onPlayerError(error: PlaybackException) {
                runOnUiThread {
                    tvStatus.text = "Error al reproducir"
                    progressBar.visibility = View.GONE
                }
            }
        })
    }

    private fun playVideo(index: Int) {
        if (index < 0 || index >= videos.size) return
        currentIndex = index
        val video = videos[index]
        tvNowPlaying.text = video.title
        tvStatus.text = "Cargando..."
        progressBar.visibility = View.VISIBLE
        updateVideoButtons()
        player?.stop()
        player?.clearMediaItems()
        player?.setMediaItem(MediaItem.fromUri(video.uri))
        player?.prepare()
        player?.play()
    }

    private fun loadVideos() {
        Thread {
            val dirs = listOf(
                File("/storage/emulated/0/Movies"),
                )

            val found = mutableListOf<VideoItem>()
            dirs.forEach { dir ->
                if (dir.exists()) {
                    dir.walkTopDown().filter { file ->
                        file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "m4v", "3gp", "webm")
                    }.forEach { file ->
                        found.add(VideoItem(
                            title = file.nameWithoutExtension,
                            uri = Uri.fromFile(file),
                            path = file.absolutePath
                        ))
                    }
                }
            }

            found.sortBy { it.title }
            videos.addAll(found)

            runOnUiThread { refreshVideoList() }
        }.start()
    }

    private fun refreshVideoList() {
        videoList.removeAllViews()
        videoButtons.clear()

        if (videos.isEmpty()) {
            videoList.addView(TextView(this).apply {
                text = "No se encontraron videos\nen Movies, DCIM o Download"
                textSize = 13f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(24), dp(8), 0)
            })
            return
        }

        videos.forEachIndexed { index, video ->
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
                text = video.title
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            btn.addView(tvNum)
            btn.addView(tvName)
            btn.setOnClickListener { playVideo(index) }
            videoButtons.add(btn)
            videoList.addView(btn)
        }
    }

    private fun updateVideoButtons() {
        videoButtons.forEachIndexed { index, btn ->
            btn.setBackgroundColor(
                if (index == currentIndex) 0xFF1A2A3A.toInt() else 0xFF222222.toInt()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        player?.release()
        player = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}




