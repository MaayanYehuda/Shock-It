package com.example.shock_it;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;


import services.Service;

public class AddProductDialogFragment extends DialogFragment {
    private String farmerEmail;
    public interface OnProductAddedListener {
        void onProductAdded(String name, String description, double price);
    }

    private OnProductAddedListener listener;

    public void setOnProductAddedListener(OnProductAddedListener listener) {
        this.listener = listener;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setLayout(width, height);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_product, null);
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        farmerEmail = prefs.getString("user_email", null);

        EditText nameInput = view.findViewById(R.id.productNameInput);
        EditText descInput = view.findViewById(R.id.productDescInput);
        EditText priceInput = view.findViewById(R.id.productPriceInput);
        Button confirmButton = view.findViewById(R.id.confirmAddButton);

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(view);

        confirmButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(getContext(), "יש למלא שם ומחיר", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);

                new Thread(() -> {
                    try {
                        String response = Service.addNewItem(name, desc, price, farmerEmail);

                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "המוצר נוסף בהצלחה", Toast.LENGTH_SHORT).show();

                            if (listener != null) {
                                listener.onProductAdded(name, desc, price);
                            }
                            dismiss();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "שגיאה בהוספת המוצר", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "מחיר לא תקין", Toast.LENGTH_SHORT).show();
            }
        });


        return dialog;
    }

}
