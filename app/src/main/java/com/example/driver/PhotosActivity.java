package com.example.driver;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.driver.api.ApiClient;
import com.example.driver.api.DriverApi;
import com.example.driver.api.FileUtils;
import com.example.driver.model.UploadResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotosActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;
    private static final int PICK_CAMERA = 200;
    private static final String TAG = "PhotosActivity";

    private ImageView profileImage;


    private AlertDialog progressDialog;

    private ImageView imgProfile, imgDLFront, imgDLBack, imgAadharFront, imgAadharBack, imgVehicle;
    private Button btnSubmit;

    private Uri profileUri, dlFrontUri, dlBackUri, aadharFrontUri, aadharBackUri, vehicleUri, cameraImageUri;

    private String uid, phone, currentType = "";

    private int totalUploads = 0;
    private int successfulUploads = 0;
    private boolean phoneLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);

        imgProfile = findViewById(R.id.imgProfile);
        imgDLFront = findViewById(R.id.imgDLFront);
        imgDLBack = findViewById(R.id.imgDLBack);
        imgAadharFront = findViewById(R.id.imgAadharFront);
        imgAadharBack = findViewById(R.id.imgAadharBack);
        imgVehicle = findViewById(R.id.imgVehicle);
        btnSubmit = findViewById(R.id.btnSubmit);
        profileImage = findViewById(R.id.profileImage);


        btnSubmit.setEnabled(false);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (uid == null) {
            Toast.makeText(this, "Authentication Error. Login again.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("drivers")
                .child(uid)
                .child("info")
                .child("phone")
                .get()
                .addOnSuccessListener(snapshot -> {

                    phone = snapshot.getValue(String.class);

                    if (phone == null || phone.isEmpty()) {
                        Toast.makeText(this, "Phone number missing!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    phoneLoaded = true;
                    Log.d(TAG, "Phone Loaded: " + phone);

                    loadAllImages();
                    updateSubmitState();
                });

        imgProfile.setOnClickListener(v -> chooseSource("PROFILE"));
        imgDLFront.setOnClickListener(v -> chooseSource("DL_FRONT"));
        imgDLBack.setOnClickListener(v -> chooseSource("DL_BACK"));
        imgAadharFront.setOnClickListener(v -> chooseSource("AADHAAR_FRONT"));
        imgAadharBack.setOnClickListener(v -> chooseSource("AADHAAR_BACK"));
        imgVehicle.setOnClickListener(v -> chooseSource("VEHICLE"));

        btnSubmit.setOnClickListener(v -> startUploading());
    }


    private void chooseSource(String type) {
        currentType = type;

        String[] options = {"📷 Camera", "🖼 Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, PICK_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        Uri selectedImage = (requestCode == PICK_CAMERA) ? cameraImageUri : (data != null ? data.getData() : null);
        if (selectedImage == null) return;

        switch (currentType) {
            case "PROFILE":
                profileUri = selectedImage;
                updateImage(profileImage, selectedImage); // <-- image goes to circle photo
                break;
            case "DL_FRONT": dlFrontUri = selectedImage; updateImage(imgDLFront, selectedImage); break;
            case "DL_BACK": dlBackUri = selectedImage; updateImage(imgDLBack, selectedImage); break;
            case "AADHAAR_FRONT": aadharFrontUri = selectedImage; updateImage(imgAadharFront, selectedImage); break;
            case "AADHAAR_BACK": aadharBackUri = selectedImage; updateImage(imgAadharBack, selectedImage); break;
            case "VEHICLE": vehicleUri = selectedImage; updateImage(imgVehicle, selectedImage); break;
        }

        updateSubmitState();
    }


    private void updateImage(ImageView imageView, Uri uri) {
        Glide.with(this).clear(imageView);
        Glide.with(this).load(uri).into(imageView);
        Toast.makeText(this, "Image Updated ✔", Toast.LENGTH_SHORT).show();
    }


    private void startUploading() {
        showLoading(true);
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading...");

        totalUploads = 6;
        successfulUploads = 0;

        uploadImage(profileUri, "PROFILE");
        uploadImage(dlFrontUri, "DL_FRONT");
        uploadImage(dlBackUri, "DL_BACK");
        uploadImage(aadharFrontUri, "AADHAAR_FRONT");
        uploadImage(aadharBackUri, "AADHAAR_BACK");
        uploadImage(vehicleUri, "VEHICLE");
    }


    private void uploadImage(Uri imageUri, String type) {

        String filePath = FileUtils.getPath(this, imageUri);
        File file = new File(filePath);

        RequestBody requestFile = RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), file);
        MultipartBody.Part multipart = MultipartBody.Part.createFormData("file", type + ".jpg", requestFile);

        RequestBody typeBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), type);
        RequestBody phoneBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), phone);

        ApiClient.getClient().create(DriverApi.class)
                .uploadDocument(phoneBody, typeBody, multipart).enqueue(new Callback<UploadResponse>() {

                    @Override
                    public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FirebaseDatabase.getInstance().getReference("drivers")
                                    .child(uid)
                                    .child("info")
                                    .child("documents")
                                    .child(type)
                                    .setValue(response.body().getUrl());
                            successfulUploads++;
                        }
                        checkCompletion();
                    }

                    @Override
                    public void onFailure(Call<UploadResponse> call, Throwable t) {
                        Toast.makeText(PhotosActivity.this, "Upload Failed: " + type, Toast.LENGTH_SHORT).show();
                        checkCompletion();
                    }
                });
    }


    private void checkCompletion() {
        if (successfulUploads == totalUploads) {

            showLoading(false);

            FirebaseDatabase.getInstance().getReference("drivers")
                    .child(uid)
                    .child("info")
                    .child("image_uploaded")
                    .setValue(true);

            Toast.makeText(this, "🎉 All documents uploaded successfully!", Toast.LENGTH_LONG).show();

            startActivity(new Intent(PhotosActivity.this, RegistrationActivity.class));
            finish();
        }
    }


    private void loadAllImages() {
        loadImage("PROFILE", imgProfile);
        loadImage("DL_FRONT", imgDLFront);
        loadImage("DL_BACK", imgDLBack);
        loadImage("AADHAAR_FRONT", imgAadharFront);
        loadImage("AADHAAR_BACK", imgAadharBack);
        loadImage("VEHICLE", imgVehicle);
    }


    private void loadImage(String type, ImageView imageView) {
        ApiClient.getClient().create(DriverApi.class)
                .getDocument(phone, type).enqueue(new Callback<UploadResponse>() {
                    @Override
                    public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Glide.with(PhotosActivity.this).load(response.body().getUrl()).into(imageView);
                        }
                    }

                    @Override
                    public void onFailure(Call<UploadResponse> call, Throwable t) {}
                });
    }


    private void updateSubmitState() {
        btnSubmit.setEnabled(
                phoneLoaded &&
                        profileUri != null &&
                        dlFrontUri != null &&
                        dlBackUri != null &&
                        aadharFrontUri != null &&
                        aadharBackUri != null &&
                        vehicleUri != null
        );
    }


    private void showLoading(boolean show) {
        if (show) {
            if (progressDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                builder.setView(R.layout.progress_loader);
                progressDialog = builder.create();
            }
            progressDialog.show();
        } else {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Show custom exit confirmation dialog
        showCustomExitDialog();
    }

    private void showCustomExitDialog() {
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_box, null);

        // Initialize views from custom layout
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        Button btnExit = dialogView.findViewById(R.id.btnExit);

        // Create alert dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dismissal by tapping outside

        // Create and show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set custom background for dialog window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Continue Registration button click
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                // User chooses to continue registration
            }
        });

        // Exit App button click
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Exit the app completely
                finishAffinity();
                System.exit(0);
            }
        });
    }
}