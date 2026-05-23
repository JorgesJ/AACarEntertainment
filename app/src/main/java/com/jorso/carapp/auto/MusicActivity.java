package com.jorso.carapp.auto;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jorso.carapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicActivity extends AppCompatActivity {

    private static final String TAG = "MusicActivity";
    private static final String PREF_NAME = "music_prefs";
    private static final String PREF_FOLDER_URI = "last_folder_uri";

    private ExoPlayer player;
    private RecyclerView songList;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private ImageButton btnPlay, btnPrev, btnNext;
    private SeekBar seekBar;
    private View emptyPanel, playerPanel;

    private final List<Song> songs = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isUpdatingSeek = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            if (player != null && !isUpdatingSeek) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0) {
                    seekBar.setProgress((int)(pos * 100 / dur));
                    tvCurrent.setText(formatTime(pos));
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<Uri> folderPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                Log.e(TAG, "Folder picker result: " + uri);
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {
                        Log.e(TAG, "takePersistableUriPermission failed", e);
                    }
                    getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putString(PREF_FOLDER_URI, uri.toString()).apply();
                    loadSongsFromFolder(uri);
                } else {
                    Log.e(TAG, "Folder picker returned null");
                }
            });

    static class Song {
        String title, artist;
        Uri uri;
        long duration;
        Song(String title, String artist, Uri uri, long duration) {
            this.title = title; this.artist = artist;
            this.uri = uri; this.duration = duration;
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
        setContentView(R.layout.activity_music);

        songList    = findViewById(R.id.music_song_list);
        tvTitle     = findViewById(R.id.music_title);
        tvArtist    = findViewById(R.id.music_artist);
        tvCurrent   = findViewById(R.id.music_current);
        tvTotal     = findViewById(R.id.music_total);
        btnPlay     = findViewById(R.id.music_btn_play);
        btnPrev     = findViewById(R.id.music_btn_prev);
        btnNext     = findViewById(R.id.music_btn_next);
        seekBar     = findViewById(R.id.music_seekbar);
        emptyPanel  = findViewById(R.id.music_empty);
        playerPanel = findViewById(R.id.music_player_panel);

        findViewById(R.id.music_back_container).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.music_btn_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.music_btn_add_folder).setOnClickListener(v -> {
            Log.e(TAG, "Opening folder picker");
            folderPicker.launch(null);
        });

        btnPlay.setOnClickListener(v -> togglePlay());
        btnPrev.setOnClickListener(v -> playIndex(currentIndex - 1));
        btnNext.setOnClickListener(v -> playIndex(currentIndex + 1));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && player != null) player.seekTo(player.getDuration() * p / 100);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { isUpdatingSeek = true; }
            @Override public void onStopTrackingTouch(SeekBar s) { isUpdatingSeek = false; }
        });

        songList.setLayoutManager(new LinearLayoutManager(this));
        handler.post(updateProgress);

        android.content.SharedPreferences settingsPrefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        boolean autoplay = settingsPrefs.getBoolean(SettingsActivity.KEY_AUTOPLAY, false);

        String savedUri = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(PREF_FOLDER_URI, null);
        if (savedUri != null) {
            // cargando silencioso
            loadSongsFromFolder(Uri.parse(savedUri));
        } else {
            showEmpty();
        }
    }

    private void loadSongsFromFolder(Uri treeUri) {
        Log.e(TAG, "loadSongsFromFolder: " + treeUri);
        executor.execute(() -> {
            List<Song> result = new ArrayList<>();
            try {
                DocumentFile dir = DocumentFile.fromTreeUri(this, treeUri);
                Log.e(TAG, "DocumentFile: " + (dir != null ? dir.getName() : "null"));
                if (dir != null) scanDir(dir, result);
            } catch (Exception e) {
                Log.e(TAG, "Error scanning folder", e);
            }
            Log.e(TAG, "Found songs: " + result.size());
            handler.post(() -> {
                songs.clear();
                songs.addAll(result);
                if (songs.isEmpty()) {
                    Log.e(TAG, "No songs found");
                    return;
                }
                songList.setAdapter(new SongAdapter());
                songList.setVisibility(View.VISIBLE);
                showPlayer();
                boolean autoplay = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE).getBoolean(SettingsActivity.KEY_AUTOPLAY, false);
                if (autoplay) playIndex(0);
                if (autoplay) playIndex(0);
            });
        });
    }

    private void scanDir(DocumentFile dir, List<Song> result) {
        DocumentFile[] files = dir.listFiles();
        Log.e(TAG, "Files in dir: " + (files != null ? files.length : 0));
        if (files == null) return;
        for (DocumentFile f : files) {
            if (f.isDirectory()) {
                scanDir(f, result);
            } else {
                String mime = f.getType();
                String name = f.getName();
                Log.e(TAG, "File: " + name + " mime: " + mime);
                if (mime != null && (mime.startsWith("audio/") ||
                        (name != null && (name.endsWith(".mp3") || name.endsWith(".flac") ||
                         name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a"))))) {
                    String title = name != null ? name.replaceAll("\\.[^.]+$", "") : "Desconocido";
                    result.add(new Song(title, "Desconocido", f.getUri(), 0));
                }
            }
        }
    }

    private boolean isVlcEngine() {
        return "vlc".equals(getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE)
                .getString(SettingsActivity.KEY_ENGINE, "exo"));
    }

    private void playWithVlc(Song s) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setPackage("org.videolan.vlc");
            intent.setDataAndType(s.uri, "audio/*");
            intent.putExtra("title", s.title);
            startActivity(intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "VLC no disponible", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void playIndex(int index) {
        if (songs.isEmpty()) return;
        currentIndex = Math.max(0, Math.min(index, songs.size() - 1));
        Song s = songs.get(currentIndex);
        tvTitle.setText(s.title);
        tvArtist.setText(s.artist);
        seekBar.setProgress(0);
        tvCurrent.setText("0:00");
        if (isVlcEngine()) { playWithVlc(songs.get(currentIndex)); return; }

        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) playIndex(currentIndex + 1);
                    if (state == Player.STATE_READY) tvTotal.setText(formatTime(player.getDuration()));
                }
                @Override public void onIsPlayingChanged(boolean isPlaying) {
                    btnPlay.setImageResource(isPlaying ? R.drawable.ic_home_pause : R.drawable.ic_home_play);
                }
            });
        }
        player.setMediaItem(MediaItem.fromUri(s.uri));
        player.prepare();
        player.play();
    }

    private void togglePlay() {
        if (player == null && !songs.isEmpty()) { playIndex(currentIndex); return; }
        if (player == null) return;
        if (player.isPlaying()) player.pause(); else player.play();
    }

    private void showLoading() {
        tvTitle.setText("Cargando...");
        emptyPanel.setVisibility(View.GONE);
        playerPanel.setVisibility(View.VISIBLE);
    }

    private void showEmpty() { emptyPanel.setVisibility(View.VISIBLE); playerPanel.setVisibility(View.GONE); }
    private void showPlayer() { emptyPanel.setVisibility(View.GONE); playerPanel.setVisibility(View.VISIBLE); }

    private String formatTime(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
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
    @Override protected void onDestroy() {
        handler.removeCallbacks(updateProgress);
        if (player != null) { player.release(); player = null; }
        executor.shutdown();
        super.onDestroy();
    }

    private class SongAdapter extends RecyclerView.Adapter<SongAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView title, artist;
            VH(View v) { super(v); title = v.findViewById(R.id.song_title); artist = v.findViewById(R.id.song_artist); }
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_song, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, @SuppressLint("RecyclerView") int pos) {
            Song s = songs.get(pos);
            h.title.setText(s.title);
            h.artist.setText(s.artist);
            h.itemView.setAlpha(pos == currentIndex ? 1f : 0.7f);
            h.itemView.setOnClickListener(v -> playIndex(pos));
        }
        @Override public int getItemCount() { return songs.size(); }
    }
}

