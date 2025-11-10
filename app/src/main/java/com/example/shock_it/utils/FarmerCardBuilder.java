package com.example.shock_it.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class FarmerCardBuilder {

    public interface OnFarmerClickListener {
        void onFarmerClick(String farmerEmail);
    }

    public static CardView buildFarmerCard(
            Context context,
            String farmerName,
            String farmerEmail,
            @Nullable JSONArray productsArray,
            boolean isFounder,
            OnFarmerClickListener listener) {

        CardView farmerCard = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        farmerCard.setLayoutParams(cardParams);
        farmerCard.setCardElevation(4f);
        farmerCard.setRadius(8f);
        farmerCard.setContentPadding(16, 16, 16, 16);

        LinearLayout cardContentLayout = new LinearLayout(context);
        cardContentLayout.setOrientation(LinearLayout.VERTICAL);
        cardContentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView farmerNameTv = new TextView(context);
        String displayName = "• " + farmerName;
        if (isFounder) {
            displayName += " (מייסד)";
            farmerNameTv.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
            farmerNameTv.setTextSize(18);
            farmerNameTv.setTypeface(null, Typeface.BOLD);
        } else {
            farmerNameTv.setTextColor(context.getResources().getColor(android.R.color.black));
            farmerNameTv.setTextSize(16);
        }
        farmerNameTv.setText(displayName);
        farmerNameTv.setPadding(0, 0, 0, 4);

        farmerNameTv.setClickable(true);
        farmerNameTv.setFocusable(true);

        farmerNameTv.setOnClickListener(v -> {
            if (listener != null) {
                if (farmerEmail != null && !farmerEmail.isEmpty()) {
                    Log.d("FarmerCardBuilder", "Clicked on farmer: " + farmerEmail);
                    listener.onFarmerClick(farmerEmail);
                } else {
                    Toast.makeText(context, "שגיאה: מייל החקלאי לא זמין.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cardContentLayout.addView(farmerNameTv);

        LinearLayout farmerProductsLayout = new LinearLayout(context);
        farmerProductsLayout.setOrientation(LinearLayout.VERTICAL);
        farmerProductsLayout.setPadding(16, 0, 0, 0);
        cardContentLayout.addView(farmerProductsLayout);

        if (productsArray != null && productsArray.length() > 0) {
            TextView productsTitle = new TextView(context);
            productsTitle.setText("מוצרים המוצעים על ידו:");
            productsTitle.setTextSize(14);
            productsTitle.setTypeface(null, Typeface.BOLD);
            productsTitle.setPadding(0, 8, 0, 4);
            farmerProductsLayout.addView(productsTitle);

            for (int i = 0; i < productsArray.length(); i++) {
                try {
                    JSONObject productObj = productsArray.getJSONObject(i);
                    String productName = productObj.optString("name", "מוצר ללא שם");

                    String priceStr = productObj.optString("price", "0.0");
                    double productPrice = 0.0;
                    try {
                        productPrice = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        Log.e("FarmerCardBuilder", "Error parsing product price string: " + priceStr, e);
                    }

                    TextView productTv = new TextView(context);
                    productTv.setText("  - " + productName + " (" + String.format("%.2f", productPrice) + " ₪)");
                    productTv.setTextSize(14);
                    productTv.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                    farmerProductsLayout.addView(productTv);
                } catch (JSONException e) {
                    Log.e("FarmerCardBuilder", "Error parsing product object for farmer: " + e.getMessage());
                }
            }
        } else {
            TextView noProductsTv = new TextView(context);
            noProductsTv.setText("  - אין מוצרים מוצעים על ידו בשוק זה.");
            noProductsTv.setTextSize(14);
            noProductsTv.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            farmerProductsLayout.addView(noProductsTv);
        }

        farmerCard.addView(cardContentLayout);
        return farmerCard;
    }
}
