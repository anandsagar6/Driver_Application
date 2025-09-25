package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Login_Activity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn;
    FirebaseAuth auth;
    TextView signupRedirectBtn,privacy,term;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        signupRedirectBtn = findViewById(R.id.signupRedirectBtn);
        privacy=findViewById(R.id.txtPrivacyPolicy);
        term=findViewById(R.id.txtTermsConditions);


        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Login_Activity.this,PrivacyPolicyActivity.class);
                startActivity(i);
            }
        });
        term.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Login_Activity.this,TermsConditionsActivity.class);
                startActivity(i);
            }
        });

        auth = FirebaseAuth.getInstance();

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
