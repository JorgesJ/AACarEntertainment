package com.jorso.carapp.auto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

class MusicPlaybackService : MediaBrowserServiceCompat() {

    companion object {
        const val ROOT_ID = "music_root"
        const val CHANNEL_ID = "music_playback"
        const val NOTIFICATION_ID = 1001
        const val PREFS_CONFIG = "app_config"
        const val KEY_MUSIC_FOLDER = "music_folder"
        var instance: MusicPlaybackService? = null
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var player: ExoPlayer? = null
    var songs: List<SongItem> = emptyList()
    var currentIndex: Int = -1
    var onStateChanged: (() -> Unit)? = null

    data class SongItem(val title: String, val uri: Uri, val path: String)

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focus ->
        when (focus) {
            AudioManager.AUDIOFOCUS_LOSS -> pausePlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
            AudioManager.AUDIOFOCUS_GAIN -> resumePlayback()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()

        mediaSession = MediaSessionCompat(this, "MusicPlaybackService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { resumePlayback() }
                override fun onPause() { pausePlayback() }
                override fun onSkipToNext() { skipToNext() }
                override fun onSkipToPrevious() { skipToPrevious() }
                override fun onSkipToQueueItem(id: Long) { playSong(id.toInt()) }
                override fun onStop() { stopPlayback() }
            })
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    updatePlaybackState()
                    onStateChanged?.invoke()
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlaybackState()
                    onStateChanged?.invoke()
                }
            })
        }

        loadSongs()
        createNotificationChannel()
    }

    private fun loadSongs() {
        // Leer carpeta desde SharedPreferences (configurada desde MainActivity)
        val prefs = getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
        val folderUri = prefs.getString(KEY_MUSIC_FOLDER, "") ?: ""

        songs = if (folderUri.isNotEmpty() && folderUri.startsWith("content://")) {
            // Carpeta seleccionada con el selector de documentos
            loadSongsFromContentUri(Uri.parse(folderUri))
        } else if (folderUri.isNotEmpty()) {
            // Ruta directa (legacy)
            loadSongsFromFile(File(folderUri))
        } else {
            // Sin carpeta configurada — intentar carpeta Music por defecto
            loadSongsFromFile(File("/storage/emulated/0/Music"))
        }
        onStateChanged?.invoke()
    }

    private fun loadSongsFromFile(dir: File): List<SongItem> {
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown().filter { file ->
            file.extension.lowercase() in listOf("mp3", "wav", "flac", "m4a", "ogg")
        }.map { file ->
            SongItem(
                title = file.nameWithoutExtension,
                uri = Uri.fromFile(file),
                path = file.absolutePath
            )
        }.sortedBy { it.title }.toList()
    }

    private fun loadSongsFromContentUri(treeUri: Uri): List<SongItem> {
        val result = mutableListOf<SongItem>()
        try {
            val docId = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, treeUri)
            docId?.listFiles()?.forEach { file ->
                if (file.isFile && file.name?.substringAfterLast(".")?.lowercase() in
                    listOf("mp3", "wav", "flac", "m4a", "ogg")) {
                    result.add(SongItem(
                        title = file.name?.substringBeforeLast(".") ?: "Sin título",
                        uri = file.uri,
                        path = file.uri.toString()
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error leyendo carpeta: ${e.message}")
        }
        return result.sortedBy { it.title }
    }

    fun reloadSongs() {
        loadSongs()
    }

    fun playSong(index: Int) {
        if (index < 0 || index >= songs.size) return
        val focus = audioManager.requestAudioFocus(audioFocusRequest)
        if (focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        currentIndex = index
        player?.apply {
            setMediaItem(MediaItem.fromUri(songs[index].uri))
            prepare()
            play()
        }
        updateMetadata()
        updatePlaybackState()
        startForegroundNotification()
        onStateChanged?.invoke()
    }

    fun pausePlayback() {
        player?.pause()
        updatePlaybackState()
        onStateChanged?.invoke()
    }

    fun resumePlayback() {
        val focus = audioManager.requestAudioFocus(audioFocusRequest)
        if (focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        player?.play()
        updatePlaybackState()
        onStateChanged?.invoke()
    }

    fun stopPlayback() {
        player?.stop()
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        currentIndex = -1
        updatePlaybackState()
        stopForeground(true)
        onStateChanged?.invoke()
    }

    fun skipToNext() {
        if (songs.isEmpty()) return
        playSong((currentIndex + 1) % songs.size)
    }

    fun skipToPrevious() {
        if (songs.isEmpty()) return
        playSong(if (currentIndex <= 0) songs.size - 1 else currentIndex - 1)
    }

    val isPlaying get() = player?.isPlaying == true

    private fun updateMetadata() {
        if (currentIndex < 0 || currentIndex >= songs.size) return
        val song = songs[currentIndex]
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "AACarEntertainment")
                .build()
        )
    }

    private fun updatePlaybackState() {
        val state = when {
            player?.isPlaying == true -> PlaybackStateCompat.STATE_PLAYING
            player?.playbackState == Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, player?.currentPosition ?: 0L, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                )
                .build()
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Reproduccion de musica",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val song = if (currentIndex >= 0) songs[currentIndex].title else "Musica"
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song)
            .setContentText("AACarEntertainment")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        val items = songs.mapIndexed { index, song ->
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(index.toString())
                    .setTitle(song.title)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
        result.sendResult(items)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        mediaSession.release()
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        instance = null
    }
}
