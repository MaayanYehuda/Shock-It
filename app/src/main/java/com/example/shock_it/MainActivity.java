package com.example.shock_it;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void goToLogin(View view) {
        Intent intent = new Intent(this, login.class);
        startActivity(intent);
    }

    public void goToMarketMap(View view) {
        Intent intent = new Intent(this, market.class);
        startActivity(intent);
    }
}