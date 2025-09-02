package com.example.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserRequest_Fragment extends Fragment implements OnMapReadyCallback {

    Button btnAccept, btnReject;
    TextView tvRideType, tvPickup, tvDestination, tvPrice, tvDistance;

    FirebaseAuth auth;
    DatabaseReference ridesRef, driversRef;
    String currentDriverId;
    String currentRideId = null;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    // Map
    private GoogleMap mMap;

    // Ride info
    Double pickupLat, pickupLng, destLat, destLng;
    String pickupName, dropName, rideType, price;

    public UserRequest_Fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_request_, container, false);

        // Firebase Auth
        auth = FirebaseAuth.getInstance();
        FirebaseUser driver = auth.getCurrentUser();

        if (driver == null) {
            // redirect to login if needed
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

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Setup map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Start location updates
        startLocationUpdates();

        // Listen for waiting rides
        ridesRef.orderByChild("status").equalTo("waiting")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot rideSnap : snapshot.getChildren()) {
                                currentRideId = rideSnap.getKey(); // Firebase key of the ride

                                rideType = rideSnap.child("rideType").getValue(String.class);
                                pickupLat = rideSnap.child("pickupLat").getValue(Double.class);
                                pickupLng = rideSnap.child("pickupLng").getValue(Double.class);
                                destLat = rideSnap.child("destLat").getValue(Double.class);
                                destLng = rideSnap.child("destLng").getValue(Double.class);
                                price = rideSnap.child("price").getValue(String.class);
                                pickupName = rideSnap.child("pickupName").getValue(String.class);
                                dropName = rideSnap.child("Drop").getValue(String.class);

                                tvRideType.setText("Ride Type: " + (rideType != null ? rideType : "N/A"));
                                tvPickup.setText("Pickup: " + (pickupName != null ? pickupName : pickupLat + ", " + pickupLng));
                                tvDestination.setText("Destination: " + (dropName != null ? dropName : destLat + ", " + destLng));
                                tvPrice.setText("Price: " + (price != null ? price : "N/A"));

                                updateDistanceWithLastLocation();
                                showMarkersOnMap();
                                break; // only first ride
                            }
                        } else {
                            clearRideUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Accept Ride
        btnAccept.setOnClickListener(v -> {
            if (currentRideId != null) {
                ridesRef.child(currentRideId).child("status").setValue("accepted");
                ridesRef.child(currentRideId).child("driverId").setValue(currentDriverId);
                driversRef.child("status").setValue("onRide");

                Intent intent = new Intent(getActivity(), RideDetail_Activity.class);
                intent.putExtra("rideId", currentRideId);
                startActivity(intent);
                requireActivity().finish();
            } else {
                Toast.makeText(getContext(), "No ride to accept", Toast.LENGTH_SHORT).show();
            }
        });

        // Reject Ride
        btnReject.setOnClickListener(v -> {
            if (currentRideId != null) {
                ridesRef.child(currentRideId).child("rejectedDrivers")
                        .child(currentDriverId).setValue(true);
                clearRideUI();
                Toast.makeText(getContext(), "Ride Rejected", Toast.LENGTH_SHORT).show();
            }
        });



        return view;
    }

    private void clearRideUI() {
        tvRideType.setText("No Ride Requests");
        tvPickup.setText("");
        tvDestination.setText("");
        tvPrice.setText("");
        tvDistance.setText("");
        currentRideId = null;
        pickupLat = null;
        pickupLng = null;
        destLat = null;
        destLng = null;
        if (mMap != null) mMap.clear();
        driversRef.child("status").setValue("available");
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        driversRef.child("currentLat").setValue(location.getLatitude());
                        driversRef.child("currentLng").setValue(location.getLongitude());

                        if (currentRideId != null) {
                            ridesRef.child(currentRideId).child("driverLat").setValue(location.getLatitude());
                            ridesRef.child(currentRideId).child("driverLng").setValue(location.getLongitude());
                        }

                        if (pickupLat != null && pickupLng != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                    pickupLat, pickupLng, results);
                            float distanceInKm = results[0] / 1000f;
                            tvDistance.setText("Distance to Pickup: " +
                                    String.format("%.2f", distanceInKm) + " km");
                        }

                        // Update map markers
                        showMarkersOnMap();
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback, Looper.getMainLooper());
    }

    private void updateDistanceWithLastLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && pickupLat != null && pickupLng != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                            pickupLat, pickupLng, results);
                    float distanceInKm = results[0] / 1000f;
                    tvDistance.setText("Distance to Pickup: " +
                            String.format("%.2f", distanceInKm) + " km");
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        showMarkersOnMap();
    }

    private void showMarkersOnMap() {
        if (mMap == null) return;
        mMap.clear();

        LatLng driverLoc = null;
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    driverLocUpdate(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            });
        }
    }

    private void driverLocUpdate(LatLng driverLoc) {
        if (mMap == null) return;

        LatLng pickup = (pickupLat != null && pickupLng != null) ? new LatLng(pickupLat, pickupLng) : null;
        LatLng dest = (destLat != null && destLng != null) ? new LatLng(destLat, destLng) : null;

        if (pickup != null)
            mMap.addMarker(new MarkerOptions().position(pickup).title("Pickup: " + (pickupName != null ? pickupName : "")));
        if (dest != null)
            mMap.addMarker(new MarkerOptions().position(dest).title("Destination: " + (dropName != null ? dropName : "")));
        if (driverLoc != null)
            mMap.addMarker(new MarkerOptions().position(driverLoc).title("Driver (You)"));

        PolylineOptions polylineOptions = new PolylineOptions().color(Color.BLUE).width(8f);
        if (driverLoc != null && pickup != null) {
            polylineOptions.add(driverLoc, pickup);
        }
        if (pickup != null && dest != null) {
            polylineOptions.add(pickup, dest);
        }
        if (polylineOptions.getPoints().size() > 1) {
            mMap.addPolyline(polylineOptions);
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        if (driverLoc != null) builder.include(driverLoc);
        if (pickup != null) builder.include(pickup);
        if (dest != null) builder.include(dest);

        if ((driverLoc != null) || (pickup != null) || (dest != null)) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        }
    }
}
