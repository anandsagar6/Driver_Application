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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Signup_Activity extends AppCompatActivity {

    private EditText fullNameInput, vehicleNumberInput, phoneInput, dlNumberInput, emailInput, passwordInput;
    private Spinner vehicleSpinner;
    private Button signupBtn;
    TextView loginRedirectBtn;

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

        // Spinner setup
        String[] vehicleTypes = {"SUV", "Sedan", "Auto"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                vehicleTypes
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        vehicleSpinner.setAdapter(adapter);

        // Sign Up
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerDriver();
            }
        });

        // Redirect to Login
        loginRedirectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Signup_Activity.this, Login_Activity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
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
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 chars");
            return;
        }

        // Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(Signup_Activity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = auth.getCurrentUser().getUid();
                            HashMap<String, String> driverMap = new HashMap<>();
                            driverMap.put("fullName", fullName);
                            driverMap.put("vehicleNumber", vehicleNumber);
                            driverMap.put("phone", phone);
                            driverMap.put("dlNumber", dlNumber);
                            driverMap.put("email", email);
                            driverMap.put("vehicleType", vehicle);

                            driverRef.child(uid).setValue(driverMap);

                            startActivity(new Intent(Signup_Activity.this, Login_Activity.class));
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            finish();
                        } else {
                            emailInput.setError("Signup failed: " + task.getException().getMessage());
                        }
                    }
                });
    }
}
