package com.example.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    Button btnLogout, btnAccept, btnReject;
    TextView tvRideType, tvPickup, tvDestination, tvPrice, tvDistance;

    FirebaseAuth auth;
    DatabaseReference ridesRef, driversRef;
    String currentDriverId;
    String currentRideId = null;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        FirebaseUser driver = auth.getCurrentUser();

        if (driver == null) {
            startActivity(new Intent(MainActivity.this, Login_Activity.class));
            finish();
            return;
        }

        currentDriverId = driver.getUid();
        ridesRef = FirebaseDatabase.getInstance().getReference("Rides");
        driversRef = FirebaseDatabase.getInstance().getReference("drivers").child(currentDriverId);

        btnLogout = findViewById(R.id.btnLogout);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);
        tvRideType = findViewById(R.id.tvRideType);
        tvPickup = findViewById(R.id.tvPickup);
        tvDestination = findViewById(R.id.tvDestination);
        tvPrice = findViewById(R.id.tvPrice);
        tvDistance = findViewById(R.id.tvDistance);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // 🔹 Start updating location
        startLocationUpdates();

        // 🔹 Listen for waiting rides
        ridesRef.orderByChild("status").equalTo("waiting")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot rideSnap : snapshot.getChildren()) {
                                currentRideId = rideSnap.getKey();

                                String rideType = rideSnap.child("rideType").getValue(String.class);
                                Double pickupLat = rideSnap.child("pickupLat").getValue(Double.class);
                                Double pickupLng = rideSnap.child("pickupLng").getValue(Double.class);
                                Double destLat = rideSnap.child("destLat").getValue(Double.class);
                                Double destLng = rideSnap.child("destLng").getValue(Double.class);
                                String price = rideSnap.child("price").getValue(String.class);
                                String destinationText = rideSnap.child("destinationText").getValue(String.class);

                                tvRideType.setText("Ride Type: " + rideType);
                                tvPickup.setText("Pickup: " + pickupLat + ", " + pickupLng);
                                tvDestination.setText(destinationText != null ?
                                        "Destination: " + destinationText :
                                        "Destination: " + destLat + ", " + destLng);
                                tvPrice.setText("Price: " + price);

                                // ✅ Distance to pickup
                                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                                        if (location != null && pickupLat != null && pickupLng != null) {
                                            float[] results = new float[1];
                                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                                    pickupLat, pickupLng, results);
                                            float distanceInKm = results[0] / 1000;
                                            tvDistance.setText("Distance to Pickup: " +
                                                    String.format("%.2f", distanceInKm) + " km");
                                        }
                                    });
                                }
                                break; // only first ride
                            }
                        } else {
                            clearRideUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // 🔹 Accept Ride
        btnAccept.setOnClickListener(v -> {
            if (currentRideId != null) {
                ridesRef.child(currentRideId).child("status").setValue("accepted");
                ridesRef.child(currentRideId).child("driverId").setValue(currentDriverId);
                driversRef.child("status").setValue("onRide");
                Toast.makeText(MainActivity.this, "Ride Accepted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No ride to accept", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔹 Reject Ride
        btnReject.setOnClickListener(v -> {
            if (currentRideId != null) {
                // Instead of rejecting permanently, mark this driver as "rejected"
                ridesRef.child(currentRideId).child("rejectedDrivers")
                        .child(currentDriverId).setValue(true);

                clearRideUI();
                Toast.makeText(MainActivity.this, "Ride Rejected", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔹 Logout
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(MainActivity.this, Login_Activity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void clearRideUI() {
        tvRideType.setText("No Ride Requests");
        tvPickup.setText("");
        tvDestination.setText("");
        tvPrice.setText("");
        tvDistance.setText("");
        currentRideId = null;
        driversRef.child("status").setValue("available");
    }

    // ✅ Start continuous location updates
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000)   // every 5 sec
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // update driver location
                        driversRef.child("currentLat").setValue(location.getLatitude());
                        driversRef.child("currentLng").setValue(location.getLongitude());

                        // if driver has accepted a ride → also update inside ride
                        if (currentRideId != null) {
                            ridesRef.child(currentRideId).child("driverLat").setValue(location.getLatitude());
                            ridesRef.child(currentRideId).child("driverLng").setValue(location.getLongitude());
                        }
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }
}
