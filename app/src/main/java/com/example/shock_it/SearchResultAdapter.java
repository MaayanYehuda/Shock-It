package com.example.shock_it;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shock_it.ui.map.MarketAdapter;
import java.util.List;
import classes.Farmer;
import classes.Market;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnSearchResultClickListener {
        void onMarketClick(Market market);
        void onFarmerClick(Farmer farmer); // 🌟 הוספת מתודת הלחיצה על חקלאי
    }

    private static final String TAG = "SearchResultAdapter";
    private static final int VIEW_TYPE_MARKET = 0;
    private static final int VIEW_TYPE_FARMER = 1;

    private final List<Object> results;
    private final OnSearchResultClickListener listener;

    public SearchResultAdapter(List<Object> results, OnSearchResultClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = results.get(position);
        if (item instanceof Market) {
            return VIEW_TYPE_MARKET;
        } else if (item instanceof Farmer) {
            return VIEW_TYPE_FARMER;
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_MARKET) {
            View view = inflater.inflate(R.layout.market_row, parent, false);
            return new MarketViewHolder(view);
        } else if (viewType == VIEW_TYPE_FARMER) {
            View view = inflater.inflate(R.layout.farmer_row, parent, false);
            return new FarmerViewHolder(view);
        }
        View view = new View(parent.getContext());
        return new RecyclerView.ViewHolder(view) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = results.get(position);

        if (holder instanceof MarketViewHolder) {
            Market market = (Market) item;
            MarketViewHolder marketHolder = (MarketViewHolder) holder;

            if (marketHolder.marketName != null) {
                marketHolder.marketName.setText(market.getLocation());
            } else {
                Log.e(TAG, "Market name TextView is null for position " + position);
            }

            if (marketHolder.marketDate != null) {
                marketHolder.marketDate.setText(market.getDate() != null ? market.getDate().toString() : "תאריך לא ידוע");
            } else {
                Log.e(TAG, "Market date TextView is null for position " + position);
            }

            if (marketHolder.itemIcon != null) {
                marketHolder.itemIcon.setImageResource(R.drawable.market);
            } else {
                Log.e(TAG, "Market icon ImageView is null for position " + position);
            }

            if (marketHolder.itemView != null) {
                marketHolder.itemView.setOnClickListener(v -> listener.onMarketClick(market));
            }

        } else if (holder instanceof FarmerViewHolder) {
            Farmer farmer = (Farmer) item;
            FarmerViewHolder farmerHolder = (FarmerViewHolder) holder;

            // הוספת שורת הדפסה שתדפיס את הנתונים
            // זה יעזור לנו לאשר שהנתונים אכן מגיעים לאדפטר
            Log.d(TAG, "Binding Farmer at position " + position + ". Name: " + farmer.getName() + ", Email: " + farmer.getEmail());

            if (farmerHolder.farmerName != null) {
                farmerHolder.farmerName.setText(farmer.getName());
            } else {
                Log.e(TAG, "Farmer name TextView is null for position " + position);
            }

            if (farmerHolder.farmerEmail != null) {
                farmerHolder.farmerEmail.setText(farmer.getEmail());
            } else {
                Log.e(TAG, "Farmer email TextView is null for position " + position);
            }

            if (farmerHolder.itemIcon != null) {
                farmerHolder.itemIcon.setImageResource(R.drawable.ic_farmer);
            } else {
                Log.e(TAG, "Farmer icon ImageView is null for position " + position);
            }

            if (farmerHolder.itemView != null) {
                farmerHolder.itemView.setOnClickListener(v -> {

                    if (listener instanceof OnSearchResultClickListener) {

                        Log.d(TAG, "Farmer row clicked. Triggering onFarmerClick for: " + farmer.getEmail());

                        listener.onFarmerClick(farmer);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class MarketViewHolder extends RecyclerView.ViewHolder {
        public final TextView marketName;
        public final TextView marketDate;
        public final ImageView itemIcon;

        public MarketViewHolder(View view) {
            super(view);
            marketName = view.findViewById(R.id.marketName);
            marketDate = view.findViewById(R.id.marketDate);
            itemIcon = view.findViewById(R.id.marketImage);
        }
    }

    public static class FarmerViewHolder extends RecyclerView.ViewHolder {
        public final TextView farmerName;
        public final TextView farmerEmail;
        public final ImageView itemIcon;

        public FarmerViewHolder(View view) {
            super(view);
            farmerName = view.findViewById(R.id.farmerName);
            farmerEmail = view.findViewById(R.id.farmerEmail);
            itemIcon = view.findViewById(R.id.farmerImage);
        }
    }
}
