package com.jorso.carapp.auto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jorso.carapp.R

class HubActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "hub_prefs"
        private const val KEY_PERMISSIONS_DONE = "permissions_done"
        private val REQUIRED_PERMISSIONS = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    data class Module(
        val titleRes: Int,
        val iconRes: Int,
        val accentColor: Int,
        val action: () -> Unit
    )

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvClock: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTemp: TextView

    private val clockRunnable = object : Runnable {
        override fun run() {
            val now = java.util.Calendar.getInstance()
            val hour = String.format("%02d", now.get(java.util.Calendar.HOUR_OF_DAY))
            val min = String.format("%02d", now.get(java.util.Calendar.MINUTE))
            tvClock.text = "$hour:$min"
            val days = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
            val months = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
            val day = days[now.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            val dayNum = now.get(java.util.Calendar.DAY_OF_MONTH)
            val month = months[now.get(java.util.Calendar.MONTH)]
            tvDate.text = "$day $dayNum $month"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = createRootLayout()
        setContentView(root)
        handler.post(clockRunnable)
        fetchTemperature()
        handler.postDelayed({ fetchTemperature() }, 600000)
        requestMissingPermissions()
    }

    private fun requestMissingPermissions() {
        // Si todos los permisos ya están concedidos, no hacer nada
        if (REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }) return

        // Si ya se pidieron antes, no volver a molestar
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PERMISSIONS_DONE, false)) return

        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PERMISSIONS_DONE, true)
                .apply()
        }
    }

    private fun fetchTemperature() {
        Thread {
            try {
                val lm = getSystemService(LOCATION_SERVICE) as LocationManager
                var lat: Double? = null
                var lon: Double? = null
                var cityName: String? = null

                try {
                    val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    lat = location?.latitude
                    lon = location?.longitude
                    if (lat != null && lon != null) {
                        try {
                            val geocoder = android.location.Geocoder(this, java.util.Locale.getDefault())
                            val addresses = geocoder.getFromLocation(lat, lon, 1)
                            cityName = addresses?.firstOrNull()?.locality
                                ?: addresses?.firstOrNull()?.subAdminArea
                        } catch (e: Exception) {}
                    }
                } catch (e: SecurityException) {}

                if (lat == null || lon == null) {
                    try {
                        val ipUrl = java.net.URL("https://ipapi.co/json/")
                        val ipConn = ipUrl.openConnection() as java.net.HttpURLConnection
                        ipConn.connectTimeout = 5000
                        ipConn.readTimeout = 5000
                        ipConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                        val ipResponse = ipConn.inputStream.bufferedReader().readText()
                        ipConn.disconnect()
                        val latRegex = """"latitude"\s*:\s*([\d.\-]+)""".toRegex()
                        val lonRegex = """"longitude"\s*:\s*([\d.\-]+)""".toRegex()
                        val cityRegex = """"city"\s*:\s*"([^"]+)"""".toRegex()
                        lat = latRegex.find(ipResponse)?.groupValues?.get(1)?.toDoubleOrNull()
                        lon = lonRegex.find(ipResponse)?.groupValues?.get(1)?.toDoubleOrNull()
                        cityName = cityRegex.find(ipResponse)?.groupValues?.get(1)
                    } catch (e: Exception) {}
                }

                if (lat != null && lon != null) {
                    val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val tempRegex = """"temperature"\s*:\s*([\d.]+)""".toRegex()
                    val temp = tempRegex.find(response)?.groupValues?.get(1)
                    if (temp != null) {
                        val display = if (cityName != null) "${temp}°C  $cityName" else "${temp}°C"
                        runOnUiThread { tvTemp.text = display }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HubActivity", "Temp error: ${e.message}")
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
    }

    private fun createRootLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(44), dp(16), dp(4))
        }

        val tvTitle = TextView(this).apply {
            text = "AACarEntertainment"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
        }

        tvClock = TextView(this).apply {
            text = "--:--"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        }

        tvDate = TextView(this).apply {
            text = ""
            textSize = 11f
            setTextColor(0xFF888888.toInt())
            gravity = android.view.Gravity.CENTER
        }

        centerLayout.addView(tvClock)
        centerLayout.addView(tvDate)

        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        tvTemp = TextView(this).apply {
            text = "--°C"
            textSize = 13f
            setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Botón SALIR — esquina derecha del header
        val btnExit = TextView(this).apply {
            text = "✕ Salir"
            textSize = 12f
            setTextColor(0xFFFF5252.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(12)
            }
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FF5252),
                null, null
            )
            setOnClickListener {
                finishAffinity()
            }
        }

        header.addView(tvTitle)
        header.addView(spacer1)
        header.addView(centerLayout)
        header.addView(spacer2)
        header.addView(tvTemp)
        header.addView(btnExit)

        val divider = View(this).apply {
            setBackgroundColor(0xFF1A237E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            )
        }

        val recycler = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@HubActivity, 5)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setPadding(dp(8), dp(4), dp(8), dp(4))
            clipToPadding = false
        }

        recycler.adapter = ModuleAdapter(getModules())

        root.addView(header)
        root.addView(divider)
        root.addView(recycler)

        return root
    }

    private fun getModules(): List<Module> = listOf(
        Module(R.string.module_music, R.drawable.ic_module_music, 0xFF4FC3F7.toInt()) { startModule("music") },
        Module(R.string.module_radio, R.drawable.ic_module_radio, 0xFF81C784.toInt()) { startModule("radio") },
        Module(R.string.module_iptv, R.drawable.ic_module_iptv, 0xFFEF9A9A.toInt()) { startModule("iptv") },
        Module(R.string.module_youtube, R.drawable.ic_module_youtube, 0xFFFF5252.toInt()) { startModule("youtube") },
        Module(R.string.module_browser, R.drawable.ic_module_browser, 0xFFCE93D8.toInt()) { startModule("browser") },
        Module(R.string.module_gps, R.drawable.ic_module_gps, 0xFF80DEEA.toInt()) { startModule("gps") },
        Module(R.string.module_mirror, R.drawable.ic_module_mirror, 0xFFFFF176.toInt()) { startModule("mirror") },
        Module(R.string.module_settings, R.drawable.ic_module_settings, 0xFFBDBDBD.toInt()) { startModule("settings") },
        Module(R.string.module_fuel, R.drawable.ic_module_fuel, 0xFFFFB300.toInt()) { startModule("fuel") },
        Module(R.string.module_video, R.drawable.ic_module_video, 0xFF80CBC4.toInt()) { startModule("video") }
    )

    private fun startModule(module: String) {
        val intent = when (module) {
            "youtube" -> Intent(this, YoutubeActivity::class.java)
            "browser" -> Intent(this, BrowserActivity::class.java)
            "radio" -> Intent(this, RadioCarActivity::class.java)
            "music" -> Intent(this, MusicCarActivity::class.java)
            "iptv" -> Intent(this, IptvCarActivity::class.java)
            "gps" -> Intent(this, GpsCarActivity::class.java)
            "mirror" -> Intent(this, MirrorCarActivity::class.java)
            "fuel" -> Intent(this, FuelActivity::class.java)
            "video" -> Intent(this, VideoCarActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }

    inner class ModuleAdapter(private val modules: List<Module>) :
        RecyclerView.Adapter<ModuleAdapter.ModuleVH>() {

        inner class ModuleVH(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleVH {
            val card = LinearLayout(this@HubActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(dp(4), dp(12), dp(4), dp(12))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isClickable = true
                isFocusable = true
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                    null, null
                )
            }

            val iconBg = android.widget.FrameLayout(this@HubActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
            }

            val circle = View(this@HubActivity).apply {
                layoutParams = ViewGroup.LayoutParams(dp(72), dp(72))
                background = createCircleDrawable(0xFF1E1E2E.toInt())
            }

            val icon = ImageView(this@HubActivity).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(dp(36), dp(36)).apply {
                    gravity = android.view.Gravity.CENTER
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            iconBg.addView(circle)
            iconBg.addView(icon)

            val label = TextView(this@HubActivity).apply {
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            card.addView(iconBg)
            card.addView(label)
            card.tag = Pair(icon, label)

            return ModuleVH(card)
        }

        override fun onBindViewHolder(holder: ModuleVH, position: Int) {
            val module = modules[position]
            val (icon, label) = holder.itemView.tag as Pair<*, *>
            (icon as ImageView).apply {
                setImageResource(module.iconRes)
                setColorFilter(module.accentColor)
            }
            (label as TextView).apply {
                text = getString(module.titleRes)
                setTextColor(module.accentColor)
            }
            holder.itemView.setOnClickListener { module.action() }
        }

        override fun getItemCount() = modules.size
    }

    private fun createCircleDrawable(color: Int): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
