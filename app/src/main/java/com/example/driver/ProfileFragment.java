package com.example.driver;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;


public class ProfileFragment extends Fragment {
    Button backToDash,account, personal_info,review,notification,fingerPrint;


    public ProfileFragment() {
        // Required empty public constructor
    }


    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        backToDash=view.findViewById(R.id.profiletodashboard);
        account=view.findViewById(R.id.account_setting);
        review=view.findViewById(R.id.review);
        notification=view.findViewById(R.id.notification);
        fingerPrint=view.findViewById(R.id.finger_print);
        personal_info =view.findViewById(R.id.profile_info);


        notification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Notification", Toast.LENGTH_SHORT).show();
            }
        });
        review.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "My Review", Toast.LENGTH_SHORT).show();
            }
        });
        fingerPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Finger Print setting", Toast.LENGTH_SHORT).show();

            }
        });


        personal_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Personal info", Toast.LENGTH_SHORT).show();
            }
        });

        account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Account Setting", Toast.LENGTH_SHORT).show();


            }
        });


        backToDash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(getActivity(), DashBoard.class);
                startActivity(intent);
            }
        });

        // Inflate the layout for this fragment
        return view;
    }
}