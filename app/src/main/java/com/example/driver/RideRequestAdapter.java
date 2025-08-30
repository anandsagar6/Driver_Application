package com.example.driver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.ViewHolder> {

    Context context;
    ArrayList<RideRequestModel> list;
    OnRideActionListener listener;

    public interface OnRideActionListener {
        void onAccept(RideRequestModel request);
        void onReject(RideRequestModel request);
    }

    public RideRequestAdapter(Context context, ArrayList<RideRequestModel> list, OnRideActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequestModel req = list.get(position);
        holder.tvCustomer.setText("Customer: " + req.getCustomerName());
        holder.tvPickup.setText("Pickup: " + req.getPickup());
        holder.tvDestination.setText("Destination: " + req.getDestination());
        holder.tvPrice.setText("Price: " + req.getPrice());

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(req));
        holder.btnReject.setOnClickListener(v -> listener.onReject(req));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomer, tvPickup, tvDestination, tvPrice;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomer = itemView.findViewById(R.id.tvCustomer);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
