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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnStrobe = findViewById(R.id.btnStrobe);
        btnSos = findViewById(R.id.btnSos);
        btnFlicker = findViewById(R.id.btnFlicker);

        btnFlashlight.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FlashlightActivity.class));
        });

        btnStrobe.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StrobeActivity.class));
        });

        btnSos.setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, SosActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Режим SOS ещё в разработке", Toast.LENGTH_SHORT).show();
            }
        });

        btnFlicker.setOnClickListener(v -> {
            Toast.makeText(this, "Flicker: режим в разработке (адаптивное мигание)", Toast.LENGTH_LONG).show();
        });
    }
}
