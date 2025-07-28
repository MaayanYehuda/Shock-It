package com.example.shock_it.ui.map.farmerProfile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout; // Still needed for marketsLayout
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; // Needed for RecyclerView
import androidx.recyclerview.widget.RecyclerView; // Needed for RecyclerView

import com.example.shock_it.AddProductDialogFragment;
import com.example.shock_it.R;
import com.example.shock_it.ProductAdapter; // Import your ProductAdapter
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
    private RecyclerView productsRecyclerView; // CHANGED: Now a RecyclerView
    private ProductAdapter productAdapter; // NEW: Declare your adapter
    private LinearLayout marketsLayout; // Still a LinearLayout for markets
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

        viewModel = new ViewModelProvider(this).get(FarmerProfileViewModel.class);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        loggedInUserEmail = prefs.getString("user_email", null);

        farmerEmail = loggedInUserEmail;


        // Link views from XML
        nameTextView = view.findViewById(R.id.farmerName);
        emailTextView = view.findViewById(R.id.farmerEmail);
        addressTextView = view.findViewById(R.id.farmerAddress);

        // Initialize RecyclerView for products
        productsRecyclerView = view.findViewById(R.id.farmerProductsRecyclerView); // Correct ID for RecyclerView
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Set a layout manager
        productAdapter = new ProductAdapter(this); // Initialize adapter, passing 'this' as the listener
        productsRecyclerView.setAdapter(productAdapter); // Set the adapter to the RecyclerView

        marketsLayout = view.findViewById(R.id.farmerMarketsLayout);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        addProductButton = view.findViewById(R.id.addProductButton);

        // Observe the Farmer LiveData
        viewModel.getFarmer().observe(getViewLifecycleOwner(), farmer -> {
            if (farmer != null) {
                // Update basic farmer info
                nameTextView.setText(farmer.getName());
                emailTextView.setText(farmer.getEmail());
                addressTextView.setText(farmer.getAddress());

                // Determine if the logged-in user is the profile owner
                boolean isProfileOwner = loggedInUserEmail != null && loggedInUserEmail.equals(farmer.getEmail());

                // Update products list using the adapter
                // Pass the isProfileOwner flag to the adapter so it can show/hide buttons
                productAdapter.setProducts(farmer.getProducts(), isProfileOwner);

                // Update markets list
                updateMarketsUI(farmer.getMarkets());

                // Show/hide edit profile and add product buttons based on logged-in user
                if (isProfileOwner) {
                    editProfileButton.setVisibility(View.VISIBLE);
                    addProductButton.setVisibility(View.VISIBLE);
                } else {
                    editProfileButton.setVisibility(View.GONE);
                    addProductButton.setVisibility(View.GONE);
                }

            } else {
                // Handle error or no data state
                nameTextView.setText("שגיאה בטעינת פרופיל / משתמש לא מחובר");
                emailTextView.setText("");
                addressTextView.setText("");
                productAdapter.setProducts(null, false); // Clear adapter data and hide edit buttons
                marketsLayout.removeAllViews();
                editProfileButton.setVisibility(View.GONE);
                addProductButton.setVisibility(View.GONE);
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
            addressTextView.setText("");
            productAdapter.setProducts(null, false); // Clear adapter data and hide edit buttons
            marketsLayout.removeAllViews();
            editProfileButton.setVisibility(View.GONE);
            addProductButton.setVisibility(View.GONE);
        }

        // Set onClickListener for Edit Profile Button
        editProfileButton.setOnClickListener(v -> {
            Farmer currentFarmer = viewModel.getFarmer().getValue();
            if (currentFarmer != null) {
                // Ensure Farmer class has getPhone() if you're passing it
                // If phone was removed from Farmer class, remove currentFarmer.getPhone() here too
                EditProfileDialogFragment dialog = EditProfileDialogFragment.newInstance(
                        currentFarmer.getName(), currentFarmer.getPhone(), currentFarmer.getAddress());
                dialog.setEditProfileDialogListener((newName, newPhone, newAddress) -> {
                    // This callback is triggered when "Save" is clicked in the dialog
                    if (farmerEmail != null) {
                        viewModel.updateFarmerProfile(farmerEmail, newName, newPhone, newAddress);
                        Toast.makeText(requireContext(), "מעדכן פרטי פרופיל...", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show(getParentFragmentManager(), "EditProfileDialog");
            } else {
                Toast.makeText(requireContext(), "לא ניתן לערוך פרופיל ריק.", Toast.LENGTH_SHORT).show();
            }
        });

        // Add Product Button
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

    // --- Implement ProductAdapter.OnProductActionListener methods ---
    @Override
    public void onEditProduct(Item item, double price) {
        // This method is called by the ProductAdapter when the edit button is clicked for an item
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
                // This specific callback might not be needed if onDeleteProduct is called directly
                // by the adapter for delete button clicks.
                // It's good practice to make sure this isn't inadvertently called for edits.
            }
            // Removed the problematic 'onProductted' method from here
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
    } // <-- ADDED THIS MISSING BRACE!

    // --- End of ProductAdapter.OnProductActionListener methods --- // This is now a comment after the method


    // Helper method to dynamically update the markets UI (remains LinearLayout)
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
            marketsLayout.addView(noMarkets); // Corrected: Use marketsLayout here
        }
    }
}
// Removed the extra closing brace from here at the very end.