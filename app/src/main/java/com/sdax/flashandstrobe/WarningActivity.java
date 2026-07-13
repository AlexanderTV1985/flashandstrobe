package com.sdax.flashandstrobe;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class WarningActivity extends AppCompatActivity {

    public static final String EXTRA_ACTION = "extra_action";
    public static final String ACTION_FLICKER = "action_flicker";
    public static final String ACTION_STROBE = "action_strobe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning);

        // Можно использовать action, если захочешь разный текст/заголовок для разных режимов
        String action = getIntent() != null ? getIntent().getStringExtra(EXTRA_ACTION) : null;

        MaterialButton btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }
}
