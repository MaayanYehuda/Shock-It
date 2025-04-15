package com.example.shock_it;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FarmerProfile extends AppCompatActivity {
    Button backToMainButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_farmer);
        backToMainButton = findViewById(R.id.backToMainButton);

        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(FarmerProfile.this, MainActivity.class);
            startActivity(intent);
            finish(); // אופציונלי: סוגר את המסך כדי שלא יחזור אליו בלחיצה על Back
        });

    }
}