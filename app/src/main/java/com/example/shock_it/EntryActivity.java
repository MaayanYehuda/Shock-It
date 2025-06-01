package com.example.shock_it;
import android.content.Intent;


import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shock_it.fragments.loginAndRegister.LoginFrag;
import com.example.shock_it.fragments.loginAndRegister.RegisterFrag;

public class EntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        // ברירת מחדל – טען את מסך login
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.loginLayout, new LoginFrag())
                .commit();
    }

    public void goToRegister(View view) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.registerLayout, new RegisterFrag())
                .addToBackStack(null)
                .commit();
    }

    public void goToLogin(View view) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.loginLayout, new LoginFrag())
                .commit();
    }
}
