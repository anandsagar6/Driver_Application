package com.example.driver;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Login_Activity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn;
    FirebaseAuth auth;
    TextView signupRedirectBtn, privacy, term;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color to blue
        setStatusBarColor(Color.parseColor("#004FFF")); // Blue color

        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
        setupLoginFunctionality();
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
                        // Reset button state
                        loginBtn.setEnabled(true);
                        loginBtn.setText("LOGIN");

                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login Successful ✅", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, DashBoard.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        } else {
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}