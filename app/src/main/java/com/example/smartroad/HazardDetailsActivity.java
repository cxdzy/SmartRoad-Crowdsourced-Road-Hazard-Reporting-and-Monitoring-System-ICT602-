package com.example.smartroad;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class HazardDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "reportId";

    private static final String[] STATUS_OPTIONS =
            {"New", "Under Investigation", "Resolved"};

    private ImageView ivPhoto;
    private TextView tvType, tvStatus, tvDescription, tvReporter,
            tvLocation, tvTimestamp, tvUserAgent;
    private MaterialButton btnChangeStatus;
    private ProgressBar progressBar;

    private String currentReportId;
    private String currentStatus;
    private String reporterUid;

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
        btnChangeStatus = findViewById(R.id.btnChangeStatus);
        progressBar     = findViewById(R.id.progressBar);

        currentReportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        if (currentReportId == null) {
            Toast.makeText(this, "Report not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnChangeStatus.setOnClickListener(v -> showStatusDialog());
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

        currentStatus = report.status != null ? report.status : "New";
        reporterUid   = report.uid;
        applyStatusBadge(currentStatus);

        tvDescription.setText(report.description != null ? report.description : "—");

        tvLocation.setText(String.format(Locale.getDefault(),
                "%.6f, %.6f", report.latitude, report.longitude));

        tvTimestamp.setText(report.timestamp != null ? report.timestamp : "—");
        tvUserAgent.setText(report.userAgent != null ? report.userAgent : "—");

        btnChangeStatus.setVisibility(View.VISIBLE);

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
        tvStatus.setBackgroundColor(getStatusColor(status));
    }

    private void showStatusDialog() {
        int currentIndex = 0;
        for (int i = 0; i < STATUS_OPTIONS.length; i++) {
            if (STATUS_OPTIONS[i].equals(currentStatus)) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setSingleChoiceItems(STATUS_OPTIONS, currentIndex, null)
                .setPositiveButton("Save", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String newStatus = STATUS_OPTIONS[selected];
                    if (!newStatus.equals(currentStatus)) {
                        saveStatus(newStatus);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveStatus(String newStatus) {
        btnChangeStatus.setEnabled(false);
        FirebaseDatabase.getInstance().getReference("hazard_reports")
                .child(currentReportId).child("status")
                .setValue(newStatus)
                .addOnSuccessListener(unused -> {
                    updateResolvedCount(currentStatus, newStatus);
                    currentStatus = newStatus;
                    applyStatusBadge(currentStatus);
                    btnChangeStatus.setEnabled(true);
                    Toast.makeText(this, "Status updated to \"" + newStatus + "\"",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnChangeStatus.setEnabled(true);
                    Toast.makeText(this, "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateResolvedCount(String oldStatus, String newStatus) {
        if (reporterUid == null) return;
        boolean wasResolved = "Resolved".equals(oldStatus);
        boolean isResolved  = "Resolved".equals(newStatus);
        if (wasResolved == isResolved) return; // no change to resolved count

        long delta = isResolved ? 1 : -1;
        FirebaseDatabase.getInstance().getReference("users")
                .child(reporterUid).child("resolvedReports")
                .setValue(com.google.firebase.database.ServerValue.increment(delta));
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
