package com.example.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.driver.Login_Activity;
import com.example.driver.MainActivity;
import com.example.driver.R;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private Button btnAccount, btnInfo, btnReview, btnNotification, btnFingerprint, btnBack, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        auth = FirebaseAuth.getInstance();

        // Initialize buttons
        btnAccount = view.findViewById(R.id.account_setting);
        btnInfo = view.findViewById(R.id.profile_info);
        btnReview = view.findViewById(R.id.review);
        btnNotification = view.findViewById(R.id.notification);
        btnFingerprint = view.findViewById(R.id.finger_print);
        btnBack = view.findViewById(R.id.profiletodashboard);
        btnLogout = view.findViewById(R.id.logoutBtn);

        // Account settings
        btnAccount.setOnClickListener(v -> {
            // TODO: Open Account Settings Activity
        });

        // Profile Info
        btnInfo.setOnClickListener(v -> {
            // TODO: Open Profile Info Activity
        });

        // Review
        btnReview.setOnClickListener(v -> {
            // TODO: Open Review Page
        });

        // Notification
        btnNotification.setOnClickListener(v -> {
            // TODO: Open Notification Page
        });

        // Fingerprint
        btnFingerprint.setOnClickListener(v -> {
            // TODO: Enable fingerprint settings
        });

        // Back to Dashboard
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DashBoard.class);
            startActivity(intent);
        });

        // Logout
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getActivity(), Login_Activity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
}
