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

// Важно: используем MaterialButton, а не обычный Button
import com.google.android.material.button.MaterialButton;
import android.content.res.ColorStateList;

public class SosActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isSosActive = false;
    private boolean hasFlash = false;

    // Изменили тип на MaterialButton
    private MaterialButton btnToggleSos;
    private TextView tvStatus;
    private TextView tvHint;

    // Тайминги (мс)
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

        // Поиск камеры со вспышкой (исправлено)
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
            tvStatus.setText(getString(R.string.about_description)); // или отдельная строка «Ошибка доступа к камере»
            btnToggleSos.setEnabled(false);
            return;
        }

        if (!hasFlash || cameraId == null) {
            tvStatus.setText(getString(R.string.about_warning)); // или отдельная строка «Нет вспышки»
            btnToggleSos.setEnabled(false);
            return;
        }

        // Запрос прав
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
        updateButtonText(isSosActive); // Меняем только текст кнопки

        if (isSosActive) {
            // ВКЛЮЧАЕМ SOS
            tvStatus.setText(getString(R.string.tv_hint_sos));
            startSosPattern();
            // УБРАЛИ: btnToggleSos.setBackgroundColor(...)
        } else {
            // ВЫКЛЮЧАЕМ SOS
            stopSos();
            tvStatus.setText(getString(R.string.tv_status_ready));
            // Возвращаем яркий цвет, когда режим выключен
            btnToggleSos.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sos_btn))
            );
        }
    }


    private void startSosPattern() {
        // Паттерн SOS: · · · — — — · · ·
        int[] pattern = {
                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, PAUSE_BETWEEN_LETTERS,

                DASH, PAUSE_BETWEEN_SIGNALS,
                DASH, PAUSE_BETWEEN_SIGNALS,
                DASH, PAUSE_BETWEEN_LETTERS,

                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, PAUSE_BETWEEN_SIGNALS,
                DOT, 1000 // пауза в конце цикла перед повторением
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
                    index = 0; // цикл повторяется
                }

                int duration = pattern[index];
                boolean turnOn = (index % 2 == 0); // чётные индексы — вспышка ВКЛ

                try {
                    cameraManager.setTorchMode(cameraId, turnOn);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    stopSos();
                    tvStatus.setText(getString(R.string.about_author)); // или спец. строка «Ошибка вспышки»
                    return;
                }

                // --- СИНХРОНИЗАЦИЯ ЦВЕТА КНОПКИ В ТАКТ ВСПЫШКЕ ---
                int colorRes = turnOn
                        ? R.color.sos_btn       // Яркий красный при вспышке
                        : R.color.sos_btn_dim;  // Тёмный красный при паузе

                btnToggleSos.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(SosActivity.this, colorRes))
                );
                // ----------------------------------------------------

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
