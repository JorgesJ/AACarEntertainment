package com.jorso.carapp.auto

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jorso.carapp.R

class HubActivity : AppCompatActivity() {

    data class Module(
        val titleRes: Int,
        val iconRes: Int,
        val colorStart: Int,
        val colorEnd: Int,
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
        root.fitsSystemWindows = true
        setContentView(root)
        handler.post(clockRunnable)
        fetchTemperature()
        handler.postDelayed({ fetchTemperature() }, 600000)
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
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF0D1B2A.toInt(), 0xFF1B263B.toInt(), 0xFF0D1B2A.toInt())
            )
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), headerPadding(), dp(20), dp(14))
        }

        val tvTitle = TextView(this).apply {
            text = "🚗 Entretenimiento"
            textSize = 15f
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
            gravity = Gravity.CENTER
        }
        tvClock = TextView(this).apply {
            text = "--:--"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        tvDate = TextView(this).apply {
            text = ""
            textSize = 11f
            setTextColor(0xFFAAB4C8.toInt())
            gravity = Gravity.CENTER
        }
        centerLayout.addView(tvClock)
        centerLayout.addView(tvDate)

        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        tvTemp = TextView(this).apply {
            text = "--°C"
            textSize = 14f
            setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.END
        }

        val btnExit = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(0xFFFF6B6B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(8), dp(8))
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FF6B6B), null, null
            )
            setOnClickListener { finishAffinity() }
        }

        header.addView(tvTitle)
        header.addView(spacer1)
        header.addView(centerLayout)
        header.addView(spacer2)
        header.addView(tvTemp)
        header.addView(btnExit)

        val recycler = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@HubActivity, 3)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setPadding(dp(12), dp(4), dp(12), dp(12))
            clipToPadding = false
        }
        recycler.adapter = ModuleAdapter(getModules())

        root.addView(header)
        root.addView(recycler)
        return root
    }

    private fun getModules(): List<Module> = listOf(
        Module(R.string.module_music, R.drawable.ic_module_music, 0xFF2196F3.toInt(), 0xFF0D47A1.toInt()) { startModule("music") },
        Module(R.string.module_radio, R.drawable.ic_module_radio, 0xFF66BB6A.toInt(), 0xFF2E7D32.toInt()) { startModule("radio") },
        Module(R.string.module_iptv, R.drawable.ic_module_iptv, 0xFFEF5350.toInt(), 0xFFB71C1C.toInt()) { startModule("iptv") },
        Module(R.string.module_youtube, R.drawable.ic_module_youtube, 0xFFFF5252.toInt(), 0xFFC62828.toInt()) { startModule("youtube") },
        Module(R.string.module_browser, R.drawable.ic_module_browser, 0xFFAB47BC.toInt(), 0xFF6A1B9A.toInt()) { startModule("browser") },
        Module(R.string.module_mirror, R.drawable.ic_module_mirror, 0xFFFFCA28.toInt(), 0xFFF57F17.toInt()) { startModule("mirror") },
        Module(R.string.module_settings, R.drawable.ic_module_settings, 0xFF78909C.toInt(), 0xFF37474F.toInt()) { startModule("settings") },
        Module(R.string.module_fuel, R.drawable.ic_module_fuel, 0xFFFFB300.toInt(), 0xFFE65100.toInt()) { startModule("fuel") },
        Module(R.string.module_video, R.drawable.ic_module_video, 0xFF26C6DA.toInt(), 0xFF00838F.toInt()) { startModule("video") }
    )

    private fun startModule(module: String) {
        val intent = when (module) {
            "youtube" -> Intent(this, YoutubeActivity::class.java)
            "browser" -> Intent(this, BrowserActivity::class.java)
            "radio" -> Intent(this, RadioCarActivity::class.java)
            "music" -> Intent(this, MusicCarActivity::class.java)
            "iptv" -> Intent(this, IptvCarActivity::class.java)
            "mirror" -> Intent(this, MirrorCarActivity::class.java)
            "settings" -> Intent(this, SettingsCarActivity::class.java)
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
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(105)
                ).apply {
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                isClickable = true
                isFocusable = true
            }

            val icon = ImageView(this@HubActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
                scaleType = ImageView.ScaleType.FIT_CENTER
                setColorFilter(0xFFFFFFFF.toInt())
            }

            val label = TextView(this@HubActivity).apply {
                textSize = 15f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, dp(12), 0, 0)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            card.addView(icon)
            card.addView(label)
            card.tag = Pair(icon, label)
            return ModuleVH(card)
        }

        override fun onBindViewHolder(holder: ModuleVH, position: Int) {
            val module = modules[position]
            val tag = holder.itemView.tag as Pair<*, *>
            val icon = tag.first as ImageView
            val label = tag.second as TextView

            icon.setImageResource(module.iconRes)
            label.text = getString(module.titleRes)

            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(module.colorStart, module.colorEnd)
            ).apply {
                cornerRadius = dp(20).toFloat()
            }
            holder.itemView.background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x44FFFFFF),
                gradient, null
            )

            holder.itemView.setOnClickListener { module.action() }
        }

        override fun getItemCount() = modules.size
    }

    private fun headerPadding(): Int {
        return try {
            val carContext = Class.forName("androidx.car.app.connection.CarConnection")
            val conn = carContext.getConstructor(android.content.Context::class.java).newInstance(this)
            val typeLive = carContext.getMethod("getType").invoke(conn)
            val type = (typeLive as? androidx.lifecycle.LiveData<*>)?.value as? Int ?: 0
            if (type != 0) dp(44) else dp(12)
        } catch (e: Exception) {
            dp(12)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
