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

    // Campos del formulario — accesibles desde el botón guardar del header
    private var fieldEuros: EditText? = null
    private var fieldLiters: EditText? = null
    private var fieldPrice: EditText? = null
    private var currentDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRefuels()
        container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF111111.toInt())
        }
        setContentView(container)
        showHome()
    }

    // ==================== PERSISTENCIA ====================

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

    // ==================== NAVEGACIÓN ====================

    private fun showHome() {
        screen = "home"
        fieldEuros = null
        fieldLiters = null
        fieldPrice = null
        container.removeAllViews()
        container.addView(buildHomeUI())
    }

    private fun showAddRefuel() {
        screen = "add"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        currentDate = dateFormat.format(Date())
        container.removeAllViews()
        container.addView(buildAddRefuelUI())
    }

    // ==================== HOME UI ====================

    private fun buildHomeUI(): View {
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
            setPadding(dp(120), dp(12), dp(16), dp(12))
        }

        header.addView(TextView(this).apply {
            text = "←"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0)
            setOnClickListener { finish() }
        })

        header.addView(TextView(this).apply {
            text = "⛽ Consumos"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        header.addView(TextView(this).apply {
            text = "+ Repostar"
            textSize = 14f
            setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener { showAddRefuel() }
        })

        root.addView(header)

        // Totales
        val totalLiters = refuels.sumOf { it.liters ?: 0.0 }
        val totalEuros = refuels.sumOf { it.totalEuros ?: 0.0 }
        val avgPrice = if (refuels.any { it.pricePerLiter != null })
            refuels.mapNotNull { it.pricePerLiter }.average() else 0.0

        val totalsCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        listOf(
            Pair("Total litros", if (totalLiters > 0) String.format("%.2f L", totalLiters) else "--"),
            Pair("Total gastado", if (totalEuros > 0) String.format("%.2f €", totalEuros) else "--"),
            Pair("Precio medio", if (avgPrice > 0) String.format("%.3f €/L", avgPrice) else "--")
        ).forEach { (label, value) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            cell.addView(TextView(this).apply {
                text = value
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            cell.addView(TextView(this).apply {
                text = label
                textSize = 11f
                setTextColor(0xFF90CAF9.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
            totalsCard.addView(cell)
        }

        root.addView(totalsCard)

        root.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        // Historial
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        if (refuels.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No hay repostajes registrados.\nPulsa el botón Repostar arriba para añadir."
                textSize = 14f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(32), 0, 0)
            })
        } else {
            refuels.reversed().forEachIndexed { index, refuel ->
                list.addView(buildRefuelItem(refuel, refuels.size - 1 - index))
            }
        }

        scrollView.addView(list)
        root.addView(scrollView)
        return root
    }

    private fun buildRefuelItem(refuel: Refuel, index: Int): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        infoLayout.addView(TextView(this).apply {
            text = "⛽ ${refuel.date}"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        val details = buildString {
            refuel.liters?.let { append(String.format("%.2f L", it)) }
            refuel.pricePerLiter?.let {
                if (isNotEmpty()) append("  •  ")
                append(String.format("%.3f €/L", it))
            }
        }
        if (details.isNotEmpty()) {
            infoLayout.addView(TextView(this).apply {
                text = details
                textSize = 12f
                setTextColor(0xFF888888.toInt())
                setPadding(0, dp(3), 0, 0)
            })
        }

        item.addView(infoLayout)

        item.addView(TextView(this).apply {
            text = refuel.totalEuros?.let { String.format("%.2f €", it) } ?: ""
            textSize = 16f
            setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.END
        })

        item.addView(TextView(this).apply {
            text = "🗑"
            textSize = 16f
            setPadding(dp(16), 0, 0, 0)
            setOnClickListener {
                refuels.removeAt(index)
                saveRefuels()
                showHome()
            }
        })

        return item
    }

    // ==================== ADD REFUEL UI ====================

    private fun buildAddRefuelUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header con botón GUARDAR visible siempre
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(120), dp(12), dp(16), dp(12))
        }

        header.addView(TextView(this).apply {
            text = "←"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(12), 0)
            setOnClickListener { showHome() }
        })

        header.addView(TextView(this).apply {
            text = "Añadir Repostaje"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        // Botón guardar en el header — siempre visible
        header.addView(TextView(this).apply {
            text = "✓ Guardar"
            textSize = 14f
            setTextColor(0xFF69F0AE.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener { saveRefuel() }
        })

        root.addView(header)

        // Fecha
        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        dateRow.addView(TextView(this).apply {
            text = "📅 $currentDate"
            textSize = 13f
            setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(dateRow)

        root.addView(View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        // Formulario
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(24))
        }

        // Campo 1 — Euros
        content.addView(buildSectionHeader("💶 Dinero total gastado (€)"))
        fieldEuros = buildField("Ej: 60.00  — lo que marcó el surtidor")
        content.addView(fieldEuros!!)

        // Campo 2 — Litros
        content.addView(buildSectionHeader("🔢 Litros repostados"))
        fieldLiters = buildField("Ej: 40.50  — litros que pusiste")
        content.addView(fieldLiters!!)

        // Campo 3 — Precio por litro
        content.addView(buildSectionHeader("🏷 Precio por litro (€/L)  — opcional"))
        fieldPrice = buildField("Ej: 1.489  — se calcula solo si pones € y L")
        content.addView(fieldPrice!!)

        // Info
        content.addView(TextView(this).apply {
            text = "ℹ️ Todos los campos son opcionales. Si introduces € y litros, el precio/litro se calcula automáticamente."
            textSize = 12f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(8), 0, 0)
        })

        scrollView.addView(content)
        root.addView(scrollView)
        return root
    }

    private fun saveRefuel() {
        val euros = fieldEuros?.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
        val liters = fieldLiters?.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
        var price = fieldPrice?.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()

        if (euros != null && liters != null && liters > 0 && price == null) {
            price = euros / liters
        }

        if (euros == null && liters == null && price == null) {
            showToast("Introduce al menos un dato")
            return
        }

        refuels.add(Refuel(date = currentDate, liters = liters, totalEuros = euros, pricePerLiter = price))
        saveRefuels()
        showToast("Repostaje guardado ✓")
        showHome()
    }

    private fun buildSectionHeader(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(0xFF90CAF9.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }
    }

    private fun buildField(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setHintTextColor(0xFF444444.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E1E1E.toInt())
            textSize = 16f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

