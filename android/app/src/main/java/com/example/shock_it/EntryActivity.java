package com.example.shock_it;


import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shock_it.fragments.loginAndRegister.LoginFrag;
import com.example.shock_it.fragments.loginAndRegister.RegisterFrag;

public class EntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new LoginFrag())
                .commit();
    }

    public void goToRegister(View view) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new RegisterFrag())
                .commit();
    }

    public void goToLogin(View view) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new LoginFrag())
                .commit();
    }
}
