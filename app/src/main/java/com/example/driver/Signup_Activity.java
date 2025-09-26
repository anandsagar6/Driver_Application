package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        setContentView(R.layout.activity_signup);

        // Firebase init
        auth = FirebaseAuth.getInstance();
        driverRef = FirebaseDatabase.getInstance().getReference("drivers");

        // Views
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

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Spinner: Gender
        String[] genders = {"Select Gender", "Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, genders);
        genderAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        // Privacy Policy click
        privacy.setOnClickListener(view -> startActivity(new Intent(Signup_Activity.this, PrivacyPolicyActivity.class)));

        // Terms & Conditions click
        term.setOnClickListener(view -> startActivity(new Intent(Signup_Activity.this, TermsConditionsActivity.class)));

        // Signup button click
        signupBtn.setOnClickListener(v -> registerDriver());

        // Login redirect click
        loginRedirectBtn.setOnClickListener(v -> {
            startActivity(new Intent(Signup_Activity.this, Login_Activity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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

        boolean hasError = false;

        // Validation
        if (TextUtils.isEmpty(firstName)) {
            firstNameInput.setError("First name required");
            hasError = true;
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameInput.setError("Last name required");
            hasError = true;
        }

        if (phone.length() != 10) {
            phoneInput.setError("Enter valid 10 digit number");
            hasError = true;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter valid email");
            hasError = true;
        }

        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 chars");
            hasError = true;
        }

        if (genderPosition == 0) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if (hasError) return;

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
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(Signup_Activity.this, RegistrationActivity.class));
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Signup failed: " + task1.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });

                    } else {
                        Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
