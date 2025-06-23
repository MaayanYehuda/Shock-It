package com.example.shock_it.fragments.loginAndRegister;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shock_it.EntryActivity;
import com.example.shock_it.FarmerHomeActivity;
import com.example.shock_it.R;

import java.io.IOException;

import services.Service;

public class LoginFrag extends Fragment {
    private boolean isLoggingIn = false;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootView= inflater.inflate(R.layout.layout_login, container, false); // זה ה־view שלך
        Button btnLogIn = rootView.findViewById(R.id.loginButton);
        btnLogIn.setOnClickListener(this::login);
        TextView goRegister = rootView.findViewById(R.id.goRegister);
        goRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new RegisterFrag())
                        .addToBackStack(null)
                        .commit();
            }
        });
        return rootView;
    }


    private void login(View view) {
        if (isLoggingIn) return;
        isLoggingIn = true;

        EditText emailInput = rootView.findViewById(R.id.email);
        EditText passwordInput = rootView.findViewById(R.id.password);
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        new Thread(() -> {
            try {
                String res = Service.login(email, password);
                Log.d("LOGIN", res);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();
                    SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_email", email); // משתנה email מכיל את האימייל של המשתמש
                    editor.apply();
                    Intent intent = new Intent(requireContext(), FarmerHomeActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                });

            } catch (IOException e) {
                Log.e("LOGIN ERROR", e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "שגיאה בהתחברות", Toast.LENGTH_SHORT).show();
                });
            } finally {
                isLoggingIn = false;
            }
        }).start();
    }


}

