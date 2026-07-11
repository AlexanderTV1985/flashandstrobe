package com.sdax.flashandstrobe;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            TextView tvVersion = findViewById(R.id.tvVersion);
            if (tvVersion != null) {
                // Используем шаблон из strings.xml, если хочешь, или просто конкатенацию
                tvVersion.setText(getString(R.string.app_name) + " v" + version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
