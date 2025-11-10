package com.example.shock_it.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.shock_it.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EditProductDialogFragment extends DialogFragment {

    public interface EditProductDialogListener {
        void onProductEdited(String originalName, String newName, String newDescription, double newPrice);
        void onProductDeleted(String productName);
    }

    private EditProductDialogListener listener;
    private String originalProductName;
    private String productDescription;
    private double productPrice;

    public static EditProductDialogFragment newInstance(String name, String description, double price) {
        EditProductDialogFragment fragment = new EditProductDialogFragment();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("description", description);
        args.putDouble("price", price);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            originalProductName = getArguments().getString("name");
            productDescription = getArguments().getString("description");
            productPrice = getArguments().getDouble("price");
        }
    }

    public void setEditProductDialogListener(EditProductDialogListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_delete_product, null);

        TextView tvOriginalName = view.findViewById(R.id.tv_original_product_name);
        EditText etNewName = view.findViewById(R.id.et_edit_product_name);
        EditText etNewDescription = view.findViewById(R.id.et_edit_product_description);
        EditText etNewPrice = view.findViewById(R.id.et_edit_product_price);

        tvOriginalName.setText("מוצר נוכחי: " + originalProductName);
        etNewName.setText(originalProductName);
        etNewDescription.setText(productDescription);
        etNewPrice.setText(String.valueOf(productPrice));

        builder.setView(view)
                .setTitle("ערוך/מחק מוצר")
                .setPositiveButton("שמור שינויים", (dialog, id) -> {
                    String newName = etNewName.getText().toString().trim();
                    String newDescription = etNewDescription.getText().toString().trim();
                    String priceStr = etNewPrice.getText().toString().trim();

                    if (newName.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(requireContext(), "שם ומחיר המוצר לא יכולים להיות ריקים", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onProductEdited(originalProductName, newName, newDescription, -1.0); // Use -1.0 to indicate error
                        }
                    } else {
                        try {
                            double newPrice = Double.parseDouble(priceStr);
                            if (listener != null) {
                                listener.onProductEdited(originalProductName, newName, newDescription, newPrice);
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "מחיר לא חוקי", Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onProductEdited(originalProductName, newName, newDescription, -1.0);
                            }
                        }
                    }
                })
                .setNegativeButton("ביטול", (dialog, id) -> dialog.cancel());
        return builder.create();
    }
}