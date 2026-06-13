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
        fieldEuros = null; fieldLiters = null; fieldPrice = null; tvSelectedDate = null
        container.removeAllViews()
        container.addView(buildHomeUI())
    }

    private fun showAddRefuel() {
        screen = "add"
        selectedCalendar.time = Date()
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

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(16), 0); setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "⛽ Consumos"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "+ Repostar"; textSize = 15f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(8), dp(8))
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
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
                text = value; textSize = 17f; setTextColor(0xFFFFFFFF.toInt())
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

        // Centro — litros en verde
        if (refuel.liters != null) {
            item.addView(TextView(this).apply {
                text = String.format("%.2f L", refuel.liters)
                textSize = 15f; setTextColor(0xFF81C784.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(12), 0)
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

    // ==================== ADD REFUEL UI — TARJETA COMPACTA CENTRADA ====================

    private fun buildAddRefuelUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF1A237E.toInt())
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(16), 0); setOnClickListener { showHome() }
        })
        header.addView(TextView(this).apply {
            text = "Añadir Repostaje"; textSize = 17f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "✓ Guardar"; textSize = 15f; setTextColor(0xFF69F0AE.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(8), dp(8))
            setOnClickListener { saveRefuel() }
        })
        root.addView(header)

        // Contenedor que centra la tarjeta — fondo oscuro alrededor
        val centerWrap = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val centerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // TARJETA compacta con ancho máximo — no de extremo a extremo
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A2A.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(
                dp(520),  // ancho máximo fijo, queda centrado
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // ===== SELECTOR DE FECHA =====
        card.addView(TextView(this).apply {
            text = "📅 Fecha"; textSize = 13f; setTextColor(0xFF90CAF9.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, dp(8))
        })

        tvSelectedDate = TextView(this).apply {
            text = formatDate(selectedCalendar)
            textSize = 20f; setTextColor(0xFF4FC3F7.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(10))
        }
        card.addView(tvSelectedDate!!)

        // Fila de botones de fecha
        val dateButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        dateButtons.addView(buildDateUnit("Día",
            { selectedCalendar.add(Calendar.DAY_OF_MONTH, -1); refreshDate() },
            { addDateCapped(Calendar.DAY_OF_MONTH) }))
        dateButtons.addView(buildDateUnit("Mes",
            { selectedCalendar.add(Calendar.MONTH, -1); refreshDate() },
            { addDateCapped(Calendar.MONTH) }))
        dateButtons.addView(buildDateUnit("Año",
            { selectedCalendar.add(Calendar.YEAR, -1); refreshDate() },
            { addDateCapped(Calendar.YEAR) }))
        card.addView(dateButtons)

        card.addView(buildCardDivider())

        // ===== CAMPOS NUMÉRICOS — TODOS VISIBLES, SIN SCROLL ENTRE ELLOS =====
        card.addView(TextView(this).apply {
            text = "💶 Dinero gastado (€)"; textSize = 13f; setTextColor(0xFF90CAF9.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(4), 0, dp(6))
        })
        fieldEuros = buildField("Ej: 60.00")
        card.addView(fieldEuros!!)

        card.addView(TextView(this).apply {
            text = "🔢 Litros repostados"; textSize = 13f; setTextColor(0xFF90CAF9.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(14), 0, dp(6))
        })
        fieldLiters = buildField("Ej: 40.50")
        card.addView(fieldLiters!!)

        card.addView(TextView(this).apply {
            text = "🏷 Precio por litro (€/L) — opcional"; textSize = 13f; setTextColor(0xFF90CAF9.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(14), 0, dp(6))
        })
        fieldPrice = buildField("Se calcula solo si pones € y L")
        card.addView(fieldPrice!!)

        card.addView(TextView(this).apply {
            text = "ℹ️ Todos los campos son opcionales."
            textSize = 11f; setTextColor(0xFF555555.toInt()); setPadding(0, dp(12), 0, 0)
        })

        // Botón guardar grande dentro de la tarjeta también
        val btnSaveBig = TextView(this).apply {
            text = "✓  GUARDAR REPOSTAJE"
            textSize = 15f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
            setPadding(dp(20), dp(16), dp(20), dp(16))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.ColorDrawable(0xFF1A237E.toInt()), null
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
            setOnClickListener { saveRefuel() }
        }
        card.addView(btnSaveBig)

        centerContainer.addView(card)
        centerWrap.addView(centerContainer)
        root.addView(centerWrap)
        return root
    }

    private fun addDateCapped(field: Int) {
        selectedCalendar.add(field, 1)
        if (selectedCalendar.after(Calendar.getInstance())) {
            selectedCalendar.time = Date()
        }
        refreshDate()
    }

    private fun refreshDate() {
        tvSelectedDate?.text = formatDate(selectedCalendar)
    }

    private fun buildDateUnit(label: String, onMinus: () -> Unit, onPlus: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            addView(TextView(this@FuelActivity).apply {
                text = label; textSize = 11f; setTextColor(0xFF888888.toInt()); gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(4))
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
            this.text = text; textSize = 22f; setTextColor(0xFF4FC3F7.toInt())
            setPadding(dp(14), dp(10), dp(14), dp(10))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x334FC3F7), null, null
            )
            setOnClickListener { onClick() }
        }
    }

    private fun buildCardDivider(): View {
        return View(this).apply {
            setBackgroundColor(0xFF333344.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { topMargin = dp(8); bottomMargin = dp(16) }
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

        refuels.add(Refuel(date = formatDate(selectedCalendar), liters = liters, totalEuros = euros, pricePerLiter = price))
        saveRefuels()
        showToast("Repostaje guardado ✓")
        showHome()
    }

    private fun buildField(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setHintTextColor(0xFF555555.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF252535.toInt())
            textSize = 18f
            // Teclado numérico con decimales. El InputFilter de abajo garantiza
            // que se acepten tanto coma como punto, independientemente del teclado
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            // InputFilter propio: permite 0-9, una coma o un punto
            filters = arrayOf(android.text.InputFilter { source, start, end, dest, dstart, dend ->
                val existing = dest.toString()
                val builder = StringBuilder()
                for (i in start until end) {
                    val c = source[i]
                    when {
                        c.isDigit() -> builder.append(c)
                        (c == '.' || c == ',') -> {
                            // Solo permitir un separador decimal en total
                            val hasSeparator = existing.contains('.') || existing.contains(',') ||
                                    builder.contains(".") || builder.contains(",")
                            if (!hasSeparator) builder.append(c)
                        }
                    }
                }
                builder.toString()
            })
            setPadding(dp(16), dp(16), dp(16), dp(16))
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
