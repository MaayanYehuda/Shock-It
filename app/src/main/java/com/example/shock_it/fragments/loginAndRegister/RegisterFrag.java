package com.example.shock_it.fragments.loginAndRegister;

import android.location.Address;
import android.location.Geocoder;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import services.Service;

public class RegisterFrag extends Fragment {

    private View rootView;
    private EditText notificationRadiusInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.layout_register, container, false);

        Button btnSignUp = rootView.findViewById(R.id.registerButton);
        btnSignUp.setOnClickListener(this::register);

        // אתחול שדה הקלט החדש עבור הרדיוס
        notificationRadiusInput = rootView.findViewById(R.id.notificationRadius);

        return rootView;
    }

    private void register(View view) {
        EditText nameInput = rootView.findViewById(R.id.fullName);
        EditText emailInput = rootView.findViewById(R.id.email);
        EditText passwordInput = rootView.findViewById(R.id.password);
        EditText phoneInput = rootView.findViewById(R.id.phone);
        EditText addressInput = rootView.findViewById(R.id.address);
        EditText confirmPasswordInput = rootView.findViewById(R.id.confirmPassword);

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // קבלת ערך הרדיוס (יכול להיות ריק)
        String notificationRadiusStr = notificationRadiusInput.getText().toString().trim();

        // ולידציה לשדות חובה
        if (name.isEmpty() || name.length() < 2 || !name.matches("^[\\p{L} .'-]+$")) {
            Toast.makeText(getContext(), "שם מלא חייב להכיל לפחות 2 אותיות וללא מספרים", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "כתובת אימייל לא תקינה", Toast.LENGTH_SHORT).show();
            return;
        }

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

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "אימות הסיסמה אינו תואם", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("^05\\d{8}$")) {
            Toast.makeText(getContext(), "מספר טלפון לא תקין", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer notificationRadius = null;
        if (!notificationRadiusStr.isEmpty()) {
            try {
                notificationRadius = Integer.parseInt(notificationRadiusStr);
                if (notificationRadius < 5 || notificationRadius > 300) {
                    Toast.makeText(getContext(), "רדיוס ההתראות חייב להיות בין 5 ל-300 קילומטר", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "רדיוס ההתראות חייב להיות מספר שלם", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (address.isEmpty() || address.length() < 5) {
            Toast.makeText(getContext(), "אנא הזן כתובת תקינה", Toast.LENGTH_SHORT).show();
            return;
        }

        // שמירה של המשתנים כ-final או effectively final
        final String finalName = name;
        final String finalEmail = email;
        final String finalPassword = password;
        final String finalPhone = phone;
        final String finalAddress = address;
        final Integer finalNotificationRadius = notificationRadius;

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(finalAddress, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address returnedAddress = addresses.get(0);
                    final double latitude = returnedAddress.getLatitude();
                    final double longitude = returnedAddress.getLongitude();

                    String response = Service.register(
                            finalName,
                            finalEmail,
                            finalPassword,
                            finalPhone,
                            finalAddress,
                            latitude,
                            longitude,
                            finalNotificationRadius
                    );

                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragmentContainer, new LoginFrag())
                                .commit();
                    });

                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "לא נמצא מיקום תואם לכתובת שהוזנה", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "שגיאה בגישה לשירות המיקום", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
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
