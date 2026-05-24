package com.jorso.carapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import com.jorso.carapp.auto.HubActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionsIfNeeded();
    }

    private void requestPermissionsIfNeeded() {
        List<String> required = new ArrayList<>();
        required.add(Manifest.permission.ACCESS_FINE_LOCATION);
        required.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        required.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.READ_MEDIA_AUDIO);
            required.add(Manifest.permission.READ_MEDIA_VIDEO);
            required.add(Manifest.permission.READ_MEDIA_IMAGES);
            required.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            required.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        List<String> missing = new ArrayList<>();
        for (String perm : required) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }

        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), REQ_PERMISSIONS);
        } else {
            launchHub();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        launchHub();
    }

    private void launchHub() {
        startActivity(new Intent(this, HubActivity.class));
        finish();
    }
}
