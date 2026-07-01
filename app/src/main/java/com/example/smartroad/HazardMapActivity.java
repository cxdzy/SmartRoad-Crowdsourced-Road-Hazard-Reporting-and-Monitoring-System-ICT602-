package com.example.smartroad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HazardMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String[] FILTER_OPTIONS =
            {"All Types", "Pothole", "Flood", "Accident", "Fallen Tree", "Traffic Light"};

    private GoogleMap mMap;
    private ValueEventListener reportsListener;

    // reportId → HazardReport / Marker, kept in sync for filtering
    private final Map<String, HazardReport> reportsCache = new HashMap<>();
    private final Map<String, Marker> markersCache = new HashMap<>();

    private int currentFilterIndex = 0; // 0 = All Types
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_map);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportHazardActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
            return true;
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFull);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        tvEmptyState = findViewById(R.id.tvEmptyState);
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            clearMap();
            attachReportsListener();
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
        });
    }

    // ── Map ───────────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnInfoWindowClickListener(marker -> {
            String reportId = (String) marker.getTag();
            if (reportId != null) {
                Intent intent = new Intent(this, HazardDetailsActivity.class);
                intent.putExtra("reportId", reportId);
                startActivity(intent);
            }
        });

        attachReportsListener();
    }

    // ── Firebase ──────────────────────────────────────────────────────────────

    private void attachReportsListener() {
        if (reportsListener != null) {
            FirebaseDatabase.getInstance().getReference("hazard_reports")
                    .removeEventListener(reportsListener);
        }

        reportsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                clearMap();
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                boolean hasMarkers = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    HazardReport report = child.getValue(HazardReport.class);
                    if (report == null) continue;
                    report.id = child.getKey();

                    reportsCache.put(report.id, report);

                    String snippet = "Status: " + report.status
                            + "\nTap for details";
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(report.latitude, report.longitude))
                            .title(report.type)
                            .snippet(snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(getMarkerHue(report.type))));

                    if (marker != null) {
                        marker.setTag(report.id);
                        markersCache.put(report.id, marker);
                        boundsBuilder.include(marker.getPosition());
                        hasMarkers = true;
                    }
                }

                applyFilter();
                tvEmptyState.setVisibility(hasMarkers ? View.GONE : View.VISIBLE);

                // Zoom to fit all markers on first load
                if (hasMarkers) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                            boundsBuilder.build(), 120));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HazardMapActivity.this,
                        "Failed to load hazards.", Toast.LENGTH_SHORT).show();
            }
        };

        FirebaseDatabase.getInstance().getReference("hazard_reports")
                .addValueEventListener(reportsListener);
    }

    private void clearMap() {
        if (mMap != null) mMap.clear();
        reportsCache.clear();
        markersCache.clear();
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    private void showFilterDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Filter by Hazard Type")
                .setSingleChoiceItems(FILTER_OPTIONS, currentFilterIndex, (dialog, which) -> {
                    currentFilterIndex = which;
                    applyFilter();
                    dialog.dismiss();
                    String label = currentFilterIndex == 0
                            ? "Showing all hazards"
                            : "Showing: " + FILTER_OPTIONS[currentFilterIndex];
                    Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void applyFilter() {
        String selectedType = currentFilterIndex == 0 ? null : FILTER_OPTIONS[currentFilterIndex];
        for (Map.Entry<String, Marker> entry : markersCache.entrySet()) {
            HazardReport report = reportsCache.get(entry.getKey());
            boolean visible = selectedType == null
                    || (report != null && selectedType.equals(report.type));
            entry.getValue().setVisible(visible);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float getMarkerHue(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_RED;
        switch (type) {
            case "Pothole":       return BitmapDescriptorFactory.HUE_RED;
            case "Flood":         return BitmapDescriptorFactory.HUE_BLUE;
            case "Accident":      return BitmapDescriptorFactory.HUE_ORANGE;
            case "Fallen Tree":   return BitmapDescriptorFactory.HUE_GREEN;
            case "Traffic Light": return BitmapDescriptorFactory.HUE_YELLOW;
            default:              return BitmapDescriptorFactory.HUE_ROSE;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportsListener != null) {
            FirebaseDatabase.getInstance().getReference("hazard_reports")
                    .removeEventListener(reportsListener);
        }
    }
}
