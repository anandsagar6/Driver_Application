package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class RegistrationActivity extends AppCompatActivity {

    private Spinner vehicleTypeSpinner, vehicleEmissionSpinner;
    private EditText vehicleModelInput, vehicleColorInput, vehicleNumberInput, dlNumberInput;
    private Button registerBtn;
    private ImageView bannerImage, genderImage;
    private TextView tvGreeting;

    private DatabaseReference driverRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        // Firebase
        auth = FirebaseAuth.getInstance();
        driverRef = FirebaseDatabase.getInstance().getReference("drivers");

        // Views
        bannerImage = findViewById(R.id.imageViewBanner);
        genderImage = findViewById(R.id.genderImageView); // your ImageView for gender
        vehicleTypeSpinner = findViewById(R.id.vehicleTypeSpinner);
        vehicleEmissionSpinner = findViewById(R.id.vehicleEmissionSpinner);
        vehicleModelInput = findViewById(R.id.vehicleModelInput);
        vehicleColorInput = findViewById(R.id.vehicleColorInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        dlNumberInput = findViewById(R.id.dlNumberInput);
        registerBtn = findViewById(R.id.registerBtn);
        tvGreeting = findViewById(R.id.tvGreeting);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Fetch user info (name & gender) from Firebase
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            driverRef.child(uid).child("info")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String firstName = snapshot.child("firstName").getValue(String.class);
                                String gender = snapshot.child("gender").getValue(String.class);

                                tvGreeting.setText(firstName != null ? "Hey " + firstName + "!" : "Hello Driver");

                                // Set gender image
                                if ("Male".equalsIgnoreCase(gender)) {
                                    genderImage.setImageResource(R.drawable.male);
                                } else if ("Female".equalsIgnoreCase(gender)) {
                                    genderImage.setImageResource(R.drawable.female);
                                } else {
                                    genderImage.setImageResource(R.drawable.user);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            tvGreeting.setText("Hello Driver");
                            genderImage.setImageResource(R.drawable.user);
                        }
                    });
        }

        // Vehicle type array with default
        String[] vehicleTypes = {"Select Vehicle Type", "SUV", "Sedan", "Auto", "Bike", "Ambulance"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, vehicleTypes);
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        vehicleTypeSpinner.setAdapter(typeAdapter);

        // Vehicle emission standards array with default
        String[] emissionStandards = {"Select Emission Standard", "BS4", "BS6"};
        ArrayAdapter<String> emissionAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, emissionStandards);
        emissionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        vehicleEmissionSpinner.setAdapter(emissionAdapter);

        // Vehicle Type Spinner listener to change banner image
        vehicleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVehicle = vehicleTypeSpinner.getSelectedItem().toString();
                switch (selectedVehicle) {
                    case "SUV":
                        bannerImage.setImageResource(R.drawable.premium);
                        break;
                    case "Sedan":
                        bannerImage.setImageResource(R.drawable.sedan);
                        break;
                    case "Auto":
                        bannerImage.setImageResource(R.drawable.auto);
                        break;
                    case "Bike":
                        bannerImage.setImageResource(R.drawable.bike);
                        break;
                    case "Ambulance":
                        bannerImage.setImageResource(R.drawable.amblance);
                        break;
                    default:
                        bannerImage.setImageResource(R.drawable.driver_registration);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                bannerImage.setImageResource(R.drawable.driver_registration);
            }
        });

        // Register button click
        registerBtn.setOnClickListener(v -> saveVehicleDetails());
    }

    private void saveVehicleDetails() {
        String uid = auth.getCurrentUser().getUid();

        int vehicleTypePosition = vehicleTypeSpinner.getSelectedItemPosition();
        int emissionPosition = vehicleEmissionSpinner.getSelectedItemPosition();

        // Spinner validation
        if (vehicleTypePosition == 0) {
            Toast.makeText(this, "Please select vehicle type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (emissionPosition == 0) {
            Toast.makeText(this, "Please select emission standard", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get values
        String vehicleType = vehicleTypeSpinner.getSelectedItem().toString();
        String emissionStandard = vehicleEmissionSpinner.getSelectedItem().toString();
        String vehicleModel = vehicleModelInput.getText().toString().trim();
        String vehicleColor = vehicleColorInput.getText().toString().trim();
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();
        String dlNumber = dlNumberInput.getText().toString().trim();

        // EditText validation
        if (TextUtils.isEmpty(vehicleModel)) {
            vehicleModelInput.setError("Enter vehicle model");
            return;
        }
        if (TextUtils.isEmpty(vehicleColor)) {
            vehicleColorInput.setError("Enter vehicle color");
            return;
        }
        if (TextUtils.isEmpty(vehicleNumber)) {
            vehicleNumberInput.setError("Enter vehicle number");
            return;
        }
        if (TextUtils.isEmpty(dlNumber)) {
            dlNumberInput.setError("Enter DL number");
            return;
        }

        // Save vehicle info under "info" node
        HashMap<String, Object> vehicleInfo = new HashMap<>();
        vehicleInfo.put("vehicleType", vehicleType);
        vehicleInfo.put("vehicleModel", vehicleModel);
        vehicleInfo.put("vehicleColor", vehicleColor);
        vehicleInfo.put("vehicleNumber", vehicleNumber);
        vehicleInfo.put("emissionStandard", emissionStandard);
        vehicleInfo.put("dlNumber", dlNumber);
        vehicleInfo.put("isRegistered", true);

        driverRef.child(uid).child("info").updateChildren(vehicleInfo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegistrationActivity.this, "Vehicle Registered Successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegistrationActivity.this, DashBoard.class));
                        finish();
                    } else {
                        Toast.makeText(RegistrationActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
