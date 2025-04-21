package com.example.shock_it;
import android.content.Intent;


import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
    }
    public void goToRegister(View view) {
        Intent intent = new Intent(this, register.class);
        startActivity(intent);
    }

    public void login(View view) {
        // handle login logic
    }
}