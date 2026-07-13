package com.sdax.flashandstrobe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import android.content.res.ColorStateList;

public class SosActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isSosActive = false;
    private boolean hasFlash = false;

    private MaterialButton btnToggleSos;
    private TextView tvStatus;
    private TextView tvHint;

    private static final int DOT = 150;
    private static final int DASH = 450;
    private static final int PAUSE_BETWEEN_SIGNALS = 150;
    private static final int PAUSE_BETWEEN_LETTERS = 300;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sosRunnable;

    private static final int REQUEST_CAMERA_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        btnToggleSos = findViewById(R.id.btnToggleSos);
        tvStatus = findViewById(R.id.tvStatus);
        tvHint = findViewById(R.id.tvHint);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds != null) {
                for (String id : cameraIds) {
                    Boolean flashAvailable = cameraManager.getCameraCharacteristics(id)
                            .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (flashAvailable != null && flashAvailable) {
                        cameraId = id;
                        hasFlash = true;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            tvStatus.setText(getString(R.string.about_description));
            btnToggleSos.setEnabled(false);
            return;
        }

        if (!hasFlash || cameraId == null) {
            tvStatus.setText(getString(R.string.about_warning));
            btnToggleSos.setEnabled(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                setupSosButton();
            }
        } else {
            setupSosButton();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupSosButton();
            } else {
                tvStatus.setText(getString(R.string.about_description));
                btnToggleSos.setEnabled(false);
                Toast.makeText(this, getString(R.string.about_license), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSosButton() {
        btnToggleSos.setOnClickListener(v -> toggleSos());
        updateButtonText(isSosActive);
    }

    private void updateButtonText(boolean isActive) {
        if (isActive) {
            btnToggleSos.setText(getString(R.string.btn_sos_off));
        } else {
            btnToggleSos.setText(getString(R.string.btn_sos_on));
        }
    }

    private void toggleSos() {
        if (!hasFlash || cameraId == null) return;

        isSosActive = !isSosActive;
        updateButtonText(isSosActive);

        if (isSosActive) {
            tvStatus.setText(getString(R.string.tv_hint_sos));
            startSosPattern();
        } else {
            stopSos();
            tvStatus.setText(getString(R.string.tv_status_ready));
            btnToggleSos.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sos_btn))
            );
        }
    }

    private void startSosPattern() {
        int[] pattern = {
                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, PAUSE_BETWEEN_LETTERS,

                DASH, PAUSE_BETWEEN_SIGNALS,
                DASH, PAUSE_BETWEEN_SIGNALS,
                DASH, PAUSE_BETWEEN_LETTERS,

                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, 1000
        };

        sosRunnable = new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                if (!isSosActive || cameraId == null || !hasFlash) {
                    stopSos();
                    return;
                }

                if (index >= pattern.length) {
                    index = 0;
                }

                int duration = pattern[index];
                boolean turnOn = (index % 2 == 0);

                try {
                    cameraManager.setTorchMode(cameraId, turnOn);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    stopSos();
                    tvStatus.setText(getString(R.string.about_author));
                    return;
                }

                int colorRes = turnOn
                        ? R.color.sos_btn
                        : R.color.sos_btn_dim;

                btnToggleSos.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(SosActivity.this, colorRes))
                );

                index++;
                handler.postDelayed(this, duration);
            }
        };

        handler.post(sosRunnable);
    }

    private void stopSos() {
        isSosActive = false;
        handler.removeCallbacks(sosRunnable);
        try {
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSos();
    }
}
