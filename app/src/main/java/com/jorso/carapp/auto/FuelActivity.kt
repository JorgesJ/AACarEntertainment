package com.jorso.carapp.auto

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FuelActivity : AppCompatActivity() {

    data class Refuel(
        val date: String,
        val liters: Double?,
        val totalEuros: Double?,
        val pricePerLiter: Double?
    )

    companion object {
        const val PREFS_NAME = "fuel_prefs"
        const val KEY_REFUELS = "refuels"
    }

    private var refuels = mutableListOf<Refuel>()
    private lateinit var container: FrameLayout
    private var screen = "home"

    private var fieldEuros: EditText? = null
    private var fieldLiters: EditText? = null
    private var fieldPrice: EditText? = null

    // Calendario para el selector de fecha
    private val selectedCalendar = Calendar.getInstance()
    private var tvSelectedDate: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRefuels()
        container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF111111.toInt())
            fitsSystemWindows = true
        }
        setContentView(container)
        showHome()
    }

    private fun loadRefuels() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REFUELS, null)
        refuels.clear()
        if (json != null) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    refuels.add(Refuel(
                        obj.getString("date"),
                        if (obj.has("liters") && !obj.isNull("liters")) obj.getDouble("liters") else null,
                        if (obj.has("totalEuros") && !obj.isNull("totalEuros")) obj.getDouble("totalEuros") else null,
                        if (obj.has("pricePerLiter") && !obj.isNull("pricePerLiter")) obj.getDouble("pricePerLiter") else null
                    ))
                }
            } catch (e: Exception) {}
        }
    }

    private fun saveRefuels() {
        val arr = JSONArray()
        refuels.forEach { r ->
            val obj = JSONObject()
            obj.put("date", r.date)
            r.liters?.let { obj.put("liters", it) }
            r.totalEuros?.let { obj.put("totalEuros", it) }
            r.pricePerLiter?.let { obj.put("pricePerLiter", it) }
            arr.put(obj)
        }
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_REFUELS, arr.toString()).apply()
    }

    private fun showHome() {
        screen = "home"
        fieldEuros = null; fieldLiters = null; fieldPrice = null; tvSelectedDate = null
        container.removeAllViews()
        container.addView(buildHomeUI())
    }

    private fun showAddRefuel() {
        screen = "add"
        selectedCalendar.time = Date() // resetear a hoy
        container.removeAllViews()
        container.addView(buildAddRefuelUI())
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
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(120), dp(12), dp(16), dp(12))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0); setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "⛽ Consumos"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "+ Repostar"; textSize = 14f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener { showAddRefuel() }
        })
        root.addView(header)

        val totalLiters = refuels.sumOf { it.liters ?: 0.0 }
        val totalEuros = refuels.sumOf { it.totalEuros ?: 0.0 }
        val avgPrice = if (refuels.any { it.pricePerLiter != null })
            refuels.mapNotNull { it.pricePerLiter }.average() else 0.0

        val totalsCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        listOf(
            Pair("Total litros", if (totalLiters > 0) String.format("%.2f L", totalLiters) else "--"),
            Pair("Total gastado", if (totalEuros > 0) String.format("%.2f €", totalEuros) else "--"),
            Pair("Precio medio", if (avgPrice > 0) String.format("%.3f €/L", avgPrice) else "--")
        ).forEach { (label, value) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            cell.addView(TextView(this).apply {
                text = value; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            })
            cell.addView(TextView(this).apply {
                text = label; textSize = 11f; setTextColor(0xFF90CAF9.toInt())
                gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0)
            })
            totalsCard.addView(cell)
        }
        root.addView(totalsCard)
        root.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        if (refuels.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No hay repostajes registrados.\nPulsa + Repostar para añadir."
                textSize = 14f; setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER; setPadding(0, dp(32), 0, 0)
            })
        } else {
            refuels.reversed().forEachIndexed { index, refuel ->
                list.addView(buildRefuelItem(refuel, refuels.size - 1 - index))
            }
        }
        scrollView.addView(list); root.addView(scrollView)
        return root
    }

    private fun buildRefuelItem(refuel: Refuel, index: Int): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }

        // Izquierda — fecha
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        infoLayout.addView(TextView(this).apply {
            text = "⛽ ${refuel.date}"; textSize = 14f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        refuel.pricePerLiter?.let {
            infoLayout.addView(TextView(this).apply {
                text = String.format("%.3f €/L", it)
                textSize = 12f; setTextColor(0xFF888888.toInt()); setPadding(0, dp(3), 0, 0)
            })
        }
        item.addView(infoLayout)

        // Centro — litros
        if (refuel.liters != null) {
            item.addView(TextView(this).apply {
                text = String.format("%.2f L", refuel.liters)
                textSize = 15f; setTextColor(0xFF81C784.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(12), 0)
            })
        }

        // Derecha — euros
        item.addView(TextView(this).apply {
            text = refuel.totalEuros?.let { String.format("%.2f €", it) } ?: ""
            textSize = 16f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.END
        })

        item.addView(TextView(this).apply {
            text = "🗑"; textSize = 16f; setPadding(dp(16), 0, 0, 0)
            setOnClickListener { refuels.removeAt(index); saveRefuels(); showHome() }
        })
        return item
    }

    private fun buildAddRefuelUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(120), dp(12), dp(16), dp(12))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0); setOnClickListener { showHome() }
        })
        header.addView(TextView(this).apply {
            text = "Añadir Repostaje"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "✓ Guardar"; textSize = 14f; setTextColor(0xFF69F0AE.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener { saveRefuel() }
        })
        root.addView(header)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(20), dp(24), dp(24))
        }

        // ===== SELECTOR DE FECHA =====
        content.addView(buildSectionHeader("📅 Fecha del repostaje"))

        val dateSelector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }

        tvSelectedDate = TextView(this).apply {
            text = formatDate(selectedCalendar)
            textSize = 18f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Botones día
        val daySection = buildDateUnit(
            label = "Día",
            onMinus = {
                selectedCalendar.add(Calendar.DAY_OF_MONTH, -1)
                tvSelectedDate?.text = formatDate(selectedCalendar)
            },
            onPlus = {
                selectedCalendar.add(Calendar.DAY_OF_MONTH, 1)
                // No permitir fecha futura
                if (selectedCalendar.after(Calendar.getInstance())) {
                    selectedCalendar.time = Date()
                }
                tvSelectedDate?.text = formatDate(selectedCalendar)
            }
        )

        // Botones mes
        val monthSection = buildDateUnit(
            label = "Mes",
            onMinus = {
                selectedCalendar.add(Calendar.MONTH, -1)
                tvSelectedDate?.text = formatDate(selectedCalendar)
            },
            onPlus = {
                selectedCalendar.add(Calendar.MONTH, 1)
                if (selectedCalendar.after(Calendar.getInstance())) {
                    selectedCalendar.time = Date()
                }
                tvSelectedDate?.text = formatDate(selectedCalendar)
            }
        )

        // Botones año
        val yearSection = buildDateUnit(
            label = "Año",
            onMinus = {
                selectedCalendar.add(Calendar.YEAR, -1)
                tvSelectedDate?.text = formatDate(selectedCalendar)
            },
            onPlus = {
                selectedCalendar.add(Calendar.YEAR, 1)
                if (selectedCalendar.after(Calendar.getInstance())) {
                    selectedCalendar.time = Date()
                }
                tvSelectedDate?.text = formatDate(selectedCalendar)
            }
        )

        dateSelector.addView(daySection)
        dateSelector.addView(monthSection)
        dateSelector.addView(yearSection)
        dateSelector.addView(tvSelectedDate!!)
        content.addView(dateSelector)

        // Botón "Hoy"
        content.addView(TextView(this).apply {
            text = "↩ Usar fecha de hoy"
            textSize = 12f; setTextColor(0xFF888888.toInt()); gravity = Gravity.END
            setPadding(0, dp(4), 0, 0)
            setOnClickListener {
                selectedCalendar.time = Date()
                tvSelectedDate?.text = formatDate(selectedCalendar)
            }
        })

        // ===== CAMPOS NUMÉRICOS =====
        content.addView(buildSectionHeader("💶 Dinero total gastado (€)"))
        fieldEuros = buildField("Ej: 60.00  — lo que marcó el surtidor")
        content.addView(fieldEuros!!)

        content.addView(buildSectionHeader("🔢 Litros repostados"))
        fieldLiters = buildField("Ej: 40.50  — litros que pusiste")
        content.addView(fieldLiters!!)

        content.addView(buildSectionHeader("🏷 Precio por litro (€/L)  — opcional"))
        fieldPrice = buildField("Ej: 1.489  — se calcula solo si pones € y L")
        content.addView(fieldPrice!!)

        content.addView(TextView(this).apply {
            text = "ℹ️ Todos los campos son opcionales. Si introduces € y litros, el precio/litro se calcula automáticamente."
            textSize = 12f; setTextColor(0xFF555555.toInt()); setPadding(0, dp(8), 0, 0)
        })

        scrollView.addView(content); root.addView(scrollView)
        return root
    }

    private fun buildDateUnit(label: String, onMinus: () -> Unit, onPlus: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT)

            addView(TextView(this@FuelActivity).apply {
                text = label; textSize = 10f; setTextColor(0xFF888888.toInt()); gravity = Gravity.CENTER
            })
            addView(LinearLayout(this@FuelActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER

                addView(buildArrowButton("◀", onMinus))
                addView(buildArrowButton("▶", onPlus))
            })
        }
    }

    private fun buildArrowButton(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 20f; setTextColor(0xFF4FC3F7.toInt())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x334FC3F7), null, null
            )
            setOnClickListener { onClick() }
        }
    }

    private fun formatDate(cal: Calendar): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }

    private fun saveRefuel() {
        val euros = fieldEuros?.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
        val liters = fieldLiters?.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
        var price = fieldPrice?.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()

        if (euros != null && liters != null && liters > 0 && price == null) {
            price = euros / liters
        }
        if (euros == null && liters == null && price == null) {
            showToast("Introduce al menos un dato"); return
        }

        val dateStr = formatDate(selectedCalendar)
        refuels.add(Refuel(date = dateStr, liters = liters, totalEuros = euros, pricePerLiter = price))
        saveRefuels()
        showToast("Repostaje guardado ✓")
        showHome()
    }

    private fun buildSectionHeader(text: String): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 13f; setTextColor(0xFF90CAF9.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }
    }

    private fun buildField(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint; setHintTextColor(0xFF444444.toInt())
            setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(0xFF1E1E1E.toInt())
            textSize = 16f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.,")
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
