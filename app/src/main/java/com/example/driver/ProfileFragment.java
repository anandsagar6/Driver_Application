package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private DatabaseReference driverRef;
    Button privacyPolicyBtn, termsConditionsBtn,about_us;
    private Button btnBack, btnLogout;
    private TextView tvName, tvEmail;
    private String driverId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Firebase
        auth = FirebaseAuth.getInstance();
        driverId = auth.getCurrentUser().getUid();
        driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId).child("info");

        // UI
        tvName = view.findViewById(R.id.profileName);
        tvEmail = view.findViewById(R.id.profileEmail);
        btnBack = view.findViewById(R.id.profiletodashboard);
        btnLogout = view.findViewById(R.id.logoutBtn);
        about_us = view.findViewById(R.id.About_us);
        privacyPolicyBtn = view.findViewById(R.id.privacy_policy);          // New
        termsConditionsBtn = view.findViewById(R.id.terms_conditions);

        // Load name & email
        fetchProfileInfo();

        // Back button
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DashBoard.class);
            startActivity(intent);
        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getActivity(), Login_Activity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        about_us.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutUsActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        });

        privacyPolicyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PrivacyPolicyActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        });

        termsConditionsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TermsConditionsActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        });

        return view;
    }

    private void fetchProfileInfo() {
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    tvName.setText(name != null ? name : "No Name");
                    tvEmail.setText(email != null ? email : "No Email");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvName.setText("Error");
                tvEmail.setText(error.getMessage());
            }
        });
    }
}
