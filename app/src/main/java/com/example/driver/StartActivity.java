package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            // Already logged in → go to dashboard
            startActivity(new Intent(StartActivity.this, DashBoard.class));
        } else {
            // Not logged in → go to login screen
            startActivity(new Intent(StartActivity.this, Login_Activity.class));
        }

        finish(); // Close this activity
    }
}
