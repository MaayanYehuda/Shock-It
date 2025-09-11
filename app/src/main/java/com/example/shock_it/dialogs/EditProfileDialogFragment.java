package com.example.shock_it.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.shock_it.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EditProfileDialogFragment extends DialogFragment {

    public interface EditProfileDialogListener {
        void onProfileEdited(String name, String phone, String address, String notificationRadius);
    }

    private EditProfileDialogListener listener;
    private String currentName, currentPhone, currentAddress, currentNotificationRadius;

    public static EditProfileDialogFragment newInstance(String name, String phone, String address, String notificationRadius) {
        EditProfileDialogFragment fragment = new EditProfileDialogFragment();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("phone", phone);
        args.putString("address", address);
        args.putString("notificationRadius", notificationRadius);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentName = getArguments().getString("name");
            currentPhone = getArguments().getString("phone");
            currentAddress = getArguments().getString("address");
            currentNotificationRadius = getArguments().getString("notificationRadius");
        }
    }

    public void setEditProfileDialogListener(EditProfileDialogListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_farmer_profile, null);

        EditText etName = view.findViewById(R.id.et_edit_name);
        EditText etPhone = view.findViewById(R.id.et_edit_phone);
        EditText etAddress = view.findViewById(R.id.et_edit_address);
        EditText etNotificationRadius = view.findViewById(R.id.et_edit_notification_radius);

        etName.setText(currentName);
        etPhone.setText(currentPhone);
        etAddress.setText(currentAddress);
        etNotificationRadius.setText(currentNotificationRadius);

        Log.d("EditProfileDialog", "Data received by dialog -> Phone: " + currentPhone + ", Radius: " + currentNotificationRadius);


        builder.setView(view)
                .setTitle("ערוך פרטי פרופיל")
                .setPositiveButton("שמור", (dialog, id) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();
                    String newAddress = etAddress.getText().toString().trim();
                    String newNotificationRadius = etNotificationRadius.getText().toString().trim();

                    if (newName.isEmpty() || newPhone.isEmpty() || newAddress.isEmpty() || newNotificationRadius.isEmpty()) {
                        Toast.makeText(requireContext(), "כל השדות חייבים להיות מלאים", Toast.LENGTH_SHORT).show();
                    } else {
                        if (listener != null) {
                            listener.onProfileEdited(newName, newPhone, newAddress, newNotificationRadius);
                        }
                    }
                })
                .setNegativeButton("ביטול", (dialog, id) -> {
                    dialog.cancel();
                });
        return builder.create();
    }
}
