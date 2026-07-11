package com.sdax.flashandstrobe;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Получаем версию приложения и ставим в TextView
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            TextView tvVersion = findViewById(R.id.tvVersion);
            if (tvVersion != null) {
                tvVersion.setText("Версия: " + version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
