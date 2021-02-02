package com.stmicroelectronics.stperf;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.stmicroelectronics.stperf.service.PerfService;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private boolean mPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // debug application, enable trace whatever the kind of build
        Timber.uprootAll();
        Timber.plant(new Timber.DebugTree());

        checkPermission();
    }

    private final int REQUEST_PERMISSION_SYSTEM_ALERT_WINDOW=1;

    private void checkPermission() {
        if (!Settings.canDrawOverlays(this)) {
            mPermission = false;
            Timber.i("SYSTEM_ALERT_WINDOW permission not granted, ask for permission");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            try {
                startActivityForResult(intent, REQUEST_PERMISSION_SYSTEM_ALERT_WINDOW);
            } catch (ActivityNotFoundException e) {
                Timber.w(e, "No manage overlay settings");
            }
        } else {
            mPermission = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PERMISSION_SYSTEM_ALERT_WINDOW) {
            if (!Settings.canDrawOverlays(this)) {
                Timber.e("SYSTEM_ALERT_WINDOW permission not granted, close application");
                finish();
            } else {
                mPermission = true;
                startPerfService();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        if (mPermission) {
            startPerfService();
        }
        super.onStart();
    }

    private void startPerfService() {
        Intent intent = new Intent(MainActivity.this, PerfService.class);
        intent.setAction(PerfService.ACTION_START);
        startForegroundService(intent);
        finish();
    }

}
