package com.example.shock_it.ui.map.farmerProfile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shock_it.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import services.Service;

public class farmerProfile extends Fragment {

    private TextView nameTextView;
    private TextView emailTextView;
    private TextView productsTextView;
    private TextView marketsTextView;

    // אימייל של החקלאי – אפשר לשנות לפי משתמש מחובר בעתיד

    public farmerProfile() {}

    public static farmerProfile newInstance() {
        return new farmerProfile();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_farmer_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // קישור רכיבים מה־XML
        nameTextView = view.findViewById(R.id.farmerName);
        emailTextView = view.findViewById(R.id.farmerEmail);
        productsTextView = view.findViewById(R.id.farmerProducts);
        marketsTextView = view.findViewById(R.id.farmerMarkets);

        // קריאה לפרופיל מהשרת
        loadFarmerProfile();
    }

    private void loadFarmerProfile() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String farmerEmail = prefs.getString("user_email", null);

        if (farmerEmail == null) {
            // לא נשמר אימייל
            nameTextView.setText("משתמש לא מחובר");
            return;
        }

        new Thread(() -> {
            try {
                String response = Service.getUserProfile(farmerEmail);
                JSONObject json = new JSONObject(response);

                String name = json.optString("name", "לא נמצא");
                String email = json.optString("email", "לא נמצא");
                String products = json.optString("products", "לא צוינו מוצרים");
                String markets = json.optString("markets", "לא צוינו שווקים");

                requireActivity().runOnUiThread(() -> {
                    nameTextView.setText(name);
                    emailTextView.setText(email);
                    productsTextView.setText(products);
                    marketsTextView.setText(markets);
                });

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    nameTextView.setText("שגיאה בטעינת פרופיל");
                });
            }
        }).start();
    }

}
