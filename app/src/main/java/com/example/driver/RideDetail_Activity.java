package com.example.driver;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RideDetail_Activity extends AppCompatActivity {

    private MapView osmMap;
    private TextView statusText, pickupText, destText, fareText, distanceText;
    private Button startRideBtn, cancelRideBtn;

    private DatabaseReference ridesRef, driversRef;
    private String rideId, driverId;
    private double pickupLat, pickupLng, destLat, destLng, driverLat, driverLng;
    private String pickupName, dropName, price, rideType, status;
    private View dimOverlay;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_ride_detail);

        // UI
        osmMap = findViewById(R.id.osmMap);
        statusText = findViewById(R.id.statusText);
        pickupText = findViewById(R.id.pickupText);
        destText = findViewById(R.id.destText);
        fareText = findViewById(R.id.fareText);
        distanceText = findViewById(R.id.distanceText);
        startRideBtn = findViewById(R.id.startRideBtn);
        cancelRideBtn = findViewById(R.id.cancelRideBtn);
        dimOverlay = findViewById(R.id.dimOverlay);
        progressBar = findViewById(R.id.progressBar);
        startRideBtn.setVisibility(Button.GONE);
        cancelRideBtn.setVisibility(Button.GONE);

        osmMap.setTileSource(TileSourceFactory.MAPNIK);
        osmMap.setMultiTouchControls(true);

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

        // Load ride details and auto-assign driver if not assigned
        loadRideDetails();

        // Button actions
        startRideBtn.setOnClickListener(v -> {
            if ("accepted".equalsIgnoreCase(status)) {

                showPinDialog();
            } else if ("ongoing".equalsIgnoreCase(status)) {
                endRide();
            }
        });

        cancelRideBtn.setOnClickListener(v -> confirmCancelRide());
    }

    private void loadRideDetails() {
        ridesRef.child(rideId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) { finish(); return; }

                pickupName = snapshot.child("pickupName").getValue(String.class);
                dropName = snapshot.child("dropName").getValue(String.class);

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

                // ✅ Auto-assign driver if not assigned yet
                String assignedDriver = snapshot.child("driverId").getValue(String.class);
                if (assignedDriver == null || assignedDriver.isEmpty()) {
                    assignDriverToRide();
                }

                // Update buttons & map
                updateButtons();
                showMarkersAndRoute();

                // Automatically handle ongoing ride
                if ("ongoing".equalsIgnoreCase(status)) {
                    startRideBtn.setText("End Ride");
                    startRideBtn.setVisibility(Button.VISIBLE);
                    cancelRideBtn.setVisibility(Button.GONE);
                } else if ("accepted".equalsIgnoreCase(status)) {
                    float distance = calculateDistance(driverLat, driverLng, pickupLat, pickupLng);
                    if (distance <= 0.2) {
                        startRideBtn.setText("Start Ride");
                        startRideBtn.setVisibility(Button.VISIBLE);
                        cancelRideBtn.setVisibility(Button.VISIBLE);
                    }
                } else if ("completed".equalsIgnoreCase(status) ||
                        "cancelled_by_driver".equalsIgnoreCase(status) ||
                        "cancelled_by_customer".equalsIgnoreCase(status)) {
                    redirectToDashboard();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void assignDriverToRide() {
        ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String customerId = snapshot.getValue(String.class);

                driversRef.child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot driverSnap) {
                        if (!driverSnap.exists()) return;

                        String firstName = driverSnap.child("firstName").getValue(String.class);
                        String lastName = driverSnap.child("lastName").getValue(String.class);
                        String driverName = (firstName != null ? firstName : "") + " " +
                                (lastName != null ? lastName : "");
                        String vehicle = driverSnap.child("vehicle").getValue(String.class);
                        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

                        // Update ride
                        ridesRef.child(rideId).child("driverId").setValue(driverId);
                        ridesRef.child(rideId).child("driverName").setValue(driverName.trim());
                        if (vehicle != null) ridesRef.child(rideId).child("vehicle").setValue(vehicle);
                        ridesRef.child(rideId).child("status").setValue("accepted");
                        ridesRef.child(rideId).child("driverAcceptTime").setValue(currentTime);

                        // Update customer node
                        DatabaseReference customerRideRef = FirebaseDatabase.getInstance()
                                .getReference("Customers").child(customerId).child("rides").child(rideId);
                        customerRideRef.child("driverId").setValue(driverId);
                        customerRideRef.child("driverName").setValue(driverName.trim());
                        if (vehicle != null) customerRideRef.child("vehicle").setValue(vehicle);
                        customerRideRef.child("status").setValue("accepted");
                        customerRideRef.child("driverAcceptTime").setValue(currentTime);

                        // Update driver node
                        DatabaseReference driverRideRef = FirebaseDatabase.getInstance()
                                .getReference("drivers").child(driverId).child("rides").child(rideId);
                        driverRideRef.child("rideId").setValue(rideId);
                        driverRideRef.child("customerId").setValue(customerId);
                        driverRideRef.child("pickupName").setValue(pickupName);
                        driverRideRef.child("Drop").setValue(dropName);
                        driverRideRef.child("price").setValue(price);
                        driverRideRef.child("status").setValue("accepted");
                        driverRideRef.child("driverAcceptTime").setValue(currentTime);

                        Toast.makeText(RideDetail_Activity.this,
                                "Ride Accepted ✅ at " + currentTime, Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private double getDouble(DataSnapshot snapshot, String key) {
        Double val = snapshot.child(key).getValue(Double.class);
        return val != null ? val : 0;
    }

    private void updateButtons() {
        float distance = calculateDistance(driverLat, driverLng, pickupLat, pickupLng);
        distanceText.setText("Driver distance: " + String.format("%.2f", distance) + " km");

        if ("accepted".equalsIgnoreCase(status) && distance <= 0.2) {
            startRideBtn.setText("Start Ride");
            startRideBtn.setVisibility(Button.VISIBLE);
            cancelRideBtn.setVisibility(Button.VISIBLE);
        } else if ("ongoing".equalsIgnoreCase(status)) {
            startRideBtn.setText("End Ride");
            startRideBtn.setVisibility(Button.VISIBLE);
            cancelRideBtn.setVisibility(Button.GONE);
        } else {
            startRideBtn.setVisibility(Button.GONE);
            cancelRideBtn.setVisibility(Button.GONE);
        }
    }

    private float calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000f;
    }

    private void showMarkersAndRoute() {
        if (osmMap == null) return;
        osmMap.getOverlays().clear();

        GeoPoint pickup = new GeoPoint(pickupLat, pickupLng);
        GeoPoint dest = new GeoPoint(destLat, destLng);
        GeoPoint driver = new GeoPoint(driverLat, driverLng);

        if (pickupLat != 0) addMarker(pickup, "Pickup: " + pickupName);
        if (destLat != 0) addMarker(dest, "Destination: " + dropName);
        if (driverLat != 0) addMarker(driver, "Driver (You)");

        // Route
        if ("accepted".equalsIgnoreCase(status)) fetchRoute(driver, pickup);
        else if ("ongoing".equalsIgnoreCase(status)) fetchRoute(pickup, dest);

        osmMap.invalidate();
    }

    private void addMarker(GeoPoint point, String title) {
        Marker m = new Marker(osmMap);
        m.setPosition(point);
        m.setTitle(title);
        osmMap.getOverlays().add(m);
    }

    private void fetchRoute(GeoPoint start, GeoPoint end) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + start.getLongitude() + "," + start.getLatitude() + ";"
                + end.getLongitude() + "," + end.getLatitude()
                + "?overview=full&geometries=geojson";
        showProgress();
        new FetchRouteTask().execute(url);
    }

    private class FetchRouteTask extends AsyncTask<String, Void, ArrayList<GeoPoint>> {
        @Override
        protected ArrayList<GeoPoint> doInBackground(String... urls) {
            ArrayList<GeoPoint> routePoints = new ArrayList<>();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);

                JSONObject obj = new JSONObject(json.toString());
                JSONArray coords = obj.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                for (int i = 0; i < coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    double lon = c.getDouble(0);
                    double lat = c.getDouble(1);
                    routePoints.add(new GeoPoint(lat, lon));
                }
            } catch (Exception e) { e.printStackTrace(); }
            return routePoints;
        }

        @Override
        protected void onPostExecute(ArrayList<GeoPoint> routePoints) {
            hideProgress();
            if (!routePoints.isEmpty()) {
                Polyline line = new Polyline();
                line.setPoints(routePoints);
                line.setColor(Color.BLUE);
                line.setWidth(8f);
                osmMap.getOverlays().add(line);

                BoundingBox box = BoundingBox.fromGeoPoints(routePoints);
                osmMap.zoomToBoundingBox(box, true, 80);
                osmMap.invalidate();
            }
        }
    }

    private void showPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Ride PIN");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        builder.setView(input);

        builder.setPositiveButton("Start Ride", (dialog, which) -> {
            String enteredPin = input.getText().toString().trim();

            ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) return;
                    String customerId = snapshot.getValue(String.class);

                    FirebaseDatabase.getInstance().getReference("Customers")
                            .child(customerId)
                            .child("pin")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot pinSnap) {
                                    String actualPin = pinSnap.getValue(String.class);
                                    if (actualPin != null && actualPin.equals(enteredPin)) {
                                        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                                        ridesRef.child(rideId).child("status").setValue("ongoing");
                                        ridesRef.child(rideId).child("rideStartTime").setValue(currentTime);
                                        FirebaseDatabase.getInstance().getReference("drivers")
                                                .child(driverId).child("rides").child(rideId).child("status").setValue("ongoing");
                                        FirebaseDatabase.getInstance().getReference("Customers")
                                                .child(customerId).child("rides").child(rideId).child("status").setValue("ongoing");

                                        Toast.makeText(RideDetail_Activity.this, "✅ Ride Started", Toast.LENGTH_SHORT).show();
                                        startRideBtn.setText("End Ride");
                                        cancelRideBtn.setVisibility(Button.GONE);
                                    } else {
                                        Toast.makeText(RideDetail_Activity.this, "❌ Wrong PIN", Toast.LENGTH_SHORT).show();
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
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        ridesRef.child(rideId).child("status").setValue("completed");
        ridesRef.child(rideId).child("rideEndTime").setValue(currentTime);

        FirebaseDatabase.getInstance().getReference("drivers")
                .child(driverId).child("rides").child(rideId).child("status").setValue("completed");

        ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String customerId = snapshot.getValue(String.class);
                    FirebaseDatabase.getInstance().getReference("Customers")
                            .child(customerId).child("rides").child(rideId).child("status").setValue("completed");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        Toast.makeText(this, "✅ Ride Completed", Toast.LENGTH_SHORT).show();
        redirectToDashboard();
    }

    private void confirmCancelRide() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel Ride");
        builder.setMessage("Are you sure you want to cancel this ride?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            // Update ride status
            ridesRef.child(rideId).child("status").setValue("cancelled_by_driver");
            ridesRef.child(rideId).child("rideEndTime").setValue(currentTime);

            // Update driver node
            FirebaseDatabase.getInstance().getReference("drivers")
                    .child(driverId).child("rides").child(rideId).child("status").setValue("cancelled_by_driver");

            // Update customer node
            ridesRef.child(rideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String customerId = snapshot.getValue(String.class);
                        FirebaseDatabase.getInstance().getReference("Customers")
                                .child(customerId).child("rides").child(rideId).child("status")
                                .setValue("cancelled_by_driver");
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            Toast.makeText(RideDetail_Activity.this, "❌ Ride Cancelled", Toast.LENGTH_SHORT).show();
            redirectToDashboard();
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void redirectToDashboard() {
        startActivity(new Intent(this, DashBoard.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if ("completed".equalsIgnoreCase(status) ||
                "cancelled_by_driver".equalsIgnoreCase(status) ||
                "cancelled_by_customer".equalsIgnoreCase(status)) {
            redirectToDashboard();
        } else {
            super.onBackPressed();
        }
    }
    private void showProgress() {
        if (dimOverlay != null) dimOverlay.setVisibility(View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        if (dimOverlay != null) dimOverlay.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }


}
