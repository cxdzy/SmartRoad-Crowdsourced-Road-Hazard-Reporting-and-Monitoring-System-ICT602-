package com.example.smartroad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HazardMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // --- FILTER ARRAYS & STATE ---
    private static final String[] FILTER_OPTIONS =
            {"All types", "Pothole", "Flood", "Accident", "Fallen Tree",
                    "Damaged Road Sign", "Broken Traffic Light"};
    private static final String[] STATUS_FILTER_OPTIONS =
            {"All statuses", "New", "Under Investigation", "Resolved"};

    private int currentFilterIndex = 0;       // 0 = All types
    private int currentStatusFilterIndex = 0; // 0 = All statuses
    // -----------------------------

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private GoogleMap mMap;
    private ValueEventListener reportsListener;

    private final Map<String, HazardReport> reportsCache = new HashMap<>();
    private final Map<String, Marker> markersCache = new HashMap<>();

    private TextView tvEmptyState;
    private TextView tvGreeting, tvLocation, tvMapCoords;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_map);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tvGreeting = findViewById(R.id.tvGreeting);
        tvLocation = findViewById(R.id.tvLocation);
        tvMapCoords = findViewById(R.id.tvMapCoords);
        tvGreeting.setText(getTimeOfDayGreeting() + ", " + firstNameOf(user.getDisplayName()));

        View cardGreeting = findViewById(R.id.cardGreeting);
        findViewById(R.id.btnCloseGreeting).setOnClickListener(v -> cardGreeting.setVisibility(View.GONE));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                return true;
            }
            if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportHazardActivity.class)
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

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFull);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        tvEmptyState = findViewById(R.id.tvEmptyState);
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            clearMap();
            attachReportsListener();
            Toast.makeText(this, "Refreshing map…", Toast.LENGTH_SHORT).show();
        });

        requestLocationPermission();
    }

    // ── Greeting ──────────────────────────────────────────────────────────────

    private String getTimeOfDayGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }

    private String firstNameOf(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) return "there";
        return displayName.trim().split("\\s+")[0];
    }

    // ── Location ──────────────────────────────────────────────────────────────

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Turn on location to see hazards near you.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                android.location.Location location = locationResult.getLastLocation();
                if (location == null) return;
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                tvMapCoords.setText(String.format(Locale.getDefault(), "📍 %.6f, %.6f", lat, lng));
                reverseGeocode(lat, lng);
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String line = address.getAddressLine(0);
                    runOnUiThread(() -> tvLocation.setText(line));
                }
            } catch (IOException ignored) {}
        }).start();
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

        // --- CUSTOM INFO WINDOW LOGIC ---
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(@NonNull Marker marker) {
                View view = getLayoutInflater().inflate(R.layout.custom_info_window, null);

                TextView tvTitle = view.findViewById(R.id.tvSnippetTitle);
                TextView tvStatus = view.findViewById(R.id.tvSnippetStatus);

                tvTitle.setText(marker.getTitle());
                tvStatus.setText(marker.getSnippet());

                return view;
            }
        });
        // ---------------------------------

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

                    String status = report.status != null ? report.status : "New";
                    String snippet = buildSnippet(status, report.type, "…");

                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(report.latitude, report.longitude))
                            .title(report.type)
                            .snippet(snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(getMarkerHue(report.type))));

                    if (marker != null) {
                        marker.setAlpha(getAlphaForStatus(status));
                        marker.setTag(report.id);
                        markersCache.put(report.id, marker);
                        boundsBuilder.include(marker.getPosition());
                        hasMarkers = true;

                        loadReporterNameForMarker(marker, report, status);
                    }
                }

                applyFilter();
                tvEmptyState.setVisibility(hasMarkers ? View.GONE : View.VISIBLE);

                if (hasMarkers) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                            boundsBuilder.build(), 120));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HazardMapActivity.this,
                        "Couldn't load hazards. Try again.", Toast.LENGTH_SHORT).show();
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

    // ── Filter (Combined UI) ──────────────────────────────────────────────────

    private void showFilterDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 48, 64, 16);

        TextView typeLabel = new TextView(this);
        typeLabel.setText("Hazard Type");
        typeLabel.setTextSize(14);
        typeLabel.setTextColor(Color.parseColor("#757575"));

        Spinner typeSpinner = new Spinner(this);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, FILTER_OPTIONS);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setSelection(currentFilterIndex);

        TextView statusLabel = new TextView(this);
        statusLabel.setText("Status");
        statusLabel.setTextSize(14);
        statusLabel.setTextColor(Color.parseColor("#757575"));
        statusLabel.setPadding(0, 48, 0, 0);

        Spinner statusSpinner = new Spinner(this);
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, STATUS_FILTER_OPTIONS);
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setSelection(currentStatusFilterIndex);

        layout.addView(typeLabel);
        layout.addView(typeSpinner);
        layout.addView(statusLabel);
        layout.addView(statusSpinner);

        new AlertDialog.Builder(this)
                .setTitle("Filter Hazards")
                .setView(layout)
                .setPositiveButton("Apply", (dialog, which) -> {
                    currentFilterIndex = typeSpinner.getSelectedItemPosition();
                    currentStatusFilterIndex = statusSpinner.getSelectedItemPosition();
                    applyFilter();
                    Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyFilter() {
        String selectedType = currentFilterIndex == 0 ? null : FILTER_OPTIONS[currentFilterIndex];
        String selectedStatus = currentStatusFilterIndex == 0 ? null : STATUS_FILTER_OPTIONS[currentStatusFilterIndex];

        for (Map.Entry<String, Marker> entry : markersCache.entrySet()) {
            HazardReport report = reportsCache.get(entry.getKey());
            if (report == null) continue;

            boolean typeMatches = selectedType == null || selectedType.equals(report.type);
            boolean statusMatches = selectedStatus == null || selectedStatus.equals(report.status);

            entry.getValue().setVisible(typeMatches && statusMatches);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildSnippet(String status, String type, String reporterName) {
        return "[" + status.toUpperCase(Locale.getDefault()) + "] · Type: " + type
                + " · Reporter: " + reporterName;
    }

    private float getAlphaForStatus(String status) {
        switch (status) {
            case "Under Investigation": return 0.75f;
            case "Resolved":            return 0.4f;
            default:                    return 1.0f; // New
        }
    }

    private void loadReporterNameForMarker(Marker marker, HazardReport report, String status) {
        if (report.uid == null) return;
        FirebaseDatabase.getInstance().getReference("users").child(report.uid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        marker.setSnippet(buildSnippet(status, report.type, name != null ? name : "Unknown"));
                        if (marker.isInfoWindowShown()) marker.showInfoWindow();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private float getMarkerHue(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_RED;
        switch (type) {
            case "Pothole":              return BitmapDescriptorFactory.HUE_RED;
            case "Flood":                return BitmapDescriptorFactory.HUE_BLUE;
            case "Accident":             return BitmapDescriptorFactory.HUE_ORANGE;
            case "Fallen Tree":          return BitmapDescriptorFactory.HUE_GREEN;
            case "Damaged Road Sign":    return BitmapDescriptorFactory.HUE_YELLOW;
            case "Broken Traffic Light": return BitmapDescriptorFactory.HUE_VIOLET;
            default:                     return BitmapDescriptorFactory.HUE_ROSE;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationCallback != null) startLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportsListener != null) {
            FirebaseDatabase.getInstance().getReference("hazard_reports")
                    .removeEventListener(reportsListener);
        }
    }
}