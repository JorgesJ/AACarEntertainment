package com.jorso.carapp.auto;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jorso.carapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RadioActivity extends AppCompatActivity {

    private ExoPlayer player;
    private TextView tvNowPlaying, tvNowFreq, tvStatus, btnTabAll, btnTabFav;
    private ImageButton btnFav;
    private ImageView nowLogo;
    private RecyclerView stationList;

    private final List<Station> allStations = new ArrayList<>();
    private final List<Station> favStations = new ArrayList<>();
    private Station currentStation = null;

    private static final String PREF_NAME = "radio_prefs";
    private static final String PREF_FAVS = "favorites";

    static class Station {
        String name, url, freq, color, logoUrl;
        boolean isFav;
        Station(String name, String url, String freq, String color, String logoUrl) {
            this.name = name; this.url = url;
            this.freq = freq; this.color = color;
            this.logoUrl = logoUrl;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        setFullscreen();
        setContentView(R.layout.activity_radio);

        tvNowPlaying = findViewById(R.id.radio_now_playing);
        tvNowFreq    = findViewById(R.id.radio_now_freq);
        tvStatus     = findViewById(R.id.radio_status);
        btnFav       = findViewById(R.id.radio_btn_fav);
        nowLogo      = findViewById(R.id.radio_now_logo);
        stationList  = findViewById(R.id.radio_station_list);
        btnTabAll    = findViewById(R.id.radio_tab_all);
        btnTabFav    = findViewById(R.id.radio_tab_fav);

        findViewById(R.id.radio_back_container).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnFav.setOnClickListener(v -> toggleFav());
        btnTabAll.setOnClickListener(v -> showTab(false));
        btnTabFav.setOnClickListener(v -> showTab(true));

        loadStations();
        loadFavs();
        stationList.setLayoutManager(new LinearLayoutManager(this));
        showTab(false);
    }

    private void loadStations() {
        allStations.clear();
        allStations.add(new Station("Los 40", "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40.mp3", "93.9 FM", "#E63946", ""));
        allStations.add(new Station("Cadena SER", "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3", "93.6 FM", "#003087", ""));
        allStations.add(new Station("Kiss FM", "https://kissfm.kissfmradio.cires21.com/kissfm.mp3", "97.0 FM", "#FF69B4", ""));
    }

    private void loadFavs() {
        favStations.clear();
        try {
            String json = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(PREF_FAVS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Station s = new Station(o.getString("name"), o.getString("url"), o.getString("freq"), o.getString("color"), o.optString("logoUrl", ""));
                s.isFav = true;
                favStations.add(s);
                for (Station st : allStations) if (st.url.equals(s.url)) st.isFav = true;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveFavs() {
        try {
            JSONArray arr = new JSONArray();
            for (Station s : favStations) {
                JSONObject o = new JSONObject();
                o.put("name", s.name); o.put("url", s.url);
                o.put("freq", s.freq); o.put("color", s.color);
                o.put("logoUrl", s.logoUrl != null ? s.logoUrl : "");
                arr.put(o);
            }
            getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putString(PREF_FAVS, arr.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void toggleFav() {
        if (currentStation == null) return;
        currentStation.isFav = !currentStation.isFav;
        if (currentStation.isFav) {
            if (!favStations.contains(currentStation)) favStations.add(currentStation);
            Toast.makeText(this, currentStation.name + " guardada en favoritos", Toast.LENGTH_SHORT).show();
        } else {
            favStations.remove(currentStation);
            Toast.makeText(this, currentStation.name + " eliminada de favoritos", Toast.LENGTH_SHORT).show();
        }
        saveFavs();
        updateFavBtn();
        if (stationList.getAdapter() != null) stationList.getAdapter().notifyDataSetChanged();
    }

    private void updateFavBtn() {
        if (currentStation != null && currentStation.isFav) {
            btnFav.setImageResource(R.drawable.ic_home_radio);
            btnFav.setAlpha(1f);
        } else {
            btnFav.setImageResource(R.drawable.ic_radio_fav_off);
            btnFav.setAlpha(0.5f);
        }
    }

    private void showTab(boolean fav) {
        btnTabAll.setAlpha(fav ? 0.4f : 1f);
        btnTabFav.setAlpha(fav ? 1f : 0.4f);
        stationList.setAdapter(new StationAdapter(fav ? favStations : allStations));
    }

    private void playStation(Station s) {
        currentStation = s;
        tvNowPlaying.setText(s.name);
        tvNowFreq.setText(s.freq);
        tvStatus.setText("Conectando...");
        updateFavBtn();
        if (s.logoUrl != null && !s.logoUrl.isEmpty()) {
            Glide.with(this).load(s.logoUrl).placeholder(R.drawable.ic_home_radio).into(nowLogo);
        } else {
            nowLogo.setImageResource(R.drawable.ic_home_radio);
        }
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_BUFFERING) tvStatus.setText("Cargando...");
                    else if (state == Player.STATE_READY) tvStatus.setText("En directo");
                    else if (state == Player.STATE_IDLE) tvStatus.setText("Parado");
                }
            });
        } else {
            player.stop();
        }
        player.setMediaItem(MediaItem.fromUri(Uri.parse(s.url)));
        player.prepare();
        player.play();
    }

    private void setFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) setFullscreen(); }
    @Override protected void onResume() { super.onResume(); setFullscreen(); }
    @Override protected void onPause() { super.onPause(); if (player != null) player.pause(); }
    @Override protected void onDestroy() { if (player != null) { player.release(); player = null; } super.onDestroy(); }

    private class StationAdapter extends RecyclerView.Adapter<StationAdapter.VH> {
        private final List<Station> list;
        StationAdapter(List<Station> list) { this.list = list; }

        class VH extends RecyclerView.ViewHolder {
            TextView name, freq;
            View colorBar;
            ImageView logo;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.radio_station_name);
                freq = v.findViewById(R.id.radio_station_freq);
                colorBar = v.findViewById(R.id.radio_station_color);
                logo = v.findViewById(R.id.radio_station_logo);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_radio_station, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, @SuppressLint("RecyclerView") int pos) {
            Station s = list.get(pos);
            h.name.setText(s.name);
            h.freq.setText(s.freq);
            try { h.colorBar.setBackgroundColor(android.graphics.Color.parseColor(s.color)); } catch (Exception e) {}
            h.itemView.setAlpha(currentStation != null && currentStation.url.equals(s.url) ? 1f : 0.75f);
            if (s.logoUrl != null && !s.logoUrl.isEmpty()) {
                Glide.with(h.itemView.getContext()).load(s.logoUrl).placeholder(R.drawable.ic_home_radio).into(h.logo);
            } else {
                h.logo.setImageResource(R.drawable.ic_home_radio);
            }
            h.itemView.setOnClickListener(v -> playStation(s));
            h.itemView.setOnLongClickListener(v -> { currentStation = s; toggleFav(); return true; });
        }

        @Override public int getItemCount() { return list.size(); }
    }
}
