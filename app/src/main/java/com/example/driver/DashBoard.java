package com.example.driver;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashBoard extends AppCompatActivity {


    private boolean doubleBackToExitPressedOnce = false;
    private static final int DOUBLE_BACK_PRESS_INTERVAL = 2000; // 2 seconds
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private DatabaseReference driverRef;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        driverRef = FirebaseDatabase.getInstance().getReference("drivers");

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Check registration status first
        checkRegistrationStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check registration status every time activity starts
        checkRegistrationStatus();
    }

    private void checkRegistrationStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // No user logged in, go to login
            redirectToLogin();
            return;
        }

        String uid = currentUser.getUid();
        driverRef.child(uid).child("info").child("isRegistered")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists() || !Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                            // User not registered, redirect to registration
                            redirectToRegistration();
                        } else {
                            // User is registered, initialize the dashboard
                            initializeDashboard();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // On error, redirect to registration for safety
                        Toast.makeText(DashBoard.this, "Error checking registration status", Toast.LENGTH_SHORT).show();
                        redirectToRegistration();
                    }
                });
    }

    private void initializeDashboard() {
        // Set default fragment only if user is registered
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new UserRequest_Fragment())
                    .commit();

            bottomNavigationView.setSelectedItemId(R.id.home);
        }

        // Bottom Navigation handling
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.home) {
                    loadFragment(new UserRequest_Fragment());
                } else if (itemId == R.id.Services) {
                    loadFragment(new Service_Fragment());
                } else if (itemId == R.id.account) {
                    loadFragment(new ProfileFragment());
                } else if (itemId == R.id.Activities) {
                    loadFragment(new Activity_Fragment());
                }
                return true;
            }
        });
    }

    private void loadFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void redirectToRegistration() {
        Toast.makeText(DashBoard.this, "Please complete vehicle registration first", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(DashBoard.this, RegistrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void redirectToLogin() {
        Toast.makeText(DashBoard.this, "Please login first", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(DashBoard.this, Login_Activity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        // First check if drawer is open
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        // Then check for double back press
        else if (doubleBackToExitPressedOnce) {
            // Second back press - exit the app
            super.onBackPressed();
            overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        }
        else {
            // First back press
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

            // Reset the flag after 2 seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, DOUBLE_BACK_PRESS_INTERVAL);
        }
    }
}