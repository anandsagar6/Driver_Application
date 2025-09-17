package com.example.driver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Activity_Fragment extends Fragment {

    private ProgressBar progressBar;
    private TextView emptyText;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rideRecyclerView;

    private RideAdapter adapter;
    private final List<HistoryRideModel> rideList = new ArrayList<>();
    private DatabaseReference ridesRef;

    public Activity_Fragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_activity_, container, false);

        progressBar = root.findViewById(R.id.progressBar);
        emptyText = root.findViewById(R.id.emptyText);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rideRecyclerView = root.findViewById(R.id.rideRecyclerView);

        adapter = new RideAdapter(requireContext(), rideList);
        rideRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rideRecyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::fetchRides);

        fetchRides(); // initial load

        return root;
    }

    private void fetchRides() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        emptyText.setVisibility(View.GONE);
        rideRecyclerView.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            emptyText.setText("Not logged in");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        String driverId = user.getUid();
        ridesRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers")
                .child(driverId)
                .child("rides");

        ridesRef.addValueEventListener(new ValueEventListener() {   // 👈 realtime listener
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rideList.clear();

                for (DataSnapshot rideSnap : snapshot.getChildren()) {
                    HistoryRideModel ride = rideSnap.getValue(HistoryRideModel.class);
                    if (ride != null) {
                        if (ride.getRideId() == null || ride.getRideId().isEmpty()) {
                            ride.setRideId(rideSnap.getKey());
                        }
                        rideList.add(ride);
                    }
                }

                Collections.reverse(rideList);
                adapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (rideList.isEmpty()) {
                    emptyText.setText("No rides yet");
                    emptyText.setVisibility(View.VISIBLE);
                    rideRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    rideRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                emptyText.setText("Could not load rides. Please try again.");
                emptyText.setVisibility(View.VISIBLE);
                Log.e("Activity_Fragment", "Firebase error: " + error.getMessage());
            }
        });
    }

}
