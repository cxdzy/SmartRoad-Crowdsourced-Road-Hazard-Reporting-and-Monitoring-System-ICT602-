package com.example.smartroad;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvGitHubUrl = findViewById(R.id.tvGitHubUrl);
        tvGitHubUrl.setOnClickListener(v -> {
            String url = getString(R.string.github_url);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
    }
}
