package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private DatabaseReference driverRef;

    private Button privacyPolicyBtn, termsConditionsBtn, about_us, deleteAccount, btnLogout;
    private TextView tvName, tvEmail;
    private ImageView ivProfileImage; // Profile image
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
        ivProfileImage = view.findViewById(R.id.profileimage);
        deleteAccount = view.findViewById(R.id.deleteAccount);
        btnLogout = view.findViewById(R.id.logoutBtn);
        about_us = view.findViewById(R.id.About_us);
        privacyPolicyBtn = view.findViewById(R.id.privacy_policy);
        termsConditionsBtn = view.findViewById(R.id.terms_conditions);

        // Load profile info
        fetchProfileInfo();

        // Delete Account Button
        deleteAccount.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DeleteAccountActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);

        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getActivity(), Login_Activity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            requireActivity().finish();
        });

        // Other buttons
        about_us.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AboutUsActivity.class));
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        privacyPolicyBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PrivacyPolicyActivity.class));
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        termsConditionsBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), TermsConditionsActivity.class));
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        return view;
    }

    private void fetchProfileInfo() {
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String name = snapshot.child("firstName").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                // AWS URL saved in Firebase
                String profileUrl = snapshot.child("documents").child("PROFILE").getValue(String.class);

                tvName.setText(name != null ? name : "User");
                tvEmail.setText(email != null ? email : "No Email");

                // ---- 🔥 Rounded Image Loading (only change here) ----
                RequestOptions requestOptions = new RequestOptions()
                        .transform(new RoundedCorners(40))
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user);

                if (profileUrl != null && !profileUrl.isEmpty()) {

                    Glide.with(requireContext())
                            .load(profileUrl)
                            .apply(requestOptions)
                            .into(ivProfileImage);

                } else {
                    Glide.with(requireContext())
                            .load(R.drawable.user)
                            .apply(requestOptions)
                            .into(ivProfileImage);
                }
                // ------------------------------------------------------
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
