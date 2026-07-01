package com.example.smartroad;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatar, tvName, tvEmail, tvTotalReports, tvResolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportHazardActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, HazardMapActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
            return true;
        });

        tvAvatar       = findViewById(R.id.tvAvatar);
        tvName         = findViewById(R.id.tvName);
        tvEmail        = findViewById(R.id.tvEmail);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvResolved     = findViewById(R.id.tvResolved);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        populateIdentity(user);
        loadStats(user.getUid());

        findViewById(R.id.btnAbout).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());
    }

    private void populateIdentity(FirebaseUser user) {
        String name = user.getDisplayName();
        tvName.setText(name != null ? name : "User");
        tvEmail.setText(user.getEmail());

        // Initials for avatar circle
        String initials = "?";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            initials = parts.length >= 2
                    ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                    : String.valueOf(parts[0].charAt(0));
        }
        tvAvatar.setText(initials.toUpperCase());
    }

    private void loadStats(String uid) {
        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long total    = snapshot.child("totalReports").getValue(Long.class);
                        Long resolved = snapshot.child("resolvedReports").getValue(Long.class);
                        tvTotalReports.setText(String.valueOf(total    != null ? total    : 0));
                        tvResolved    .setText(String.valueOf(resolved != null ? resolved : 0));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
