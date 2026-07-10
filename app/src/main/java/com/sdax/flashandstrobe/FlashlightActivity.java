package com.sdax.flashandstrobe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class FlashlightActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;
    private Button btnToggleFlash;
    private TextView tvStatus;

    // Код запроса прав (для API 23+)
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashlight);

        btnToggleFlash = findViewById(R.id.btnToggleFlash);
        tvStatus = findViewById(R.id.tvStatus);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            // Получаем ID камеры со вспышкой (обычно это задняя камера)
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                if (cameraManager.getCameraCharacteristics(
                                cameraManager.getCameraIdList()[0])
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            tvStatus.setText("Ошибка доступа к камере");
            btnToggleFlash.setEnabled(false);
        }

        // Если нет камеры со вспышкой — сразу отключаем кнопку
        if (cameraId == null) {
            tvStatus.setText("На этом устройстве нет вспышки");
            btnToggleFlash.setEnabled(false);
            return;
        }

        // Проверка и запрос прав для Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                // Права уже есть — можно работать
                setupFlashButton();
            }
        } else {
            // Для старых Android (ниже 6.0) права уже в манифесте — можно работать
            setupFlashButton();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupFlashButton();
            } else {
                tvStatus.setText("Нужны права на камеру, чтобы включить фонарик");
                btnToggleFlash.setEnabled(false);
                Toast.makeText(this, "Без разрешения фонарик не работает", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupFlashButton() {
        btnToggleFlash.setOnClickListener(v -> toggleFlash());
    }

    private void toggleFlash() {
        if (cameraId == null) return;

        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);

            if (isFlashOn) {
                btnToggleFlash.setText("ВЫКЛЮЧИТЬ ФОНАРИК");
                btnToggleFlash.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.flash_on)
                );
                tvStatus.setText("Фонарик включён");
            } else {
                btnToggleFlash.setText("ВКЛЮЧИТЬ ФОНАРИК");
                btnToggleFlash.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.flash_off)
                );
                tvStatus.setText("Фонарик выключен");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка управления вспышкой", Toast.LENGTH_SHORT).show();
            isFlashOn = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Важно: выключаем фонарик, если пользователь ушёл с экрана
        if (isFlashOn && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false);
                isFlashOn = false;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
