package com.example.driver;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


public class Home_Fragment extends Fragment {

    TextView textView,see_more_teacher;
    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view= inflater.inflate(R.layout.fragment_home_, container, false);
        textView=view.findViewById(R.id.name);
        CardView c=view.findViewById(R.id.cardView1);
        see_more_teacher=view.findViewById(R.id.see_more_teacher);


        see_more_teacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Text clicked!", Toast.LENGTH_SHORT).show();

                // Create a new instance of the ProfileFragment
                ProfileFragment profileFragment = new ProfileFragment();

                // Get the FragmentManager and start a transaction
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                // Replace the current fragment with the ProfileFragment
                fragmentTransaction.replace(R.id.fragment_container, profileFragment);

                // Add the transaction to the back stack (optional)
                fragmentTransaction.addToBackStack(null);

                // Commit the transaction
                fragmentTransaction.commit();
            }
        });






        c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new instance of the ProfileFragment
                ProfileFragment profileFragment = new ProfileFragment();

                // Get the FragmentManager and start a transaction
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                // Replace the current fragment with the ProfileFragment
                fragmentTransaction.replace(R.id.fragment_container, profileFragment);

                // Add the transaction to the back stack (optional)
                fragmentTransaction.addToBackStack(null);

                // Commit the transaction
                fragmentTransaction.commit();
            }

        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            String s = bundle.getString("first");
            textView.setText(s);
        } else {
            // Debugging: Log a message if bundle is null
            Log.d("HomeFragment", "Bundle is null");
        }

        return view;
    }



}