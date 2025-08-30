package com.example.driver;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RideDetail_Activity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView statusText, pickupText, destText, fareText, distanceText;
    private Button startRideBtn, cancelRideBtn;

    private DatabaseReference ridesRef, driversRef;
    private String rideId, driverId;
    private double pickupLat, pickupLng, destLat, destLng, driverLat, driverLng;
    private String pickupName, dropName, price, rideType, status;

    // Ripple
    private Circle rippleCircle;
    private ValueAnimator rippleAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);

        // UI
        statusText = findViewById(R.id.statusText);
        pickupText = findViewById(R.id.pickupText);
        destText = findViewById(R.id.destText);
        fareText = findViewById(R.id.fareText);
        distanceText = findViewById(R.id.distanceText);
        startRideBtn = findViewById(R.id.startRideBtn);
        cancelRideBtn = findViewById(R.id.cancelRideBtn);

        startRideBtn.setVisibility(Button.GONE);
        cancelRideBtn.setVisibility(Button.GONE);

        // Firebase
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        ridesRef = db.getReference("Rides");
        driversRef = db.getReference("drivers");
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get rideId
        rideId = getIntent().getStringExtra("rideId");
        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Ride ID missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Assign driver
        assignDriverToRide();

        // Load ride details
        loadRideDetails();

        // Map setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Button actions
        startRideBtn.setOnClickListener(v -> {
            if ("accepted".equalsIgnoreCase(status)) {
                showPinDialog(); // verify PIN
            } else if ("ongoing".equalsIgnoreCase(status)) {
                endRide(); // end ride
            }
        });

        cancelRideBtn.setOnClickListener(v -> confirmCancelRide());
    }

    // Save driver info in ride node
    private void assignDriverToRide() {
        ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(RideDetail_Activity.this, "No customer linked to this ride", Toast.LENGTH_SHORT).show();
                    return;
                }

                String customerId = snapshot.getValue(String.class);

                driversRef.child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot driverSnap) {
                        if (driverSnap.exists()) {
                            String firstName = driverSnap.child("firstName").getValue(String.class);
                            String lastName = driverSnap.child("lastName").getValue(String.class);
                            String driverName = (firstName != null ? firstName : "") + " " +
                                    (lastName != null ? lastName : "");
                            String vehicle = driverSnap.child("vehicle").getValue(String.class);

                            String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(new Date());

                            // ✅ Update ride node
                            ridesRef.child(rideId).child("driverId").setValue(driverId);
                            ridesRef.child(rideId).child("driverName").setValue(driverName.trim());
                            if (vehicle != null)
                                ridesRef.child(rideId).child("vehicle").setValue(vehicle);
                            ridesRef.child(rideId).child("status").setValue("accepted");
                            ridesRef.child(rideId).child("driverAcceptTime").setValue(currentTime);

                            // ✅ Update customer’s rides
                            DatabaseReference customerRideRef = FirebaseDatabase.getInstance()
                                    .getReference("Customers")
                                    .child(customerId)
                                    .child("rides")
                                    .child(rideId);

                            customerRideRef.child("driverId").setValue(driverId);
                            customerRideRef.child("driverName").setValue(driverName.trim());
                            if (vehicle != null)
                                customerRideRef.child("vehicle").setValue(vehicle);
                            customerRideRef.child("status").setValue("accepted");
                            customerRideRef.child("driverAcceptTime").setValue(currentTime);

                            // ✅ Also update driver’s rides
                            DatabaseReference driverRideRef = FirebaseDatabase.getInstance()
                                    .getReference("drivers")
                                    .child(driverId)
                                    .child("rides")
                                    .child(rideId);

                            driverRideRef.child("rideId").setValue(rideId);
                            driverRideRef.child("customerId").setValue(customerId);
                            driverRideRef.child("pickupName").setValue(pickupName);
                            driverRideRef.child("Drop").setValue(dropName);
                            driverRideRef.child("price").setValue(price);   // ✅ add price
                            driverRideRef.child("status").setValue("accepted");
                            driverRideRef.child("driverAcceptTime").setValue(currentTime);


                            Toast.makeText(getApplicationContext(),
                                    "Ride Accepted ✅ at " + currentTime,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadRideDetails() {
        ridesRef.child(rideId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(RideDetail_Activity.this, "Ride not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                pickupName = snapshot.child("pickupName").getValue(String.class);
                dropName = snapshot.child("Drop").getValue(String.class);
                price = snapshot.child("price").getValue(String.class);
                rideType = snapshot.child("rideType").getValue(String.class);
                status = snapshot.child("status").getValue(String.class);

                pickupLat = getDouble(snapshot, "pickupLat");
                pickupLng = getDouble(snapshot, "pickupLng");
                destLat = getDouble(snapshot, "destLat");
                destLng = getDouble(snapshot, "destLng");
                driverLat = getDouble(snapshot, "driverLat");
                driverLng = getDouble(snapshot, "driverLng");

                pickupText.setText("Pickup: " + (pickupName != null ? pickupName : "Unknown"));
                destText.setText("Drop: " + (dropName != null ? dropName : "Unknown"));
                fareText.setText("Fare: ₹" + (price != null ? price : "N/A"));
                statusText.setText("Status: " + (status != null ? status : "N/A"));

                // Distance
                if (driverLat != 0 && driverLng != 0 && pickupLat != 0 && pickupLng != 0) {
                    float distance = calculateDistance(driverLat, driverLng, pickupLat, pickupLng);
                    distanceText.setText("Driver distance: " + distance + " km");

                    if (distance <= 0.2 && "accepted".equalsIgnoreCase(status)) {
                        startRideBtn.setText("Start Ride");
                        startRideBtn.setVisibility(Button.VISIBLE);
                        cancelRideBtn.setVisibility(Button.VISIBLE);
                        startRippleAnimation(new LatLng(driverLat, driverLng));
                    } else if ("ongoing".equalsIgnoreCase(status)) {
                        startRideBtn.setText("End Ride");
                        startRideBtn.setVisibility(Button.VISIBLE);
                        cancelRideBtn.setVisibility(Button.GONE);
                        stopRippleAnimation();
                    } else {
                        startRideBtn.setVisibility(Button.GONE);
                        cancelRideBtn.setVisibility(Button.GONE);
                        stopRippleAnimation();
                    }
                }

                if (mMap != null) showMarkersOnMap();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RideDetail_Activity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double getDouble(DataSnapshot snapshot, String key) {
        Double val = snapshot.child(key).getValue(Double.class);
        return val != null ? val : 0;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        showMarkersOnMap();
    }

    private void showMarkersOnMap() {
        if (mMap == null) return;
        mMap.clear();

        LatLng pickup = new LatLng(pickupLat, pickupLng);
        LatLng dest = new LatLng(destLat, destLng);
        LatLng driverLoc = new LatLng(driverLat, driverLng);

        if (pickupLat != 0) mMap.addMarker(new MarkerOptions().position(pickup).title("Pickup: " + pickupName));
        if (destLat != 0) mMap.addMarker(new MarkerOptions().position(dest).title("Destination: " + dropName));
        if (driverLat != 0) mMap.addMarker(new MarkerOptions().position(driverLoc).title("Driver (You)"));

        if (driverLat != 0 && pickupLat != 0) {
            mMap.addPolyline(new PolylineOptions().add(driverLoc, pickup).color(Color.GREEN).width(8f));
        }
        if (pickupLat != 0 && destLat != 0) {
            mMap.addPolyline(new PolylineOptions().add(pickup, dest).color(Color.BLUE).width(8f));
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        if (pickupLat != 0) builder.include(pickup);
        if (destLat != 0) builder.include(dest);
        if (driverLat != 0) builder.include(driverLoc);

        if (pickupLat != 0 || destLat != 0 || driverLat != 0) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        }
    }

    private float calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000f;
    }

    private void startRippleAnimation(LatLng driverLocation) {
        stopRippleAnimation();
        rippleCircle = mMap.addCircle(new CircleOptions()
                .center(driverLocation)
                .radius(0)
                .strokeWidth(4f)
                .strokeColor(Color.parseColor("#4CAF50"))
                .fillColor(Color.parseColor("#334CAF50")));

        rippleAnimator = ValueAnimator.ofFloat(0, 100);
        rippleAnimator.setDuration(1500);
        rippleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rippleAnimator.setRepeatMode(ValueAnimator.RESTART);

        rippleAnimator.addUpdateListener(animation -> {
            float radius = (float) animation.getAnimatedValue();
            if (rippleCircle != null) {
                rippleCircle.setRadius(radius);
                float alpha = 1f - (radius / 100f);
                rippleCircle.setFillColor(Color.argb((int) (alpha * 80), 76, 175, 80));
            }
        });
        rippleAnimator.start();
    }

    private void stopRippleAnimation() {
        if (rippleAnimator != null) {
            rippleAnimator.cancel();
            rippleAnimator = null;
        }
        if (rippleCircle != null) {
            rippleCircle.remove();
            rippleCircle = null;
        }
    }

    private void showPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter 4-digit Ride PIN");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        builder.setView(input);

        builder.setPositiveButton("Start", (dialog, which) -> {
            String enteredPin = input.getText().toString().trim();
            if (enteredPin.isEmpty()) {
                Toast.makeText(this, "Please enter PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(RideDetail_Activity.this, "No customer linked to this ride", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String customerId = snapshot.getValue(String.class);
                    DatabaseReference customersRef = FirebaseDatabase.getInstance().getReference("Customers");
                    customersRef.child(customerId).child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String actualPin = snapshot.getValue(String.class);
                            if (actualPin != null && actualPin.equals(enteredPin)) {
                                updateRideStatusForBoth("ongoing");
                                Toast.makeText(RideDetail_Activity.this, "Ride Started 🚖", Toast.LENGTH_SHORT).show();
                                startRideBtn.setText("End Ride");
                                stopRippleAnimation();
                            } else {
                                Toast.makeText(RideDetail_Activity.this, "Invalid PIN ❌", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }

                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void endRide() {
        updateRideStatusForBoth("completed");
        Toast.makeText(RideDetail_Activity.this, "Ride Completed ✅", Toast.LENGTH_LONG).show();
        startRideBtn.setVisibility(Button.GONE);
        stopRippleAnimation();
    }

    private void confirmCancelRide() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage("Are you sure you want to cancel this ride?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String customerId = snapshot.getValue(String.class);
                                cancelRideByDriver(rideId, customerId);
                                Toast.makeText(RideDetail_Activity.this,"Ride Cancelled ❌", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(RideDetail_Activity.this,"No customer linked to this ride", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelRideByDriver(String rideId, String customerId) {
        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("Rides").child(rideId);
        DatabaseReference customerRideRef = FirebaseDatabase.getInstance().getReference("Customers")
                .child(customerId).child("rides").child(rideId);
        DatabaseReference driverRideRef = FirebaseDatabase.getInstance().getReference("drivers")
                .child(driverId).child("rides").child(rideId);

        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        rideRef.child("status").setValue("cancelled_by_driver");
        rideRef.child("cancelledTime").setValue(currentTime);
        customerRideRef.child("status").setValue("cancelled_by_driver");
        customerRideRef.child("cancelledTime").setValue(currentTime);
        driverRideRef.child("status").setValue("cancelled_by_driver");
        driverRideRef.child("cancelledTime").setValue(currentTime);

        FirebaseDatabase.getInstance().getReference("drivers")
                .child(driverId).child("status").setValue("available");
    }

    // ✅ unified status values for all three nodes (Rides, Customers, Drivers)
    private void updateRideStatusForBoth(String status) {
        ridesRef.child(rideId).child("status").setValue(status);

        ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String customerId = snapshot.getValue(String.class);
                    DatabaseReference customerRideRef = FirebaseDatabase.getInstance()
                            .getReference("Customers")
                            .child(customerId)
                            .child("rides")
                            .child(rideId);

                    DatabaseReference driverRideRef = FirebaseDatabase.getInstance()
                            .getReference("drivers")
                            .child(driverId)
                            .child("rides")
                            .child(rideId);

                    customerRideRef.child("status").setValue(status);
                    driverRideRef.child("status").setValue(status);

                    String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                    if (status.equals("ongoing")) {
                        ridesRef.child(rideId).child("startTime").setValue(currentTime);
                        customerRideRef.child("startTime").setValue(currentTime);
                        driverRideRef.child("startTime").setValue(currentTime);
                    } else if (status.equals("completed")) {
                        ridesRef.child(rideId).child("endTime").setValue(currentTime);
                        customerRideRef.child("endTime").setValue(currentTime);
                        driverRideRef.child("endTime").setValue(currentTime);
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
