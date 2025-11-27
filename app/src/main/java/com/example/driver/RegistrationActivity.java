package com.example.driver;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private EditText vehicleModelInput, vehicleColorInput, vehicleNumberInput;
    private Button registerBtn;
    private ImageView bannerImage, genderImage;
    private TextView tvGreeting;

    private DatabaseReference driverRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set blue status bar before setting content
        setBlueStatusBar();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        initializeFirebase();
        initializeViews();
        setupSpinners();
        loadUserData();
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
        bannerImage = findViewById(R.id.imageViewBanner);
        genderImage = findViewById(R.id.genderImageView);
        vehicleTypeSpinner = findViewById(R.id.vehicleTypeSpinner);
        vehicleEmissionSpinner = findViewById(R.id.vehicleEmissionSpinner);
        vehicleModelInput = findViewById(R.id.vehicleModelInput);
        vehicleColorInput = findViewById(R.id.vehicleColorInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        registerBtn = findViewById(R.id.registerBtn);
        tvGreeting = findViewById(R.id.tvGreeting);
    }

    private void setupSpinners() {
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
                updateBannerImage();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                bannerImage.setImageResource(R.drawable.driver_registration);
            }
        });
    }

    private void updateBannerImage() {
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
                bannerImage.setImageResource(R.drawable.ambulance);
                break;
            default:
                bannerImage.setImageResource(R.drawable.driver_registration);
                break;
        }
    }

    private void loadUserData() {
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

                                tvGreeting.setText(firstName != null ? "Hello " + firstName + "..." : "Hello Driver");

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
    }

    private void setupClickListeners() {
        registerBtn.setOnClickListener(v -> saveVehicleDetails());
    }

    private void saveVehicleDetails() {
        if (!validateInputs()) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String vehicleType = vehicleTypeSpinner.getSelectedItem().toString();
        String emissionStandard = vehicleEmissionSpinner.getSelectedItem().toString();
        String vehicleModel = vehicleModelInput.getText().toString().trim();
        String vehicleColor = vehicleColorInput.getText().toString().trim();
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();


        // Show loading state
        registerBtn.setEnabled(false);
        registerBtn.setText("Registering...");

        HashMap<String, Object> vehicleInfo = new HashMap<>();
        vehicleInfo.put("vehicleType", vehicleType);
        vehicleInfo.put("vehicleModel", vehicleModel);
        vehicleInfo.put("vehicleColor", vehicleColor);
        vehicleInfo.put("vehicleNumber", vehicleNumber);
        vehicleInfo.put("emissionStandard", emissionStandard);

        vehicleInfo.put("isRegistered", true);

        driverRef.child(uid).child("info").updateChildren(vehicleInfo)
                .addOnCompleteListener(task -> {
                    // Reset button state
                    registerBtn.setEnabled(true);
                    registerBtn.setText("REGISTER VEHICLE");

                    if (task.isSuccessful()) {
                        Toast.makeText(RegistrationActivity.this, "Vehicle Registered Successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegistrationActivity.this, DashBoard.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(RegistrationActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    registerBtn.setEnabled(true);
                    registerBtn.setText("REGISTER VEHICLE");
                    Toast.makeText(RegistrationActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs() {
        int vehicleTypePosition = vehicleTypeSpinner.getSelectedItemPosition();
        int emissionPosition = vehicleEmissionSpinner.getSelectedItemPosition();

        // Spinner validation
        if (vehicleTypePosition == 0) {
            Toast.makeText(this, "Please select vehicle type", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (emissionPosition == 0) {
            Toast.makeText(this, "Please select emission standard", Toast.LENGTH_SHORT).show();
            return false;
        }

        // EditText validation
        if (TextUtils.isEmpty(vehicleModelInput.getText().toString().trim())) {
            vehicleModelInput.setError("Enter vehicle model");
            return false;
        }
        if (TextUtils.isEmpty(vehicleColorInput.getText().toString().trim())) {
            vehicleColorInput.setError("Enter vehicle color");
            return false;
        }
        if (TextUtils.isEmpty(vehicleNumberInput.getText().toString().trim())) {
            vehicleNumberInput.setError("Enter vehicle number");
            return false;
        }


        return true;
    }

    @Override
    public void onBackPressed() {
        // Show custom exit confirmation dialog
        showCustomExitDialog();
    }

    private void showCustomExitDialog() {
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_exit_dialog, null);

        // Initialize views from custom layout
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        Button btnExit = dialogView.findViewById(R.id.btnExit);

        // Create alert dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dismissal by tapping outside

        // Create and show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set custom background for dialog window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Continue Registration button click
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                // User chooses to continue registration
            }
        });

        // Exit App button click
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Exit the app completely
                finishAffinity();
                System.exit(0);
            }
        });
    }
}