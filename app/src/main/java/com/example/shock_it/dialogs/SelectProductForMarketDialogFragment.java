package com.example.shock_it.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.shock_it.R;
import classes.Item;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SelectProductForMarketDialogFragment extends DialogFragment {

    private static final String ARG_FARMER_PRODUCTS = "farmerProducts";
    private static final String ARG_ITEM_PRICES_MAP = "itemPricesMap";

    private List<Item> farmerProducts;
    private Map<String, Double> itemPricesMap;
    private OnProductsSelectedListener listener;
    private Map<String, Double> selectedProductsWithPrices;
    private Map<String, EditText> productPriceEditTexts;


    public interface OnProductsSelectedListener {
        void onProductsSelected(List<JSONObject> selectedProducts);
    }

    public void setOnProductSelectedListener(OnProductsSelectedListener listener) {
        this.listener = listener;
    }

    public static SelectProductForMarketDialogFragment newInstance(List<Item> farmerProducts, Map<String, Double> itemPricesMap) {
        SelectProductForMarketDialogFragment fragment = new SelectProductForMarketDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FARMER_PRODUCTS, (Serializable) farmerProducts);
        args.putSerializable(ARG_ITEM_PRICES_MAP, (Serializable) itemPricesMap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            farmerProducts = (List<Item>) getArguments().getSerializable(ARG_FARMER_PRODUCTS);
            itemPricesMap = (Map<String, Double>) getArguments().getSerializable(ARG_ITEM_PRICES_MAP);
        } else {
            farmerProducts = new ArrayList<>();
            itemPricesMap = new HashMap<>();
        }
        if (farmerProducts == null) {
            farmerProducts = new ArrayList<>();
        }
        if (itemPricesMap == null) {
            itemPricesMap = new HashMap<>();
        }
        selectedProductsWithPrices = new HashMap<>();
        productPriceEditTexts = new HashMap<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_select_product_for_market, null);

        TextView dialogTitle = view.findViewById(R.id.dialogTitle);
        LinearLayout productsContainer = view.findViewById(R.id.productsContainer);
        Button buttonConfirmSelection = view.findViewById(R.id.buttonConfirmSelection);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);

        if (farmerProducts.isEmpty()) {
            TextView noProductsTv = new TextView(getContext());
            noProductsTv.setText("אין מוצרים זמינים לבחירה.");
            noProductsTv.setTextSize(16);
            noProductsTv.setPadding(0, 16, 0, 16);
            productsContainer.addView(noProductsTv);
        } else {
            for (Item item : farmerProducts) {
                addProductRow(productsContainer, item);
            }
        }

        buttonConfirmSelection.setOnClickListener(v -> {
            if (listener != null) {
                List<JSONObject> selectedProducts = new ArrayList<>();
                if (selectedProductsWithPrices.isEmpty()) {
                    Toast.makeText(getContext(), "אנא בחר לפחות מוצר אחד.", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Map.Entry<String, Double> entry : selectedProductsWithPrices.entrySet()) {
                    String productName = entry.getKey();
                    double productPrice = entry.getValue();

                    try {
                        JSONObject productJson = new JSONObject();
                        productJson.put("name", productName);
                        productJson.put("price", productPrice);
                        selectedProducts.add(productJson);
                    } catch (JSONException e) {
                        Log.e("SelectProductDialog", "Error creating product JSON: " + e.getMessage());
                    }
                }

                listener.onProductsSelected(selectedProducts);
                dismiss();
            }
        });

        buttonCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    private void addProductRow(LinearLayout container, Item item) {
        LinearLayout rowLayout = new LinearLayout(getContext());
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        rowLayout.setPadding(0, 8, 0, 8);

        CheckBox checkBox = new CheckBox(getContext());
        checkBox.setText(item.getName());
        checkBox.setTextSize(16);
        checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.6f
        ));

        EditText priceEditText = new EditText(getContext());
        priceEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        priceEditText.setHint("מחיר");
        priceEditText.setTextSize(16);
        priceEditText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.4f
        ));
        priceEditText.setEnabled(false);

        // הגדר מחיר ברירת מחדל משדה itemPricesMap
        Double defaultPrice = itemPricesMap.getOrDefault(item.getName(), 0.0);
        priceEditText.setText(String.format(Locale.getDefault(), "%.2f", defaultPrice));
        productPriceEditTexts.put(item.getName(), priceEditText);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            priceEditText.setEnabled(isChecked);
            if (isChecked) {
                try {
                    double currentPrice = Double.parseDouble(priceEditText.getText().toString());
                    selectedProductsWithPrices.put(item.getName(), currentPrice);
                } catch (NumberFormatException e) {
                    Log.e("SelectProductDialog", "Error parsing price on check: " + priceEditText.getText().toString(), e);
                    selectedProductsWithPrices.put(item.getName(), defaultPrice);
                }
            } else {
                selectedProductsWithPrices.remove(item.getName());
            }
        });

        priceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (checkBox.isChecked()) {
                    try {
                        double newPrice = Double.parseDouble(s.toString());
                        selectedProductsWithPrices.put(item.getName(), newPrice);
                    } catch (NumberFormatException e) {

                        Log.e("SelectProductDialog", "Invalid price input for " + item.getName() + ": " + s.toString());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        rowLayout.addView(checkBox);
        rowLayout.addView(priceEditText);
        container.addView(rowLayout);
    }
}
