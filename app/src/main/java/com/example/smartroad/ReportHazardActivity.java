package com.example.smartroad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportHazardActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    private AutoCompleteTextView actvHazardType;
    private TextInputEditText etDescription;
    private ImageView ivPhotoPreview;
    private TextView tvNoPhoto, tvLatLng, tvDateTime;
    private ProgressBar progressBar;
    private MaterialButton btnTakePhoto, btnSelectPhoto, btnSubmit;

    private Uri selectedPhotoUri = null;
    private double currentLat = 0, currentLng = 0;
    private boolean locationAcquired = false;

    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;

    private ActivityResultLauncher<String> photoPickerLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_hazard);

        // --- CORRECTED BOTTOM NAVIGATION LOGIC ---
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_report);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_report) {
                return true;
            }

            if (id == R.id.nav_map) {
                startActivity(new Intent(this, HazardMapActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return false;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                overridePendingTransition(0, 0);
                return false;
            }
            return false;
        });
        // -----------------------------------------

        // Initialize Views
        actvHazardType = findViewById(R.id.actvHazardType);
        etDescription = findViewById(R.id.etDescription);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        tvNoPhoto = findViewById(R.id.tvNoPhoto);
        tvLatLng = findViewById(R.id.tvLatLng);
        tvDateTime = findViewById(R.id.tvDateTime);
        progressBar = findViewById(R.id.progressBar);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Setup Dropdown Menu
        String[] hazards = new String[]{"Pothole", "Flood", "Accident", "Fallen Tree",
                "Damaged Road Sign", "Broken Traffic Light"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, hazards);
        actvHazardType.setAdapter(adapter);

        // Auto-fill date and time
        String now = new SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault()).format(new Date());
        tvDateTime.setText(now);

        // Photo Picker (Gallery) Setup
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        ivPhotoPreview.setImageURI(uri);
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                        tvNoPhoto.setVisibility(View.GONE);
                    }
                });

        // Camera Setup
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                    if (bitmap != null) {
                        ivPhotoPreview.setImageBitmap(bitmap);
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                        tvNoPhoto.setVisibility(View.GONE);
                        selectedPhotoUri = getImageUriFromBitmap(bitmap);
                    }
                });

        // Button Click Listeners
        btnSelectPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        btnTakePhoto.setOnClickListener(v -> cameraLauncher.launch(null));
        btnSubmit.setOnClickListener(v -> submitReport());

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
        String hazardType = actvHazardType.getText().toString().trim();
        if (hazardType.isEmpty()) {
            Toast.makeText(this, "Please select a Hazard Type.", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = etDescription.getText() != null
                ? etDescription.getText().toString().trim() : "";
        if (description.isEmpty()) {
            etDescription.setError("Add a short description");
            etDescription.requestFocus();
            return;
        }

        if (!locationAcquired) {
            Toast.makeText(this, "Still finding your location. Try again in a moment.",
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
                    Toast.makeText(this, "Couldn't upload the photo: " + e.getMessage(),
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
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(user.getUid()).child("totalReports")
                            .setValue(ServerValue.increment(1));

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    setFormEnabled(true);
                    Toast.makeText(this, "Couldn't save the report: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "captured_image.jpg");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setFormEnabled(boolean enabled) {
        actvHazardType.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        btnTakePhoto.setEnabled(enabled);
        btnSelectPhoto.setEnabled(enabled);
        btnSubmit.setEnabled(enabled);
    }
}