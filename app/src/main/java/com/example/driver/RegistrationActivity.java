package com.example.driver;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;

public class RegistrationActivity extends AppCompatActivity {

    private Spinner vehicleTypeSpinner, vehicleEmissionSpinner;
    private EditText vehicleModelInput, vehicleColorInput, vehicleNumberInput;
    private Button registerBtn;
    private ImageView bannerImage, profileImage; // 🔥 updated
    private TextView tvGreeting;

    private DatabaseReference driverRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStatusBar();
        setContentView(R.layout.activity_registration);

        initializeFirebase();
        initializeViews();
        setupSpinners();
        loadUserData();
        setupClickListeners();
    }

    private void setStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            try {
                w.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_blue));
            } catch (Exception e) {
                w.setStatusBarColor(Color.parseColor("#004FFF"));
            }
        }
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        driverRef = FirebaseDatabase.getInstance().getReference("drivers");
    }

    private void initializeViews() {
        bannerImage = findViewById(R.id.imageViewBanner);
        profileImage = findViewById(R.id.genderImageView); // 🔥 uses same id from XML
        vehicleTypeSpinner = findViewById(R.id.vehicleTypeSpinner);
        vehicleEmissionSpinner = findViewById(R.id.vehicleEmissionSpinner);
        vehicleModelInput = findViewById(R.id.vehicleModelInput);
        vehicleColorInput = findViewById(R.id.vehicleColorInput);
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput);
        registerBtn = findViewById(R.id.registerBtn);
        tvGreeting = findViewById(R.id.tvGreeting);
    }

    private void setupSpinners() {
        String[] vehicleTypes = {"Select Vehicle Type", "SUV", "Sedan", "Auto", "Bike", "Ambulance"};
        vehicleTypeSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, vehicleTypes));

        String[] emissionStandards = {"Select Emission Standard", "BS4", "BS6"};
        vehicleEmissionSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, emissionStandards));

        vehicleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { updateBannerImage(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateBannerImage() {
        switch (vehicleTypeSpinner.getSelectedItem().toString()) {
            case "SUV": bannerImage.setImageResource(R.drawable.premium); break;
            case "Sedan": bannerImage.setImageResource(R.drawable.sedan); break;
            case "Auto": bannerImage.setImageResource(R.drawable.auto); break;
            case "Bike": bannerImage.setImageResource(R.drawable.bike); break;
            case "Ambulance": bannerImage.setImageResource(R.drawable.ambulance); break;
            default: bannerImage.setImageResource(R.drawable.driver_registration); break;
        }
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        driverRef.child(user.getUid()).child("info")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        String firstName = snapshot.child("firstName").getValue(String.class);
                        tvGreeting.setText(firstName != null ? "Hello " + firstName + "..." : "Hello Driver");

                        // 🔥 Load Profile Photo
                        String profileUrl = snapshot.child("documents").child("PROFILE").getValue(String.class);
                        if (profileUrl != null) {
                            Glide.with(RegistrationActivity.this)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.user)
                                    .circleCrop()
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.user);
                        }
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void setupClickListeners() {
        registerBtn.setOnClickListener(v -> saveVehicleDetails());
    }

    private void saveVehicleDetails() {
        if (!validateInputs()) return;

        String uid = auth.getCurrentUser().getUid();
        HashMap<String, Object> vehicleInfo = new HashMap<>();
        vehicleInfo.put("vehicleType", vehicleTypeSpinner.getSelectedItem().toString());
        vehicleInfo.put("vehicleModel", vehicleModelInput.getText().toString().trim());
        vehicleInfo.put("vehicleColor", vehicleColorInput.getText().toString().trim());
        vehicleInfo.put("vehicleNumber", vehicleNumberInput.getText().toString().trim());
        vehicleInfo.put("emissionStandard", vehicleEmissionSpinner.getSelectedItem().toString());

        registerBtn.setEnabled(false);
        registerBtn.setText("Registering...");

        driverRef.child(uid).child("info").child("isRegistered").setValue(true);

        driverRef.child(uid).child("info").updateChildren(vehicleInfo)
                .addOnCompleteListener(task -> {
                    registerBtn.setEnabled(true);
                    registerBtn.setText("REGISTER");

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Vehicle Registered Successfully! 🚗", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, DashBoard.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        if (vehicleTypeSpinner.getSelectedItemPosition() == 0) return error("Select vehicle type");
        if (vehicleEmissionSpinner.getSelectedItemPosition() == 0) return error("Select emission standard");
        if (empty(vehicleModelInput)) return errorField(vehicleModelInput);
        if (empty(vehicleColorInput)) return errorField(vehicleColorInput);
        if (empty(vehicleNumberInput)) return errorField(vehicleNumberInput);
        return true;
    }

    private boolean empty(EditText e) { return TextUtils.isEmpty(e.getText().toString().trim()); }

    private boolean error(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); return false; }

    private boolean errorField(EditText e) { e.setError("Required"); e.requestFocus(); return false; }

    @Override
    public void onBackPressed() { showExitDialog(); }

    private void showExitDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.custom_exit_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
        view.findViewById(R.id.btnContinue).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnExit).setOnClickListener(v -> { finishAffinity(); System.exit(0); });
        dialog.show();
    }
}
