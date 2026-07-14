package com.sdax.flashandstrobe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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
                tvVersion.setText(getString(R.string.app_name) + " v" + version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy);
        if (btnPrivacyPolicy != null) {
            btnPrivacyPolicy.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://alexandertv1985.github.io/flashandstrobe/index.html"));
                startActivity(browserIntent);
            });
        }
    }
}
