package com.sdax.flashandstrobe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Используем MaterialButton, чтобы типы совпадали с XML
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnFlashlight;
    private MaterialButton btnStrobe;
    private MaterialButton btnSos;
    private MaterialButton btnFlicker;
    private MaterialButton btnAbout;

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

        // Тексты уже заданы в XML через @string, поэтому setText здесь не нужен.
        // Если захочешь менять текст динамически — тогда используй setText.

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
    private void setOnClickListener(MaterialButton button, Class<?> targetActivity) {
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
