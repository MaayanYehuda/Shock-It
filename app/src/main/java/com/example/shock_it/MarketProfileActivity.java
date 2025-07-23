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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.activity.OnBackPressedCallback;

import com.example.shock_it.manageMarket.ManageMarketFragment; // Correct import for the Fragment

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import services.Service;

public class MarketProfileActivity extends AppCompatActivity {

    Button backToMainButton;
    Button navigateButton;
    Button manageMarketButton; // This button's visibility will be managed dynamically
    ImageView marketImage;
    TextView marketName, marketLocation, marketHours, marketDate;
    LinearLayout farmersList;

    private View marketProfileContentScrollView; // Reference to the ScrollView containing market details

    String location;
    String date;
    String marketId; // This variable will be initialized when data is loaded from the server.

    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_profile);

        // Get parameters from the Intent
        Intent intent = getIntent();
        location = intent.getStringExtra("location");
        date = intent.getStringExtra("date");

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);
        Log.d("MarketProfileActivity", "Logged-in user email: " + userEmail);

        // Initialize the ScrollView that holds the main market profile content
        marketProfileContentScrollView = findViewById(R.id.market_profile_content_scroll_view);

        marketImage = findViewById(R.id.marketImage);
        marketName = findViewById(R.id.marketName);
        marketLocation = findViewById(R.id.marketLocation);
        marketHours = findViewById(R.id.marketHours);
        marketDate = findViewById(R.id.marketDate);
        farmersList = findViewById(R.id.farmersList);
        backToMainButton = findViewById(R.id.backToMainButton);
        navigateButton = findViewById(R.id.navigateButton);
        manageMarketButton = findViewById(R.id.manageMarketButton);

        marketLocation.setText("📍 מיקום: " + location);
        marketDate.setText("📅 תאריך: " + date);
        // marketHours will be updated from the server response in loadMarketProfile()

        // Initially hide the manageMarketButton until market ID is retrieved and user is verified
        manageMarketButton.setVisibility(View.GONE);
        manageMarketButton.setEnabled(false); // Also disable it

        backToMainButton.setOnClickListener(v -> {
            Intent backIntent;
            if (userEmail == null || userEmail.isEmpty()) {
                backIntent = new Intent(MarketProfileActivity.this, MainActivity.class);
            } else {
                backIntent = new Intent(MarketProfileActivity.this, FarmerHomeActivity.class);
            }
            startActivity(backIntent);
            finish();
        });

        navigateButton.setOnClickListener(v -> {
            openWazeNavigation("32.0853,34.7818"); // Example coordinates
        });

        // The click listener for the "Manage Market" button.
        // This will ONLY be triggered if the button is visible and enabled,
        // which means marketId should already be populated.
        manageMarketButton.setOnClickListener(v -> {
            // Because we're managing visibility and enabled state in loadMarketProfile(),
            // the marketId should be valid here. This check is more for robustness.
            if (marketId == null || marketId.isEmpty()) {
                Toast.makeText(MarketProfileActivity.this, "שגיאה פנימית: Market ID אינו זמין. 🛑", Toast.LENGTH_LONG).show();
                return;
            }

            // Create an instance of the ManageMarketFragment
            ManageMarketFragment manageMarketFragment = new ManageMarketFragment();

            // Pass data (marketId, location, date) to the fragment using Bundle
            Bundle args = new Bundle();
            args.putString("marketId", marketId);
            args.putString("market_location", location);
            args.putString("market_date", date);
            manageMarketFragment.setArguments(args);

            // Hide the original market profile content and show the fragment container
            marketProfileContentScrollView.setVisibility(View.GONE);
            findViewById(R.id.fragment_container_manage_market).setVisibility(View.VISIBLE);

            // Perform the fragment transaction
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // Replace the content of the fragment container with the new fragment
            fragmentTransaction.replace(R.id.fragment_container_manage_market, manageMarketFragment);

            // Add the transaction to the back stack. This allows the user to press the back button
            // and return to the market profile.
            fragmentTransaction.addToBackStack(null);

            // Commit the transaction
            fragmentTransaction.commit();
        });

        // Handle the device's back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    // If there are fragments in the back stack, pop the last one
                    fragmentManager.popBackStack();
                    // After returning from a management fragment, show the market profile content again
                    marketProfileContentScrollView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_manage_market).setVisibility(View.GONE); // Hide the fragment container
                } else {
                    // If no fragments in the back stack, handle normal Activity back behavior (exit Activity)
                    setEnabled(false); // Temporarily disable this callback to prevent infinite loop
                    MarketProfileActivity.super.onBackPressed();
                }
            }
        });

        // Start loading market profile data as soon as the Activity is created.
        // This will eventually populate marketId and update the manageMarketButton's visibility.
        loadMarketProfile();
    }

    private void openWazeNavigation(String coordinates) {
        try {
            String url = "https://waze.com/ul?ll=" + coordinates + "&navigate=yes";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.waze");
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "אפליקציית Waze אינה מותקנת. מנווט לחנות.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.waze"));
            startActivity(intent);
        }
    }

    private void loadMarketProfile() {
        new Thread(() -> {
            Log.d("MarketProfileActivity", "Attempting to load market profile for: " + location + ", " + date);
            try {
                String response = Service.getMarketProfile(location, date);
                Log.d("MarketProfileActivity", "Server Response: " + response);

                JSONObject json = new JSONObject(response);
                Log.d("MarketProfileActivity", "JSON parsed successfully for: " + location + ", " + date);

                String name = json.optString("name", location);
                String hours = json.optString("hours", "09:00 - 14:00");
                String founderName = json.optString("founderName", null);
                String founderEmail = json.optString("founderEmail", null);

                // ⭐ THIS IS WHERE marketId IS INITIALIZED FROM THE SERVER RESPONSE ⭐
                marketId = json.optString("marketId", null);

                Log.d("MarketProfileActivity", "Founder Email from server: " + founderEmail);
                Log.d("MarketProfileActivity", "Market ID from server: " + marketId);

                JSONArray farmersArray = json.optJSONArray("otherFarmers");

                runOnUiThread(() -> {
                    marketName.setText(name);
                    marketHours.setText("🕒 שעות: " + hours); // Update hours from server

                    // ⭐ Update visibility and enabled state of manageMarketButton ONLY after marketId is set ⭐
                    if (userEmail != null && founderEmail != null && userEmail.equals(founderEmail) ) {
                        manageMarketButton.setVisibility(View.VISIBLE);
                        manageMarketButton.setEnabled(true); // Ensure it's enabled
                        Log.d("MarketProfileActivity", "User is founder and marketId available. Manage button visible.");
                    } else {
                        manageMarketButton.setVisibility(View.GONE);
                        manageMarketButton.setEnabled(false); // Ensure it's disabled
                        Log.d("MarketProfileActivity", "User is NOT founder or marketId not available. Manage button hidden.");
                    }

                    farmersList.removeAllViews();
                    if (founderName != null && !founderName.isEmpty()) {
                        TextView founderTv = new TextView(MarketProfileActivity.this);
                        founderTv.setText("• " + founderName + " (מייסד)");
                        founderTv.setTextSize(18);
                        founderTv.setPadding(0, 8, 0, 8);
                        founderTv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                        farmersList.addView(founderTv);
                    }
                    if (farmersArray != null && farmersArray.length() > 0) {
                        for (int i = 0; i < farmersArray.length(); i++) {
                            try {
                                String farmerName = farmersArray.getString(i);
                                TextView tv = new TextView(MarketProfileActivity.this);
                                tv.setText("• " + farmerName);
                                tv.setTextSize(16);
                                tv.setPadding(0, 4, 0, 4);
                                tv.setTextColor(getResources().getColor(android.R.color.black));
                                farmersList.addView(tv);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        // Display "אין חקלאים משתתפים" only if no founder and no other farmers
                        if (founderName == null || founderName.isEmpty()) {
                            TextView noFarmers = new TextView(MarketProfileActivity.this);
                            noFarmers.setText("אין חקלאים משתתפים");
                            noFarmers.setTextSize(16);
                            noFarmers.setPadding(0, 4, 0, 4);
                            noFarmers.setTextColor(getResources().getColor(android.R.color.darker_gray));
                            farmersList.addView(noFarmers);
                        }
                    }
                });

            } catch (IOException e) {
                Log.e("MarketProfileActivity", "Network error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: בעיית רשת", Toast.LENGTH_LONG).show();
                    // In case of error, ensure button is hidden/disabled
                    manageMarketButton.setVisibility(View.GONE);
                    manageMarketButton.setEnabled(false);
                });
            } catch (JSONException e) {
                Log.e("MarketProfileActivity", "JSON parsing error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: פורמט נתונים שגוי", Toast.LENGTH_LONG).show();
                    manageMarketButton.setVisibility(View.GONE);
                    manageMarketButton.setEnabled(false);
                });
            }
        }).start();
    }
}