package com.example.driver;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
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

        setBoldLabel(holder.tvPickup, "From: ", safe(r.getPickupName()));
        setBoldLabel(holder.tvDrop, "To: ", safe(r.getDropAddress()));

        // Rider name with bold label
        setBoldLabel(holder.tvriderName, "Rider: ", safe(r.getRiderName()));

        // Date and time
        String dateTime = formatDateTime(safe(r.getBookingDate()), safe(r.getBookingTime()));
        holder.tvDateTime.setText(dateTime);

        // Price with rupee symbol
        holder.tvPrice.setText(formatPrice(safe(r.getPrice())));

        // Status with color (capitalized)
        String status = safe(r.getStatus()).toLowerCase();
        holder.tvStatus.setText(formatStatus(status));
        holder.tvStatus.setTextColor(getStatusColor(status));

        // Rating - only show for completed rides
        if ("completed".equals(status)) {
            setupRating(holder.ratingBar, r.getRating());
            holder.ratingBar.setVisibility(View.VISIBLE);

            // Apply star color programmatically (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.star_selected));
                holder.ratingBar.setProgressTintList(colorStateList);

                ColorStateList secondaryColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.star_unselected));
                holder.ratingBar.setProgressBackgroundTintList(secondaryColorStateList);
                holder.ratingBar.setSecondaryProgressTintList(secondaryColorStateList);
            }
        } else {
            holder.ratingBar.setVisibility(View.GONE);
        }

        // Item click
        holder.itemView.setOnClickListener(v -> {
            // handle click if needed
        });
    }

    @Override
    public int getItemCount() {
        return rides == null ? 0 : rides.size();
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickup, tvDrop, tvDateTime, tvPrice, tvStatus, tvriderName;
        RatingBar ratingBar;

        RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDrop = itemView.findViewById(R.id.tvDrop);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvriderName = itemView.findViewById(R.id.tvriderName);
            ratingBar = itemView.findViewById(R.id.ratingBar);

            // Ensure rating bar is not clickable
            ratingBar.setIsIndicator(true);
            ratingBar.setFocusable(false);
            ratingBar.setClickable(false);
        }
    }

    private void setBoldLabel(TextView tv, String label, String value) {
        SpannableString spannable = new SpannableString(label + value);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, label.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(spannable);
    }

    // Helper: safe string
    private static String safe(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.isEmpty() ? "" : t;
    }

    // Helper: format date and time
    private String formatDateTime(String date, String time) {
        if (date.isEmpty() && time.isEmpty()) return "";
        if (time.isEmpty()) return date;
        if (date.isEmpty()) return time;
        return date + "  " + time;
    }

    // Helper: format price with rupee symbol
    private String formatPrice(String price) {
        if (price.isEmpty()) return "₹0";
        // Remove any existing rupee symbol to avoid duplication
        String cleanPrice = price.replace("₹", "").trim();
        return "₹" + cleanPrice;
    }

    // Helper: format status text
    private String formatStatus(String status) {
        // Convert snake_case to Title Case
        String formatted = status.replace('_', ' ');
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    // Helper: get status color
    private int getStatusColor(String status) {
        switch (status) {
            case "cancelled_by_driver":
            case "cancelled_by_system":
                return ContextCompat.getColor(context, android.R.color.holo_red_dark);
            case "accepted":
            case "waiting":
                return ContextCompat.getColor(context, android.R.color.holo_orange_light);
            case "completed":
                return ContextCompat.getColor(context, android.R.color.holo_green_dark);
            default:
                return ContextCompat.getColor(context, android.R.color.black);
        }
    }

    // Helper: setup rating bar
    private void setupRating(RatingBar ratingBar, Object rating) {
        float ratingValue = 0;

        if (rating instanceof Number) {
            ratingValue = ((Number) rating).floatValue();
        } else if (rating instanceof String) {
            try {
                ratingValue = Float.parseFloat((String) rating);
            } catch (NumberFormatException e) {
                ratingValue = 0;
            }
        }

        // Ensure rating is within valid range (0-5)
        ratingValue = Math.max(0, Math.min(5, ratingValue));
        ratingBar.setRating(ratingValue);

        // Make sure rating bar is not clickable
        ratingBar.setIsIndicator(true);
        ratingBar.setFocusable(false);
        ratingBar.setClickable(false);
    }
}