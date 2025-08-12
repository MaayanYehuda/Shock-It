package com.example.shock_it.ui.map.farmerProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.AddProductDialogFragment;
import com.example.shock_it.MarketProfileActivity;
import com.example.shock_it.R;
import com.example.shock_it.ProductAdapter;
import com.example.shock_it.dialogs.EditProductDialogFragment;
import com.example.shock_it.dialogs.EditProfileDialogFragment;

import java.util.Map;
import java.util.TreeSet;

import classes.Farmer;
import classes.FarmerMarket;
import classes.Item;

// Implement the new interface from ProductAdapter
public class farmerProfile extends Fragment implements ProductAdapter.OnProductActionListener {

    private FarmerProfileViewModel viewModel;
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView addressTextView;
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private LinearLayout marketsLayout;
    private Button editProfileButton;
    private Button addProductButton;
    private String farmerEmail;
    private String loggedInUserEmail;

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

        // Initialize viewModel once at the beginning
        viewModel = new ViewModelProvider(this).get(FarmerProfileViewModel.class);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        loggedInUserEmail = prefs.getString("user_email", null);

        if (getArguments() != null) {
            farmerEmail = getArguments().getString("farmer_email_key");
            Log.d("FarmerProfile", "Farmer email from arguments: " + farmerEmail);
        } else {
            farmerEmail = loggedInUserEmail;
            Log.d("FarmerProfile", "No arguments found, defaulting to logged-in user email: " + farmerEmail);
        }

        nameTextView = view.findViewById(R.id.farmerName);
        emailTextView = view.findViewById(R.id.farmerEmail);
        addressTextView = view.findViewById(R.id.farmerAddress);

        productsRecyclerView = view.findViewById(R.id.farmerProductsRecyclerView);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        productAdapter = new ProductAdapter(this);
        productsRecyclerView.setAdapter(productAdapter);

        marketsLayout = view.findViewById(R.id.farmerMarketsLayout);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        addProductButton = view.findViewById(R.id.addProductButton);

        viewModel.getFarmer().observe(getViewLifecycleOwner(), farmer -> {
            if (farmer != null) {
                // 🆕 הלוג הזה קריטי - הוא יראה לנו מה הערכים האמיתיים של האובייקט
                Log.d("FarmerProfileFragment", "Observed Farmer object -> Phone: " + farmer.getPhone() + ", Radius: " + farmer.getNotificationRadius());

                nameTextView.setText(farmer.getName());
                emailTextView.setText(farmer.getEmail());
                addressTextView.setText(farmer.getAddress());

                boolean isProfileOwner = loggedInUserEmail != null && loggedInUserEmail.equals(farmer.getEmail());

                productAdapter.setProducts(farmer.getProducts(), isProfileOwner);
                updateMarketsUI(farmer.getMarkets());

                if (isProfileOwner) {
                    editProfileButton.setVisibility(View.VISIBLE);
                    addProductButton.setVisibility(View.VISIBLE);
                } else {
                    editProfileButton.setVisibility(View.GONE);
                    addProductButton.setVisibility(View.GONE);
                }

            } else {
                nameTextView.setText("שגיאה בטעינת פרופיל / משתמש לא מחובר");
                emailTextView.setText("");
                addressTextView.setText("");
                productAdapter.setProducts(null, false);
                marketsLayout.removeAllViews();
                editProfileButton.setVisibility(View.GONE);
                addProductButton.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "לא ניתן לטעון פרופיל חקלאי.", Toast.LENGTH_LONG).show();
            }
        });

        if (farmerEmail != null) {
            viewModel.loadFarmerProfile(farmerEmail);
        } else {
            Toast.makeText(requireContext(), "שגיאה: מייל חקלאי לא נמצא.", Toast.LENGTH_LONG).show();
            nameTextView.setText("משתמש לא מחובר");
            emailTextView.setText("");
            addressTextView.setText("");
            productAdapter.setProducts(null, false);
            marketsLayout.removeAllViews();
            editProfileButton.setVisibility(View.GONE);
            addProductButton.setVisibility(View.GONE);
        }

        editProfileButton.setOnClickListener(v -> {
            Farmer currentFarmer = viewModel.getFarmer().getValue();
            if (currentFarmer != null) {
                String phone = currentFarmer.getPhone() != null ? currentFarmer.getPhone() : "";
                String notificationRadius = String.valueOf(currentFarmer.getNotificationRadius());

                Log.d("FarmerProfileFragment", "Sending to dialog -> Phone: " + phone + ", Radius: " + notificationRadius);

                EditProfileDialogFragment dialog = EditProfileDialogFragment.newInstance(
                        currentFarmer.getName(),
                        phone,
                        currentFarmer.getAddress(),
                        notificationRadius
                );

                dialog.setEditProfileDialogListener((newName, newPhone, newAddress, newRadiusStr) -> {
                    if (farmerEmail != null) {
                        viewModel.updateFarmerProfile(farmerEmail, newName, newPhone, newAddress, newRadiusStr);
                        Toast.makeText(requireContext(), "מעדכן פרטי פרופיל...", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show(getParentFragmentManager(), "EditProfileDialog");
            } else {
                Toast.makeText(requireContext(), "לא ניתן לערוך פרופיל ריק.", Toast.LENGTH_SHORT).show();
            }
        });

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

    @Override
    public void onEditProduct(Item item, double price) {
        EditProductDialogFragment dialog = EditProductDialogFragment.newInstance(
                item.getName(), item.getDescription(), price
        );
        dialog.setEditProductDialogListener(new EditProductDialogFragment.EditProductDialogListener() {
            @Override
            public void onProductEdited(String originalName, String newName, String newDescription, double newPrice) {
                if (newPrice != -1.0) {
                    if (farmerEmail != null) {
                        viewModel.editProduct(farmerEmail, originalName, newName, newDescription, newPrice);
                        Toast.makeText(requireContext(), "מעדכן מוצר...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "מחיר לא חוקי, שינויים לא נשמרו.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProductDeleted(String productName) {
                // Not used here, handled by onDeleteProduct
            }
        });
        dialog.show(getParentFragmentManager(), "EditProductDialog");
    }

    @Override
    public void onDeleteProduct(Item item) {
        Log.d("FarmerProfileFragment", "onDeleteProduct called for item: " + item.getName());
        if (farmerEmail != null) {
            viewModel.deleteProduct(farmerEmail, item.getName());
            Toast.makeText(requireContext(), "מוחק מוצר...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "אין מייל חקלאי למחיקת מוצר.", Toast.LENGTH_SHORT).show();
            Log.e("FarmerProfileFragment", "farmerEmail is null, cannot delete product.");
        }
    }

    private void updateMarketsUI(TreeSet<FarmerMarket> markets) {
        marketsLayout.removeAllViews();

        if (markets != null && !markets.isEmpty()) {
            for (FarmerMarket fm : markets) {
                View marketCardView = getLayoutInflater().inflate(R.layout.market_card_item, marketsLayout, false);

                TextView marketNameTextView = marketCardView.findViewById(R.id.marketName);
                TextView marketDateTextView = marketCardView.findViewById(R.id.marketDate);

                String marketLocation = fm.getMarket().getLocation();
                String marketDate = fm.getMarket().getDate().toString();

                marketNameTextView.setText(marketLocation);
                marketDateTextView.setText(marketDate);

                marketCardView.setOnClickListener(v -> {
                    Log.d("FarmerProfile", "Clicked on market card: " + marketLocation + ", " + marketDate);
                    navigateToMarketProfile(marketLocation, marketDate);
                });

                marketsLayout.addView(marketCardView);
            }
        } else {
            TextView noMarkets = new TextView(requireContext());
            noMarkets.setText("לא צוינו שווקים קשורים");
            noMarkets.setTextSize(16);
            noMarkets.setPadding(0, 4, 0, 4);
            noMarkets.setTextColor(getResources().getColor(android.R.color.darker_gray));
            marketsLayout.addView(noMarkets);
        }
    }

    private void navigateToMarketProfile(String location, String date) {
        Intent intent = new Intent(requireActivity(), MarketProfileActivity.class);
        intent.putExtra("location", location);
        intent.putExtra("date", date);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
