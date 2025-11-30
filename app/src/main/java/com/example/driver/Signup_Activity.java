package com.example.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Patterns;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

    private boolean doubleBackToExitPressedOnce = false;
    private static final int DOUBLE_BACK_PRESS_INTERVAL = 2000;

    private FirebaseAuth auth;
    private DatabaseReference driverRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setBlueStatusBar();
        setContentView(R.layout.activity_signup);

        requestNotificationPermission(); // 👈 IMPORTANT

        initializeFirebase();
        initializeViews();
        setupGenderSpinner();
        setupClickListeners();
    }

    // 👉 Runtime Notification Permission for Android 13+
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }

    private void setBlueStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            try {
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_blue));
            } catch (Exception e) {
                window.setStatusBarColor(Color.parseColor("#004FFF"));
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, genders);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        privacy.setOnClickListener(view -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        term.setOnClickListener(view -> startActivity(new Intent(this, TermsConditionsActivity.class)));

        signupBtn.setOnClickListener(v -> registerDriver());

        loginRedirectBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
        });
    }

    private void registerDriver() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        int genderPosition = genderSpinner.getSelectedItemPosition();

        if (!validateInputs(firstName, lastName, phone, email, password, genderPosition)) return;

        signupProgressBar.setVisibility(View.VISIBLE);
        signupBtn.setEnabled(false);

        // 🔥 Check if phone already exists
        driverRef.orderByChild("info/phone").equalTo(phone)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {

                        if (snapshot.exists()) {
                            signupProgressBar.setVisibility(View.GONE);
                            signupBtn.setEnabled(true);
                            phoneInput.setError("Phone already registered");
                            Toast.makeText(Signup_Activity.this, "Phone already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        createFirebaseUser(firstName, lastName, phone, email, password);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                });
    }

    private void createFirebaseUser(String firstName, String lastName, String phone, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    signupProgressBar.setVisibility(View.GONE);
                    signupBtn.setEnabled(true);

                    if (task.isSuccessful()) {
                        saveDriverData(auth.getCurrentUser().getUid(), firstName, lastName, phone, email);
                    } else {
                        Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String firstName, String lastName, String phone,
                                   String email, String password, int genderPosition) {

        if (TextUtils.isEmpty(firstName)) return error(firstNameInput, "Required");
        if (TextUtils.isEmpty(lastName)) return error(lastNameInput, "Required");
        if (phone.length() != 10) return error(phoneInput, "Enter valid 10 digit phone");
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return error(emailInput, "Invalid email");
        if (password.length() < 8) return error(passwordInput, "Min 8 characters");
        if (genderPosition == 0) { Toast.makeText(this, "Select gender", Toast.LENGTH_SHORT).show(); return false; }

        return true;
    }

    private boolean error(EditText field, String msg) {
        field.setError(msg);
        field.requestFocus();
        return false;
    }

    private void saveDriverData(String uid, String firstName, String lastName, String phone, String email) {

        HashMap<String, Object> driverInfo = new HashMap<>();
        driverInfo.put("firstName", firstName);
        driverInfo.put("lastName", lastName);
        driverInfo.put("phone", phone);
        driverInfo.put("email", email);
        driverInfo.put("gender", genderSpinner.getSelectedItem().toString());
        driverInfo.put("image_uploaded", false);
        driverInfo.put("isRegistered", false);

        HashMap<String, Object> driverStatus = new HashMap<>();
        driverStatus.put("status", "available");
        driverStatus.put("currentLat", 0);
        driverStatus.put("currentLng", 0);

        HashMap<String, Object> driverData = new HashMap<>();
        driverData.put("info", driverInfo);
        driverData.put("status", driverStatus);

        driverRef.child(uid).updateChildren(driverData)
                .addOnSuccessListener(unused -> {

                    showSignupNotification();
                    Toast.makeText(this, "Signup Successful 🎉", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(this, PhotosActivity.class));
                    finish();
                });
    }

    // 🔔 Notification Method
    private void showSignupNotification() {

        String channelId = "signup_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(channelId, "Signup Alerts", android.app.NotificationManager.IMPORTANCE_HIGH);
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, Login_Activity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.main_logo)
                .setContentTitle("Signup Successful 🎉")
                .setContentText("Welcome to the app! Complete verification to continue.")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build());
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) { super.onBackPressed(); return; }
        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, DOUBLE_BACK_PRESS_INTERVAL);
    }
}
