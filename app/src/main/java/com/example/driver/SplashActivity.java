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

    private static final int SPLASH_TIME = 2500;
    private static final int ANIMATION_DURATION = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tagline = findViewById(R.id.tagline);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_animation_for_tagline);
        fadeIn.setDuration(ANIMATION_DURATION);
        tagline.startAnimation(fadeIn);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        long startTime = System.currentTimeMillis();

        new Handler().postDelayed(() -> {
            if (currentUser != null) {
                checkUserStatus(startTime);
            } else {
                goWithDelay(Login_Activity.class, startTime);
            }
        }, ANIMATION_DURATION);
    }

    private void checkUserStatus(long startTime) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(uid).child("info");

        driverRef.get().addOnSuccessListener(snapshot -> {

            boolean imagesUploaded = false;
            boolean isRegistered = false;

            if (snapshot.child("image_uploaded").exists())
                imagesUploaded = snapshot.child("image_uploaded").getValue(Boolean.class);

            if (snapshot.child("isRegistered").exists())
                isRegistered = snapshot.child("isRegistered").getValue(Boolean.class);

            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingTime = SPLASH_TIME - elapsedTime;

            Runnable nextAction;

            if (!imagesUploaded) {
                nextAction = () -> goToActivityWithAnimation(PhotosActivity.class);
            } else if (!isRegistered) {
                nextAction = () -> goToActivityWithAnimation(RegistrationActivity.class);
            } else {
                nextAction = () -> goToActivityWithAnimation(DashBoard.class);
            }

            if (remainingTime > 0) {
                new Handler().postDelayed(nextAction, remainingTime);
            } else {
                nextAction.run();
            }

        }).addOnFailureListener(e -> goWithDelay(Login_Activity.class, startTime));
    }

    private void goWithDelay(Class<?> cls, long startTime) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingTime = SPLASH_TIME - elapsedTime;

        if (remainingTime > 0) {
            new Handler().postDelayed(() -> goToActivityWithAnimation(cls), remainingTime);
        } else {
            goToActivityWithAnimation(cls);
        }
    }

    private void goToActivityWithAnimation(Class<?> cls) {
        findViewById(android.R.id.content).animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction(() -> {
                    startActivity(new Intent(SplashActivity.this, cls));
                    overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
                    finish();
                })
                .start();
    }
}
