package com.example.smartroad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView tvGreeting, tvLocation;
    private boolean cameraMovedToUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvLocation = findViewById(R.id.tvLocation);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        tvGreeting.setText("Hello, " + user.getDisplayName() + "!");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapHome);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        setupBottomNav();
        requestLocationPermission();
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
            Toast.makeText(this, "Location permission required for full functionality.",
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
                tvLocation.setText(String.format(Locale.getDefault(),
                        "Your Location\nLat: %.5f  Lng: %.5f", lat, lng));

                // Move camera to user on first fix
                if (mMap != null && !cameraMovedToUser) {
                    cameraMovedToUser = true;
                    LatLng userLatLng = new LatLng(lat, lng);
                    mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f));
                }

                // Reverse geocode for human-readable address
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
                    runOnUiThread(() -> tvLocation.setText(
                            String.format(Locale.getDefault(),
                                    "%.5f, %.5f\n%s", lat, lng, line)));
                }
            } catch (IOException ignored) {}
        }).start();
    }

    // ── Map ───────────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        loadHazardMarkers();
    }

    private void loadHazardMarkers() {
        FirebaseDatabase.getInstance().getReference("hazard_reports")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mMap == null) return;
                        // Clear only hazard markers (keep "Your Location" marker)
                        // Re-add all markers from a fresh state isn't ideal for production,
                        // but is simple and matches lab patterns for this scale.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Double lat = child.child("latitude").getValue(Double.class);
                            Double lng = child.child("longitude").getValue(Double.class);
                            String type = child.child("type").getValue(String.class);
                            String status = child.child("status").getValue(String.class);
                            if (lat == null || lng == null) continue;

                            String title = (type != null ? type : "Hazard")
                                    + (status != null ? " · " + status : "");
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .title(title)
                                    .icon(BitmapDescriptorFactory.defaultMarker(getMarkerHue(type))));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

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

    // ── Bottom Navigation ─────────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportHazardActivity.class));
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, HazardMapActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
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
}
