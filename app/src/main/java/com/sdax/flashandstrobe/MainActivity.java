package com.sdax.flashandstrobe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnFlashlight = findViewById(R.id.btnFlashlight);
        Button btnStrobe = findViewById(R.id.btnStrobe);
        Button btnSos = findViewById(R.id.btnSos);
        Button btnFlicker = findViewById(R.id.btnFlicker);

        btnFlashlight.setOnClickListener(v -> openActivity(FlashlightActivity.class));
        btnStrobe.setOnClickListener(v -> openActivity(StrobeActivity.class));
        btnSos.setOnClickListener(v -> openActivity(SosActivity.class));
        btnFlicker.setOnClickListener(v -> openActivity(FlickerActivity.class));
    }

    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }
}
