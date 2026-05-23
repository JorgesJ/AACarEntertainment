package com.jorso.carapp.auto;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jorso.carapp.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IptvActivity extends AppCompatActivity {

    private static final String PREF_NAME = "iptv_prefs";
    private static final String PREF_URL  = "last_url";

    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar progressBar;
    private ScrollView selectorPanel;
    private LinearLayout playerPanel;
    private RecyclerView channelList;
    private EditText urlInput;
    private TextView nowPlaying;

    private final List<Channel> channels = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) loadM3uFromUri(uri);
            });

    static class Channel {
        String name, url, logo, epg;
        Channel(String name, String url, String logo, String epg) {
            this.name = name; this.url = url; this.logo = logo; this.epg = epg;
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
        setContentView(R.layout.activity_iptv);

        playerView    = findViewById(R.id.iptv_player_view);
        progressBar   = findViewById(R.id.iptv_progress);
        selectorPanel = findViewById(R.id.iptv_selector);
        playerPanel   = findViewById(R.id.iptv_player_panel);
        channelList   = findViewById(R.id.iptv_channel_list);
        urlInput      = findViewById(R.id.iptv_url_input);
        nowPlaying    = findViewById(R.id.iptv_now_playing);

        findViewById(R.id.iptv_back_container).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.iptv_btn_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.iptv_btn_open_file).setOnClickListener(v ->
                filePicker.launch(new String[]{"audio/x-mpegurl", "application/x-mpegurl", "*/*"}));
        findViewById(R.id.iptv_btn_open_url).setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (url.isEmpty()) { Toast.makeText(this, "Introduce una URL", Toast.LENGTH_SHORT).show(); return; }
            saveUrl(url);
            loadM3uFromUrl(url);
        });

        // Cargar ultima URL guardada
        String savedUrl = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(PREF_URL, "");
        if (!savedUrl.isEmpty()) {
            urlInput.setText(savedUrl);
            loadM3uFromUrl(savedUrl);
        } else {
            showSelector();
        }
    }

    private void saveUrl(String url) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putString(PREF_URL, url).apply();
    }

    private void showSelector() {
        selectorPanel.setVisibility(View.VISIBLE);
        playerPanel.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void loadM3uFromUri(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                List<Channel> result = parseM3u(new BufferedReader(new InputStreamReader(is)));
                handler.post(() -> showChannels(result));
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, "Error al leer archivo", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadM3uFromUrl(String url) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                InputStream is = new URL(url).openStream();
                List<Channel> result = parseM3u(new BufferedReader(new InputStreamReader(is)));
                handler.post(() -> showChannels(result));
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(this, "Error al cargar lista", Toast.LENGTH_SHORT).show();
                    showSelector();
                });
            }
        });
    }

    private List<Channel> parseM3u(BufferedReader reader) throws Exception {
        List<Channel> list = new ArrayList<>();
        String line;
        String name = null, logo = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#EXTINF:")) {
                name = line.contains(",") ? line.substring(line.lastIndexOf(',') + 1).trim() : "Canal";
                logo = line.contains("tvg-logo=\"") ?
                        line.replaceAll(".*tvg-logo=\"([^\"]+)\".*", "$1") : null;
            } else if (!line.startsWith("#") && !line.isEmpty() && name != null) {
                list.add(new Channel(name, line, logo, ""));
                name = null; logo = null;
            }
        }
        reader.close();
        return list;
    }

    private void showChannels(List<Channel> result) {
        progressBar.setVisibility(View.GONE);
        if (result.isEmpty()) { Toast.makeText(this, "Lista vacía", Toast.LENGTH_SHORT).show(); showSelector(); return; }
        channels.clear();
        channels.addAll(result);
        selectorPanel.setVisibility(View.GONE);
        playerPanel.setVisibility(View.VISIBLE);
        channelList.setLayoutManager(new LinearLayoutManager(this));
        channelList.setAdapter(new ChannelAdapter());
        playChannel(channels.get(0));
    }

    private void playChannel(Channel ch) {
        nowPlaying.setText(ch.name);
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    progressBar.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                }
            });
        }
        player.setMediaItem(MediaItem.fromUri(Uri.parse(ch.url)));
        player.prepare();
        player.play();
    }

    @Override
    public void onBackPressed() {
        if (playerPanel.getVisibility() == View.VISIBLE) {
            if (player != null) { player.stop(); player.release(); player = null; }
            showSelector();
        } else {
            super.onBackPressed();
        }
    }

    private void setFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) setFullscreen(); }
    @Override protected void onResume() { super.onResume(); setFullscreen(); if (player != null) player.play(); }
    @Override protected void onPause() { super.onPause(); if (player != null) player.pause(); }
    @Override protected void onDestroy() { if (player != null) { player.release(); player = null; } executor.shutdown(); super.onDestroy(); }

    private class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView name;
            VH(View v) { super(v); name = v.findViewById(R.id.iptv_channel_name); }
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_iptv_channel, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, @SuppressLint("RecyclerView") int position) {
            Channel ch = channels.get(position);
            holder.name.setText(ch.name);
            TextView epgView = holder.itemView.findViewById(R.id.iptv_channel_epg);
            if (epgView != null) epgView.setText(ch.epg != null && !ch.epg.isEmpty() ? ch.epg : "");
            holder.itemView.setOnClickListener(v -> playChannel(ch));
        }
        @Override public int getItemCount() { return channels.size(); }
    }
}
