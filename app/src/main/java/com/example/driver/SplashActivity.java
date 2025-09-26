package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 2500; // 2.5 seconds
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference driverRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate tagline
        TextView tagline = findViewById(R.id.tagline);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_animation_for_tagline);
        tagline.startAnimation(fadeIn);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        new Handler().postDelayed(() -> {
            if (currentUser != null) {
                String uid = currentUser.getUid();
                driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(uid).child("info");

                // Check registration status in Firebase
                driverRef.child("isRegistered").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Boolean isRegistered = task.getResult().getValue(Boolean.class);

                        if (isRegistered != null && isRegistered) {
                            // ✅ Registered → go to Dashboard
                            goToActivity(DashBoard.class);
                        } else {
                            // ❌ Not registered yet → go to Registration page
                            goToActivity(RegistrationActivity.class);
                        }
                    } else {
                        // In case of error → safe fallback to Registration
                        goToActivity(RegistrationActivity.class);
                    }
                });

            } else {
                // Not logged in → go to login screen
                goToActivity(Login_Activity.class);
            }
        }, SPLASH_TIME);
    }

    private void goToActivity(Class<?> cls) {
        Intent intent = new Intent(SplashActivity.this, cls);
        startActivity(intent);
        overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        finish();
    }
}
