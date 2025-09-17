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

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 2500; // 2.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate tagline
        TextView tagline = findViewById(R.id.tagline);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_animation_for_tagline);
        tagline.startAnimation(fadeIn);

        // Delay and move to next activity
        new Handler().postDelayed(() -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            Intent intent;
            if (user != null) {
                // Already logged in → go to dashboard
                intent = new Intent(SplashActivity.this, DashBoard.class);
            } else {
                // Not logged in → go to login screen
                intent = new Intent(SplashActivity.this, Login_Activity.class);
            }

            startActivity(intent);
            overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
            finish();
        }, SPLASH_TIME);
    }
}
