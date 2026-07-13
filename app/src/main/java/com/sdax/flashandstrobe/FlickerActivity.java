package com.sdax.flashandstrobe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import android.content.res.ColorStateList;

import java.util.Random;

public class FlickerActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlickerActive = false;
    private boolean hasFlash = false;

    private MaterialButton btnToggleFlicker;
    private android.widget.TextView tvStatus;

    private static final int MIN_FLASH_DURATION = 80;
    private static final int MAX_FLASH_DURATION = 250;
    private static final int RARE_MIN_PAUSE = 400;
    private static final int RARE_MAX_PAUSE = 1200;
    private static final int DENSE_MIN_PAUSE = 100;
    private static final int DENSE_MAX_PAUSE = 300;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable flickerRunnable;
    private Random random = new Random();

    private static final int REQUEST_CAMERA_PERMISSION = 102;
    private static final int REQUEST_WARNING_DIALOG = 1001;

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_FLICKER_WARNING_SHOWN = "flicker_warning_shown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flicker);

        btnToggleFlicker = findViewById(R.id.btnToggleFlicker);
        tvStatus = findViewById(R.id.tvStatus);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds == null || cameraIds.length == 0) {
                tvStatus.setText(getString(R.string.about_description));
                btnToggleFlicker.setEnabled(false);
                return;
            }

            for (String id : cameraIds) {
                Boolean flashAvailable = cameraManager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (flashAvailable != null && flashAvailable) {
                    cameraId = id;
                    hasFlash = true;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            tvStatus.setText(getString(R.string.about_description));
            btnToggleFlicker.setEnabled(false);
            return;
        }

        if (!hasFlash || cameraId == null) {
            tvStatus.setText(getString(R.string.about_warning));
            btnToggleFlicker.setEnabled(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                setupFlickerButton();
            }
        } else {
            setupFlickerButton();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupFlickerButton();
            } else {
                tvStatus.setText(getString(R.string.tv_status_ready));
                btnToggleFlicker.setEnabled(false);
                Toast.makeText(this, getString(R.string.permission_denied_message), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupFlickerButton() {
        updateButtonText(isFlickerActive);
        btnToggleFlicker.setOnClickListener(v -> tryStartFlicker());
    }

    private void updateButtonText(boolean isActive) {
        if (isActive) {
            btnToggleFlicker.setText(getString(R.string.btn_flicker_off));
        } else {
            btnToggleFlicker.setText(getString(R.string.btn_flicker_on));
        }
    }

    private void tryStartFlicker() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean warned = prefs.getBoolean(KEY_FLICKER_WARNING_SHOWN, false);

        if (!warned) {
            Intent intent = new Intent(this, WarningActivity.class);
            intent.putExtra(WarningActivity.EXTRA_ACTION, WarningActivity.ACTION_FLICKER);
            startActivityForResult(intent, REQUEST_WARNING_DIALOG);
        } else {
            toggleFlicker();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WARNING_DIALOG && resultCode == RESULT_OK) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_FLICKER_WARNING_SHOWN, true).apply();
            toggleFlicker();
        }
    }

    private void toggleFlicker() {
        isFlickerActive = !isFlickerActive;
        updateButtonText(isFlickerActive);

        if (isFlickerActive) {
            tvStatus.setText(getString(R.string.tv_status_flicker_active));
            startFlickering();
        } else {
            stopFlickering();
            tvStatus.setText(getString(R.string.tv_status_ready));
            btnToggleFlicker.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.flicker_btn))
            );
        }
    }

    private void startFlickering() {
        flickerRunnable = () -> {
            try {
                cameraManager.setTorchMode(cameraId, true);

                btnToggleFlicker.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(FlickerActivity.this, R.color.flicker_btn))
                );

                int flashDuration = random.nextInt(MAX_FLASH_DURATION - MIN_FLASH_DURATION + 1) + MIN_FLASH_DURATION;

                handler.postDelayed(() -> {
                    try {
                        cameraManager.setTorchMode(cameraId, false);

                        btnToggleFlicker.setBackgroundTintList(
                                ColorStateList.valueOf(ContextCompat.getColor(FlickerActivity.this, R.color.flicker_btn_dim))
                        );

                        int pause;
                        if (random.nextBoolean()) {
                            pause = random.nextInt(RARE_MAX_PAUSE - RARE_MIN_PAUSE + 1) + RARE_MIN_PAUSE;
                        } else {
                            pause = random.nextInt(DENSE_MAX_PAUSE - DENSE_MIN_PAUSE + 1) + DENSE_MIN_PAUSE;
                        }

                        if (isFlickerActive) {
                            handler.postDelayed(flickerRunnable, pause);
                        }
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        stopFlickering();
                        Toast.makeText(FlickerActivity.this, getString(R.string.flash_error_message), Toast.LENGTH_SHORT).show();
                    }
                }, flashDuration);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                stopFlickering();
                Toast.makeText(FlickerActivity.this, getString(R.string.flash_error_message), Toast.LENGTH_SHORT).show();
            }
        };

        handler.post(flickerRunnable);
    }

    private void stopFlickering() {
        isFlickerActive = false;
        if (flickerRunnable != null) {
            handler.removeCallbacks(flickerRunnable);
            flickerRunnable = null;
        }
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
        stopFlickering();
    }
}
