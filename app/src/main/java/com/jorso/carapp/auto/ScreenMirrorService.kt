package com.jorso.carapp.auto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat

class ScreenMirrorService : Service() {

    companion object {
        const val TAG = "ScreenMirrorService"
        const val CHANNEL_ID = "screen_mirror_channel"
        const val NOTIFICATION_ID = 9101
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "projectionData"

        var instance: ScreenMirrorService? = null

        // Surface estática — igual que el APK de referencia
        @Volatile var surface: Surface? = null
        var screenWidth = 1080
        var screenHeight = 2400
        var screenDpi = 440
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Arrancar foreground con tipo mediaProjection
        startForeground(NOTIFICATION_ID, buildNotification(), 0x20)

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)

        if (resultCode != 0 && data != null) {
            try {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = manager.getMediaProjection(resultCode, data)

                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        stopMirroring()
                    }
                }, null)

                startMirroring()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start projection: ${e.message}")
                stopSelf()
            }
        } else {
            Log.e(TAG, "Invalid resultCode or missing projection data")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    fun startMirroring() {
        val s = surface
        val mp = mediaProjection

        if (s == null) {
            Log.e(TAG, "Surface not ready")
            stopMirroring()
            return
        }

        if (mp == null) {
            Log.e(TAG, "MediaProjection not ready")
            return
        }

        try {
            // WakeLock para mantener pantalla activa
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AACarEntertainment:ScreenMirror"
            )
            wakeLock?.acquire(30 * 60 * 1000L)

            virtualDisplay = mp.createVirtualDisplay(
                "AACarMirror",
                screenWidth,
                screenHeight,
                screenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                s,
                null,
                handler
            )
            Log.d(TAG, "VirtualDisplay created: ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create VirtualDisplay: ${e.message}")
            stopMirroring()
        }
    }

    fun stopMirroring() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection?.stop()
            mediaProjection = null
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
            surface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mirror: ${e.message}")
        }
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Mirror",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mirror activo")
            .setContentText("AACarEntertainment — duplicando pantalla")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMirroring()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
