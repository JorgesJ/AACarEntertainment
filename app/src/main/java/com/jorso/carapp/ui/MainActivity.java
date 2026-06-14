package com.jorso.carapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.jorso.carapp.auto.HubActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "welcome_prefs";
    private static final String KEY_WELCOME_SHOWN = "welcome_shown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean welcomeShown = prefs.getBoolean(KEY_WELCOME_SHOWN, false);

        // Si ya se mostro el mensaje de bienvenida alguna vez, ir directo al Hub
        if (welcomeShown) {
            launchHub();
            return;
        }

        // Primera vez: mostrar mensaje informativo (solo una vez)
        setContentView(buildWelcomeScreen());
    }

    private View buildWelcomeScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0D1B2A);
        scroll.setFitsSystemWindows(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(32), dp(40), dp(32), dp(32));

        TextView title = new TextView(this);
        title.setText("Bienvenido a Entretenimiento");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24f);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tu centro multimedia para el coche");
        subtitle.setTextColor(0xFF90CAF9);
        subtitle.setTextSize(14f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(28));
        root.addView(subtitle);

        // Tarjeta informativa
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF1B263B);
        cardBg.setCornerRadius(dp(16));
        card.setBackground(cardBg);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(cardLp);

        TextView infoTitle = new TextView(this);
        infoTitle.setText("Para un funcionamiento óptimo");
        infoTitle.setTextColor(Color.WHITE);
        infoTitle.setTextSize(16f);
        infoTitle.setTypeface(null, Typeface.BOLD);
        infoTitle.setPadding(0, 0, 0, dp(12));
        card.addView(infoTitle);

        TextView infoText = new TextView(this);
        infoText.setText("Se recomienda conceder los permisos necesarios para que la aplicación funcione correctamente:\n\n" +
                "🎵  Música y audio\n" +
                "🎬  Fotos y vídeos\n" +
                "📍  Ubicación\n" +
                "🔔  Notificaciones\n\n" +
                "Puedes conceder estos permisos desde los ajustes de la aplicación en tu dispositivo. " +
                "Si algún módulo no funciona, revisa que los permisos estén activados.");
        infoText.setTextColor(0xFFC8D4E8);
        infoText.setTextSize(14f);
        infoText.setLineSpacing(dp(4), 1f);
        card.addView(infoText);

        root.addView(card);

        TextView note = new TextView(this);
        note.setText("Este mensaje solo aparece la primera vez.");
        note.setTextColor(0xFF6C7A92);
        note.setTextSize(12f);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(20), 0, dp(24));
        root.addView(note);

        // Boton continuar
        TextView btnContinue = new TextView(this);
        btnContinue.setText("ENTENDIDO, CONTINUAR");
        btnContinue.setTextColor(Color.WHITE);
        btnContinue.setTextSize(16f);
        btnContinue.setTypeface(null, Typeface.BOLD);
        btnContinue.setGravity(Gravity.CENTER);
        btnContinue.setPadding(dp(24), dp(16), dp(24), dp(16));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(0xFF2196F3);
        btnBg.setCornerRadius(dp(12));
        btnContinue.setBackground(btnBg);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnContinue.setLayoutParams(btnLp);
        btnContinue.setClickable(true);
        btnContinue.setFocusable(true);
        btnContinue.setOnClickListener(v -> {
            // Marcar que ya se mostro el mensaje para no volver a mostrarlo
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit().putBoolean(KEY_WELCOME_SHOWN, true).apply();
            launchHub();
        });
        root.addView(btnContinue);

        scroll.addView(root);
        return scroll;
    }

    private void launchHub() {
        startActivity(new Intent(this, HubActivity.class));
        finish();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
