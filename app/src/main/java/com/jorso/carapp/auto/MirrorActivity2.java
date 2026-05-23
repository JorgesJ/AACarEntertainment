package com.jorso.carapp.auto;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jorso.carapp.R;

import java.util.ArrayList;
import java.util.List;

public class MirrorActivity2 extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> projectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Toast.makeText(this, "Proyeccion de pantalla iniciada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Proyeccion cancelada", Toast.LENGTH_SHORT).show();
                }
            });

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
        setContentView(R.layout.activity_mirror);

        findViewById(R.id.mirror_back_container).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.mirror_btn_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        RecyclerView list = findViewById(R.id.mirror_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new OptionAdapter(buildOptions()));
    }

    private List<MirrorOption> buildOptions() {
        List<MirrorOption> opts = new ArrayList<>();
        opts.add(new MirrorOption(
                "Proyeccion al coche",
                "Proyecta la pantalla del movil en el head unit del coche",
                "#0EA5E9",
                R.drawable.ic_home_mirror,
                this::startCarProjection
        ));
        opts.add(new MirrorOption(
                "Captura de pantalla",
                "Captura y comparte la pantalla con otras apps o dispositivos",
                "#22C55E",
                R.drawable.ic_home_mirror,
                this::startScreenCapture
        ));
        opts.add(new MirrorOption(
                "Cast a TV / Chromecast",
                "Emite contenido a un televisor en tu red WiFi",
                "#A855F7",
                R.drawable.ic_home_mirror,
                this::startCast
        ));
        return opts;
    }

    private void startCarProjection() {
        try {
            // Usar MirrorDisplay de Fermata que ya gestiona la proyeccion al coche
            Object md = null;
            if (md != null) {
                Toast.makeText(this, "Proyeccion activa", Toast.LENGTH_SHORT).show();
            } else {
                MediaProjectionManager mpm = (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);
                projectionLauncher.launch(mpm.createScreenCaptureIntent());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Conecta el movil al coche primero", Toast.LENGTH_SHORT).show();
        }
    }

    private void startScreenCapture() {
        try {
            MediaProjectionManager mpm = (MediaProjectionManager)
                    getSystemService(MEDIA_PROJECTION_SERVICE);
            projectionLauncher.launch(mpm.createScreenCaptureIntent());
        } catch (Exception e) {
            Toast.makeText(this, "No disponible en este dispositivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCast() {
        try {
            androidx.mediarouter.media.MediaRouteSelector selector =
                    new androidx.mediarouter.media.MediaRouteSelector.Builder()
                    .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_LIVE_VIDEO)
                    .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .build();
            androidx.mediarouter.app.MediaRouteChooserDialog dialog =
                    new androidx.mediarouter.app.MediaRouteChooserDialog(this);
            dialog.setRouteSelector(selector);
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Cast no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) setFullscreen(); }

    static class MirrorOption {
        String title, desc, color;
        int icon;
        Runnable action;
        MirrorOption(String t, String d, String c, int i, Runnable a) {
            title=t; desc=d; color=c; icon=i; action=a;
        }
    }

    private class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.VH> {
        private final List<MirrorOption> list;
        OptionAdapter(List<MirrorOption> l) { list = l; }

        class VH extends RecyclerView.ViewHolder {
            TextView title, desc;
            View colorBar;
            ImageButton icon;
            VH(View v) {
                super(v);
                title    = v.findViewById(R.id.mirror_opt_title);
                desc     = v.findViewById(R.id.mirror_opt_desc);
                colorBar = v.findViewById(R.id.mirror_opt_color);
                icon     = v.findViewById(R.id.mirror_opt_icon);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_mirror_option, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            MirrorOption o = list.get(pos);
            h.title.setText(o.title);
            h.desc.setText(o.desc);
            try { h.colorBar.setBackgroundColor(android.graphics.Color.parseColor(o.color)); } catch (Exception e) {}
            h.icon.setImageResource(o.icon);
            try { h.icon.setColorFilter(android.graphics.Color.parseColor(o.color)); } catch (Exception e) {}
            h.itemView.setOnClickListener(v -> o.action.run());
        }

        @Override public int getItemCount() { return list.size(); }
    }
}
