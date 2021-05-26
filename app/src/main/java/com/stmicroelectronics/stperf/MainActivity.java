package com.stmicroelectronics.stperf;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.stmicroelectronics.stperf.service.PerfService;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private boolean mPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    private void checkPermission() {
        if (!Settings.canDrawOverlays(this)) {
            mPermission = false;
            Timber.i("SYSTEM_ALERT_WINDOW permission not granted, ask for permission");

            ActivityResultLauncher<Intent> mGetActivityResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        mPermission = true;
                        startPerfService();
                    } else {
                        if (!Settings.canDrawOverlays(getApplicationContext())) {
                            Timber.e("SYSTEM_ALERT_WINDOW permission not granted, close application");
                            finish();
                        }
                    }
                }
            });
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            mGetActivityResult.launch(intent);
        } else {
            mPermission = true;
        }
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
