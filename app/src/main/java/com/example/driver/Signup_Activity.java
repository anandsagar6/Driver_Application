package com.example.driver;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Signup_Activity extends AppCompatActivity {

    private EditText firstNameInput, lastNameInput, phoneInput, emailInput, passwordInput;
    private Spinner genderSpinner;
    private Button signupBtn;
    private TextView loginRedirectBtn, privacy, term;
    private ProgressBar signupProgressBar;

    private FirebaseAuth auth;
    private DatabaseReference driverRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set blue status bar before setting content
        setBlueStatusBar();

        setContentView(R.layout.activity_signup);

        initializeFirebase();
        initializeViews();
        setupGenderSpinner();
        setupClickListeners();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void setBlueStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Try to get color from resources, fallback to hex color
            try {
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_blue));
            } catch (Exception e) {
                window.setStatusBarColor(Color.parseColor("#004FFF"));
            }

            // Set light status bar icons for better visibility on blue background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        driverRef = FirebaseDatabase.getInstance().getReference("drivers");
    }

    private void initializeViews() {
        firstNameInput = findViewById(R.id.fullNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        genderSpinner = findViewById(R.id.GenderSpinner);
        signupBtn = findViewById(R.id.signupBtn);
        loginRedirectBtn = findViewById(R.id.loginRedirectBtn);
        privacy = findViewById(R.id.txtPrivacyPolicy);
        term = findViewById(R.id.txtTermsConditions);
        signupProgressBar = findViewById(R.id.signupProgressBar);
    }

    private void setupGenderSpinner() {
        String[] genders = {"Select Gender", "Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, genders);
        genderAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);
    }

    private void setupClickListeners() {
        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Signup_Activity.this, PrivacyPolicyActivity.class);
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        term.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Signup_Activity.this, TermsConditionsActivity.class);
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        signupBtn.setOnClickListener(v -> registerDriver());

        loginRedirectBtn.setOnClickListener(v -> {
            startActivity(new Intent(Signup_Activity.this, Login_Activity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void registerDriver() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        int genderPosition = genderSpinner.getSelectedItemPosition();

        if (!validateInputs(firstName, lastName, phone, email, password, genderPosition)) {
            return;
        }

        // Show progress bar
        signupProgressBar.setVisibility(View.VISIBLE);
        signupBtn.setEnabled(false);

        String gender = genderSpinner.getSelectedItem().toString();

        // Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(Signup_Activity.this, task -> {
                    signupProgressBar.setVisibility(View.GONE);
                    signupBtn.setEnabled(true);

                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();
                        saveDriverData(uid, firstName, lastName, phone, email, gender);
                    } else {
                        String errorMessage = "Signup failed";
                        if (task.getException() != null) {
                            errorMessage = getFriendlyErrorMessage(task.getException().getMessage());
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    signupProgressBar.setVisibility(View.GONE);
                    signupBtn.setEnabled(true);
                    Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs(String firstName, String lastName, String phone,
                                   String email, String password, int genderPosition) {
        boolean isValid = true;

        if (TextUtils.isEmpty(firstName)) {
            firstNameInput.setError("First name required");
            isValid = false;
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameInput.setError("Last name required");
            isValid = false;
        }

        if (phone.length() != 10) {
            phoneInput.setError("Enter valid 10 digit number");
            isValid = false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter valid email");
            isValid = false;
        }

        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters");
            isValid = false;
        }

        if (genderPosition == 0) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void saveDriverData(String uid, String firstName, String lastName,
                                String phone, String email, String gender) {
        HashMap<String, Object> driverInfo = new HashMap<>();
        driverInfo.put("firstName", firstName);
        driverInfo.put("lastName", lastName);
        driverInfo.put("phone", phone);
        driverInfo.put("email", email);
        driverInfo.put("gender", gender);

        HashMap<String, Object> driverStatus = new HashMap<>();
        driverStatus.put("currentLat", 0);
        driverStatus.put("currentLng", 0);
        driverStatus.put("status", "available");

        HashMap<String, Object> driverData = new HashMap<>();
        driverData.put("info", driverInfo);
        driverData.put("status", driverStatus);

        driverRef.child(uid).updateChildren(driverData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Signup_Activity.this, RegistrationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                        Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getFriendlyErrorMessage(String errorMessage) {
        if (errorMessage.contains("email address is already")) {
            return "This email is already registered";
        } else if (errorMessage.contains("password is invalid")) {
            return "Password is too weak";
        } else if (errorMessage.contains("network error")) {
            return "Network error. Please check your connection";
        } else {
            return errorMessage;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}