package com.example.shock_it;

import android.content.ActivityNotFoundException;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_profile);

        // קבלת הפרמטרים מהאינטנט
        Intent intent = getIntent();
        location = intent.getStringExtra("location");
        date = intent.getStringExtra("date");

        // התחברות ל-Views
        marketImage = findViewById(R.id.marketImage);
        marketName = findViewById(R.id.marketName);
        marketLocation = findViewById(R.id.marketLocation);
        marketHours = findViewById(R.id.marketHours);
        marketDate = findViewById(R.id.marketDate);
        farmersList = findViewById(R.id.farmersList);
        backToMainButton = findViewById(R.id.backToMainButton);
        navigateButton = findViewById(R.id.navigateButton);

        // לדוגמה: אם יש לך תמונה ספציפית לפי מיקום, אפשר להחליף את התמונה פה
        // marketImage.setImageResource(R.drawable.some_image_based_on_location);

        // הצגת מיקום ותאריך שמגיעים מהאינטנט
        marketLocation.setText("מיקום: " + location);
        marketDate.setText("תאריך: " + date);

        // אם שעות פעילות לא מגיעות מהשרת, אפשר להשאיר סטטי או להחליף לפי צורך
        marketHours.setText("שעות: 09:00 - 14:00");

        backToMainButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(MarketProfileActivity.this, FarmerHomeActivity.class);
            startActivity(backIntent);
            finish();
        });

        navigateButton.setOnClickListener(v -> {
            // אם יש לך קואורדינטות אמיתיות, כדאי להעביר אותן באינטנט ולנווט אליהן
            // כרגע דוגמה עם תל אביב:
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
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.waze"));
            startActivity(intent);
        }
    }

    private void loadMarketProfile() {
        new Thread(() -> {
            try {
                String response = Service.getMarketProfile(location, date);
                JSONObject json = new JSONObject(response);

                // לדוגמה: מידע מהשרת
                String name = json.optString("name", location);  // אם אין שם, נשתמש במיקום
                String hours = json.optString("hours", "09:00 - 14:00");

                JSONArray farmersArray = json.optJSONArray("farmers"); // מערך של חקלאים

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
                        noFarmers.setPadding(0,4,0,4);
                        farmersList.addView(noFarmers);
                    }
                });

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
