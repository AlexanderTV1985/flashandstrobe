package com.sdax.flashandstrobe;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.button.MaterialButton;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;

public class FlashlightActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    private MaterialButton btnToggleFlash;
    private TextView tvStatus;
    private LinearLayout timerPanel;
    private TextView tvTimerDisplay;

    private AppCompatImageView btnTimerDec;
    private AppCompatImageView btnTimerInc;
    private MaterialButton btnTimerToggle;

    private static final String PREFS_NAME = "flashlight_prefs";
    private static final String KEY_TIMER_ENABLED = "timer_enabled";
    private static final String KEY_TIMEOUT_MS = "timeout_ms";

    private SharedPreferences prefs;
    private CountDownTimer timer;

    private long currentTimerSeconds = 30;
    private boolean isTimerActive = false;

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    // Цвета для состояний
    private ColorStateList colorTimerDisabled;
    private ColorStateList colorTimerEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashlight);

        btnToggleFlash = findViewById(R.id.btnToggleFlash);
        tvStatus = findViewById(R.id.tvStatus);
        timerPanel = findViewById(R.id.timerPanel);
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);

        btnTimerDec = findViewById(R.id.btnTimerDec);
        btnTimerInc = findViewById(R.id.btnTimerInc);
        btnTimerToggle = findViewById(R.id.btnTimerToggle);

        // Инициализация цветовых состояний
        int colorDisabled = ContextCompat.getColor(this, R.color.timer_text_color_disabled); // Должен быть в colors.xml (серый)
        int colorEnabled = ContextCompat.getColor(this, R.color.timer_text_color);           // Цвет из XML

        colorTimerDisabled = ColorStateList.valueOf(colorDisabled);
        colorTimerEnabled = ColorStateList.valueOf(colorEnabled);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        long savedMs = prefs.getLong(KEY_TIMEOUT_MS, 30_000);
        currentTimerSeconds = savedMs / 1000;
        isTimerActive = prefs.getBoolean(KEY_TIMER_ENABLED, false);

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

        setupTimerControls();

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

        // Применяем начальное состояние UI
        updateTimerUIState();
    }

    private void setupTimerControls() {
        updateTimerDisplayText();

        btnTimerDec.setOnClickListener(v -> {
            if (currentTimerSeconds >= 5) {
                currentTimerSeconds -= 5;
                updateTimerDisplayText();
                saveTimerState();
            }
        });

        btnTimerInc.setOnClickListener(v -> {
            if (currentTimerSeconds < 180) {
                currentTimerSeconds += 5;
                updateTimerDisplayText();
                saveTimerState();
            }
        });

        btnTimerToggle.setOnClickListener(v -> {
            // Если таймер неактивен И фонарик ВКЛ — вообще не обрабатываем клик
            if (!isTimerActive && isFlashOn) {
                return; // ничего не делаем, кнопка не реагирует
            }

            isTimerActive = !isTimerActive;
            if (isTimerActive) {
                btnTimerToggle.setText(getString(R.string.timer_on));
            } else {
                btnTimerToggle.setText(getString(R.string.timer_off));
                stopTimer();
            }
            saveTimerState();
            updateTimerUIState(); // пересчитаем цвета и enabled
        });

        // УБРАЛИ: ручное задание цвета при старте
        if (isTimerActive) {
            btnTimerToggle.setText(getString(R.string.timer_on));
        } else {
            btnTimerToggle.setText(getString(R.string.timer_off));
        }
    }


    private void updateTimerUIState() {
        int myGrayColor = ContextCompat.getColor(this, R.color.timer_text_color_disabled);
        ColorStateList grayTint = ColorStateList.valueOf(myGrayColor);

        // Цвет активной кнопки — тот самый оранжевый из flash_on
        int activeBtnColor = ContextCompat.getColor(this, R.color.flash_on);
        ColorStateList activeBtnTint = ColorStateList.valueOf(activeBtnColor);

        if (!isTimerActive) {
            // Сценарий 1: таймер НЕАКТИВЕН
            setTimerControlsEnabled(false, colorTimerDisabled);
            tvTimerDisplay.setTextColor(myGrayColor);

            if (isFlashOn) {
                // Таймер неактивен, фонарик ВКЛ → серая, НЕ нажимается
                btnTimerToggle.setEnabled(false);
                btnTimerToggle.setBackgroundTintList(grayTint);
            } else {
                // Таймер неактивен, фонарик ВЫКЛ → серая, нажимается
                btnTimerToggle.setEnabled(true);
                btnTimerToggle.setBackgroundTintList(grayTint);
            }
        } else {
            // Таймер АКТИВЕН
            if (isFlashOn) {
                // Сценарий 2: таймер активен, фонарик ВКЛ → серая, НЕ нажимается
                btnTimerDec.setEnabled(false);
                btnTimerInc.setEnabled(false);
                btnTimerDec.setImageTintList(colorTimerEnabled);
                btnTimerInc.setImageTintList(colorTimerEnabled);

                btnTimerToggle.setEnabled(false);
                btnTimerToggle.setBackgroundTintList(grayTint);

                tvTimerDisplay.setTextColor(ContextCompat.getColor(this, R.color.timer_text_color));
            } else {
                // Сценарий 3: таймер активен, фонарик ВЫКЛ → оранжевая (flash_on), нажимается
                setTimerControlsEnabled(true, colorTimerEnabled);
                tvTimerDisplay.setTextColor(ContextCompat.getColor(this, R.color.timer_text_color));

                btnTimerToggle.setEnabled(true);
                btnTimerToggle.setBackgroundTintList(activeBtnTint); // <-- твой цвет #FF5722
            }
        }
    }
    private void setTimerControlsEnabled(boolean enabled, ColorStateList tint) {
        btnTimerDec.setEnabled(enabled);
        btnTimerInc.setEnabled(enabled);
        btnTimerDec.setImageTintList(tint);
        btnTimerInc.setImageTintList(tint);
    }

    private void updateTimerDisplayText() {
        int minutes = (int) (currentTimerSeconds / 60);
        int seconds = (int) (currentTimerSeconds % 60);
        String text = String.format("%02d:%02d", minutes, seconds);
        tvTimerDisplay.setText(text);
    }

    private void saveTimerState() {
        prefs.edit()
                .putBoolean(KEY_TIMER_ENABLED, isTimerActive)
                .putLong(KEY_TIMEOUT_MS, currentTimerSeconds * 1000L)
                .apply();
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

    private void toggleFlash() {
        if (cameraId == null) return;

        try {
            if (!isTimerActive) {
                // Логика без таймера
                isFlashOn = !isFlashOn;
                cameraManager.setTorchMode(cameraId, isFlashOn);
                updateButtonText(isFlashOn);

                if (isFlashOn) {
                    tvStatus.setText(getString(R.string.tv_hint_flash_active));
                    btnToggleFlash.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.flash_on))
                    );
                } else {
                    tvStatus.setText(getString(R.string.tv_status_ready));
                    btnToggleFlash.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.flash_off))
                    );
                }
            } else {
                // Логика с таймером
                if (isFlashOn) {
                    // Выключаем фонарик -> таймер продолжает работать, кнопки разблокируются
                    stopTimer(); // Останавливаем обратный отсчет, если нужно, или оставляем?
                    // По ТЗ: "После выключения... всё опять разблокируется".
                    // Обычно таймер сбрасывают или ставят на паузу. Здесь просто выключаем свет.

                    cameraManager.setTorchMode(cameraId, false);
                    isFlashOn = false;
                    updateButtonText(false);
                    tvStatus.setText(getString(R.string.tv_status_ready));
                    btnToggleFlash.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.flash_off))
                    );

                    // ВАЖНО: Разблокируем элементы
                    updateTimerUIState();
                } else {
                    // Включаем фонарик -> запускаем таймер, блокируем кнопки изменения времени
                    cameraManager.setTorchMode(cameraId, true);
                    isFlashOn = true;
                    updateButtonText(true);
                    tvStatus.setText(getString(R.string.tv_hint_flash_active));
                    btnToggleFlash.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.flash_on))
                    );

                    startTimer(currentTimerSeconds * 1000L);
                    // Блокировка произойдет внутри startTimer (через onTick/onFinish логика не нужна,
                    // блокировка нужна сразу при включении). Но лучше вызвать updateTimerUIState() здесь.
                    updateTimerUIState();
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.flash_error_message), Toast.LENGTH_SHORT).show();
            isFlashOn = false;
            updateButtonText(false);
            updateTimerUIState(); // На случай ошибки разблокируем
        }
    }

    private void startTimer(long ms) {
        stopTimer();
        timer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                int m = (int)(seconds / 60);
                int s = (int)(seconds % 60);
                String text = String.format("%02d:%02d", m, s);
                tvTimerDisplay.setText(text);
            }

            @Override
            public void onFinish() {
                timer = null;
                try {
                    if (cameraManager != null && cameraId != null) {
                        cameraManager.setTorchMode(cameraId, false);
                        isFlashOn = false;
                        updateButtonText(false);
                        tvStatus.setText(getString(R.string.tv_status_ready));
                        btnToggleFlash.setBackgroundTintList(
                                ColorStateList.valueOf(ContextCompat.getColor(FlashlightActivity.this, R.color.flash_off))
                        );

                        // Таймер истек, фонарик выключен.
                        // По логике: таймер все еще "активен" (isTimerActive = true), но свет погас.
                        // Кнопки должны разблокироваться.
                        updateTimerUIState();
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
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
        stopTimer();
    }

    private void updateButtonText(boolean isOn) {
        if (isOn) {
            btnToggleFlash.setText(getString(R.string.btn_flash_off));
        } else {
            btnToggleFlash.setText(getString(R.string.btn_flash_on));
        }
    }
}
