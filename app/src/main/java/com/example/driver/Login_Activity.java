package com.example.driver;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login_Activity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn;
    FirebaseAuth auth;
    TextView signupRedirectBtn, privacy, term;
    private DatabaseReference driverRef;

    private boolean doubleBackToExitPressedOnce = false;
    private static final int DOUBLE_BACK_PRESS_INTERVAL = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color to blue
        setStatusBarColor(Color.parseColor("#004FFF")); // Blue color

        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
        setupLoginFunctionality();

        // Initialize Firebase Database reference
        driverRef = FirebaseDatabase.getInstance().getReference("drivers");

        // Check if user is already logged in but not registered
        checkExistingUser();
    }

    private void checkExistingUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, check registration status
            checkUserRegistrationStatus(currentUser);
        }
    }

    private void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(color);

            // If you want light status bar icons (white icons)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int flags = window.getDecorView().getSystemUiVisibility();
                if (isColorDark(color)) {
                    // Dark background - use light icons
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    // Light background - use dark icons
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                window.getDecorView().setSystemUiVisibility(flags);
            }
        }
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        signupRedirectBtn = findViewById(R.id.signupRedirectBtn);
        privacy = findViewById(R.id.txtPrivacyPolicy);
        term = findViewById(R.id.txtTermsConditions);

        auth = FirebaseAuth.getInstance();
    }

    private void setupClickListeners() {
        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Login_Activity.this, PrivacyPolicyActivity.class);
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        term.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Login_Activity.this, TermsConditionsActivity.class);
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        signupRedirectBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Login_Activity.this, Signup_Activity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void setupLoginFunctionality() {
        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading state
            loginBtn.setEnabled(false);
            loginBtn.setText("Logging in...");

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Login successful, now check registration status
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                checkUserRegistrationStatus(user);
                            }
                        } else {
                            // Reset button state on login failure
                            loginBtn.setEnabled(true);
                            loginBtn.setText("LOGIN");

                            String errorMessage = "Login failed";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                                // User-friendly error messages
                                if (errorMessage.contains("password is invalid")) {
                                    errorMessage = "Invalid password";
                                } else if (errorMessage.contains("no user record")) {
                                    errorMessage = "Account not found";
                                } else if (errorMessage.contains("network error")) {
                                    errorMessage = "Network error. Check your connection";
                                }
                            }
                            Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Reset button state on failure
                        loginBtn.setEnabled(true);
                        loginBtn.setText("LOGIN");
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void checkUserRegistrationStatus(FirebaseUser user) {
        String uid = user.getUid();

        driverRef.child(uid).child("info")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        Boolean imageUploaded = snapshot.child("image_uploaded").getValue(Boolean.class);
                        Boolean isRegistered = snapshot.child("isRegistered").getValue(Boolean.class);

                        if (imageUploaded == null) imageUploaded = false;
                        if (isRegistered == null) isRegistered = false;

                        if (!imageUploaded) {
                            // First task → upload photos
                            Toast.makeText(Login_Activity.this, "Please upload documents", Toast.LENGTH_LONG).show();
                            navigateTo(PhotosActivity.class);
                        }
                        else if (!isRegistered) {
                            // Next step → fill vehicle details
                            Toast.makeText(Login_Activity.this, "Please complete registration", Toast.LENGTH_LONG).show();
                            navigateTo(RegistrationActivity.class);
                        }
                        else {
                            // Completed → Dashboard
                            Toast.makeText(Login_Activity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            navigateTo(DashBoard.class);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(Login_Activity.this, "Error checking status", Toast.LENGTH_SHORT).show();
                        loginBtn.setEnabled(true);
                        loginBtn.setText("LOGIN");
                    }
                });
    }

    private void navigateTo(Class<?> activity) {
        Intent intent = new Intent(Login_Activity.this, activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    private void navigateToDashboard() {
        Intent intent = new Intent(Login_Activity.this, DashBoard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(Login_Activity.this, RegistrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            // Second back press - navigate back with animation
            super.onBackPressed();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back again to go back", Toast.LENGTH_SHORT).show();

        // Reset the flag after 2 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, DOUBLE_BACK_PRESS_INTERVAL);
    }


}