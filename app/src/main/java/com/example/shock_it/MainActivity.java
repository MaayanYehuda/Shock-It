package com.example.shock_it;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    Button btnOpenFarmerProfile, marketProfileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // וודא שזה השם של קובץ ה-XML שלך

        btnOpenFarmerProfile = findViewById(R.id.btnGoToProfile);
        marketProfileButton = findViewById(R.id.marketProfileButton);

        btnOpenFarmerProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FarmerProfile.class);
                startActivity(intent);
            }
        } );
        marketProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MarketProfileActivity.class);
            startActivity(intent);
        });
    }
}