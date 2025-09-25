package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Signup_Activity extends AppCompatActivity {

    private EditText fullNameInput, vehicleNumberInput, phoneInput, dlNumberInput, emailInput, passwordInput;
    private Spinner vehicleSpinner;
    private Button signupBtn;
    private TextView loginRedirectBtn,privacy,term;

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
        fullNameInput = findViewById(R.id.fullNameInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        phoneInput = findViewById(R.id.phoneInput);
        dlNumberInput = findViewById(R.id.dlNumberInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        vehicleSpinner = findViewById(R.id.vehicleSpinner);
        signupBtn = findViewById(R.id.signupBtn);
        loginRedirectBtn = findViewById(R.id.loginRedirectBtn);
        privacy=findViewById(R.id.txtPrivacyPolicy);
        term=findViewById(R.id.txtTermsConditions);


        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Signup_Activity.this,PrivacyPolicyActivity.class);
                startActivity(i);
            }
        });
        term.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Signup_Activity.this,TermsConditionsActivity.class);
                startActivity(i);
            }
        });

        // Spinner setup
        String[] vehicleTypes = {"SUV", "Sedan", "Auto", "Bike", "Ambulance"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                vehicleTypes
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        vehicleSpinner.setAdapter(adapter);

        // Sign Up
        signupBtn.setOnClickListener(v -> registerDriver());

        // Redirect to Login
        loginRedirectBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Signup_Activity.this, Login_Activity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    private void registerDriver() {
        String fullName = fullNameInput.getText().toString().trim();
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String dlNumber = dlNumberInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String vehicle = vehicleSpinner.getSelectedItem().toString();

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full name required");
            return;
        }
        if (TextUtils.isEmpty(vehicleNumber)) {
            vehicleNumberInput.setError("Vehicle number required");
            return;
        }
        if (phone.length() != 10) {
            phoneInput.setError("Enter valid 10 digit number");
            return;
        }
        if (TextUtils.isEmpty(dlNumber)) {
            dlNumberInput.setError("DL number required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter valid email");
            return;
        }
        if (password.length() < 7) {
            passwordInput.setError("Password must be at least 6 chars");
            return;
        }

        // Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(Signup_Activity.this, task -> {
                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();

                        // Driver static info
                        HashMap<String, Object> driverInfo = new HashMap<>();
                        driverInfo.put("fullName", fullName);
                        driverInfo.put("vehicleNumber", vehicleNumber);
                        driverInfo.put("phone", phone);
                        driverInfo.put("dlNumber", dlNumber);
                        driverInfo.put("email", email);
                        driverInfo.put("vehicleType", vehicle);

                        // Driver dynamic info
                        HashMap<String, Object> driverStatus = new HashMap<>();
                        driverStatus.put("currentLat", 0);
                        driverStatus.put("currentLng", 0);
                        driverStatus.put("status", "available");

                        // Combine under structured node
                        HashMap<String, Object> driverData = new HashMap<>();
                        driverData.put("info", driverInfo);
                        driverData.put("status", driverStatus);

                        // Save in Firebase
                        driverRef.child(uid).updateChildren(driverData)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        startActivity(new Intent(Signup_Activity.this, Login_Activity.class));
                                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                        finish();
                                    } else {
                                        emailInput.setError("Signup failed: " + task1.getException().getMessage());
                                    }
                                });

                    } else {
                        emailInput.setError("Signup failed: " + task.getException().getMessage());
                    }
                });
    }
}
