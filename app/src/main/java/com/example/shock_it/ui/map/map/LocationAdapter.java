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

/**
 * אדפטר חכם שיודע להציג גם שווקים וגם חקלאים.
 * הוא מזהה את סוג האובייקט (Market או Farmer) ומשתמש ב-ViewHolder המתאים.
 */
public class LocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // הגדרת סוגי View-ים
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
        return -1; // לא אמור לקרות
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

    // ViewHolder עבור Market
    static class MarketViewHolder extends RecyclerView.ViewHolder {
        // שינוי: כאן מצאתי את האלמנטים לפי ה-ID-ים שמופיעים בקובץ market_row.xml.
        // ה-ID-ים בקובץ XML היו marketName ו-marketDate.
        private TextView locationTextView;
        private TextView dateTextView;

        public MarketViewHolder(@NonNull View itemView) {
            super(itemView);
            // מצא את ה-Views מתוך market_row.xml
            // שינוי: תיקון ID מ-marketLocationTextView ל-marketName
            locationTextView = itemView.findViewById(R.id.marketName);
            // שינוי: תיקון ID מ-marketDateTextView ל-marketDate
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

    // ViewHolder עבור Farmer
    static class FarmerViewHolder extends RecyclerView.ViewHolder {
        // שינוי: כאן מצאתי את האלמנטים לפי ה-ID-ים שמופיעים בקובץ farmer_row.xml.
        // ה-ID-ים בקובץ XML היו farmerName ו-farmerEmail.
        private TextView farmerNameTextView;
        private TextView farmerEmailTextView;

        public FarmerViewHolder(@NonNull View itemView) {
            super(itemView);
            // מצא את ה-Views מתוך farmer_row.xml
            farmerNameTextView = itemView.findViewById(R.id.farmerName);
            // שינוי: תיקון ID מ-farmer ו-farmer_address ל-farmerEmail
            farmerEmailTextView = itemView.findViewById(R.id.farmerEmail);
        }

        public void bind(Farmer farmer, OnLocationClickListener listener) {
            farmerNameTextView.setText(farmer.getName());
            farmerEmailTextView.setText(farmer.getEmail()); // השתמשתי ב-getEmail() כדי להתאים ל-TextView של האימייל
            //farmerPhoneTextView.setText(farmer.getPhone()); // הקו הזה נמחק כי אין לו מקום בפריסה הנוכחית
            //farmerAddressTextView.setText(farmer.getAddress()); // הקו הזה נמחק כי אין לו מקום בפריסה הנוכחית
            itemView.setOnClickListener(v -> listener.onLocationClick(farmer));
        }
    }
}
