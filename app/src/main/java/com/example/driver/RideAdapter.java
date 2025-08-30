package com.example.driver;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private final Context context;
    private final List<HistoryRideModel> rides;

    public RideAdapter(Context context, List<HistoryRideModel> rides) {
        this.context = context;
        this.rides = rides;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_ride, parent, false);
        return new RideViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        HistoryRideModel r = rides.get(position);

        // Pickup & Drop
        holder.tvPickup.setText("📍 From: " + safe(r.getPickupName()));
        holder.tvDrop.setText("🎯 To: " + safe(r.getDrop()));

        // Booking Date + Time
        String dateTime = (safe(r.getBookingDate()) + (TextUtils.isEmpty(r.getBookingTime()) ? "" : "  " + r.getBookingTime())).trim();
        holder.tvDateTime.setText(!TextUtils.isEmpty(dateTime) ? dateTime : "—");

        // Price
        holder.tvPrice.setText(safe(r.getPrice()));

        // Vehicle type
        holder.tvType.setText(safe(r.getVehicle()));

        // Status
        String status = safe(r.getStatus()).toLowerCase();
        holder.tvStatus.setText(!status.equals("—") ? status.replace('_', ' ') : "—");
        int colorRes;
        switch (status) {
            case "cancelled_by_driver":
            case "cancelled_by_system":
                colorRes = android.R.color.holo_red_dark;
                break;
            case "accepted":
            case "waiting":
                colorRes = android.R.color.holo_orange_light;
                break;
            case "completed":
                colorRes = android.R.color.holo_green_dark;
                break;
            default:
                colorRes = android.R.color.black;
        }
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, colorRes));

        // Optional: show driver accept/start/end times
        holder.tvStartEndTimes.setText("🚦 Start: " + safe(r.getStartTime()) + " | ⏹ End: " + safe(r.getEndTime()));

        // Item click: you can open ride details if needed
        holder.itemView.setOnClickListener(v -> {
            // handle click if needed
        });
    }

    @Override
    public int getItemCount() {
        return rides == null ? 0 : rides.size();
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickup, tvDrop, tvDateTime, tvPrice, tvStatus, tvType, tvStartEndTimes;

        RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDrop = itemView.findViewById(R.id.tvDrop);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvType = itemView.findViewById(R.id.tvType);

            // Add new TextView in layout to show start/end times
            tvStartEndTimes = itemView.findViewById(R.id.tvStartEndTimes);
        }
    }

    // Helpers
    private static String safe(String s) {
        if (s == null) return "—";
        String t = s.trim();
        return t.isEmpty() ? "—" : t;
    }
}
