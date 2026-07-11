package com.sdax.flashandstrobe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnFlashlight;
    private Button btnStrobe;
    private Button btnSos;
    private Button btnFlicker;
    private Button btnAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация кнопок
        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnStrobe = findViewById(R.id.btnStrobe);
        btnSos = findViewById(R.id.btnSos);
        btnFlicker = findViewById(R.id.btnFlicker);
        btnAbout = findViewById(R.id.btnAbout);

        // Установка текстов из ресурсов (чтобы можно было локализовать)
        if (btnFlashlight != null) btnFlashlight.setText(getString(R.string.btn_flashlight));
        if (btnStrobe != null) btnStrobe.setText(getString(R.string.btn_strobe));
        if (btnSos != null) btnSos.setText(getString(R.string.btn_sos));
        if (btnFlicker != null) btnFlicker.setText(getString(R.string.btn_flicker));
        if (btnAbout != null) btnAbout.setText(getString(R.string.btn_about));

        // Обработчики нажатий
        setOnClickListener(btnFlashlight, FlashlightActivity.class);
        setOnClickListener(btnStrobe, StrobeActivity.class);
        setOnClickListener(btnSos, SosActivity.class);
        setOnClickListener(btnFlicker, FlickerActivity.class);
        setOnClickListener(btnAbout, AboutActivity.class);
    }

    /**
     * Вспомогательный метод: вешает startActivity на кнопку, если она не null.
     */
    private void setOnClickListener(Button button, Class<?> targetActivity) {
        if (button == null) return;

        button.setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, targetActivity));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(
                        MainActivity.this,
                        "Не удалось открыть экран: " + targetActivity.getSimpleName(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}
