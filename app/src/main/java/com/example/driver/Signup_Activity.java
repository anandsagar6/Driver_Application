package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;

public class Signup_Activity extends AppCompatActivity {

    EditText firstNameInput, lastNameInput, phoneInput, dlNumberInput, emailInput, passwordInput;
    Spinner vehicleSpinner;
    Button signupBtn, loginRedirectBtn;
    FirebaseAuth auth;
    DatabaseReference driversRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize inputs
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        dlNumberInput = findViewById(R.id.dlNumberInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        vehicleSpinner = findViewById(R.id.vehicleSpinner);
        signupBtn = findViewById(R.id.signupBtn);
        loginRedirectBtn = findViewById(R.id.loginRedirectBtn);

        auth = FirebaseAuth.getInstance();
        driversRef = FirebaseDatabase.getInstance().getReference("drivers");

        // ✅ Setup Spinner values
        String[] vehicleTypes = {"SUV", "Sedan", "Auto"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleSpinner.setAdapter(adapter);

        signupBtn.setOnClickListener(v -> {
            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String vehicle = vehicleSpinner.getSelectedItem().toString();
            String dlNumber = dlNumberInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // ✅ Validations
            if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || vehicle.isEmpty() ||
                    dlNumber.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (phone.length() != 10 || !phone.matches("\\d{10}")) {
                Toast.makeText(this, "Enter valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Create user
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String firebaseUid = auth.getCurrentUser().getUid();

                    // Save driver info in DB (❌ removed driverId)
                    HashMap<String, Object> driverMap = new HashMap<>();
                    driverMap.put("firstName", firstName);
                    driverMap.put("lastName", lastName);
                    driverMap.put("phone", phone);
                    driverMap.put("vehicle", vehicle);
                    driverMap.put("dlNumber", dlNumber);
                    driverMap.put("email", email);
                    driverMap.put("status", "offline");

                    driversRef.child(firebaseUid).setValue(driverMap).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            setDriverOnlineStatus(firebaseUid);
                            Toast.makeText(this, "Driver Registered ✅", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, Login_Activity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "DB Error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loginRedirectBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
        });
    }

    // ✅ Function to set online/offline status
    private void setDriverOnlineStatus(String driverId) {
        DatabaseReference statusRef = driversRef.child(driverId).child("status");

        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(connected)) {
                    statusRef.setValue("online");
                    statusRef.onDisconnect().setValue("offline");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
