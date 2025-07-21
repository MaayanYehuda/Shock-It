package com.example.shock_it;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import services.Service;

public class MarketProfileActivity extends AppCompatActivity {

    Button backToMainButton;
    Button navigateButton;
    ImageView marketImage;
    TextView marketName, marketLocation, marketHours, marketDate;
    LinearLayout farmersList;

    String location;
    String date;

    String email; // This is the class member you want to check

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_profile);

        // Get parameters from the Intent
        Intent intent = getIntent();
        location = intent.getStringExtra("location");
        date = intent.getStringExtra("date");

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        // Assign the retrieved email to the class member 'email'
        email = prefs.getString("user_email", null);

        // Initialize Views
        marketImage = findViewById(R.id.marketImage);
        marketName = findViewById(R.id.marketName);
        marketLocation = findViewById(R.id.marketLocation);
        marketHours = findViewById(R.id.marketHours);
        marketDate = findViewById(R.id.marketDate);
        farmersList = findViewById(R.id.farmersList);
        backToMainButton = findViewById(R.id.backToMainButton);
        navigateButton = findViewById(R.id.navigateButton);

        // Example: If you have a specific image based on location, you can replace the image here
        // marketImage.setImageResource(R.drawable.some_image_based_on_location);

        // Display location and date from the Intent
        marketLocation.setText("מיקום: " + location);
        marketDate.setText("תאריך: " + date);

        // If operating hours don't come from the server, you can leave them static or change as needed
        marketHours.setText("שעות: 09:00 - 14:00");

        backToMainButton.setOnClickListener(v -> {
            Intent backIntent;
            // Now 'email' will correctly hold the user's email or null
            if (email == null || email.isEmpty()) {
                backIntent = new Intent(MarketProfileActivity.this, MainActivity.class);
            } else {
                backIntent = new Intent(MarketProfileActivity.this, FarmerHomeActivity.class);
            }
            startActivity(backIntent);
            finish();
        });

        navigateButton.setOnClickListener(v -> {
            // If you have actual coordinates, you should pass them in the Intent and navigate to them
            // For now, example with Tel Aviv:
            openWazeNavigation("32.0853,34.7818");
        });

        loadMarketProfile();
    }

    private void openWazeNavigation(String coordinates) {
        try {
            String url = "https://waze.com/ul?ll=" + coordinates + "&navigate=yes";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.waze");
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            // If Waze is not installed, open Play Store to Waze
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.waze"));
            startActivity(intent);
        }
    }

    private void loadMarketProfile() {
        new Thread(() -> {
            Log.d("MarketProfileActivity", "Attempting to load market profile...");
            try {
                String response = Service.getMarketProfile(location, date);
                Log.d("MarketProfileActivity", "Server Response: " + response);
                JSONObject json = new JSONObject(response);
                Log.d("MarketProfileActivity", "JSON parsed successfully for: " + location + ", " + date);

                String name = json.optString("name", location);  // If no name, use location
                String hours = json.optString("hours", "09:00 - 14:00");

                JSONArray farmersArray = json.optJSONArray("farmers"); // Array of farmers

                runOnUiThread(() -> {
                    marketName.setText(name);
                    marketHours.setText("שעות: " + hours);

                    farmersList.removeAllViews();
                    if (farmersArray != null && farmersArray.length() > 0) {
                        for (int i = 0; i < farmersArray.length(); i++) {
                            try {
                                String farmerName = farmersArray.getString(i);
                                TextView tv = new TextView(MarketProfileActivity.this);
                                tv.setText("• " + farmerName);
                                tv.setTextSize(16);
                                tv.setPadding(0, 4, 0, 4);
                                farmersList.addView(tv);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        TextView noFarmers = new TextView(MarketProfileActivity.this);
                        noFarmers.setText("אין חקלאים משתתפים");
                        noFarmers.setPadding(0, 4, 0, 4);
                        farmersList.addView(noFarmers);
                    }
                });

            } catch (IOException e) {
                Log.e("MarketProfileActivity", "Network error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: בעיית רשת", Toast.LENGTH_LONG).show();
                });
            } catch (JSONException e) {
                Log.e("MarketProfileActivity", "JSON parsing error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: פורמט נתונים שגוי", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}