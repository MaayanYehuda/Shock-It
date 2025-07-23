package com.example.shock_it.ui.map.farmerProfile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout; // 🆕 Corrected import for LinearLayout
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.shock_it.AddProductDialogFragment;
import com.example.shock_it.R;

import java.util.Map;
import java.util.TreeSet;

import classes.Farmer;
import classes.FarmerMarket;
import classes.Item;

public class farmerProfile extends Fragment {

    private FarmerProfileViewModel viewModel;
    private TextView nameTextView;
    private TextView emailTextView;
    private LinearLayout productsLayout; // 🆕 Changed to LinearLayout
    private LinearLayout marketsLayout;  // 🆕 Changed to LinearLayout
    private String farmerEmail;

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

        viewModel = new ViewModelProvider(this).get(FarmerProfileViewModel.class);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        farmerEmail = prefs.getString("user_email", null);

        // Link views from XML
        nameTextView = view.findViewById(R.id.farmerName);
        emailTextView = view.findViewById(R.id.farmerEmail);
        // 🆕 Bind to the LinearLayouts, not TextViews
        productsLayout = view.findViewById(R.id.farmerProductsLayout);
        marketsLayout = view.findViewById(R.id.farmerMarketsLayout);

        // 🆕 Observe the Farmer LiveData
        viewModel.getFarmer().observe(getViewLifecycleOwner(), farmer -> {
            if (farmer != null) {
                // Update basic farmer info
                nameTextView.setText(farmer.getName());
                emailTextView.setText(farmer.getEmail());

                // Update products list
                updateProductsUI(farmer.getProducts());

                // Update markets list
                updateMarketsUI(farmer.getMarkets());
            } else {
                // Handle error or no data state
                nameTextView.setText("שגיאה בטעינת פרופיל / משתמש לא מחובר");
                emailTextView.setText("");
                productsLayout.removeAllViews();
                marketsLayout.removeAllViews();
                Toast.makeText(requireContext(), "לא ניתן לטעון פרופיל חקלאי.", Toast.LENGTH_LONG).show();
            }
        });

        // Load data if email is available
        if (farmerEmail != null) {
            viewModel.loadFarmerProfile(farmerEmail);
        } else {
            Toast.makeText(requireContext(), "שגיאה: מייל חקלאי לא נמצא.", Toast.LENGTH_LONG).show();
            nameTextView.setText("משתמש לא מחובר");
            emailTextView.setText("");
            productsLayout.removeAllViews();
            marketsLayout.removeAllViews();
        }

        // Add Product Button
        Button addProductButton = view.findViewById(R.id.addProductButton);
        addProductButton.setOnClickListener(v -> {
            AddProductDialogFragment dialog = new AddProductDialogFragment();
            dialog.setOnProductAddedListener((name, desc, price) -> {
                Item newItem = new Item(name, desc);
                if (farmerEmail != null) {
                    viewModel.addProduct(farmerEmail, newItem, price);
                } else {
                    Toast.makeText(requireContext(), "אין מייל חקלאי להוספת מוצר.", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show(getParentFragmentManager(), "AddProductDialog");
        });
    }

    // 🆕 Helper method to dynamically update the products UI
    private void updateProductsUI(Map<Item, Double> products) {
        productsLayout.removeAllViews(); // Clear previous views

        if (products != null && !products.isEmpty()) {
            for (Map.Entry<Item, Double> entry : products.entrySet()) {
                TextView productTextView = new TextView(requireContext());
                productTextView.setText("• " + entry.getKey().getName() + " - " + String.format("%.2f", entry.getValue()) + " ₪");
                productTextView.setTextSize(16);
                productTextView.setPadding(0, 4, 0, 4);
                productsLayout.addView(productTextView);
            }
        } else {
            TextView noProducts = new TextView(requireContext());
            noProducts.setText("לא צוינו מוצרים");
            noProducts.setTextSize(16);
            noProducts.setPadding(0, 4, 0, 4);
            productsLayout.addView(noProducts);
        }
    }

    // 🆕 Helper method to dynamically update the markets UI
    private void updateMarketsUI(TreeSet<FarmerMarket> markets) {
        marketsLayout.removeAllViews(); // Clear previous views

        if (markets != null && !markets.isEmpty()) {
            for (FarmerMarket fm : markets) {
                TextView marketTextView = new TextView(requireContext());
                // Accessing Market details through FarmerMarket
                String marketInfo = "• " + fm.getMarket().getLocation();
                if (fm.getMarket().getDate() != null) {
                    marketInfo += " (" + fm.getMarket().getDate().toString() + ")"; // Format date as needed
                }
                marketTextView.setText(marketInfo);
                marketTextView.setTextSize(16);
                marketTextView.setPadding(0, 4, 0, 4);
                marketsLayout.addView(marketTextView);
            }
        } else {
            TextView noMarkets = new TextView(requireContext());
            noMarkets.setText("לא צוינו שווקים קשורים");
            noMarkets.setTextSize(16);
            noMarkets.setPadding(0, 4, 0, 4);
            marketsLayout.addView(noMarkets);
        }
    }
}