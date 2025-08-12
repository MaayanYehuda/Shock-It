package com.example.shock_it;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shock_it.R;
import java.util.HashMap;
import java.util.List;

public class RecomendedMarketAdapter extends RecyclerView.Adapter<RecomendedMarketAdapter.MarketViewHolder> {

    public List<HashMap<String, String>> marketList;
    private final OnItemActionListener listener;
    private int viewType;

    // שינוי: הוספת מתודה חדשה לממשק עבור לחיצה על כפתור "צפה בפרופיל"
    public interface OnItemActionListener {
        void onAccept(HashMap<String, String> marketData);
        void onDecline(HashMap<String, String> marketData);
        void onRequest(HashMap<String, String> marketData);
        void onViewProfile(HashMap<String, String> marketData);
    }

    public RecomendedMarketAdapter(List<HashMap<String, String>> marketList, OnItemActionListener listener, int viewType) {
        this.marketList = marketList;
        this.listener = listener;
        this.viewType = viewType;
    }

    @NonNull
    @Override
    public MarketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) { // VIEW_TYPE_INVITATIONS
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.market_item_invitation, parent, false);
        } else { // VIEW_TYPE_RECOMMENDATIONS
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.market_item_recommended, parent, false);
        }
        return new MarketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarketViewHolder holder, int position) {
        HashMap<String, String> currentMarket = marketList.get(position);

        holder.marketNameTextView.setText(currentMarket.get("marketName"));
        holder.marketLocationTextView.setText(currentMarket.get("location"));
        holder.marketDateTextView.setText(currentMarket.get("date"));

        // הגדרת מאזינים לכפתורים
        if (holder.requestButton != null) {
            holder.requestButton.setOnClickListener(v -> listener.onRequest(currentMarket));
        }

        // שינוי: הוספת מאזין ללחיצה על הכפתור החדש
        if (holder.viewProfileButton != null) {
            holder.viewProfileButton.setOnClickListener(v -> listener.onViewProfile(currentMarket));
        }

        if (holder.acceptButton != null) {
            holder.acceptButton.setOnClickListener(v -> listener.onAccept(currentMarket));
        }
        if (holder.declineButton != null) {
            holder.declineButton.setOnClickListener(v -> listener.onDecline(currentMarket));
        }
    }

    @Override
    public int getItemCount() {
        return marketList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    public void setMarketList(List<HashMap<String, String>> newMarketList) {
        this.marketList.clear();
        if (newMarketList != null) {
            this.marketList.addAll(newMarketList);
        }
        notifyDataSetChanged();
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    // שינוי: הוספת הפניה לכפתור החדש
    static class MarketViewHolder extends RecyclerView.ViewHolder {
        TextView marketNameTextView, marketLocationTextView, marketDateTextView;
        Button requestButton, acceptButton, declineButton, viewProfileButton;

        MarketViewHolder(@NonNull View itemView) {
            super(itemView);
            marketNameTextView = itemView.findViewById(R.id.marketNameTextView);
            marketLocationTextView = itemView.findViewById(R.id.marketLocationTextView);
            marketDateTextView = itemView.findViewById(R.id.marketDateTextView);
            requestButton = itemView.findViewById(R.id.requestButton);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
            viewProfileButton = itemView.findViewById(R.id.viewProfileButton); // חדש
        }
    }
}
