package com.example.shock_it;
import android.content.Intent;


import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shock_it.fragments.loginAndRegister.RegisterFrag;

public class EntryActivity extends AppCompatActivity {

    View loginLayout;
    View registerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        loginLayout = findViewById(R.id.loginLayout);
        registerLayout = findViewById(R.id.registerLayout);
    }

    public void goToRegister(View view) {
        loginLayout.setVisibility(View.GONE);
        registerLayout.setVisibility(View.VISIBLE);
    }

    public void goToLogin(View view) {
        loginLayout.setVisibility(View.VISIBLE);
        registerLayout.setVisibility(View.GONE);
    }

    public void login(View view) {
        // בדוק את הנתונים
    }

    public void registerFarmer(View view) {
        // בדוק את הנתונים
    }
}