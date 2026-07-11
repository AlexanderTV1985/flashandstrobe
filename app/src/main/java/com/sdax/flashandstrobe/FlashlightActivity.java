package com.sdax.flashandstrobe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashlight);

        btnToggleFlash = findViewById(R.id.btnToggleFlash);
        tvStatus = findViewById(R.id.tvStatus);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds != null) {
                for (String id : cameraIds) {
                    Boolean flashAvailable = cameraManager.getCameraCharacteristics(id)
                            .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (flashAvailable != null && flashAvailable) {
                        cameraId = id;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            tvStatus.setText(getString(R.string.tv_status_ready));
            btnToggleFlash.setEnabled(false);
            return;
        }

        if (cameraId == null) {
            tvStatus.setText(getString(R.string.about_warning));
            btnToggleFlash.setEnabled(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                setupFlashButton();
            }
        } else {
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
                tvStatus.setText(getString(R.string.tv_status_ready));
                btnToggleFlash.setEnabled(false);
                Toast.makeText(this, getString(R.string.permission_denied_message), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupFlashButton() {
        btnToggleFlash.setOnClickListener(v -> toggleFlash());
        updateButtonText(isFlashOn);
    }

    private void updateButtonText(boolean isOn) {
        Button btn = findViewById(R.id.btnToggleFlash);
        if (isOn) {
            btn.setText(getString(R.string.btn_flash_off));
        } else {
            btn.setText(getString(R.string.btn_flash_on));
        }
    }

    private void toggleFlash() {
        if (cameraId == null) return;

        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);
            updateButtonText(isFlashOn);

            if (isFlashOn) {
                tvStatus.setText(getString(R.string.tv_hint_flash));
                btnToggleFlash.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.flash_on)
                );
            } else {
                tvStatus.setText(getString(R.string.tv_status_ready));
                btnToggleFlash.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.flash_off)
                );
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.flash_error_message), Toast.LENGTH_SHORT).show();
            isFlashOn = false;
            updateButtonText(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
