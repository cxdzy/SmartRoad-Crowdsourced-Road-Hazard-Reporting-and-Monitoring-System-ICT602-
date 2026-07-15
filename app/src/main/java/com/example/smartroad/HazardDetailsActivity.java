package com.example.smartroad;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class HazardDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "reportId";

    private ImageView ivPhoto;
    private TextView tvType, tvStatus, tvDescription, tvReporter,
            tvLocation, tvTimestamp, tvUserAgent;
    private ProgressBar progressBar;

    private String currentReportId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivPhoto         = findViewById(R.id.ivPhoto);
        tvType          = findViewById(R.id.tvType);
        tvStatus        = findViewById(R.id.tvStatus);
        tvDescription   = findViewById(R.id.tvDescription);
        tvReporter      = findViewById(R.id.tvReporter);
        tvLocation      = findViewById(R.id.tvLocation);
        tvTimestamp     = findViewById(R.id.tvTimestamp);
        tvUserAgent     = findViewById(R.id.tvUserAgent);
        progressBar     = findViewById(R.id.progressBar);

        currentReportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        if (currentReportId == null) {
            Toast.makeText(this, "Report not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadReport(currentReportId);
    }

    private void loadReport(String reportId) {
        FirebaseDatabase.getInstance().getReference("hazard_reports").child(reportId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HazardReport report = snapshot.getValue(HazardReport.class);
                        if (report == null) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(HazardDetailsActivity.this,
                                    "Report not found.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        report.id = snapshot.getKey();
                        populateViews(report);
                        loadReporterName(report.uid);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(HazardDetailsActivity.this,
                                "Failed to load report.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateViews(HazardReport report) {
        progressBar.setVisibility(View.GONE);

        tvType.setText(report.type != null ? report.type : "—");

        applyStatusBadge(report.status != null ? report.status : "New");

        tvDescription.setText(report.description != null ? report.description : "—");

        tvLocation.setText(String.format(Locale.getDefault(),
                "%.6f, %.6f", report.latitude, report.longitude));

        tvTimestamp.setText(report.timestamp != null ? report.timestamp : "—");
        tvUserAgent.setText(report.userAgent != null ? report.userAgent : "—");

        // Photo
        if (report.photoUrl != null && !report.photoUrl.isEmpty()) {
            ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(report.photoUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivPhoto);
        }
    }

    private void applyStatusBadge(String status) {
        tvStatus.setText(status);
        android.graphics.drawable.Drawable pill = tvStatus.getBackground().mutate();
        pill.setTint(getStatusColor(status));
        tvStatus.setBackground(pill);
    }

    private void loadReporterName(String uid) {
        if (uid == null) {
            tvReporter.setText("Unknown");
            return;
        }
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        tvReporter.setText(name != null ? name : "Unknown");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvReporter.setText("Unknown");
                    }
                });
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Resolved":            return Color.parseColor("#4CAF50");
            case "Under Investigation": return Color.parseColor("#FF9800");
            default:                    return Color.parseColor("#F44336"); // New
        }
    }
}
