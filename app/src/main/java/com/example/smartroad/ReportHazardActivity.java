package com.example.smartroad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportHazardActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    private RadioGroup rgHazardType;
    private TextInputEditText etDescription;
    private ImageView ivPhotoPreview;
    private TextView tvLatLng, tvDateTime;
    private ProgressBar progressBar;

    private Uri selectedPhotoUri = null;
    private double currentLat = 0, currentLng = 0;
    private boolean locationAcquired = false;

    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_hazard);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_report);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, HazardMapActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
            return true;
        });

        rgHazardType = findViewById(R.id.rgHazardType);
        etDescription = findViewById(R.id.etDescription);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        tvLatLng = findViewById(R.id.tvLatLng);
        tvDateTime = findViewById(R.id.tvDateTime);
        progressBar = findViewById(R.id.progressBar);

        // Auto-fill date and time
        String now = new SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault()).format(new Date());
        tvDateTime.setText(now);

        // Photo picker (no storage permission needed on Android 10+)
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        ivPhotoPreview.setImageURI(uri);
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                    }
                });

        findViewById(R.id.btnSelectPhoto).setOnClickListener(v ->
                photoPickerLauncher.launch("image/*"));

        findViewById(R.id.btnSubmit).setOnClickListener(v -> submitReport());

        fusedLocationClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this);
        acquireLocation();
    }

    // ── Location ──────────────────────────────────────────────────────────────

    private void acquireLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                locationAcquired = true;
                tvLatLng.setText(String.format(Locale.getDefault(),
                        "%.6f, %.6f", currentLat, currentLng));
            } else {
                tvLatLng.setText("Location unavailable — move outdoors and retry");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            acquireLocation();
        }
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private void submitReport() {
        // Validate hazard type
        int selectedId = rgHazardType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select a hazard type.", Toast.LENGTH_SHORT).show();
            return;
        }
        String hazardType = getHazardTypeLabel(selectedId);

        // Validate description
        String description = etDescription.getText() != null
                ? etDescription.getText().toString().trim() : "";
        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (!locationAcquired) {
            Toast.makeText(this, "Waiting for GPS location. Try again in a moment.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setFormEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        if (selectedPhotoUri != null) {
            uploadPhotoThenSave(hazardType, description);
        } else {
            writeReportToDatabase(hazardType, description, "");
        }
    }

    private void uploadPhotoThenSave(String hazardType, String description) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String filename = user.getUid() + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = FirebaseStorage.getInstance()
                .getReference("hazard_photos").child(filename);

        photoRef.putFile(selectedPhotoUri)
                .addOnSuccessListener(taskSnapshot ->
                        photoRef.getDownloadUrl().addOnSuccessListener(uri ->
                                writeReportToDatabase(hazardType, description, uri.toString())))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    setFormEnabled(true);
                    Toast.makeText(this, "Photo upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void writeReportToDatabase(String hazardType, String description, String photoUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("hazard_reports");
        String reportId = reportsRef.push().getKey();

        String userAgent = Build.MANUFACTURER + " " + Build.MODEL
                + " (Android " + Build.VERSION.RELEASE + ")";
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        Map<String, Object> report = new HashMap<>();
        report.put("id", reportId);
        report.put("uid", user.getUid());
        report.put("type", hazardType);
        report.put("description", description);
        report.put("latitude", currentLat);
        report.put("longitude", currentLng);
        report.put("photoUrl", photoUrl);
        report.put("status", "New");
        report.put("timestamp", timestamp);
        report.put("userAgent", userAgent);

        reportsRef.child(reportId).setValue(report)
                .addOnSuccessListener(unused -> {
                    // Increment user's total report count
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(user.getUid()).child("totalReports")
                            .setValue(ServerValue.increment(1));

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    setFormEnabled(true);
                    Toast.makeText(this, "Failed to save report: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getHazardTypeLabel(int radioId) {
        if (radioId == R.id.rbPothole)      return "Pothole";
        if (radioId == R.id.rbFlood)        return "Flood";
        if (radioId == R.id.rbAccident)     return "Accident";
        if (radioId == R.id.rbFallenTree)   return "Fallen Tree";
        if (radioId == R.id.rbTrafficLight) return "Traffic Light";
        return "Unknown";
    }

    private void setFormEnabled(boolean enabled) {
        rgHazardType.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        findViewById(R.id.btnSelectPhoto).setEnabled(enabled);
        findViewById(R.id.btnSubmit).setEnabled(enabled);
    }
}
