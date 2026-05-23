package com.jorso.carapp.auto

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GpsCarActivity : AppCompatActivity() {

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
            setPadding(dp(16), dp(48), dp(16), dp(12))
        }

        val btnBack = TextView(this).apply {
            text = "←"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
        }

        val tvTitle = TextView(this).apply {
            text = "GPS / Navegación"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        header.addView(btnBack)
        header.addView(tvTitle)
        root.addView(header)

        // Contenido centrado
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setPadding(dp(32), dp(16), dp(32), dp(16))
        }

        val tvInfo = TextView(this).apply {
            text = "Selecciona tu app de navegación"
            textSize = 15f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }
        content.addView(tvInfo)

        // Google Maps
        if (isAppInstalled("com.google.android.apps.maps")) {
            content.addView(buildAppButton("🗺  Google Maps", 0xFF4285F4.toInt()) {
                launchApp("com.google.android.apps.maps", "geo:0,0")
            })
        }

        // Waze — lanzamiento directo sin Intent chooser
        if (isAppInstalled("com.waze")) {
            content.addView(buildAppButton("🔵  Waze", 0xFF33CCFF.toInt()) {
                launchWazeDirect()
            })
        }

        // HERE Maps
        if (isAppInstalled("com.here.app.maps")) {
            content.addView(buildAppButton("📍  HERE Maps", 0xFF00AFAA.toInt()) {
                launchApp("com.here.app.maps", null)
            })
        }

        // Si no hay ninguna
        if (!isAppInstalled("com.google.android.apps.maps") &&
            !isAppInstalled("com.waze") &&
            !isAppInstalled("com.here.app.maps")) {
            val tvNoApps = TextView(this).apply {
                text = "No se encontraron apps de navegación.\nInstala Google Maps o Waze."
                textSize = 14f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
            }
            content.addView(tvNoApps)
        }

        root.addView(content)
        return root
    }

    private fun buildAppButton(text: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(20), dp(32), dp(20))
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.ColorDrawable(color), null
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
            setOnClickListener { onClick() }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }
    }

    private fun launchWazeDirect() {
        try {
            // Lanzar Waze directamente sin pasar por Android Auto
            val intent = packageManager.getLaunchIntentForPackage("com.waze")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (intent != null) {
                startActivity(intent)
            } else {
                // Fallback con URI
                val uri = Intent(Intent.ACTION_VIEW, Uri.parse("waze://")).apply {
                    setPackage("com.waze")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(uri)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Abre Waze desde el móvil", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun launchApp(packageName: String, geoUri: String?) {
        try {
            val intent = if (geoUri != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)).apply {
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            intent?.let { startActivity(it) }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Error al abrir la app", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

