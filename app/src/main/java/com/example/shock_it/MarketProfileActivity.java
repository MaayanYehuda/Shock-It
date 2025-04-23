package com.example.shock_it;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MarketProfileActivity extends AppCompatActivity {
    Button backToMainButton;
    private Button navigateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_profile);
        backToMainButton = findViewById(R.id.backToMainButton);

        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(MarketProfileActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // אופציונלי: סוגר את המסך כדי שלא יחזור אליו בלחיצה על Back
        });

        navigateButton = findViewById(R.id.navigateButton);

        // לחיצה על כפתור ניווט
        navigateButton.setOnClickListener(v -> openWazeNavigation("32.0853,34.7818")); // דוגמה: תל אביב
    }

    private void openWazeNavigation(String coordinates) {
        try {
            String url = "https://waze.com/ul?ll=" + coordinates + "&navigate=yes";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.waze");
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            // אם Waze לא מותקן
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.waze"));
            startActivity(intent);
        }
    }
}