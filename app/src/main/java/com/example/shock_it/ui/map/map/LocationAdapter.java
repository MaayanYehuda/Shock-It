package com.example.shock_it.ui.map.map;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shock_it.R;
import classes.Farmer;
import classes.Market;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MARKET = 1;
    private static final int VIEW_TYPE_FARMER = 2;
    private List<Object> locationList;
    private OnLocationClickListener listener;
    public interface OnLocationClickListener {
        void onLocationClick(Object location);
    }

    public LocationAdapter(List<Object> locationList, OnLocationClickListener listener) {
        this.locationList = locationList;
        this.listener = listener;
    }

    public void setLocationList(List<Object> locationList) {
        this.locationList = locationList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (locationList.get(position) instanceof Market) {
            return VIEW_TYPE_MARKET;
        } else if (locationList.get(position) instanceof Farmer) {
            return VIEW_TYPE_FARMER;
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MARKET) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.market_row, parent, false);
            return new MarketViewHolder(view);
        } else if (viewType == VIEW_TYPE_FARMER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.farmer_row, parent, false);
            return new FarmerViewHolder(view);
        }
        throw new IllegalArgumentException("Invalid view type");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object location = locationList.get(position);

        if (holder instanceof MarketViewHolder && location instanceof Market) {
            ((MarketViewHolder) holder).bind((Market) location, listener);
        } else if (holder instanceof FarmerViewHolder && location instanceof Farmer) {
            ((FarmerViewHolder) holder).bind((Farmer) location, listener);
        }
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    static class MarketViewHolder extends RecyclerView.ViewHolder {
        private TextView locationTextView;
        private TextView dateTextView;

        public MarketViewHolder(@NonNull View itemView) {
            super(itemView);
            locationTextView = itemView.findViewById(R.id.marketName);
            dateTextView = itemView.findViewById(R.id.marketDate);
        }

        public void bind(Market market, OnLocationClickListener listener) {
            locationTextView.setText(market.getLocation());
            if (market.getDate() != null) {
                dateTextView.setText(market.getDate().toString());
            } else {
                dateTextView.setText("תאריך לא ידוע");
            }

            itemView.setOnClickListener(v -> listener.onLocationClick(market));
        }
    }

    static class FarmerViewHolder extends RecyclerView.ViewHolder {
        private TextView farmerNameTextView;
        private TextView farmerEmailTextView;

        public FarmerViewHolder(@NonNull View itemView) {
            super(itemView);
            farmerNameTextView = itemView.findViewById(R.id.farmerName);
            farmerEmailTextView = itemView.findViewById(R.id.farmerEmail);
        }

        public void bind(Farmer farmer, OnLocationClickListener listener) {
            farmerNameTextView.setText(farmer.getName());
            farmerEmailTextView.setText(farmer.getEmail());
            itemView.setOnClickListener(v -> listener.onLocationClick(farmer));
        }
    }
}
