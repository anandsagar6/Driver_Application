package com.example.driver;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
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

    private static final long MARKER_ANIM_DURATION = 1000; // ms
    private static final double AVG_SPEED_KMPH = 25.0;     // for driver → pickup ETA

    // Distance / ETA strings for UI
    private String pickupDistanceStr = null;  // "2.35 km"
    private String pickupEtaStr = null;       // "6 min"
    private String tripDistanceStr = null;    // "225.10 km"
    private String tripEtaStr = null;         // "5 hr 10 min"

    // Throttle driver->pickup route drawing
    private long lastPickupRouteRequestTime = 0L;

    public UserRequest_Fragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
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
        driversRef = FirebaseDatabase.getInstance()
                .getReference("drivers")
                .child(currentDriverId);

        // Bind UI
        btnAccept = view.findViewById(R.id.btnAccept);
        btnReject = view.findViewById(R.id.btnReject);
        tvRideType = view.findViewById(R.id.tvRideType);
        tvPickup = view.findViewById(R.id.tvPickup);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvDistance = view.findViewById(R.id.tvDistance);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // OSM Map setup
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        osmMap = view.findViewById(R.id.osmMap);
        osmMap.setTileSource(TileSourceFactory.MAPNIK);
        osmMap.setMultiTouchControls(true);
        osmMap.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
        );

        setupButtons();
        startLocationUpdates();
        listenForRides();

        // Initially: no ride
        clearRideUI();

        return view;
    }

    // -------------------------------------------------------------------------
    // LISTEN FOR WAITING RIDES
    // -------------------------------------------------------------------------
    private void listenForRides() {
        ridesRef.orderByChild("status").equalTo("waiting")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            // No waiting rides at all
                            if (currentRideId != null) {
                                clearRideUI();
                            }
                            return;
                        }

                        boolean foundValidRide = false;

                        for (DataSnapshot rideSnap : snapshot.getChildren()) {

                            String rideId = rideSnap.getKey();
                            if (rideId == null) continue;

                            String status = rideSnap.child("status").getValue(String.class);
                            if (status == null || !status.equals("waiting")) continue;
                            if ("cancelled".equalsIgnoreCase(status)) continue;

                            // If we are already showing this same ride, don't reset UI
                            if (currentRideId != null && currentRideId.equals(rideId)) {
                                foundValidRide = true;
                                break;
                            }

                            // New ride for driver
                            currentRideId = rideId;

                            rideType = rideSnap.child("rideType").getValue(String.class);
                            pickupLat = rideSnap.child("pickupLat").getValue(Double.class);
                            pickupLng = rideSnap.child("pickupLng").getValue(Double.class);
                            destLat = rideSnap.child("destLat").getValue(Double.class);
                            destLng = rideSnap.child("destLng").getValue(Double.class);
                            price = rideSnap.child("price").getValue(String.class);
                            pickupName = rideSnap.child("pickupName").getValue(String.class);
                            dropAddress = rideSnap.child("dropAddress").getValue(String.class);

                            if (pickupLat == null || pickupLng == null ||
                                    destLat == null || destLng == null) {
                                // malformed ride, skip
                                currentRideId = null;
                                continue;
                            }

                            // Reset state only for NEW ride
                            pickupDistanceStr = null;
                            pickupEtaStr = null;
                            tripDistanceStr = null;
                            tripEtaStr = null;
                            lastPickupRouteRequestTime = 0L;

                            updateRideUI();
                            foundValidRide = true;
                            break; // show only one ride
                        }

                        // We had a ride but now it's no longer in waiting list
                        if (!foundValidRide && currentRideId != null) {
                            clearRideUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(),
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // -------------------------------------------------------------------------
    // BUTTONS
    // -------------------------------------------------------------------------
    private void setupButtons() {
        btnAccept.setOnClickListener(v -> acceptRide());
        btnReject.setOnClickListener(v -> rejectRide());
    }

    // -------------------------------------------------------------------------
    // ACCEPT RIDE
    // -------------------------------------------------------------------------
    private void acceptRide() {
        if (currentRideId == null) {
            Toast.makeText(getContext(), "No ride to accept", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAccept.setEnabled(false);

        ridesRef.child(currentRideId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(getContext(),
                                    "Ride was cancelled by customer ❌",
                                    Toast.LENGTH_SHORT).show();
                            clearRideUI();
                            goBackToDashboard();
                            return;
                        }

                        String status = snapshot.child("status").getValue(String.class);
                        String customerId = snapshot.child("customerId").getValue(String.class);

                        if (status == null ||
                                status.toLowerCase().contains("cancelled") ||
                                customerId == null) {

                            Toast.makeText(getContext(),
                                    "Ride cannot be accepted ❌",
                                    Toast.LENGTH_SHORT).show();
                            clearRideUI();
                            goBackToDashboard();
                            return;
                        }

                        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                                .getReference("Customers")
                                .child(customerId);

                        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot custSnap) {

                                // Extra safety: if customer ride is already cancelled by system,
                                // don't allow driver to accept.
                                String custRideStatus = custSnap.child("rides")
                                        .child(currentRideId)
                                        .child("status")
                                        .getValue(String.class);

                                if ("cancelled_by_system".equalsIgnoreCase(custRideStatus)) {
                                    Toast.makeText(getContext(),
                                            "Ride expired (no response from driver) ❌",
                                            Toast.LENGTH_SHORT).show();
                                    clearRideUI();
                                    goBackToDashboard();
                                    return;
                                }

                                proceedAcceptRide(snapshot, custSnap, customerRef, customerId);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void proceedAcceptRide(DataSnapshot rideSnap,
                                   DataSnapshot custSnap,
                                   DatabaseReference customerRef,
                                   String customerId) {

        String custName = custSnap.child("name").getValue(String.class);
        String custPhone = custSnap.child("phone").getValue(String.class);
        String bookingDate = rideSnap.child("bookingDate").getValue(String.class);
        String bookingTime = rideSnap.child("bookingTime").getValue(String.class);

        // 1️⃣ Update global ride
        DatabaseReference rideRef = ridesRef.child(currentRideId);
        rideRef.child("status").setValue("accepted");
        rideRef.child("driverId").setValue(currentDriverId);
        rideRef.child("driverName").setValue("Anand Sagar"); // or dynamic later
        rideRef.child("pickupName").setValue(
                pickupName != null ? pickupName : (pickupLat + ", " + pickupLng));
        rideRef.child("dropAddress").setValue(
                dropAddress != null ? dropAddress : (destLat + ", " + destLng));

        // 2️⃣ Update driver's ride entry (only after ACCEPT)
        DatabaseReference driverRideRef = driversRef.child("rides").child(currentRideId);
        driverRideRef.child("rideId").setValue(currentRideId);
        driverRideRef.child("customerId").setValue(customerId);
        driverRideRef.child("riderName").setValue(custName != null ? custName : "N/A");
        driverRideRef.child("riderPhone").setValue(custPhone != null ? custPhone : "N/A");
        driverRideRef.child("driverAcceptTime").setValue(getCurrentTime());
        driverRideRef.child("pickupName").setValue(
                pickupName != null ? pickupName : (pickupLat + ", " + pickupLng));
        driverRideRef.child("dropAddress").setValue(
                dropAddress != null ? dropAddress : (destLat + ", " + destLng));
        driverRideRef.child("bookingDate").setValue(
                bookingDate != null ? bookingDate : getCurrentDate());
        driverRideRef.child("bookingTime").setValue(
                bookingTime != null ? bookingTime : getCurrentTime());
        driverRideRef.child("price").setValue(price != null ? price : "N/A");
        driverRideRef.child("status").setValue("accepted");

        // 👉 Save distances / ETA ONLY AFTER ACCEPT
        if (pickupDistanceStr != null)
            driverRideRef.child("driverToPickupDistance").setValue(pickupDistanceStr);
        if (pickupEtaStr != null)
            driverRideRef.child("driverToPickupETA").setValue(pickupEtaStr);
        if (tripDistanceStr != null)
            driverRideRef.child("tripDistance").setValue(tripDistanceStr);
        if (tripEtaStr != null)
            driverRideRef.child("tripETA").setValue(tripEtaStr);

        // 3️⃣ Driver status
        driversRef.child("status").setValue("onRide");

        // 4️⃣ Customer ride entry
        DatabaseReference custRideRef = customerRef.child("rides").child(currentRideId);
        custRideRef.child("status").setValue("accepted");
        custRideRef.child("pickupName").setValue(
                pickupName != null ? pickupName : (pickupLat + ", " + pickupLng));
        custRideRef.child("dropAddress").setValue(
                dropAddress != null ? dropAddress : (destLat + ", " + destLng));
        custRideRef.child("driverId").setValue(currentDriverId);
        custRideRef.child("driverName").setValue("Anand Sagar");
        custRideRef.child("driverAcceptTime").setValue(getCurrentTime());
        custRideRef.child("riderName").setValue(custName != null ? custName : "N/A");
        custRideRef.child("riderPhone").setValue(custPhone != null ? custPhone : "N/A");
        custRideRef.child("bookingDate").setValue(
                bookingDate != null ? bookingDate : getCurrentDate());
        custRideRef.child("bookingTime").setValue(
                bookingTime != null ? bookingTime : getCurrentTime());

        // 5️⃣ Open ride detail activity
        if (getActivity() != null && isAdded()) {
            Intent intent = new Intent(getActivity(), RideDetail_Activity.class);
            intent.putExtra("rideId", currentRideId);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            requireActivity().finish();
        }
    }

    // -------------------------------------------------------------------------
    // DATE / TIME HELPERS
    // -------------------------------------------------------------------------
    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd-MMM-yyyy");
        return sdf.format(new java.util.Date());
    }

    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("hh:mm a");
        return sdf.format(new java.util.Date());
    }

    // -------------------------------------------------------------------------
    // REJECT RIDE
    // -------------------------------------------------------------------------
    private void rejectRide() {
        if (currentRideId == null) {
            Toast.makeText(getContext(), "No ride to reject ❌", Toast.LENGTH_SHORT).show();
            return;
        }

        String rideId = currentRideId;
        currentRideId = null;

        driversRef.child("status").setValue("available");

        // Mark ride cancelled
        ridesRef.child(rideId).child("status").setValue("cancelled");
        ridesRef.child(rideId).child("rejectedDrivers")
                .child(currentDriverId)
                .setValue(true);

        // Clean driver node for that ride
        driversRef.child("rides").child(rideId).removeValue();

        // Update customer's record if exists
        ridesRef.child(rideId).child("customerId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String customerId = snapshot.getValue(String.class);
                        if (customerId != null) {
                            FirebaseDatabase.getInstance().getReference("Customers")
                                    .child(customerId)
                                    .child("rides")
                                    .child(rideId)
                                    .child("status")
                                    .setValue("cancelled");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });

        clearRideUI();
        Toast.makeText(getContext(), "Ride Rejected ❌", Toast.LENGTH_SHORT).show();
        // Listener is still active → next waiting ride will appear automatically
    }

    // -------------------------------------------------------------------------
    // CLEAR UI WHEN NO RIDE
    // -------------------------------------------------------------------------
    private void clearRideUI() {

        tvRideType.setText("Ride Not Available");

        tvPickup.setVisibility(View.GONE);
        tvDestination.setVisibility(View.GONE);
        tvPrice.setVisibility(View.GONE);
        tvDistance.setVisibility(View.GONE);

        btnAccept.setVisibility(View.GONE);
        btnReject.setVisibility(View.GONE);

        currentRideId = null;

        pickupLat = pickupLng = destLat = destLng = null;
        pickupName = dropAddress = rideType = price = null;

        pickupDistanceStr = null;
        pickupEtaStr = null;
        tripDistanceStr = null;
        tripEtaStr = null;
        lastPickupRouteRequestTime = 0L;

        pickupMarker = null;
        destMarker = null;
        currentRoute = null;
        pickupRoute = null;

        if (osmMap != null) {
            osmMap.getOverlays().clear();
            osmMap.invalidate();
        }

        driversRef.child("status").setValue("available");
    }

    // -------------------------------------------------------------------------
    // UPDATE UI WHEN RIDE PRESENT
    // -------------------------------------------------------------------------
    private void updateRideUI() {

        // Show all detail views and buttons
        tvPickup.setVisibility(View.VISIBLE);
        tvDestination.setVisibility(View.VISIBLE);
        tvPrice.setVisibility(View.VISIBLE);
        tvDistance.setVisibility(View.VISIBLE);
        btnAccept.setVisibility(View.VISIBLE);
        btnReject.setVisibility(View.VISIBLE);

        setStyledText(tvRideType, "Ride Type:", rideType);
        setStyledText(tvPickup, "Pickup:", pickupName);
        setStyledText(tvDestination, "Destination:", dropAddress);
        setStyledText(tvPrice, "Price:", price);

        // Start with "calculating..." texts
        updateDistanceText();

        // Trip distance / ETA (pickup → destination) from OSRM
        if (pickupLat != null && pickupLng != null &&
                destLat != null && destLng != null) {

            GeoPoint pickupPoint = new GeoPoint(pickupLat, pickupLng);
            GeoPoint destPoint = new GeoPoint(destLat, destLng);
            fetchRoute(pickupPoint, destPoint, Color.RED, true); // trip route
        }
    }

    // -------------------------------------------------------------------------
    // BUILD DISTANCE / ETA TEXT
    // -------------------------------------------------------------------------
    private void updateDistanceText() {
        if (tvDistance == null || tvDistance.getVisibility() != View.VISIBLE) return;

        String pickupDist = (pickupDistanceStr != null) ? pickupDistanceStr : "calculating...";
        String pickupEta = (pickupEtaStr != null) ? pickupEtaStr : "calculating...";

        String tripDist = (tripDistanceStr != null) ? tripDistanceStr : "calculating...";
        String tripEta = (tripEtaStr != null) ? tripEtaStr : "calculating...";

        String text = "📍 Distance to Pickup — " + pickupDist +
                "\n⏱ ETA to Pickup — " + pickupEta +
                "\n\n🚗 Trip Distance — " + tripDist +
                "\n⏱ Trip ETA — " + tripEta;

        tvDistance.setText(text);
    }

    // -------------------------------------------------------------------------
    // LOCATION UPDATES
    // -------------------------------------------------------------------------
    private void startLocationUpdates() {
        LocationRequest request =
                LocationRequest.create()
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

                    // Update Firebase driver location
                    driversRef.child("currentLat").setValue(lat);
                    driversRef.child("currentLng").setValue(lng);

                    if (currentRideId != null) {
                        ridesRef.child(currentRideId).child("driverLat").setValue(lat);
                        ridesRef.child(currentRideId).child("driverLng").setValue(lng);
                    }

                    // Always show driver on map
                    updatePickupDistanceAndEta(lat, lng);
                    driverLocUpdate(new GeoPoint(lat, lng), bearing);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    // straight-line distance + ETA driver → pickup
    private void updatePickupDistanceAndEta(double driverLat, double driverLng) {
        if (pickupLat == null || pickupLng == null || currentRideId == null) {
            // no active ride
            return;
        }

        float[] results = new float[1];
        Location.distanceBetween(driverLat, driverLng, pickupLat, pickupLng, results);
        double distanceKm = results[0] / 1000.0;

        pickupDistanceStr = String.format("%.2f km", distanceKm);

        double hours = distanceKm / AVG_SPEED_KMPH;
        int totalMinutes = (int) Math.round(hours * 60.0);
        pickupEtaStr = formatMinutes(totalMinutes);

        updateDistanceText();
    }

    // -------------------------------------------------------------------------
    // DRIVER MARKER & ROUTES
    // -------------------------------------------------------------------------
    private void driverLocUpdate(@Nullable GeoPoint driverLoc, float bearing) {
        if (osmMap == null) return;

        GeoPoint pickup = (pickupLat != null && pickupLng != null)
                ? new GeoPoint(pickupLat, pickupLng) : null;
        GeoPoint dest = (destLat != null && destLng != null)
                ? new GeoPoint(destLat, destLng) : null;

        // Driver marker
        if (driverLoc != null) {
            if (driverMarker == null) {
                Bitmap originalBitmap = BitmapFactory.decodeResource(
                        getResources(), R.drawable.driver_image);
                if (originalBitmap != null) {
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                            originalBitmap, 100, 100, false);
                    driverMarker = new Marker(osmMap);
                    driverMarker.setPosition(driverLoc);
                    driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    driverMarker.setIcon(new BitmapDrawable(getResources(), resizedBitmap));
                    driverMarker.setTitle("Driver (You)");
                    driverMarker.setRotation(bearing);
                    osmMap.getOverlays().add(driverMarker);
                } else {
                    driverMarker = new Marker(osmMap);
                    driverMarker.setPosition(driverLoc);
                    driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    driverMarker.setTitle("Driver (You)");
                    driverMarker.setRotation(bearing);
                    osmMap.getOverlays().add(driverMarker);
                }
                osmMap.getController().setZoom(15);
                osmMap.getController().setCenter(driverLoc);
            } else {
                animateMarkerTo(driverMarker, driverLoc, bearing, MARKER_ANIM_DURATION);
            }
        }

        // Pickup marker
        if (pickup != null && pickupMarker == null) {
            Bitmap originalPickupBitmap = BitmapFactory.decodeResource(
                    getResources(), R.drawable.pickup_location);
            if (originalPickupBitmap != null) {
                Bitmap resizedPickupBitmap =
                        Bitmap.createScaledBitmap(originalPickupBitmap, 100, 100, false);
                pickupMarker = new Marker(osmMap);
                pickupMarker.setPosition(pickup);
                pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                pickupMarker.setIcon(new BitmapDrawable(getResources(), resizedPickupBitmap));
                pickupMarker.setTitle("Pickup: " + (pickupName != null ? pickupName : ""));
                osmMap.getOverlays().add(pickupMarker);
            } else {
                pickupMarker = createMarker(
                        pickup,
                        "Pickup: " + (pickupName != null ? pickupName : ""),
                        R.drawable.pickup_location
                );
                osmMap.getOverlays().add(pickupMarker);
            }
        }

        // Destination marker
        if (dest != null && destMarker == null) {
            Bitmap originalDestBitmap = BitmapFactory.decodeResource(
                    getResources(), R.drawable.drop_location_icon);
            if (originalDestBitmap != null) {
                Bitmap resizedDestBitmap =
                        Bitmap.createScaledBitmap(originalDestBitmap, 100, 100, false);
                destMarker = new Marker(osmMap);
                destMarker.setPosition(dest);
                destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                destMarker.setIcon(new BitmapDrawable(getResources(), resizedDestBitmap));
                destMarker.setTitle("Destination: " + (dropAddress != null ? dropAddress : ""));
                osmMap.getOverlays().add(destMarker);
            } else {
                destMarker = createMarker(
                        dest,
                        "Destination: " + (dropAddress != null ? dropAddress : ""),
                        R.drawable.drop_location_icon
                );
                osmMap.getOverlays().add(destMarker);
            }
        }

        // Driver → Pickup route (blue), throttled to avoid flicker
        if (pickup != null && driverLoc != null && currentRideId != null) {
            long now = System.currentTimeMillis();
            if (pickupRoute == null || now - lastPickupRouteRequestTime > 20000L) { // 20 sec
                lastPickupRouteRequestTime = now;
                fetchRoute(driverLoc, pickup, Color.BLUE, false);
            }
        }

        osmMap.invalidate();
    }

    private Marker createMarker(GeoPoint point, String title, int iconRes) {
        Marker marker = new Marker(osmMap);
        marker.setPosition(point);
        marker.setTitle(title);
        if (getContext() != null) {
            marker.setIcon(ContextCompat.getDrawable(requireContext(), iconRes));
        }
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        return marker;
    }

    // -------------------------------------------------------------------------
    // OSRM ROUTE FETCH
    // -------------------------------------------------------------------------
    private void fetchRoute(GeoPoint start,
                            GeoPoint end,
                            int color,
                            boolean isTripRoute) {

        if (getActivity() == null) return;

        String url = "https://router.project-osrm.org/route/v1/driving/"
                + start.getLongitude() + "," + start.getLatitude() + ";"
                + end.getLongitude() + "," + end.getLatitude()
                + "?overview=full&geometries=geojson";

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Route fetch failed",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || getActivity() == null) return;

                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    JSONObject routeObj =
                            json.getJSONArray("routes").getJSONObject(0);

                    double distanceMeters = routeObj.optDouble("distance", 0);
                    double durationSeconds = routeObj.optDouble("duration", 0);

                    JSONArray coords = routeObj
                            .getJSONObject("geometry")
                            .getJSONArray("coordinates");

                    Polyline routeLine = new Polyline();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray point = coords.getJSONArray(i);
                        routeLine.addPoint(
                                new GeoPoint(point.getDouble(1),
                                        point.getDouble(0)));
                    }
                    routeLine.setColor(color);
                    routeLine.setWidth(color == Color.RED ? 8f : 6f);

                    requireActivity().runOnUiThread(() -> {
                        if (isTripRoute) {
                            if (currentRoute != null)
                                osmMap.getOverlays().remove(currentRoute);
                            currentRoute = routeLine;
                        } else {
                            if (pickupRoute != null)
                                osmMap.getOverlays().remove(pickupRoute);
                            pickupRoute = routeLine;
                        }

                        osmMap.getOverlays().add(routeLine);
                        osmMap.invalidate();

                        if (isTripRoute) {
                            updateTripDistanceAndEtaFromRoute(
                                    distanceMeters,
                                    durationSeconds
                            );
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Trip distance + ETA from OSRM (pickup -> dest)
    // -------------------------------------------------------------------------
    private void updateTripDistanceAndEtaFromRoute(double distanceMeters,
                                                   double durationSeconds) {
        if (distanceMeters <= 0 || durationSeconds <= 0) return;

        double km = distanceMeters / 1000.0;
        String etaStr = formatDuration(durationSeconds);

        tripDistanceStr = String.format("%.2f km", km);
        tripEtaStr = etaStr;

        updateDistanceText();
    }

    private String formatDuration(double durationSeconds) {
        if (durationSeconds <= 0) return "N/A";
        int totalMinutes = (int) Math.round(durationSeconds / 60.0);
        return formatMinutes(totalMinutes);
    }

    private String formatMinutes(int totalMinutes) {
        if (totalMinutes <= 0) return "N/A";
        if (totalMinutes < 60) {
            return totalMinutes + " min";
        } else {
            int hours = totalMinutes / 60;
            int mins = totalMinutes % 60;
            if (mins == 0) return hours + " hr";
            return hours + " hr " + mins + " min";
        }
    }

    // -------------------------------------------------------------------------
    // MARKER ANIMATION
    // -------------------------------------------------------------------------
    private void animateMarkerTo(Marker marker,
                                 GeoPoint toPosition,
                                 float toRotation,
                                 long duration) {

        GeoPoint startPosition = marker.getPosition();
        float startRotation = marker.getRotation();

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float t = animation.getAnimatedFraction();
            double lat = startPosition.getLatitude() +
                    (toPosition.getLatitude() - startPosition.getLatitude()) * t;
            double lon = startPosition.getLongitude() +
                    (toPosition.getLongitude() - startPosition.getLongitude()) * t;
            marker.setPosition(new GeoPoint(lat, lon));

            float rot = interpolateRotation(startRotation, toRotation, t);
            marker.setRotation(rot);
            osmMap.getController().setCenter(marker.getPosition());
            osmMap.invalidate();
        });
        animator.start();
    }

    private float interpolateRotation(float start, float end, float fraction) {
        float normalizedEnd = end % 360;
        float normalizedStart = start % 360;

        float diff = normalizedEnd - normalizedStart;

        if (diff > 180) diff -= 360;
        else if (diff < -180) diff += 360;

        return normalizedStart + (diff * fraction);
    }

    // -------------------------------------------------------------------------
    // MISC
    // -------------------------------------------------------------------------
    private void setStyledText(TextView tv, String label, String value) {
        if (value == null) value = "N/A";
        SpannableString ss = new SpannableString(label + " " + value);
        ss.setSpan(new StyleSpan(Typeface.BOLD),
                0,
                label.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
