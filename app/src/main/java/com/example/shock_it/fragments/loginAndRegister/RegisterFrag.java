package com.example.shock_it.fragments.loginAndRegister;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shock_it.R;

import classes.Farmer;
import services.Service;

public class RegisterFrag extends Fragment {
    private View rootView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView= inflater.inflate(R.layout.layout_register, container, false);

        Button btnSignUp = rootView.findViewById(R.id.registerButton);
        btnSignUp.setOnClickListener(this::register);
        return rootView;
    }

    private void register(View view) {
        EditText nameInput = rootView.findViewById(R.id.fullName);
        EditText emailInput = rootView.findViewById(R.id.email);
        EditText passwordInput = rootView.findViewById(R.id.password);
        EditText phoneInput = rootView.findViewById(R.id.phone);
        EditText addressInput = rootView.findViewById(R.id.address);

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        Farmer farmer = new Farmer(name, email, password, phone, address);

        new Thread(() -> {
            try {
                String response = Service.register(
                        farmer.getName(),
                        farmer.getEmail(),
                        farmer.getPassword(),
                        farmer.getPhone(),
                        farmer.getAddress()
                );

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                    // ניקוי שדות או מעבר למסך אחר
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    String msg = e.getMessage() != null && e.getMessage().contains("Email already exists") ?
                            "אימייל זה כבר קיים במערכת" :
                            "שגיאה בהרשמה: " + e.getMessage();

                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

}
