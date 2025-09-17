package com.example.driver;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserRequest_Fragment extends Fragment {

    private Button btnAccept, btnReject;
    private TextView tvRideType, tvPickup, tvDestination, tvPrice, tvDistance;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private DatabaseReference ridesRef, driversRef;
    private String currentDriverId;
    private String currentRideId = null;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private MapView osmMap;

    // Ride info
    private Double pickupLat, pickupLng, destLat, destLng;
    private String pickupName, dropAddress, rideType, price;

    private OkHttpClient httpClient = new OkHttpClient();

    // Map markers and routes
    private Marker driverMarker, pickupMarker, destMarker;
    private Polyline currentRoute, pickupRoute;

    private float distanceInKm;

    private static final long MARKER_ANIM_DURATION = 1000; // ms

    public UserRequest_Fragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_request_, container, false);

        // Firebase
        auth = FirebaseAuth.getInstance();
        FirebaseUser driver = auth.getCurrentUser();
        if (driver == null) {
            startActivity(new Intent(getActivity(), Login_Activity.class));
            requireActivity().finish();
            return view;
        }
        currentDriverId = driver.getUid();
        ridesRef = FirebaseDatabase.getInstance().getReference("Rides");
        driversRef = FirebaseDatabase.getInstance().getReference("drivers").child(currentDriverId);

        // Bind UI
        btnAccept = view.findViewById(R.id.btnAccept);
        btnReject = view.findViewById(R.id.btnReject);
        tvRideType = view.findViewById(R.id.tvRideType);
        tvPickup = view.findViewById(R.id.tvPickup);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvDistance = view.findViewById(R.id.tvDistance);
        progressBar = view.findViewById(R.id.progressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // OSM Map setup
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        osmMap = view.findViewById(R.id.osmMap);
        osmMap.setTileSource(TileSourceFactory.MAPNIK);
        osmMap.setMultiTouchControls(true);
        osmMap.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        startLocationUpdates();
        listenForRides();
        setupButtons();

        return view;
    }

    /** LISTEN FOR WAITING RIDES **/
    private void listenForRides() {
        ridesRef.orderByChild("status").equalTo("waiting")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            clearRideUI();
                            return;
                        }

                        for (DataSnapshot rideSnap : snapshot.getChildren()) {
                            currentRideId = rideSnap.getKey();
                            rideType = rideSnap.child("rideType").getValue(String.class);
                            pickupLat = rideSnap.child("pickupLat").getValue(Double.class);
                            pickupLng = rideSnap.child("pickupLng").getValue(Double.class);
                            destLat = rideSnap.child("destLat").getValue(Double.class);
                            destLng = rideSnap.child("destLng").getValue(Double.class);
                            price = rideSnap.child("price").getValue(String.class);
                            pickupName = rideSnap.child("pickupName").getValue(String.class);
                            dropAddress = rideSnap.child("dropAddress").getValue(String.class);

                            updateRideUI();
                            break; // only first waiting ride
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** SETUP BUTTONS **/
    private void setupButtons() {
        btnAccept.setOnClickListener(v -> acceptRide());
        btnReject.setOnClickListener(v -> rejectRide());
    }

    /** ACCEPT RIDE **/
    /** ACCEPT RIDE SAFELY **/
    private void acceptRide() {
        if (currentRideId == null) {
            Toast.makeText(getContext(), "No ride to accept", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAccept.setEnabled(false);

        ridesRef.child(currentRideId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "Ride was cancelled by customer ❌", Toast.LENGTH_SHORT).show();
                    clearRideUI();
                    goBackToDashboard();
                    return;
                }

                String status = snapshot.child("status").getValue(String.class);
                String customerId = snapshot.child("customerId").getValue(String.class);

                if (status == null || status.toLowerCase().contains("cancelled") || customerId == null) {
                    Toast.makeText(getContext(), "Ride cannot be accepted ❌", Toast.LENGTH_SHORT).show();
                    clearRideUI();
                    goBackToDashboard();
                    return;
                }

                // Get customer info
                DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("Customers").child(customerId);
                customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot custSnap) {
                        String custName = custSnap.child("name").getValue(String.class);
                        String custPhone = custSnap.child("phone").getValue(String.class);
                        String bookingDate = snapshot.child("bookingDate").getValue(String.class);
                        String bookingTime = snapshot.child("bookingTime").getValue(String.class);

                        // 1️⃣ Update ride status in global "Rides"
                        DatabaseReference rideRef = ridesRef.child(currentRideId);
                        rideRef.child("status").setValue("accepted");
                        rideRef.child("driverId").setValue(currentDriverId);
                        rideRef.child("driverName").setValue("Anand Sagar"); // or fetch dynamically
                        rideRef.child("pickupName").setValue(pickupName != null ? pickupName : (pickupLat + ", " + pickupLng));
                        rideRef.child("dropAddress").setValue(dropAddress != null ? dropAddress : (destLat + ", " + destLng));

                        // 2️⃣ Update driver's ride entry
                        DatabaseReference driverRideRef = driversRef.child("rides").child(currentRideId);
                        driverRideRef.child("rideId").setValue(currentRideId);
                        driverRideRef.child("customerId").setValue(customerId);
                        driverRideRef.child("riderName").setValue(custName != null ? custName : "N/A"); // use riderName
                        driverRideRef.child("riderPhone").setValue(custPhone != null ? custPhone : "N/A");
                        driverRideRef.child("driverAcceptTime").setValue(getCurrentTime());
                        driverRideRef.child("pickupName").setValue(pickupName != null ? pickupName : (pickupLat + ", " + pickupLng));
                        driverRideRef.child("dropAddress").setValue(dropAddress != null ? dropAddress : (destLat + ", " + destLng));
                        driverRideRef.child("bookingDate").setValue(bookingDate != null ? bookingDate : getCurrentDate());
                        driverRideRef.child("bookingTime").setValue(bookingTime != null ? bookingTime : getCurrentTime());
                        driverRideRef.child("price").setValue(price != null ? price : "N/A");
                        driverRideRef.child("status").setValue("accepted");

                        // 3️⃣ Update driver status
                        driversRef.child("status").setValue("onRide");

                        // 4️⃣ Update customer's ride entry
                        DatabaseReference custRideRef = customerRef.child("rides").child(currentRideId);
                        custRideRef.child("status").setValue("accepted");
                        custRideRef.child("pickupName").setValue(pickupName != null ? pickupName : (pickupLat + ", " + pickupLng));
                        custRideRef.child("dropAddress").setValue(dropAddress != null ? dropAddress : (destLat + ", " + destLng));
                        custRideRef.child("driverId").setValue(currentDriverId);
                        custRideRef.child("driverName").setValue("Anand Sagar"); // Or fetch dynamically
                        custRideRef.child("driverAcceptTime").setValue(getCurrentTime());
                        custRideRef.child("riderName").setValue(custName != null ? custName : "N/A");
                        custRideRef.child("riderPhone").setValue(custPhone != null ? custPhone : "N/A");
                        custRideRef.child("bookingDate").setValue(bookingDate != null ? bookingDate : getCurrentDate());
                        custRideRef.child("bookingTime").setValue(bookingTime != null ? bookingTime : getCurrentTime());

                        // 5️⃣ Open RideDetail Activity
                        if (getActivity() != null && isAdded()) {
                            Intent intent = new Intent(getActivity(), RideDetail_Activity.class);
                            intent.putExtra("rideId", currentRideId);
                            startActivity(intent);
                            requireActivity().finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /** Helper: Get current date **/
    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy");
        return sdf.format(new java.util.Date());
    }

    /** Helper: Get current time **/
    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a");
        return sdf.format(new java.util.Date());
    }







    /** REJECT RIDE **/
    private void rejectRide() {
        if (currentRideId == null) return;

        // Update ride rejected by driver
        ridesRef.child(currentRideId).child("rejectedDrivers").child(currentDriverId).setValue(true);
        ridesRef.child(currentRideId).child("status").setValue("cancelled");
        driversRef.child("rides").child(currentRideId).child("status").setValue("cancelled");

        ridesRef.child(currentRideId).child("customerId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String customerId = snapshot.getValue(String.class);
                if (customerId != null) {
                    FirebaseDatabase.getInstance().getReference("Customers")
                            .child(customerId)
                            .child("rides")
                            .child(currentRideId)
                            .child("status")
                            .setValue("cancelled");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        clearRideUI();
        Toast.makeText(getContext(), "Ride Rejected ❌", Toast.LENGTH_SHORT).show();
    }

    /** CLEAR UI AND RESET STATE **/
    private void clearRideUI() {
        tvRideType.setText("No Ride Requests");
        tvPickup.setText("");
        tvDestination.setText("");
        tvPrice.setText("");
        tvDistance.setText("");

        currentRideId = null;
        pickupLat = pickupLng = destLat = destLng = null;
        pickupMarker = destMarker = driverMarker = null;
        currentRoute = pickupRoute = null;

        if (osmMap != null) {
            osmMap.getOverlays().clear();
            osmMap.invalidate();
        }

        if (driversRef != null) {
            driversRef.child("status").setValue("available");
        }
    }

    /** UPDATE RIDE INFO UI **/
    private void updateRideUI() {
        setStyledText(tvRideType, "Ride Type:", rideType);
        setStyledText(tvPickup, "Pickup:", pickupName != null ? pickupName : (pickupLat + ", " + pickupLng));
        setStyledText(tvDestination, "Destination:", dropAddress != null ? dropAddress : (destLat + ", " + destLng));
        setStyledText(tvPrice, "Price:", price);
    }

    /** LOCATION UPDATES **/
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location == null) continue;

                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    float bearing = location.getBearing();

                    // Update Firebase
                    driversRef.child("currentLat").setValue(lat);
                    driversRef.child("currentLng").setValue(lng);

                    if (currentRideId != null) {
                        ridesRef.child(currentRideId).child("driverLat").setValue(lat);
                        ridesRef.child(currentRideId).child("driverLng").setValue(lng);
                    }

                    updateDistance(lat, lng);
                    driverLocUpdate(new GeoPoint(lat, lng), bearing);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void updateDistance(double driverLat, double driverLng) {
        if (pickupLat != null && pickupLng != null) {
            float[] results = new float[1];
            Location.distanceBetween(driverLat, driverLng, pickupLat, pickupLng, results);
            distanceInKm = results[0] / 1000f;
            tvDistance.setText("Distance to Pickup: " + String.format("%.2f", distanceInKm) + " km");
        }
    }

    /** DRIVER MARKER & ROUTES **/
    private void driverLocUpdate(@Nullable GeoPoint driverLoc, float bearing) {
        if (osmMap == null) return;

        GeoPoint pickup = pickupLat != null && pickupLng != null ? new GeoPoint(pickupLat, pickupLng) : null;
        GeoPoint dest = destLat != null && destLng != null ? new GeoPoint(destLat, destLng) : null;

        // Add pickup marker
        if (pickup != null && pickupMarker == null) {
            pickupMarker = createMarker(pickup, "Pickup: " + (pickupName != null ? pickupName : ""), R.drawable.pickup_location_icon);
            osmMap.getOverlays().add(pickupMarker);
        }

        // Add destination marker
        if (dest != null && destMarker == null) {
            destMarker = createMarker(dest, "Destination: " + (dropAddress != null ? dropAddress : ""), R.drawable.drop_location_icon);
            osmMap.getOverlays().add(destMarker);
        }

        // Driver marker
        if (driverLoc != null) {
            if (driverMarker == null) {
                driverMarker = createMarker(driverLoc, "Driver (You)", R.drawable.driver_location_icon);
                driverMarker.setRotation(bearing);
                osmMap.getOverlays().add(driverMarker);
                osmMap.getController().setZoom(15);
                osmMap.getController().setCenter(driverLoc);
            } else {
                animateMarkerTo(driverMarker, driverLoc, bearing, MARKER_ANIM_DURATION);
            }
        } else if (pickup != null) {
            osmMap.getController().setZoom(15);
            osmMap.getController().setCenter(pickup);
        }

        // Routes
        if (pickup != null && dest != null && currentRoute == null) fetchRoute(pickup, dest, Color.RED, true);
        if (pickup != null && driverLoc != null) fetchRoute(driverLoc, pickup, Color.BLUE, false);

        osmMap.invalidate();
    }

    private Marker createMarker(GeoPoint point, String title, int iconRes) {
        Marker marker = new Marker(osmMap);
        marker.setPosition(point);
        marker.setTitle(title);
        if (getContext() != null)
            marker.setIcon(ContextCompat.getDrawable(requireContext(), iconRes));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        return marker;
    }

    private void fetchRoute(GeoPoint start, GeoPoint end, int color, boolean isCurrentRoute) {
        progressBar.setVisibility(View.VISIBLE);
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + start.getLongitude() + "," + start.getLatitude() + ";"
                + end.getLongitude() + "," + end.getLatitude()
                + "?overview=full&geometries=geojson";

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Route fetch failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;

                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    JSONArray coords = json.getJSONArray("routes")
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONArray("coordinates");

                    Polyline routeLine = new Polyline();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray point = coords.getJSONArray(i);
                        routeLine.addPoint(new GeoPoint(point.getDouble(1), point.getDouble(0)));
                    }
                    routeLine.setColor(color);
                    routeLine.setWidth(color == Color.RED ? 8f : 6f);

                    requireActivity().runOnUiThread(() -> {
                        if (isCurrentRoute && currentRoute != null) osmMap.getOverlays().remove(currentRoute);
                        if (!isCurrentRoute && pickupRoute != null) osmMap.getOverlays().remove(pickupRoute);

                        if (isCurrentRoute) currentRoute = routeLine;
                        else pickupRoute = routeLine;

                        osmMap.getOverlays().add(routeLine);
                        osmMap.invalidate();
                        progressBar.setVisibility(View.GONE);
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    /** ANIMATE DRIVER MARKER **/
    private void animateMarkerTo(Marker marker, GeoPoint toPosition, float toRotation, long duration) {
        GeoPoint startPosition = marker.getPosition();
        float startRotation = marker.getRotation();

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float t = animation.getAnimatedFraction();
            double lat = startPosition.getLatitude() + (toPosition.getLatitude() - startPosition.getLatitude()) * t;
            double lon = startPosition.getLongitude() + (toPosition.getLongitude() - startPosition.getLongitude()) * t;
            marker.setPosition(new GeoPoint(lat, lon));

            float rot = interpolateRotation(startRotation, toRotation, t);
            marker.setRotation(rot);
            osmMap.getController().setCenter(marker.getPosition());
            osmMap.invalidate();
        });
        animator.start();
    }

    private float interpolateRotation(float start, float end, float fraction) {
        float diff = ((end - start + 540) % 360) - 180;
        return (start + diff * fraction + 360) % 360;
    }

    private void setStyledText(TextView tv, String label, String value) {
        if (value == null) value = "N/A";
        SpannableString ss = new SpannableString(label + " " + value);
        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(ss);
    }

    private void goBackToDashboard() {
        Intent intent = new Intent(getActivity(), DashBoard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
