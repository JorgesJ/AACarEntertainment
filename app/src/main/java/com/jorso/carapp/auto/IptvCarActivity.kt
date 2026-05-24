package com.jorso.carapp.auto

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class IptvCarActivity : AppCompatActivity() {

    data class Channel(val name: String, val url: String, val group: String)
    data class Playlist(val name: String, val url: String, val isFile: Boolean = false)
    data class Group(val name: String, val channels: List<Channel>)

    companion object {
        const val PREFS_NAME = "iptv_prefs"
        const val KEY_PLAYLISTS = "playlists"
        const val DEFAULT_URL = "https://www.tdtchannels.com/lists/tv.m3u8"
        const val DEFAULT_NAME = "TDT España"
        const val GROUP_ALL = "Todos"
    }

    private var playlists = mutableListOf<Playlist>()
    private var allChannels = mutableListOf<Channel>()
    private var groups = mutableListOf<Group>()
    private var currentChannels = mutableListOf<Channel>()
    private var currentChannel: Channel? = null
    private var currentPlaylist: Playlist? = null
    private var screen = "home"

    private var player: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private lateinit var container: FrameLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var tvChannelName: TextView
    private lateinit var tvPlayerStatus: TextView
    private lateinit var progressBarPlayer: ProgressBar
    private val channelButtons = mutableListOf<View>()

    private var addNameField: EditText? = null
    private var addUrlField: EditText? = null
    private var pendingFileUri: String? = null

    private var isFullscreen = false
    private var leftPanelRef: LinearLayout? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> pendingFileUri = uri.toString() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPlaylists()
        initPlayer()
        container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF111111.toInt())
        }
        container.fitsSystemWindows = true
        setContentView(container)
        showHome()
    }

    private fun loadPlaylists() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PLAYLISTS, null)
        playlists.clear()
        if (json != null) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    playlists.add(Playlist(
                        obj.getString("name"),
                        obj.getString("url"),
                        obj.optBoolean("isFile", false)
                    ))
                }
            } catch (e: Exception) {}
        }
        if (playlists.isEmpty()) {
            playlists.add(Playlist(DEFAULT_NAME, DEFAULT_URL))
            savePlaylists()
        }
    }

    private fun savePlaylists() {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject()
            obj.put("name", pl.name)
            obj.put("url", pl.url)
            obj.put("isFile", pl.isFile)
            arr.put(obj)
        }
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    private fun getClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
    }

    private fun showHome() {
        screen = "home"
        container.removeAllViews()
        container.addView(buildHomeUI())
    }

    private fun showGroups(playlist: Playlist) {
        screen = "groups"
        currentPlaylist = playlist
        container.removeAllViews()
        container.addView(buildLoadingUI(playlist.name))
        loadChannelsForGroups(playlist)
    }

    private fun showGroupChannels(group: Group) {
        screen = "player"
        currentChannels.clear()
        currentChannels.addAll(group.channels)
        container.removeAllViews()
        container.addView(buildPlayerUI(group.name))
    }

    private fun showAddPlaylist() {
        screen = "add"
        pendingFileUri = null
        container.removeAllViews()
        container.addView(buildAddPlaylistUI())
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when (screen) {
            "player" -> {
                if (isFullscreen) {
                    isFullscreen = false
                    leftPanelRef?.visibility = View.VISIBLE
                    surfaceView.setOnClickListener(null)
                } else {
                    showGroupsScreen()
                }
            }
            "groups" -> showHome()
            "add" -> showHome()
            else -> super.onBackPressed()
        }
    }

    private fun showGroupsScreen() {
        screen = "groups"
        isFullscreen = false
        leftPanelRef = null
        container.removeAllViews()
        container.addView(buildGroupsUI())
    }

    private fun buildHomeUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(headerPadding(), dp(12), dp(16), dp(12))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "IPTV — Mis Listas"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "+ Añadir"; textSize = 14f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener { showAddPlaylist() }
        })
        root.addView(header)
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        if (playlists.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No hay listas. Pulsa + Añadir para agregar una."
                textSize = 14f; setTextColor(0xFF888888.toInt()); gravity = Gravity.CENTER
                setPadding(0, dp(32), 0, 0)
            })
        } else {
            playlists.forEachIndexed { index, playlist -> list.addView(buildPlaylistItem(playlist, index)) }
        }
        scrollView.addView(list)
        root.addView(scrollView)
        return root
    }

    private fun buildPlaylistItem(playlist: Playlist, index: Int): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.ColorDrawable(0xFF1E1E1E.toInt()), null
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        val tvInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvInfo.addView(TextView(this).apply {
            text = if (playlist.isFile) "📁" else "🌐"
            textSize = 20f; setPadding(0, 0, dp(16), 0)
        })
        tvInfo.addView(TextView(this).apply {
            text = playlist.name; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        tvInfo.addView(TextView(this).apply {
            text = if (playlist.isFile) "Archivo local" else playlist.url
            textSize = 11f; setTextColor(0xFF666666.toInt()); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        })
        item.addView(tvInfo)
        item.addView(TextView(this).apply {
            text = "🗑"; textSize = 18f; setPadding(dp(16), 0, dp(8), 0)
            setOnClickListener { playlists.removeAt(index); savePlaylists(); showHome() }
        })
        item.addView(TextView(this).apply {
            text = "▶"; textSize = 18f; setTextColor(0xFF4FC3F7.toInt())
        })
        item.setOnClickListener { showGroups(playlist) }
        return item
    }

    private fun buildLoadingUI(title: String): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(buildHeader(title) { showHome() })
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        content.addView(ProgressBar(this).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        })
        content.addView(TextView(this).apply {
            text = "Cargando lista..."; textSize = 14f; setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER; setPadding(0, dp(16), 0, 0)
        })
        root.addView(content)
        return root
    }

    private fun loadChannelsForGroups(playlist: Playlist) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val content = if (playlist.isFile) {
                    contentResolver.openInputStream(Uri.parse(playlist.url))?.bufferedReader()?.readText() ?: ""
                } else {
                    val url = java.net.URL(playlist.url)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 10000; conn.readTimeout = 15000
                    val text = conn.inputStream.bufferedReader().readText()
                    conn.disconnect(); text
                }
                val parsed = parseM3U(content)
                withContext(Dispatchers.Main) {
                    allChannels.clear(); allChannels.addAll(parsed)
                    buildGroupsFromChannels()
                    container.removeAllViews()
                    container.addView(buildGroupsUI())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showToast("Error al cargar la lista"); showHome() }
            }
        }
    }

    private fun buildGroupsFromChannels() {
        val groupMap = linkedMapOf<String, MutableList<Channel>>()
        allChannels.forEach { ch ->
            val g = ch.group.ifEmpty { "Sin grupo" }
            groupMap.getOrPut(g) { mutableListOf() }.add(ch)
        }
        groups.clear()
        groups.add(Group(GROUP_ALL, allChannels.toList()))
        groupMap.forEach { (name, channels) -> groups.add(Group(name, channels)) }
    }

    private fun buildGroupsUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(buildHeader(currentPlaylist?.name ?: "IPTV") { showHome() })
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val grid = GridLayout(this).apply {
            columnCount = 3; setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        groups.forEach { group -> grid.addView(buildGroupCard(group)) }
        scrollView.addView(grid); root.addView(scrollView)
        return root
    }

    private fun buildGroupCard(group: Group): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(8), dp(16), dp(8), dp(16))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.ColorDrawable(0xFF1A237E.toInt()), null
            )
            val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = GridLayout.LayoutParams(spec, spec).apply {
                width = 0; height = dp(110); setMargins(dp(4), dp(4), dp(4), dp(4))
            }
        }
        card.addView(TextView(this).apply { text = "📺"; textSize = 18f; gravity = Gravity.CENTER })
        card.addView(TextView(this).apply {
            text = "${group.channels.size}"; textSize = 16f; setTextColor(0xFFBBDEFB.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(2))
        })
        card.addView(TextView(this).apply {
            text = group.name; textSize = 10f; setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.setOnClickListener { showGroupChannels(group) }
        return card
    }

    private fun buildPlayerUI(groupName: String): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFF000000.toInt())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val leftPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF111111.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        leftPanelRef = leftPanel

        val leftHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt()); setPadding(headerPadding(), dp(12), dp(12), dp(12))
        }
        leftHeader.addView(TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 0, dp(8), 0)
            setOnClickListener { player?.stop(); player?.clearMediaItems(); showGroupsScreen() }
        })
        leftHeader.addView(TextView(this).apply {
            text = groupName; textSize = 14f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        leftPanel.addView(leftHeader)
        leftPanel.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val channelListView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        channelButtons.clear()
        currentChannels.forEach { channel ->
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                isClickable = true; isFocusable = true; tag = channel
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                    android.graphics.drawable.ColorDrawable(0xFF1A1A1A.toInt()), null
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(2) }
            }
            btn.addView(TextView(this).apply {
                text = channel.name; textSize = 13f; setTextColor(0xFFFFFFFF.toInt())
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            })
            btn.setOnClickListener { playChannel(channel, channelListView) }
            channelButtons.add(btn); channelListView.addView(btn)
        }
        scrollView.addView(channelListView); leftPanel.addView(scrollView)

        val rightPanel = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setBackgroundColor(0xFF000000.toInt())
        }
        surfaceView = SurfaceView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) { player?.setVideoSurface(holder.surface) }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) { player?.setVideoSurface(null) }
        })

        val infoOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        tvChannelName = TextView(this).apply {
            text = ""; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
        }
        tvPlayerStatus = TextView(this).apply {
            text = ""; textSize = 13f; setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0)
        }
        progressBarPlayer = ProgressBar(this).apply {
            isIndeterminate = true; visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { topMargin = dp(12) }
        }
        infoOverlay.addView(progressBarPlayer); infoOverlay.addView(tvChannelName); infoOverlay.addView(tvPlayerStatus)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xCC000000.toInt()); setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            )
        }
        val tvCurrentCh = TextView(this).apply {
            text = ""; textSize = 12f; setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(4), 0, dp(4), 0)
        }
        tvChannelName = tvCurrentCh

        val btnPrev = buildControlButton("⏮") {
            val idx = currentChannels.indexOf(currentChannel)
            if (idx > 0) playChannel(currentChannels[idx - 1], channelListView)
        }
        val btnPlayPause = buildControlButton("⏸") {
            if (player?.isPlaying == true) player?.pause() else player?.play()
        }
        val btnNext = buildControlButton("⏭") {
            val idx = currentChannels.indexOf(currentChannel)
            if (idx < currentChannels.size - 1) playChannel(currentChannels[idx + 1], channelListView)
        }
        val btnAspect = buildControlButton("⛶") { showAspectMenu() }

        controls.addView(tvCurrentCh)
        controls.addView(btnPrev)
        controls.addView(btnPlayPause)
        controls.addView(btnNext)
        controls.addView(btnAspect)

        rightPanel.addView(surfaceView)
        rightPanel.addView(infoOverlay)
        rightPanel.addView(controls)

        root.addView(leftPanel)
        root.addView(rightPanel)
        return root
    }

    private fun showAspectMenu() {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0xBB000000.toInt())
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1E1E2E.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = FrameLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }

        data class Option(val label: String, val action: () -> Unit)
        val options = listOf(
            Option("⛶  Pantalla completa") {
                isFullscreen = true
                leftPanelRef?.visibility = View.GONE
                applyAspect("FIT")
                surfaceView.setOnClickListener {
                    isFullscreen = false
                    leftPanelRef?.visibility = View.VISIBLE
                    surfaceView.setOnClickListener(null)
                }
            },
            Option("▭  Ajustado (por defecto)") { isFullscreen = false; leftPanelRef?.visibility = View.VISIBLE; applyAspect("FIT") },
            Option("▬  16:9") { applyAspect("16:9") },
            Option("▪  4:3") { applyAspect("4:3") },
            Option("🔍  Zoom") { applyAspect("ZOOM") },
            Option("✕  Cancelar") { }
        )

        options.forEach { option ->
            menu.addView(TextView(this).apply {
                text = option.label
                textSize = 15f
                setTextColor(if (option.label.contains("Cancelar")) 0xFF888888.toInt() else 0xFFFFFFFF.toInt())
                setPadding(dp(16), dp(14), dp(16), dp(14))
                isClickable = true; isFocusable = true
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                    android.graphics.drawable.ColorDrawable(0xFF1E1E2E.toInt()), null
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(2) }
                setOnClickListener {
                    option.action()
                    container.removeView(overlay)
                }
            })
        }

        overlay.addView(menu)
        overlay.setOnClickListener { container.removeView(overlay) }
        container.addView(overlay)
    }

    private fun applyAspect(ratio: String) {
        val params = surfaceView.layoutParams as FrameLayout.LayoutParams
        val parent = surfaceView.parent as FrameLayout
        when (ratio) {
            "FIT" -> {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.CENTER
            }
            "16:9" -> {
                val w = parent.width
                params.width = w
                params.height = w * 9 / 16
                params.gravity = Gravity.CENTER
            }
            "4:3" -> {
                val w = parent.width
                params.width = w
                params.height = w * 3 / 4
                params.gravity = Gravity.CENTER
            }
            "ZOOM" -> {
                params.width = (parent.width * 1.3).toInt()
                params.height = (parent.height * 1.3).toInt()
                params.gravity = Gravity.CENTER
            }
        }
        surfaceView.layoutParams = params
    }

    private fun buildAddPlaylistUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(buildHeader("Añadir Lista IPTV") { showHome() })

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        // Recuadro portapapeles en tiempo real
        val tvClipLabel = TextView(this).apply {
            text = "📋 Portapapeles"
            textSize = 12f; setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, dp(4))
        }
        val tvClipContent = TextView(this).apply {
            text = getClipboardText().ifEmpty { "— vacío —" }
            textSize = 13f
            setTextColor(if (getClipboardText().isNotEmpty()) 0xFF4FC3F7.toInt() else 0xFF555555.toInt())
            setBackgroundColor(0xFF1A1A2A.toInt())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Sin listener continuo — solo lee al pulsar los botones para evitar el punto rojo del sistema

        // Botones usar portapapeles como nombre o URL
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(20) }
        }
        val btnUseAsName = TextView(this).apply {
            text = "→ Usar como nombre"
            textSize = 12f; setTextColor(0xFF81C784.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x3381C784),
                android.graphics.drawable.ColorDrawable(0xFF1A2A1A.toInt()), null
            )
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
            setOnClickListener {
                val text = getClipboardText()
                if (text.isNotEmpty()) { addNameField?.setText(text); showToast("Nombre pegado") }
                else showToast("Portapapeles vacío")
            }
        }
        val btnUseAsUrl = TextView(this).apply {
            text = "→ Usar como URL"
            textSize = 12f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x334FC3F7),
                android.graphics.drawable.ColorDrawable(0xFF1A2A3A.toInt()), null
            )
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) }
            setOnClickListener {
                val text = getClipboardText()
                if (text.isNotEmpty()) { addUrlField?.setText(text); showToast("URL pegada") }
                else showToast("Portapapeles vacío")
            }
        }
        btnRow.addView(btnUseAsName)
        btnRow.addView(btnUseAsUrl)

        content.addView(tvClipLabel)
        content.addView(tvClipContent)
        content.addView(btnRow)

        // Campo nombre
        content.addView(TextView(this).apply {
            text = "Nombre de la lista"; textSize = 13f; setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, dp(6))
        })
        addNameField = EditText(this).apply {
            hint = "Mi lista de IPTV"; setHintTextColor(0xFF555555.toInt()); setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E1E1E.toInt()); textSize = 15f; setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(20) }
        }
        content.addView(addNameField!!)

        // Campo URL
        content.addView(TextView(this).apply {
            text = "URL de la lista (M3U / M3U8)"; textSize = 13f; setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, dp(6))
        })
        addUrlField = EditText(this).apply {
            hint = "https://ejemplo.com/lista.m3u8"; setHintTextColor(0xFF555555.toInt()); setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E1E1E.toInt()); textSize = 13f; setPadding(dp(16), dp(12), dp(16), dp(12))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }
        content.addView(addUrlField!!)

        // Separador
        content.addView(TextView(this).apply {
            text = "— O —"; textSize = 13f; setTextColor(0xFF555555.toInt()); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8); bottomMargin = dp(16)
            }
        })

        content.addView(buildActionButton("📁  Seleccionar archivo M3U del móvil", 0xFF263238.toInt()) {
            filePicker.launch(Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) })
        })

        val btnSave = buildActionButton("✓  Guardar lista", 0xFF1A237E.toInt()) { saveNewPlaylist() }
        (btnSave.layoutParams as LinearLayout.LayoutParams).topMargin = dp(24)
        content.addView(btnSave)

        scrollView.addView(content); root.addView(scrollView)
        return root
    }



    private fun saveNewPlaylist() {
        val name = addNameField?.text?.toString()?.trim() ?: ""
        val url = addUrlField?.text?.toString()?.trim() ?: ""
        val fileUri = pendingFileUri
        if (name.isEmpty()) { showToast("Introduce un nombre"); return }
        if (url.isEmpty() && fileUri == null) { showToast("Introduce una URL o selecciona archivo"); return }
        playlists.add(if (fileUri != null) Playlist(name, fileUri, true) else Playlist(name, url))
        savePlaylists(); showToast("Lista '$name' guardada"); showHome()
    }

    private fun buildHeader(title: String, onBack: () -> Unit): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt()); setPadding(headerPadding(), dp(12), dp(16), dp(12))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 0, dp(12), 0)
            setOnClickListener { onBack() }
        })
        header.addView(TextView(this).apply {
            text = title; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        return header
    }

    private fun buildControlButton(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(6), dp(12), dp(6)); isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF), null, null
            )
            setOnClickListener { onClick() }
        }
    }

    private fun buildActionButton(text: String, color: Int, onClick: () -> Unit): TextView {
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

    private fun initPlayer() {
        requestAudioFocus()
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                runOnUiThread {
                    when (state) {
                        Player.STATE_BUFFERING -> { tvPlayerStatus.text = "Cargando..."; progressBarPlayer.visibility = View.VISIBLE }
                        Player.STATE_READY -> { tvPlayerStatus.text = ""; progressBarPlayer.visibility = View.GONE }
                        else -> { tvPlayerStatus.text = ""; progressBarPlayer.visibility = View.GONE }
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                runOnUiThread { tvPlayerStatus.text = "Error al reproducir"; progressBarPlayer.visibility = View.GONE }
            }
        })
    }

    private fun playChannel(channel: Channel, channelListView: LinearLayout) {
        currentChannel = channel
        tvChannelName.text = channel.name
        tvPlayerStatus.text = "Conectando..."
        progressBarPlayer.visibility = View.VISIBLE
        updateChannelButtons(channelListView)
        player?.stop(); player?.clearMediaItems()
        player?.setMediaItem(MediaItem.fromUri(channel.url))
        player?.prepare(); player?.play()
    }

    private fun updateChannelButtons(channelListView: LinearLayout) {
        for (i in 0 until channelListView.childCount) {
            val btn = channelListView.getChildAt(i)
            btn.setBackgroundColor(if (btn.tag == currentChannel) 0xFF1E2E1E.toInt() else 0xFF1A1A1A.toInt())
        }
    }

    private fun parseM3U(content: String): List<Channel> {
        val result = mutableListOf<Channel>()
        val lines = content.lines()
        var currentName = ""
        var currentGroup = ""
        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    currentName = line.substringAfterLast(",").trim()
                    currentGroup = if (line.contains("group-title=\""))
                        line.substringAfter("group-title=\"").substringBefore("\"") else ""
                }
                (line.startsWith("http") || line.startsWith("rtmp") || line.startsWith("rtsp")) && currentName.isNotEmpty() -> {
                    result.add(Channel(currentName, line.trim(), currentGroup))
                    currentName = ""; currentGroup = ""
                }
            }
        }
        return result
    }

    private fun requestAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { focus ->
                if (focus == AudioManager.AUDIOFOCUS_LOSS) runOnUiThread { player?.pause() }
            }.build()
        audioManager?.requestAudioFocus(audioFocusRequest!!)
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        player?.release(); player = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun headerPadding(): Int {
        // En Android Auto el sistema pone un botón nativo a la izquierda que ocupa ~120dp
        // En el móvil no existe ese botón, usamos padding normal
        return try {
            val carContext = Class.forName("androidx.car.app.connection.CarConnection")
            val conn = carContext.getConstructor(android.content.Context::class.java).newInstance(this)
            val typeLive = carContext.getMethod("getType").invoke(conn)
            val type = (typeLive as? androidx.lifecycle.LiveData<*>)?.value as? Int ?: 0
            if (type != 0) dp(120) else dp(16)
        } catch (e: Exception) {
            dp(16)
        }
    }
}
