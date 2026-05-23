package com.jorso.carapp.auto;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.jorso.carapp.R;

public class HomeActivity extends AppCompatActivity {

    public enum AppModule {
        YOUTUBE(R.string.home_tile_youtube, R.string.home_tile_youtube_sub, R.drawable.ic_home_youtube, R.color.home_accent_red, R.color.home_icon_yt_bg),
        BROWSER(R.string.home_tile_browser, R.string.home_tile_browser_sub, R.drawable.ic_home_browser, R.color.home_accent_blue, R.color.home_icon_web_bg),
        IPTV(R.string.home_tile_iptv, R.string.home_tile_iptv_sub, R.drawable.ic_home_iptv, R.color.home_accent_purple, R.color.home_icon_iptv_bg),
        MUSIC(R.string.home_tile_music, R.string.home_tile_music_sub, R.drawable.ic_home_music, R.color.home_accent_green, R.color.home_icon_music_bg),
        RADIO(R.string.home_tile_radio, R.string.home_tile_radio_sub, R.drawable.ic_home_radio, R.color.home_accent_amber, R.color.home_icon_radio_bg),
        MIRROR(R.string.home_tile_mirror, R.string.home_tile_mirror_sub, R.drawable.ic_home_mirror, R.color.home_accent_cyan, R.color.home_icon_mirror_bg),
        GPS(R.string.home_tile_gps, R.string.home_tile_gps_sub, R.drawable.ic_home_gps, R.color.home_accent_orange, R.color.home_icon_gps_bg),
        SETTINGS(R.string.home_tile_settings, R.string.home_tile_settings_sub, R.drawable.ic_home_settings, R.color.home_accent_gray, R.color.home_icon_settings_bg);

        public final int labelRes, sublabelRes, iconRes, accentColorRes, iconBgColorRes;

        AppModule(int labelRes, int sublabelRes, int iconRes, int accentColorRes, int iconBgColorRes) {
            this.labelRes = labelRes;
            this.sublabelRes = sublabelRes;
            this.iconRes = iconRes;
            this.accentColorRes = accentColorRes;
            this.iconBgColorRes = iconBgColorRes;
        }
    }

    private TextView clockView;
    private TextView wifiStatus;
    private View miniPlayer;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = this::updateClock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        setContentView(R.layout.activity_home);
        applySettings();
        bindViews();
        setupGrid();
        updateClock();
        updateWifiStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(clockRunnable);
        updateWifiStatus();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockRunnable);
    }

    private void applySettings() {
        android.content.SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        View root = findViewById(R.id.home_root);

        // Color de fondo
        String bg = prefs.getString(SettingsActivity.KEY_BG, "#0A0A0C");
        try { root.setBackgroundColor(android.graphics.Color.parseColor(bg)); } catch (Exception ignored) {}

        // Color de acento en topbar
        View topbar = findViewById(R.id.home_topbar);
        try { topbar.setBackgroundColor(android.graphics.Color.parseColor(bg.replace("#0A0A0C","#0F0F12").replace("#121218","#161620").replace("#0A0F1E","#0F1528").replace("#1A1A24","#1F1F2E"))); } catch (Exception ignored) {}

        // Tamaño de texto
        int textSize = prefs.getInt(SettingsActivity.KEY_TEXT_SIZE, 13);
        try {
            TextView clock = findViewById(R.id.home_clock);
            if (clock != null) clock.setTextSize(textSize + 9f);
            TextView logo = findViewById(R.id.home_logo);
            if (logo != null) logo.setTextSize(textSize + 3f);
        } catch (Exception ignored) {}

        // Acento en logo
        String accent = prefs.getString(SettingsActivity.KEY_ACCENT, "#2F7FFF");
        try {
            TextView logo = findViewById(R.id.home_logo);
            if (logo != null) logo.setTextColor(android.graphics.Color.parseColor(accent));
            TextView wifi = findViewById(R.id.home_wifi_status);
            if (wifi != null) wifi.setTextColor(android.graphics.Color.parseColor(accent));
        } catch (Exception ignored) {}
    }

    private void bindViews() {
        clockView  = findViewById(R.id.home_clock);
        wifiStatus = findViewById(R.id.home_wifi_status);
        miniPlayer = findViewById(R.id.home_miniplayer);
        miniPlayer.setVisibility(View.GONE);
    }

    private void setupGrid() {
        RecyclerView grid = findViewById(R.id.home_grid);
        int columns = isLandscape() ? 4 : 2;
        grid.setLayoutManager(new GridLayoutManager(this, columns));
        List<AppModule> modules = new ArrayList<>();
        for (AppModule m : AppModule.values()) modules.add(m);
        grid.setAdapter(new TileAdapter(modules, this::onModuleTapped));
        grid.setHasFixedSize(true);
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    private void onModuleTapped(AppModule module) {
        switch (module) {
            case YOUTUBE:
                startActivity(new Intent(this, YoutubeActivity.class));
                break;
            case BROWSER:   startActivity(new Intent(this, BrowserActivity.class)); break;
            case IPTV:      startActivity(new Intent(this, IptvActivity.class)); break;
            case MUSIC:     startActivity(new Intent(this, MusicActivity.class)); break;
            case RADIO:     startActivity(new Intent(this, RadioActivity.class)); break;
            case MIRROR:
                startActivity(new Intent(this, MirrorActivity2.class));
                break;
            case GPS:       launchGps(); break;
            case SETTINGS:  startActivity(new Intent(this, SettingsActivity.class)); break;
        }
    }

    private void launchGps() {
        String[] navPackages = {"com.google.android.apps.maps","com.waze","com.here.app.maps","com.sygic.aura","org.osmand"};
        android.content.pm.PackageManager pm = getPackageManager();
        java.util.List<String> found = new java.util.ArrayList<>();
        for (String pkg : navPackages) {
            try { pm.getPackageInfo(pkg, 0); found.add(pkg); } catch (Exception ignored) {}
        }
        if (found.size() <= 1) {
            try {
                Intent i = found.isEmpty()
                    ? new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q="))
                    : pm.getLaunchIntentForPackage(found.get(0));
                if (i != null) startActivity(i);
            } catch (Exception ignored) {}
            return;
        }
        String[] names = new String[found.size()];
        for (int i = 0; i < found.size(); i++) {
            try { names[i] = (String) pm.getApplicationLabel(pm.getApplicationInfo(found.get(i), 0)); }
            catch (Exception e) { names[i] = found.get(i); }
        }
        java.util.List<String> finalFound = found;
        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.HomeGpsDialog)
            .setTitle("Selecciona navegador GPS")
            .setItems(names, (d, w) -> {
                try { startActivity(pm.getLaunchIntentForPackage(finalFound.get(w))); }
                catch (Exception ignored) {}
            }).show();
    }

    private void updateClock() {
        if (clockView == null) return;
        clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        handler.postDelayed(clockRunnable, 30_000);
    }

    private void updateWifiStatus() {
        if (wifiStatus == null) return;
        boolean connected = isNetworkConnected();
        wifiStatus.setText(connected ? "WiFi" : "Sin red");
        android.content.SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        String accent = prefs.getString(SettingsActivity.KEY_ACCENT, "#2F7FFF");
        try {
            wifiStatus.setTextColor(connected
                ? android.graphics.Color.parseColor(accent)
                : getColor(R.color.home_accent_gray));
        } catch (Exception ignored) {}
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public void showMiniPlayer(String title, String artist) {
        if (miniPlayer == null) return;
        TextView t = miniPlayer.findViewById(R.id.mp_title);
        TextView a = miniPlayer.findViewById(R.id.mp_artist);
        if (t != null) t.setText(title);
        if (a != null) a.setText(artist != null ? artist : "");
        miniPlayer.setVisibility(View.VISIBLE);
    }

    public void hideMiniPlayer() {
        if (miniPlayer != null) miniPlayer.setVisibility(View.GONE);
    }

    public interface OnModuleClickListener {
        void onModuleClick(AppModule module);
    }

    private static final class TileAdapter extends RecyclerView.Adapter<TileAdapter.TileViewHolder> {
        private final List<AppModule> modules;
        private final OnModuleClickListener listener;

        TileAdapter(List<AppModule> modules, OnModuleClickListener listener) {
            this.modules = modules;
            this.listener = listener;
        }

        @NonNull
        @Override
        public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_tile, parent, false);
            return new TileViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
            holder.bind(modules.get(position), listener);
        }

        @Override
        public int getItemCount() { return modules.size(); }

        static final class TileViewHolder extends RecyclerView.ViewHolder {
            private final View      accentBar;
            private final View      iconBg;
            private final ImageView icon;
            private final TextView  label;
            private final TextView  sublabel;

            TileViewHolder(@NonNull View itemView) {
                super(itemView);
                accentBar = itemView.findViewById(R.id.tile_accent);
                iconBg    = itemView.findViewById(R.id.tile_icon_bg);
                icon      = itemView.findViewById(R.id.tile_icon);
                label     = itemView.findViewById(R.id.tile_label);
                sublabel  = itemView.findViewById(R.id.tile_sublabel);
            }

            void bind(AppModule module, OnModuleClickListener listener) {
                Context ctx = itemView.getContext();
                label.setText(ctx.getString(module.labelRes));
                sublabel.setText(ctx.getString(module.sublabelRes));
                icon.setImageResource(module.iconRes);
                icon.setColorFilter(ctx.getColor(module.accentColorRes));

                GradientDrawable iconBgD = new GradientDrawable();
                iconBgD.setShape(GradientDrawable.RECTANGLE);
                iconBgD.setCornerRadius(ctx.getResources().getDimension(R.dimen.home_icon_radius));
                iconBgD.setColor(ctx.getColor(module.iconBgColorRes));
                iconBg.setBackground(iconBgD);

                GradientDrawable accentD = new GradientDrawable();
                accentD.setShape(GradientDrawable.RECTANGLE);
                accentD.setColor(ctx.getColor(module.accentColorRes));
                accentBar.setBackground(accentD);
                accentBar.setAlpha(0.6f);

                itemView.setOnClickListener(v -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                            .start();
                    listener.onModuleClick(module);
                });
            }
        }
    }
}










