package com.jorso.carapp.auto

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsCarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = buildUI()
        root.fitsSystemWindows = true
        setContentView(root)
    }

    private fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D1B2A.toInt())
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
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, dp(16), 0)
            isClickable = true; isFocusable = true
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "⚙️ Ajustes"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        root.addView(header)

        // Contenido centrado con el mensaje
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }

        center.addView(TextView(this).apply {
            text = "🛠️"
            textSize = 56f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        })

        center.addView(TextView(this).apply {
            text = "En breve estará disponible esta opción"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        })

        center.addView(TextView(this).apply {
            text = "Estamos trabajando en nuevas configuraciones:\ntamaño de letra, colores, resolución y más."
            textSize = 14f
            setTextColor(0xFF90CAF9.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(28))
        })

        // Botón volver al menú
        center.addView(TextView(this).apply {
            text = "← Volver al menú principal"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(14), dp(28), dp(14))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF),
                android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF2196F3.toInt())
                    cornerRadius = dp(12).toFloat()
                }, null
            )
            setOnClickListener { finish() }
        })

        root.addView(center)
        return root
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
