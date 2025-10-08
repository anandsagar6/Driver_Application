package com.example.driver;

import android.annotation.SuppressLint;
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

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 2500; // Minimum splash duration in ms
    private static final int ANIMATION_DURATION = 1000; // Animation duration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate tagline immediately
        TextView tagline = findViewById(R.id.tagline);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_animation_for_tagline);
        fadeIn.setDuration(ANIMATION_DURATION);
        tagline.startAnimation(fadeIn);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        long startTime = System.currentTimeMillis();

        // Wait for animation to complete before checking Firebase
        new Handler().postDelayed(() -> {
            if (currentUser != null) {
                checkUserRegistration(startTime);
            } else {
                handleNoUser(startTime);
            }
        }, ANIMATION_DURATION);
    }

    private void checkUserRegistration(long startTime) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(uid).child("info");

        driverRef.child("isRegistered").get().addOnCompleteListener(task -> {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingTime = SPLASH_TIME - elapsedTime;

            Runnable goNext = () -> {
                if (task.isSuccessful()) {
                    Boolean isRegistered = task.getResult().getValue(Boolean.class);
                    if (isRegistered != null && isRegistered) {
                        goToActivityWithAnimation(DashBoard.class);
                    } else {
                        goToActivityWithAnimation(RegistrationActivity.class);
                    }
                } else {
                    goToActivityWithAnimation(RegistrationActivity.class);
                }
            };

            if (remainingTime > 0) {
                new Handler().postDelayed(goNext, remainingTime);
            } else {
                goNext.run();
            }
        });
    }

    private void handleNoUser(long startTime) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingTime = SPLASH_TIME - elapsedTime;

        if (remainingTime > 0) {
            new Handler().postDelayed(() ->
                    goToActivityWithAnimation(Login_Activity.class), remainingTime);
        } else {
            goToActivityWithAnimation(Login_Activity.class);
        }
    }

    private void goToActivityWithAnimation(Class<?> cls) {
        // Create a fade-out animation for smooth transition
        findViewById(android.R.id.content).animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction(() -> {
                    Intent intent = new Intent(SplashActivity.this, cls);
                    startActivity(intent);
                    overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
                    finish();
                })
                .start();
    }
}