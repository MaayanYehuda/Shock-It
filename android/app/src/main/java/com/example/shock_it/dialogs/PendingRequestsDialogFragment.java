package com.example.shock_it.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.view.Gravity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import com.example.shock_it.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PendingRequestsDialogFragment extends DialogFragment {


    private static final String ARG_REQUESTS = "pendingRequests";
    private static final String ARG_MARKET_ID = "marketId";

    private List<JSONObject> pendingRequests;
    private String marketId;
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onRequestApproved(String farmerEmail);
        void onRequestDeclined(String farmerEmail);
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public static PendingRequestsDialogFragment newInstance(List<JSONObject> requests, String marketId) {
        PendingRequestsDialogFragment fragment = new PendingRequestsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_REQUESTS, (Serializable) requests);
        args.putString(ARG_MARKET_ID, marketId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pendingRequests = (List<JSONObject>) getArguments().getSerializable(ARG_REQUESTS);
            marketId = getArguments().getString(ARG_MARKET_ID);
        } else {
            pendingRequests = new ArrayList<>();
        }
        if (pendingRequests == null) {
            pendingRequests = new ArrayList<>();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_pending_requests, null);

        LinearLayout requestsContainer = view.findViewById(R.id.requestsContainer);
        Button closeButton = view.findViewById(R.id.closeButton);

        if (pendingRequests.isEmpty()) {
            TextView noRequestsTv = new TextView(getContext());
            noRequestsTv.setText("אין בקשות הצטרפות ממתינות כרגע.");
            noRequestsTv.setTextSize(16);
            noRequestsTv.setPadding(0, 16, 0, 16);
            noRequestsTv.setGravity(Gravity.CENTER);
            requestsContainer.addView(noRequestsTv);
        } else {
            for (JSONObject request : pendingRequests) {
                addRequestCard(requestsContainer, request);
            }
        }

        closeButton.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    private void addRequestCard(LinearLayout container, JSONObject requestObj) {
        CardView card = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setCardElevation(4f);
        card.setRadius(8f);
        card.setContentPadding(16, 16, 16, 16);

        LinearLayout cardContentLayout = new LinearLayout(getContext());
        cardContentLayout.setOrientation(LinearLayout.VERTICAL);
        cardContentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        String farmerName = requestObj.optString("farmerName", "שם לא ידוע");
        String farmerEmail = requestObj.optString("farmerEmail", "מייל לא ידוע");
        JSONArray requestedProductsArray = requestObj.optJSONArray("requestedProducts");

        TextView farmerInfoTv = new TextView(getContext());
        farmerInfoTv.setText("חקלאי: " + farmerName + " (" + farmerEmail + ")");
        farmerInfoTv.setTextSize(16);
        farmerInfoTv.setTypeface(null, android.graphics.Typeface.BOLD);
        cardContentLayout.addView(farmerInfoTv);

        if (requestedProductsArray != null && requestedProductsArray.length() > 0) {
            TextView productsTitle = new TextView(getContext());
            productsTitle.setText("מוצרים מבוקשים:");
            productsTitle.setTextSize(14);
            productsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            productsTitle.setPadding(0, 8, 0, 4);
            cardContentLayout.addView(productsTitle);

            for (int i = 0; i < requestedProductsArray.length(); i++) {
                try {
                    JSONObject productObj = requestedProductsArray.getJSONObject(i);
                    String productName = productObj.optString("name", "מוצר ללא שם");

                    String priceStr = productObj.optString("price", "0.0");
                    double productPrice = 0.0;
                    try {
                        productPrice = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        Log.e("PendingRequestsDialog", "Error parsing product price string: " + priceStr, e);
                    }

                    TextView productTv = new TextView(getContext());
                    productTv.setText("  - " + productName + " (" + String.format("%.2f", productPrice) + " ₪)");
                    productTv.setTextSize(14);
                    cardContentLayout.addView(productTv);
                } catch (JSONException e) {
                    Log.e("PendingRequestsDialog", "Error parsing requested product object: " + e.getMessage());
                }
            }
        } else {
            TextView noProductsTv = new TextView(getContext());
            noProductsTv.setText("  - לא צוינו מוצרים.");
            noProductsTv.setTextSize(14);
            cardContentLayout.addView(noProductsTv);
        }

        LinearLayout buttonLayout = new LinearLayout(getContext());
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        buttonLayout.setGravity(Gravity.END);
        buttonLayout.setPadding(0, 16, 0, 0);

        Button approveButton = new Button(getContext());
        approveButton.setText("אשר");
        approveButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
        approveButton.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams approveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        approveParams.setMarginEnd(8);
        approveButton.setLayoutParams(approveParams);
        approveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestApproved(farmerEmail);
                dismiss();
            }
        });
        buttonLayout.addView(approveButton);

        Button declineButton = new Button(getContext());
        declineButton.setText("דחה");
        declineButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
        declineButton.setTextColor(getResources().getColor(android.R.color.white));
        declineButton.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        declineButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestDeclined(farmerEmail);
                dismiss();
            }
        });
        buttonLayout.addView(declineButton);

        cardContentLayout.addView(buttonLayout);
        card.addView(cardContentLayout);
        container.addView(card);
    }
}