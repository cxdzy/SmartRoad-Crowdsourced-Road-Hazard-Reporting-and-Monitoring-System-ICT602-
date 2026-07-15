package com.example.smartroad;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        findViewById(R.id.btnSignUp).setOnClickListener(v -> attemptRegister());

        TextView tvSignIn = findViewById(R.id.tvSignIn);
        tvSignIn.setText(buildSignInPrompt());
        tvSignIn.setOnClickListener(v -> finish());
    }

    private SpannableString buildSignInPrompt() {
        String full = "Already have an account? Sign In";
        String highlight = "Sign In";
        SpannableString spannable = new SpannableString(full);
        int start = full.indexOf(highlight);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.accent)),
                start, start + highlight.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private void attemptRegister() {
        String fullName = etFullName.getText() == null ? "" : etFullName.getText().toString().trim();
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText() == null ? "" : etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateProfileAndSave(user, fullName);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.w(TAG, "createUserWithEmailAndPassword:failure", task.getException());
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed.";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateProfileAndSave(FirebaseUser user, String fullName) {
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdate).addOnCompleteListener(profileTask -> {
            DatabaseReference usersRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(user.getUid());
            usersRef.child("name").setValue(fullName);
            usersRef.child("email").setValue(user.getEmail());
            usersRef.child("totalReports").setValue(0);
            usersRef.child("resolvedReports").setValue(0);

            progressBar.setVisibility(View.GONE);
            goToHazardMap();
        });
    }

    private void goToHazardMap() {
        Intent intent = new Intent(this, HazardMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}