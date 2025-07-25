package com.example.shock_it.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.shock_it.R;
import classes.Item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; // Import Map

public class SelectProductForMarketDialogFragment extends DialogFragment {

    private static final String ARG_PRODUCTS = "products";
    private static final String ARG_PRODUCT_PRICES = "productPrices"; // NEW: Key for prices map

    private List<Item> farmerProducts;
    private Map<String, Double> itemPricesMap; // NEW: Map to store item name to price

    private OnProductSelectedListener listener;

    public interface OnProductSelectedListener {
        void onProductSelected(Item selectedItem, double marketPrice);
    }

    public void setOnProductSelectedListener(OnProductSelectedListener listener) {
        this.listener = listener;
    }

    // Modified newInstance to accept both products and their prices
    public static SelectProductForMarketDialogFragment newInstance(List<Item> products, Map<String, Double> prices) {
        SelectProductForMarketDialogFragment fragment = new SelectProductForMarketDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PRODUCTS, new ArrayList<>(products));
        args.putSerializable(ARG_PRODUCT_PRICES, (Serializable) prices); // Cast Map to Serializable
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            farmerProducts = (List<Item>) getArguments().getSerializable(ARG_PRODUCTS);
            // Retrieve the prices map
            itemPricesMap = (Map<String, Double>) getArguments().getSerializable(ARG_PRODUCT_PRICES);
        } else {
            farmerProducts = new ArrayList<>();
            itemPricesMap = new HashMap<>(); // Initialize as empty
        }
        // Ensure lists/maps are not null
        if (farmerProducts == null) {
            farmerProducts = new ArrayList<>();
        }
        if (itemPricesMap == null) {
            itemPricesMap = new HashMap<>();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_select_product_for_market, null);

        NumberPicker productPicker = view.findViewById(R.id.productPicker);
        EditText etMarketPrice = view.findViewById(R.id.etMarketPrice);

        // Prepare product names for the picker
        String[] productNames = new String[farmerProducts.size()];
        for (int i = 0; i < farmerProducts.size(); i++) {
            Item currentItem = farmerProducts.get(i);
            Double price = itemPricesMap.get(currentItem.getName()); // Get price from map using item name
            if (price == null) {
                price = 0.0; // Default if price not found (shouldn't happen if map is built correctly)
            }
            productNames[i] = currentItem.getName() + " (" + String.format("%.2f", price) + "₪)";
        }

        if (productNames.length == 0) {
            Toast.makeText(getContext(), "אין מוצרים זמינים לבחירה.", Toast.LENGTH_SHORT).show();
            dismiss();
            return builder.create();
        }

        productPicker.setMinValue(0);
        productPicker.setMaxValue(productNames.length - 1);
        productPicker.setDisplayedValues(productNames);
        productPicker.setWrapSelectorWheel(false);

        // Set initial price to selected product's default price
        if (!farmerProducts.isEmpty()) {
            Item initialItem = farmerProducts.get(0);
            Double initialPrice = itemPricesMap.get(initialItem.getName());
            if (initialPrice != null) {
                etMarketPrice.setText(String.valueOf(initialPrice));
            } else {
                etMarketPrice.setText("0.0"); // Fallback
            }
        }

        productPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (newVal >= 0 && newVal < farmerProducts.size()) {
                Item selectedItem = farmerProducts.get(newVal);
                Double price = itemPricesMap.get(selectedItem.getName());
                if (price != null) {
                    etMarketPrice.setText(String.valueOf(price));
                } else {
                    etMarketPrice.setText("0.0"); // Fallback
                }
            }
        });

        builder.setView(view)
                .setTitle("בחר מוצר לשוק")
                .setPositiveButton("הוסף", (dialog, id) -> {
                    // Handled in setOnShowListener
                })
                .setNegativeButton("בטל", (dialog, id) -> {
                    dialog.cancel();
                });

        Dialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view1 -> {
                if (listener != null) {
                    int selectedIndex = productPicker.getValue();
                    if (selectedIndex >= 0 && selectedIndex < farmerProducts.size()) {
                        Item selectedItem = farmerProducts.get(selectedIndex); // Get the Item object
                        String priceText = etMarketPrice.getText().toString();
                        if (priceText.isEmpty() || !priceText.matches("\\d*\\.?\\d+")) {
                            Toast.makeText(getContext(), "אנא הזן מחיר חוקי למוצר בשוק.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        try {
                            double marketPrice = Double.parseDouble(priceText);
                            listener.onProductSelected(selectedItem, marketPrice); // Pass the Item and the entered price
                            dismiss();
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "מחיר לא חוקי. אנא הזן מספר.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        });
        return dialog;
    }
}