package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Login_Activity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn;
    FirebaseAuth auth;
    TextView signupRedirectBtn;
    ImageView showPasswordBtn;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        signupRedirectBtn = findViewById(R.id.signupRedirectBtn);
        showPasswordBtn = findViewById(R.id.showPasswordBtn);

        auth = FirebaseAuth.getInstance();

        // 👁️ Toggle password visibility
        showPasswordBtn.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide Password
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                showPasswordBtn.setImageResource(R.drawable.eye_closed);
                isPasswordVisible = false;
            } else {
                // Show Password
                passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                showPasswordBtn.setImageResource(R.drawable.eye_open);
                isPasswordVisible = true;
            }

            // Move cursor to end of text
            passwordInput.setSelection(passwordInput.getText().length());
        });

        // 🔑 Login button
        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login Successful ✅", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, DashBoard.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 🔄 Signup redirect
        signupRedirectBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Login_Activity.this, Signup_Activity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }
}
