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

        // ולידציה שם מלא
        if (name.isEmpty() || name.length() < 2 || !name.matches("^[\\p{L} .'-]+$")) {
            Toast.makeText(getContext(), "שם מלא חייב להכיל לפחות 2 אותיות וללא מספרים", Toast.LENGTH_SHORT).show();
            return;
        }

        // ולידציה אימייל
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "כתובת אימייל לא תקינה", Toast.LENGTH_SHORT).show();
            return;
        }

        // ולידציה סיסמה
        String passwordPattern =
                "^(?=.*[0-9])" +"(?=.*[a-z])" +
                        "(?=.*[A-Z])" +
                        "(?=.*[!@#$%^&+=])" +
                        "(?=\\S+$).{8,}$";

        if (!password.matches(passwordPattern)) {
            Toast.makeText(getContext(),
                    "הסיסמה חייבת להכיל לפחות 8 תווים, אות גדולה, אות קטנה, מספר ותו מיוחד",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // ולידציה מספר טלפון
        if (!phone.matches("^05\\d{8}$")) { // 10 ספרות שמתחילות ב-05
            Toast.makeText(getContext(), "מספר טלפון לא תקין", Toast.LENGTH_SHORT).show();
            return;
        }

        // ולידציה כתובת
        if (address.isEmpty() || address.length() < 5) {
            Toast.makeText(getContext(), "אנא הזן כתובת תקינה", Toast.LENGTH_SHORT).show();
            return;
        }

        // אם הכל תקין, צור את האובייקט והמשך לרישום
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
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, new LoginFrag())
                            .commit();
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
