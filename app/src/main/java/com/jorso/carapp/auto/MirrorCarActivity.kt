package com.jorso.carapp.auto

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MirrorCarActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MirrorCarActivity"
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var tvStatus: TextView
    private var isMirroring = false

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.d(TAG, "Projection permission granted")
            startMirrorService(result.resultCode, result.data!!)
        } else {
            Log.e(TAG, "Projection permission denied")
            showToast("Permiso denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUI())
    }

    private fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val btnBack = TextView(this).apply {
            text = "←"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
        }

        val tvTitle = TextView(this).apply {
            text = "Mirror — Pantalla del móvil"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnStop = TextView(this).apply {
            text = "⏹ Parar"
            textSize = 14f
            setTextColor(0xFFEF9A9A.toInt())
            setOnClickListener { stopMirror() }
        }

        header.addView(btnBack)
        header.addView(tvTitle)
        header.addView(btnStop)
        root.addView(header)

        // Status
        tvStatus = TextView(this).apply {
            text = "Pulsa 'Iniciar Mirror' para comenzar"
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(0xFF1A1A1A.toInt())
        }
        root.addView(tvStatus)

        // SurfaceView
        val surfaceContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
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
                Log.d(TAG, "Surface created")
                ScreenMirrorService.surface = holder.surface
                if (isMirroring) {
                    ScreenMirrorService.instance?.startMirroring()
                }
            }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
                Log.d(TAG, "Surface changed: ${w}x${h}")
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "Surface destroyed")
                ScreenMirrorService.surface = null
            }
        })

        // Reenviar toques al móvil
        surfaceView.setOnTouchListener { v, event ->
            if (TouchForwardService.instance != null) {
                val scaleX = ScreenMirrorService.screenWidth.toFloat() / v.width
                val scaleY = ScreenMirrorService.screenHeight.toFloat() / v.height
                val realX = event.x * scaleX
                val realY = event.y * scaleY
                when (event.action) {
                    MotionEvent.ACTION_UP -> TouchForwardService.performTouch(realX, realY)
                    MotionEvent.ACTION_MOVE -> TouchForwardService.performScroll(
                        event.x * scaleX, event.y * scaleY,
                        event.x * scaleX, event.y * scaleY
                    )
                }
            }
            true
        }

        surfaceContainer.addView(surfaceView)
        root.addView(surfaceContainer)

        // Controles
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF1A1A1A.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val btnStart = buildButton("▶ Iniciar Mirror", 0xFF4FC3F7.toInt()) {
            checkAndStart()
        }

        val btnAccessibility = buildButton("⚙ Accesibilidad", 0xFF81C784.toInt()) {
            openAccessibilitySettings()
        }

        controls.addView(btnStart)
        controls.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), 1)
        })
        controls.addView(btnAccessibility)
        root.addView(controls)

        return root
    }

    private fun buildButton(text: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(14), dp(20), dp(14))
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.ColorDrawable(color), null
            )
            setOnClickListener { onClick() }
        }
    }

    private fun checkAndStart() {
        if (TouchForwardService.instance == null) {
            tvStatus.text = "⚠ Activa el servicio de accesibilidad primero"
            showToast("Activa la accesibilidad primero")
            openAccessibilitySettings()
            return
        }
        tvStatus.text = "Solicitando permiso de captura..."
        requestMirrorPermission()
    }

    private fun requestMirrorPermission() {
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(manager.createScreenCaptureIntent())
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting projection: ${e.message}")
            showToast("Error al solicitar permiso")
        }
    }

    private fun startMirrorService(resultCode: Int, data: Intent) {
        val metrics = resources.displayMetrics
        ScreenMirrorService.screenWidth = metrics.widthPixels
        ScreenMirrorService.screenHeight = metrics.heightPixels
        ScreenMirrorService.screenDpi = metrics.densityDpi

        Log.d(TAG, "Starting mirror: ${metrics.widthPixels}x${metrics.heightPixels} @ ${metrics.densityDpi}dpi")

        val intent = Intent(this, ScreenMirrorService::class.java).apply {
            putExtra(ScreenMirrorService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenMirrorService.EXTRA_DATA, data)
        }

        startForegroundService(intent)
        isMirroring = true
        tvStatus.text = "▶ Mirror activo — toca la pantalla para interactuar"
    }

    private fun stopMirror() {
        isMirroring = false
        ScreenMirrorService.instance?.stopMirroring()
        stopService(Intent(this, ScreenMirrorService::class.java))
        tvStatus.text = "Mirror parado"
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            showToast("Abre Ajustes → Accesibilidad manualmente")
        }
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isMirroring) {
            stopMirror()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

