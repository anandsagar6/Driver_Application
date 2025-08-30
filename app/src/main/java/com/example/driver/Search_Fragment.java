package com.example.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Search_Fragment extends Fragment {

    private TextView customerName, priceText;
    private Button acceptBtn, rejectBtn, logoutBtn;

    private DatabaseReference ridesRef;
    private String currentRideId = null;
    private String currentDriverId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_, container, false);

        customerName = view.findViewById(R.id.customerName);
        priceText = view.findViewById(R.id.priceText);
        acceptBtn = view.findViewById(R.id.acceptButton);
        rejectBtn = view.findViewById(R.id.rejectButton);
        logoutBtn = view.findViewById(R.id.logoutButton);

        FirebaseUser driver = FirebaseAuth.getInstance().getCurrentUser();
        if (driver != null) {
            currentDriverId = driver.getUid();
        }

        ridesRef = FirebaseDatabase.getInstance().getReference("Rides");

        // 🔹 Listen for new ride requests
        ridesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean foundRide = false;

                for (DataSnapshot rideSnap : snapshot.getChildren()) {
                    String status = rideSnap.child("status").getValue(String.class);

                    if ("pending".equals(status)) { // only show pending requests
                        foundRide = true;
                        currentRideId = rideSnap.getKey();

                        String customer = rideSnap.child("customerName").getValue(String.class);
                        String price = rideSnap.child("price").getValue(String.class);

                        customerName.setText("Customer: " + customer);
                        priceText.setText("Price: " + price);
                        break;
                    }
                }

                if (!foundRide) {
                    customerName.setText("Customer: No request");
                    priceText.setText("Price: -");
                    currentRideId = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });

        // Accept button
        acceptBtn.setOnClickListener(v -> {
            if (currentRideId != null) {
                ridesRef.child(currentRideId).child("status").setValue("accepted");
                ridesRef.child(currentRideId).child("driverId").setValue(currentDriverId);
                Toast.makeText(getContext(), "Ride Accepted", Toast.LENGTH_SHORT).show();
            }
        });

        // Reject button
        rejectBtn.setOnClickListener(v -> {
            if (currentRideId != null) {
                ridesRef.child(currentRideId).child("status").setValue("rejected");
                Toast.makeText(getContext(), "Ride Rejected", Toast.LENGTH_SHORT).show();
                currentRideId = null;
            }
        });

        // Logout button
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        });

        return view;
    }
}
