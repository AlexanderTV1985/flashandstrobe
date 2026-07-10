package com.sdax.flashandstrobe;

import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StrobeActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isStrobeOn = false;
    private long flashIntervalMs = 200;

    // Это поле нужно, чтобы мы сами хранили следующее состояние вспышки
    private boolean nextTorchState = true;

    private Handler handler = new Handler();
    private Runnable strobeRunnable;

    // Переменные названы так же, как ID в XML
    private Button btnToggleStrobe;
    private TextView tvStatusStrobe;
    private SeekBar seekFrequency;
    private TextView tvFreqValue;

    private static final int REQUEST_CAMERA_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strobe);

        // Эти строки ПРАВИЛЬНЫЕ. Не меняй их.
        btnToggleStrobe = findViewById(R.id.btnToggleStrobe);
        tvStatusStrobe = findViewById(R.id.tvStatusStrobe);
        seekFrequency = findViewById(R.id.seekFrequency);
        tvFreqValue = findViewById(R.id.tvFreqValue);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length > 0) {
                cameraId = cameraIds[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            tvStatusStrobe.setText("Ошибка доступа к камере");
            btnToggleStrobe.setEnabled(false);
        }

        if (cameraId == null) {
            tvStatusStrobe.setText("На этом устройстве нет вспышки");
            btnToggleStrobe.setEnabled(false);
            seekFrequency.setEnabled(false);
        } else {
            seekFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    flashIntervalMs = 300 - (progress * 13);
                    if (flashIntervalMs < 50) flashIntervalMs = 50;
                    tvFreqValue.setText("Частота: " + (1000 / flashIntervalMs) + " Гц");
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupStrobeButton();
            } else {
                tvStatusStrobe.setText("Нужны права на камеру для стробоскопа");
                btnToggleStrobe.setEnabled(false);
                Toast.makeText(this, "Без разрешения стробоскоп не работает", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupStrobeButton() {
        btnToggleStrobe.setOnClickListener(v -> toggleStrobe());
    }

    private void toggleStrobe() {
        if (cameraId == null) return;

        isStrobeOn = !isStrobeOn;

        if (isStrobeOn) {
            btnToggleStrobe.setText("ВЫКЛЮЧИТЬ СТРОБОСКОП");
            // Если у тебя нет цветов в colors.xml — закомментируй строку ниже
            btnToggleStrobe.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_on));

            tvStatusStrobe.setText("Стробоскоп работает");
            startStrobeLoop();
        } else {
            btnToggleStrobe.setText("ВКЛЮЧИТЬ СТРОБОСКОП");
            btnToggleStrobe.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_off));

            tvStatusStrobe.setText("Готов к работе");
            stopStrobeLoop();
        }
    }

    private void startStrobeLoop() {
        strobeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isStrobeOn) {
                    stopStrobeLoop();
                    return;
                }

                try {
                    // Мы сами управляем состоянием, поэтому не нужно спрашивать камеру
                    cameraManager.setTorchMode(cameraId, nextTorchState);
                    nextTorchState = !nextTorchState; // инвертируем для следующего кадра
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    stopStrobeLoop();
                }
                handler.postDelayed(this, flashIntervalMs);
            }
        };
        handler.post(strobeRunnable);
    }

    private void stopStrobeLoop() {
        handler.removeCallbacks(strobeRunnable);
        try {
            cameraManager.setTorchMode(cameraId, false);
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
