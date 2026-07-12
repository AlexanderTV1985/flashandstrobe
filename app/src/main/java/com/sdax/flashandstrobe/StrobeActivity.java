package com.sdax.flashandstrobe;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class StrobeActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isStrobeOn = false;
    private long flashIntervalMs = 200;
    private boolean currentTorchState = false;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable strobeRunnable;

    private MaterialButton btnToggleStrobe;
    private TextView tvStatus;
    private SeekBar seekFrequency;
    private TextView tvFreqValue;

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_STROBE_WARNING_SHOWN = "strobe_warning_shown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strobe);

        btnToggleStrobe = findViewById(R.id.btnToggleStrobe);
        tvStatus = findViewById(R.id.tvStatus);
        seekFrequency = findViewById(R.id.seekFrequency);
        tvFreqValue = findViewById(R.id.tvFreqValue);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds != null && cameraIds.length > 0) {
                cameraId = cameraIds[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            tvStatus.setText(getString(R.string.about_description));
            btnToggleStrobe.setEnabled(false);
            seekFrequency.setEnabled(false);
            return;
        }

        if (cameraId == null) {
            tvStatus.setText(getString(R.string.about_warning));
            btnToggleStrobe.setEnabled(false);
            seekFrequency.setEnabled(false);
            return;
        }

        // Начальное значение частоты
        int initialHz = (int) (1000.0 / flashIntervalMs);
        tvFreqValue.setText(String.format("%d Гц", initialHz));

        seekFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                flashIntervalMs = 300 - (progress * 13);
                if (flashIntervalMs < 50) flashIntervalMs = 50;
                int hz = (int) (1000.0 / flashIntervalMs);

                // ПРАВИЛЬНО: String.format вместо getString(R.string...)
                tvFreqValue.setText(String.format("%d Гц", hz));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                setupStrobeButton();
            }
        } else {
            setupStrobeButton();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupStrobeButton();
            } else {
                tvStatus.setText(getString(R.string.tv_status_ready));
                btnToggleStrobe.setEnabled(false);
                seekFrequency.setEnabled(false);
                Toast.makeText(this, getString(R.string.permission_denied_message), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupStrobeButton() {
        btnToggleStrobe.setOnClickListener(v -> {
            v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(100)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(60)
                            .start())
                    .start();

            tryStartStrobe();
        });
        updateButtonText(isStrobeOn);
    }

    private void updateButtonText(boolean isOn) {
        if (isOn) {
            btnToggleStrobe.setText(getString(R.string.btn_strobe_off));
        } else {
            btnToggleStrobe.setText(getString(R.string.btn_strobe_on));
        }
    }

    private void tryStartStrobe() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean warned = prefs.getBoolean(KEY_STROBE_WARNING_SHOWN, false);

        if (!warned) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.alert_title))
                    .setMessage(getString(R.string.flicker_strobe_warning_message))
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                        prefs.edit().putBoolean(KEY_STROBE_WARNING_SHOWN, true).apply();
                        toggleStrobe();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            toggleStrobe();
        }
    }

    private void toggleStrobe() {
        if (cameraId == null) return;

        isStrobeOn = !isStrobeOn;
        updateButtonText(isStrobeOn);

        if (isStrobeOn) {
            currentTorchState = false;
            tvStatus.setText(getString(R.string.tv_hint_strobe));

            btnToggleStrobe.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.strobe_btn)
            );
            startStrobeLoop();
        } else {
            tvStatus.setText(getString(R.string.tv_status_ready));
            stopStrobeLoop();

            btnToggleStrobe.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.strobe_btn)
            );
        }
    }

    private void startStrobeLoop() {
        strobeRunnable = () -> {
            try {
                currentTorchState = !currentTorchState;
                cameraManager.setTorchMode(cameraId, currentTorchState);

                int colorRes = currentTorchState
                        ? R.color.strobe_btn
                        : R.color.strobe_btn_dim;

                btnToggleStrobe.setBackgroundTintList(
                        ContextCompat.getColorStateList(StrobeActivity.this, colorRes)
                );
            } catch (CameraAccessException e) {
                e.printStackTrace();
                stopStrobeLoop();
                Toast.makeText(StrobeActivity.this, getString(R.string.flash_error_message), Toast.LENGTH_SHORT).show();
                return;
            }
            handler.postDelayed(strobeRunnable, flashIntervalMs);
        };
        handler.post(strobeRunnable);
    }

    private void stopStrobeLoop() {
        handler.removeCallbacks(strobeRunnable);
        strobeRunnable = null;
        try {
            cameraManager.setTorchMode(cameraId, false);
            currentTorchState = false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStrobeLoop();
    }
}
