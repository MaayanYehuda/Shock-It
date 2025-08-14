package com.example.shock_it.ui.map;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.R;

import java.time.format.DateTimeFormatter;
import java.util.List;
import classes.Market;

public class  MarketAdapter extends RecyclerView.Adapter<MarketAdapter.MarketViewHolder> {
    public void setMarketList(List<Market> markets) {
        this.markets=markets;
    }

    public interface OnMarketClickListener {
        void onMarketClick(Market market);
    }
    private OnMarketClickListener listener;
    private List<Market> markets;
    public MarketAdapter(List<Market> markets, OnMarketClickListener listener) {
        this.markets = markets;
        this.listener = listener;
    }

    public MarketAdapter(List<Market> markets) {
        this.markets = markets;
    }

    @NonNull
    @Override
    public MarketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.market_row, parent, false);
        return new MarketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarketViewHolder holder, int position) {
        Market market = markets.get(position);
        holder.locationText.setText(market.getLocation());

        DateTimeFormatter formatter = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        }
        String formattedDate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formattedDate = market.getDate() != null ? market.getDate().format(formatter) : "ללא תאריך";
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMarketClick(market);
            }
        });

        holder.dateText.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return markets.size();
    }

    static class MarketViewHolder extends RecyclerView.ViewHolder {
        TextView locationText, dateText;

        public MarketViewHolder(@NonNull View itemView) {
            super(itemView);
            locationText = itemView.findViewById(R.id.marketName);
            dateText = itemView.findViewById(R.id.marketDate);
        }
    }
}
