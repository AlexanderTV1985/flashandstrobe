package com.sdax.flashandstrobe;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class FlickerActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlickerActive = false;
    private boolean hasFlash = false;

    private Button btnToggleFlicker;
    private TextView tvStatus;

    // Базовые диапазоны
    private static final int MIN_FLASH_DURATION = 80;
    private static final int MAX_FLASH_DURATION = 250;

    // Для «редких помех» — большие паузы
    private static final int RARE_MIN_PAUSE = 400;
    private static final int RARE_MAX_PAUSE = 1200;

    // Для «плотного шума» — маленькие паузы
    private static final int DENSE_MIN_PAUSE = 100;
    private static final int DENSE_MAX_PAUSE = 300;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable flickerRunnable;
    private Random random = new Random();

    private static final int REQUEST_CAMERA_PERMISSION = 102;
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
                tvStatus.setText("Нет камер на устройстве");
                btnToggleFlicker.setEnabled(false);
                return;
            }

            Boolean flashAvailable = cameraManager.getCameraCharacteristics(cameraIds[0])
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);

            if (flashAvailable != null && flashAvailable) {
                cameraId = cameraIds[0];
                hasFlash = true;
            } else {
                for (String id : cameraIds) {
                    flashAvailable = cameraManager.getCameraCharacteristics(id)
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
            tvStatus.setText("Ошибка доступа к камере");
            btnToggleFlicker.setEnabled(false);
            return;
        }

        if (!hasFlash || cameraId == null) {
            tvStatus.setText("На этом устройстве нет вспышки");
            btnToggleFlicker.setEnabled(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
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
                tvStatus.setText("Нужны права на камеру для режима мерцания");
                btnToggleFlicker.setEnabled(false);
                Toast.makeText(this, "Без разрешения режим мерцания не работает", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupFlickerButton() {
        btnToggleFlicker.setOnClickListener(v -> tryStartFlicker());
    }

    /**
     * Входной шлюз: сначала проверяет, нужно ли показать предупреждение.
     */
    private void tryStartFlicker() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean warned = prefs.getBoolean(KEY_FLICKER_WARNING_SHOWN, false);

        if (!warned) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Внимание")
                    .setMessage("Режим мерцания (Flicker) может быть опасен для людей с фоточувствительной эпилепсией. Используйте с осторожностью.")
                    .setPositiveButton("Понял, включаю", (dialog, which) -> {
                        prefs.edit().putBoolean(KEY_FLICKER_WARNING_SHOWN, true).apply();
                        toggleFlicker(); // Запускаем реальную логику
                    })
                    .setCancelable(false)
                    .show();
        } else {
            toggleFlicker();
        }
    }

    private void toggleFlicker() {
        if (!hasFlash || cameraId == null) return;

        isFlickerActive = !isFlickerActive;

        if (isFlickerActive) {
            btnToggleFlicker.setText("ВЫКЛЮЧИТЬ МЕРЦАНИЕ");
            btnToggleFlicker.setBackgroundColor(ContextCompat.getColor(this, R.color.flicker_btn));
            tvStatus.setText("Мерцание: режим «космический шум» активен");
            startFlickerLoop();
        } else {
            stopFlicker();
            btnToggleFlicker.setText("ВКЛЮЧИТЬ МЕРЦАНИЕ");
            btnToggleFlicker.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_off));
            tvStatus.setText("Мерцание остановлено");
        }
    }

    private void startFlickerLoop() {
        flickerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFlickerActive || cameraId == null || !hasFlash) {
                    stopFlicker();
                    return;
                }

                boolean isDenseNoise = random.nextBoolean();

                int minPause = isDenseNoise ? DENSE_MIN_PAUSE : RARE_MIN_PAUSE;
                int maxPause = isDenseNoise ? DENSE_MAX_PAUSE : RARE_MAX_PAUSE;

                int segmentLength = random.nextInt(3) + 2; // от 2 до 4 вспышек в группе

                runSegment(0, segmentLength, minPause, maxPause);
            }
        };

        handler.post(flickerRunnable);
    }

    private void runSegment(int currentIndex, int totalCount, int minPause, int maxPause) {
        if (!isFlickerActive || cameraId == null || !hasFlash) {
            stopFlicker();
            return;
        }

        if (currentIndex >= totalCount) {
            int longPause = random.nextInt(600) + 400; // пауза 400–1000 мс между «волнами»
            handler.postDelayed(() -> {
                handler.post(flickerRunnable);
            }, longPause);
            return;
        }

        int flashDuration = random.nextInt(MAX_FLASH_DURATION - MIN_FLASH_DURATION + 1) + MIN_FLASH_DURATION;
        int pauseDuration = random.nextInt(maxPause - minPause + 1) + minPause;

        try {
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            stopFlicker();
            tvStatus.setText("Ошибка вспышки");
            return;
        }

        handler.postDelayed(() -> {
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                stopFlicker();
                tvStatus.setText("Ошибка вспышки");
                return;
            }

            handler.postDelayed(() ->
                            runSegment(currentIndex + 1, totalCount, minPause, maxPause),
                    pauseDuration
            );
        }, flashDuration);
    }

    private void stopFlicker() {
        isFlickerActive = false;
        handler.removeCallbacks(flickerRunnable);
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
        stopFlicker();
    }
}
