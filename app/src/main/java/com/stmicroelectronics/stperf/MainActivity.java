package com.stmicroelectronics.stperf;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.stmicroelectronics.stperf.service.PerfService;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private boolean mPermissionOverlay = false;
    private boolean mPermissionNotification = false;
    private final int REQUEST_PERMISSION_NOTIFICATION_STATE=1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    private void checkPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Timber.i("SYSTEM_ALERT_WINDOW permission not granted, ask for permission");

            ActivityResultLauncher<Intent> mGetActivityResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            mPermissionOverlay = true;
                            if (mPermissionNotification) {
                                startPerfService();
                            }
                        } else {
                            if (!Settings.canDrawOverlays(getApplicationContext())) {
                                Timber.e("SYSTEM_ALERT_WINDOW permission not granted, close application");
                                finish();
                            }
                        }
                    });
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            mGetActivityResult.launch(intent);
        } else {
            mPermissionOverlay = true;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSION_NOTIFICATION_STATE);
        } else {
            mPermissionNotification = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_NOTIFICATION_STATE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermissionNotification = true;
                if (mPermissionOverlay) {
                    startPerfService();
                }
            } else {
                Timber.w("Notification permission not granted");
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        if (mPermissionOverlay && mPermissionNotification) {
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
