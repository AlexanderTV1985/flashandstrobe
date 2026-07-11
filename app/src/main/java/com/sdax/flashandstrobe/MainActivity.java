package com.sdax.flashandstrobe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

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

        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnStrobe = findViewById(R.id.btnStrobe);
        btnSos = findViewById(R.id.btnSos);
        btnFlicker = findViewById(R.id.btnFlicker);
        btnAbout = findViewById(R.id.btnAbout);

        btnFlashlight.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FlashlightActivity.class))
        );

        btnStrobe.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StrobeActivity.class))
        );

        btnSos.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SosActivity.class))
        );

        btnFlicker.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FlickerActivity.class))
        );

        btnAbout.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AboutActivity.class))
        );
    }
}
