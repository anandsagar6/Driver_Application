// DeleteAccountActivity.java
package com.example.driver;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class DeleteAccountActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private static final String GOOGLE_FORM_URL = "https://forms.gle/LRaB1REDtg7EWxcJ9";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        // Set WebView client to monitor form submission and loading progress
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Show progress bar when page starts loading
                progressBar.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Hide progress bar when page finishes loading
                progressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);

                // Check if we're on the form submission confirmation page
                if (url.contains("/formResponse") || url.contains("formResponse")) {
                    // Form submitted successfully - logout user
                    logoutUser();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Hide progress bar on error
                progressBar.setVisibility(View.GONE);
                super.onReceivedError(view, errorCode, description, failingUrl);

                // Optional: Show error message
                android.widget.Toast.makeText(DeleteAccountActivity.this, "Failed to load page", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Load the Google Form
        webView.loadUrl(GOOGLE_FORM_URL);
    }

    private void logoutUser() {
        // Perform logout and redirect to login
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(DeleteAccountActivity.this, Login_Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Optional: Add a toast message
        android.widget.Toast.makeText(this, "Account deletion request submitted. You have been logged out.", android.widget.Toast.LENGTH_LONG).show();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}