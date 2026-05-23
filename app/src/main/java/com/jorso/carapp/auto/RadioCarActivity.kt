package com.jorso.carapp.auto

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jorso.carapp.R

class RadioCarActivity : AppCompatActivity() {

    data class Station(
        val name: String,
        val url: String,
        val freq: String,
        @DrawableRes val logo: Int
    )

    private val stations = listOf(
        // Generalistas
        Station("COPE",    "https://flucast09-h-cloud.flumotion.com/cope/net1.aac", "Generalistas", R.drawable.radio_cope),
        Station("esRadio", "http://livestreaming.esradio.fm/stream64.mp3",          "Generalistas", R.drawable.radio_esradio),

        // Música
        Station("Los 40",      "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_SC",       "Música", R.drawable.radio_los40),
        Station("Cadena Dial", "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENADIAL.mp3", "Música", R.drawable.radio_cadenadial),
        Station("Cadena 100",  "https://cadena100-streamers-mp3.flumotion.com/cope/cadena100.mp3",                "Música", R.drawable.radio_cadena100),
        Station("KISS FM",     "https://kissfm.kissfmradio.cires21.com/kissfm.mp3",                               "Música", R.drawable.radio_kissfm),
        Station("Hit FM",      "https://hitfm.kissfmradio.cires21.com/hitfm.mp3",                                 "Música", R.drawable.radio_hitfm),
    )

    private var player: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentStation: Station? = null

    private lateinit var tvNowPlaying: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var stationLogo: ImageView
    private val stationButtons = mutableListOf<LinearLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUI())
        initPlayer()
    }

    private fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Panel izquierdo — lista de emisoras
        val leftPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A1A.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(260), ViewGroup.LayoutParams.MATCH_PARENT)
        }

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
            text = "Radio"
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
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val stationList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val groups = stations.groupBy { it.freq }
        groups.forEach { (groupName, groupStations) ->
            stationList.addView(TextView(this).apply {
                text = groupName
                textSize = 11f
                setTextColor(0xFF81C784.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                letterSpacing = 0.12f
                setPadding(dp(16), dp(12), dp(16), dp(4))
            })
            stationList.addView(View(this).apply {
                setBackgroundColor(0xFF333333.toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                    bottomMargin = dp(4)
                }
            })
            groupStations.forEach { station ->
                val btn = buildStationButton(station)
                stationButtons.add(btn)
                stationList.addView(btn)
            }
        }

        scrollView.addView(stationList)
        leftPanel.addView(scrollView)

        // Panel derecho — player
        val rightPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        // Logo de la emisora
        stationLogo = ImageView(this).apply {
            setImageResource(R.drawable.radio_kissfm) // placeholder inicial
            visibility = View.INVISIBLE
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(160), dp(160))
        }

        val tvLabel = TextView(this).apply {
            text = "EN ANTENA"
            textSize = 10f
            setTextColor(0xFF81C784.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.15f
            setPadding(0, dp(16), 0, dp(8))
        }

        tvNowPlaying = TextView(this).apply {
            text = "Selecciona\nuna emisora"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }

        tvStatus = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(0xFF81C784.toInt())
            gravity = Gravity.CENTER
        }

        progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                topMargin = dp(12)
            }
        }

        rightPanel.addView(stationLogo)
        rightPanel.addView(tvLabel)
        rightPanel.addView(tvNowPlaying)
        rightPanel.addView(tvStatus)
        rightPanel.addView(progressBar)

        root.addView(leftPanel)
        root.addView(rightPanel)
        return root
    }

    private fun buildStationButton(station: Station): LinearLayout {
        val btn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.ColorDrawable(0xFF222222.toInt()), null
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }

        val tvName = TextView(this).apply {
            text = station.name
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 1
        }

        val tvFreq = TextView(this).apply {
            text = station.freq
            textSize = 12f
            setTextColor(0xFF81C784.toInt())
            setPadding(0, dp(2), 0, 0)
        }

        btn.addView(tvName)
        btn.addView(tvFreq)

        btn.setOnClickListener {
            if (currentStation?.url == station.url) stopPlayback()
            else playStation(station)
        }

        return btn
    }

    private fun initPlayer() {
        requestAudioFocus()
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                runOnUiThread { updateUI(state) }
            }
            override fun onPlayerError(error: PlaybackException) {
                runOnUiThread {
                    tvStatus.text = "Error de conexión"
                    progressBar.visibility = View.GONE
                    currentStation = null
                    stationLogo.visibility = View.INVISIBLE
                    updateStationButtons()
                }
            }
        })
    }

    private fun playStation(station: Station) {
        currentStation = station
        tvNowPlaying.text = station.name
        tvStatus.text = "Conectando..."
        progressBar.visibility = View.VISIBLE
        stationLogo.setImageResource(station.logo)
        stationLogo.visibility = View.VISIBLE
        updateStationButtons()
        player?.stop()
        player?.clearMediaItems()
        player?.setMediaItem(MediaItem.fromUri(Uri.parse(station.url)))
        player?.prepare()
        player?.play()
    }

    private fun stopPlayback() {
        player?.stop()
        player?.clearMediaItems()
        currentStation = null
        tvNowPlaying.text = "Selecciona\nuna emisora"
        tvStatus.text = ""
        progressBar.visibility = View.GONE
        stationLogo.visibility = View.INVISIBLE
        updateStationButtons()
    }

    private fun updateUI(state: Int) {
        when (state) {
            Player.STATE_BUFFERING -> {
                tvStatus.text = "Cargando..."
                progressBar.visibility = View.VISIBLE
            }
            Player.STATE_READY -> {
                tvStatus.text = "▶ Reproduciendo"
                progressBar.visibility = View.GONE
            }
            Player.STATE_IDLE, Player.STATE_ENDED -> {
                tvStatus.text = ""
                progressBar.visibility = View.GONE
            }
        }
        updateStationButtons()
    }

    private fun updateStationButtons() {
        var buttonIndex = 0
        val groups = stations.groupBy { it.freq }
        groups.forEach { (_, groupStations) ->
            groupStations.forEach { station ->
                val isPlaying = currentStation?.url == station.url
                stationButtons.getOrNull(buttonIndex)?.setBackgroundColor(
                    if (isPlaying) 0xFF1A2A1A.toInt() else 0xFF222222.toInt()
                )
                buttonIndex++
            }
        }
    }

    private fun requestAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { focus ->
                if (focus == AudioManager.AUDIOFOCUS_LOSS) runOnUiThread { stopPlayback() }
            }
            .build()
        audioManager?.requestAudioFocus(audioFocusRequest!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        player?.release()
        player = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
