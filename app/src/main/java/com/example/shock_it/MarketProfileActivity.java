package com.example.shock_it;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.activity.OnBackPressedCallback;
import androidx.cardview.widget.CardView;

import com.example.shock_it.manageMarket.ManageMarketFragment;
import com.example.shock_it.dialogs.SelectProductForMarketDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import services.Service;
import classes.Item;

public class MarketProfileActivity extends AppCompatActivity {

    Button backToMainButton;
    Button navigateButton;
    Button manageMarketButton;
    ImageView marketImage;
    TextView marketName, marketLocation, marketHours, marketDate;
    LinearLayout farmersListContainer;
    LinearLayout marketProductsListContainer; // הצהרה על הקונטיינר החדש
    FloatingActionButton fabAddProduct;

    private View marketProfileContentScrollView;

    String location;
    String date;
    String marketId;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_profile);

        Intent intent = getIntent();
        location = intent.getStringExtra("location");
        date = intent.getStringExtra("date");

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);
        Log.d("MarketProfileActivity", "Logged-in user email: " + userEmail);

        marketProfileContentScrollView = findViewById(R.id.market_profile_content_scroll_view);

        marketImage = findViewById(R.id.marketImage);
        marketName = findViewById(R.id.marketName);
        marketLocation = findViewById(R.id.marketLocation);
        marketHours = findViewById(R.id.marketHours);
        marketDate = findViewById(R.id.marketDate);
        farmersListContainer = findViewById(R.id.farmersList);
        marketProductsListContainer = findViewById(R.id.marketProductsList); // איתחול הקונטיינר החדש
        backToMainButton = findViewById(R.id.backToMainButton);
        navigateButton = findViewById(R.id.navigateButton);
        manageMarketButton = findViewById(R.id.manageMarketButton);
        fabAddProduct = findViewById(R.id.fab_add_product);

        marketLocation.setText("📍 מיקום: " + location);
        marketDate.setText("📅 תאריך: " + date);

        manageMarketButton.setVisibility(View.GONE);
        manageMarketButton.setEnabled(false);
        fabAddProduct.setVisibility(View.GONE); // הגדרה התחלתית כ-GONE

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
            openWazeNavigation("32.0853,34.7818"); // חשוב: וודא שזה מתעדכן למיקום השוק בפועל
        });

        manageMarketButton.setOnClickListener(v -> {
            if (marketId == null || marketId.isEmpty()) {
                Toast.makeText(MarketProfileActivity.this, "שגיאה פנימית: Market ID אינו זמין. 🛑", Toast.LENGTH_LONG).show();
                return;
            }

            ManageMarketFragment manageMarketFragment = new ManageMarketFragment();
            Bundle args = new Bundle();
            args.putString("marketId", marketId);
            args.putString("market_location", location);
            args.putString("market_date", date);
            manageMarketFragment.setArguments(args);

            marketProfileContentScrollView.setVisibility(View.GONE);
            findViewById(R.id.fragment_container_manage_market).setVisibility(View.VISIBLE);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container_manage_market, manageMarketFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        // Click listener for the Add Product FAB
        fabAddProduct.setOnClickListener(v -> {
            if (userEmail == null || marketId == null || marketId.isEmpty()) {
                Toast.makeText(MarketProfileActivity.this, "שגיאה: לא ניתן להוסיף מוצר ללא פרטי משתמש או שוק. 🛑", Toast.LENGTH_LONG).show();
                return;
            }
            showAddProductToMarketDialog(userEmail, marketId);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (findViewById(R.id.fragment_container_manage_market).getVisibility() == View.VISIBLE) {
                    // אם הפראגמנט גלוי, חזור ממנו
                    fragmentManager.popBackStack();
                    marketProfileContentScrollView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_manage_market).setVisibility(View.GONE);
                } else if (fragmentManager.getBackStackEntryCount() > 0) {
                    // אם יש משהו ב-back stack (לדוגמה, פראגמנטים אחרים)
                    fragmentManager.popBackStack();
                } else {
                    // אם אין פראגמנטים ב-back stack, אפשר את פעולת ה-back הרגילה
                    setEnabled(false);
                    MarketProfileActivity.super.onBackPressed();
                }
            }
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
                Log.d("MarketProfileActivity", "Server Response for Market Profile: " + response);

                JSONObject json = new JSONObject(response);

                String name = json.optString("location", location);
                String hours = json.optString("hours", "09:00 - 14:00");
                String founderName = json.optString("founderName", null);
                String founderEmail = json.optString("founderEmail", null);
                marketId = json.optString("id", null);

                // ⭐ קבלת מערך החקלאים המשתתפים (עם מוצריהם) ⭐
                JSONArray participatingFarmersArray = json.optJSONArray("participatingFarmers");
                // ⭐ קבלת מערך מוצרי השוק (עם פרטי החקלאי המציע) ⭐
                JSONArray marketProductsArray = json.optJSONArray("marketProducts");

                // משתנה לקביעת נראות ה-FAB
                boolean isUserParticipating = false;

                // 1. בדוק אם המשתמש הוא המייסד של השוק
                if (userEmail != null && founderEmail != null && userEmail.equals(founderEmail)) {
                    isUserParticipating = true;
                    Log.d("FAB_VISIBILITY", "User is founder. FAB should be visible.");
                }

                // 2. בדוק אם המשתמש הוא חקלאי משתתף דרך קשר 'INVITE' או 'WILL_BE'
                //    זה אומר שהשרת כבר אישר את השתתפותו.
                if (!isUserParticipating && participatingFarmersArray != null) {
                    for (int i = 0; i < participatingFarmersArray.length(); i++) {
                        JSONObject farmerObj = participatingFarmersArray.getJSONObject(i);
                        if (userEmail != null && userEmail.equals(farmerObj.optString("email"))) {
                            isUserParticipating = true;
                            Log.d("FAB_VISIBILITY", "User is a participating farmer. FAB should be visible.");
                            break;
                        }
                    }
                }

                final boolean finalIsUserParticipating = isUserParticipating;

                runOnUiThread(() -> {
                    marketName.setText(name);
                    marketHours.setText("🕒 שעות: " + hours);

                    // ניהול נראות כפתור "ניהול שוק" (למייסד בלבד)
                    if (userEmail != null && founderEmail != null && userEmail.equals(founderEmail)) {
                        manageMarketButton.setVisibility(View.VISIBLE);
                        manageMarketButton.setEnabled(true);
                    } else {
                        manageMarketButton.setVisibility(View.GONE);
                        manageMarketButton.setEnabled(false);
                    }

                    // ניהול נראות כפתור הוספת מוצר (למייסד או חקלאי משתתף)
                    Log.d("FAB_DEBUG", "Final Is User Participating: " + finalIsUserParticipating + ", marketId: " + marketId);
                    if (finalIsUserParticipating && marketId != null && !marketId.isEmpty()) {
                        fabAddProduct.setVisibility(View.VISIBLE);
                        Log.d("FAB_DEBUG", "FAB set to VISIBLE.");
                    } else {
                        fabAddProduct.setVisibility(View.GONE);
                        Log.d("FAB_DEBUG", "FAB set to GONE.");
                    }

                    // ⭐ הצגת חקלאים משתתפים ⭐
                    farmersListContainer.removeAllViews();
                    boolean atLeastOneFarmerDisplayed = false;

                    // הצג מייסד (אם יש)
                    if (founderName != null && !founderName.isEmpty()) {
                        addFarmerToDisplay(founderName, founderEmail, null, true, null);
                        atLeastOneFarmerDisplayed = true;
                    }

                    // הצג חקלאים משתתפים אחרים
                    if (participatingFarmersArray != null && participatingFarmersArray.length() > 0) {
                        for (int i = 0; i < participatingFarmersArray.length(); i++) {
                            try {
                                JSONObject farmerObj = participatingFarmersArray.getJSONObject(i);
                                String farmerName = farmerObj.optString("name");
                                String farmerEmailInMarket = farmerObj.optString("email");
                                JSONArray productsArray = farmerObj.optJSONArray("products");

                                // וודא שהמייסד לא מוצג שוב אם הוא גם ברשימת ה-INVITE
                                if (founderEmail != null && founderEmail.equals(farmerEmailInMarket)) {
                                    continue; // אם ה-founder כבר טופל בנפרד, דלג עליו כאן
                                }
                                addFarmerToDisplay(farmerName, farmerEmailInMarket, productsArray, false, null);
                                atLeastOneFarmerDisplayed = true;

                            } catch (JSONException e) {
                                Log.e("MarketProfileActivity", "Error parsing farmer object in array: " + e.getMessage(), e);
                            }
                        }
                    }

                    if (!atLeastOneFarmerDisplayed) {
                        TextView noFarmers = new TextView(MarketProfileActivity.this);
                        noFarmers.setText("אין חקלאים משתתפים כרגע.");
                        noFarmers.setTextSize(16);
                        noFarmers.setPadding(0, 4, 0, 4);
                        noFarmers.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        farmersListContainer.addView(noFarmers);
                    }

                    // ⭐ הצגת מוצרי השוק ⭐
                    marketProductsListContainer.removeAllViews();
                    if (marketProductsArray != null && marketProductsArray.length() > 0) {
                        TextView marketProductsTitle = new TextView(MarketProfileActivity.this);
                        marketProductsTitle.setText("מוצרים המוצעים בשוק:");
                        marketProductsTitle.setTextSize(16);
                        marketProductsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                        marketProductsTitle.setPadding(0, 16, 0, 8);
                        marketProductsListContainer.addView(marketProductsTitle);

                        for (int i = 0; i < marketProductsArray.length(); i++) {
                            try {
                                JSONObject productObj = marketProductsArray.getJSONObject(i);
                                String productName = productObj.optString("name", "מוצר ללא שם");
                                double productPrice = productObj.optDouble("price", 0.0);
                                String offeringFarmerName = productObj.optString("offeringFarmerName", "לא ידוע");

                                TextView productTv = new TextView(MarketProfileActivity.this);
                                String productText = "• " + productName + " (" + String.format("%.2f", productPrice) + " ₪) מבית " + offeringFarmerName;
                                productTv.setText(productText);
                                productTv.setTextSize(15);
                                productTv.setPadding(0, 4, 0, 4);
                                productTv.setTextColor(getResources().getColor(android.R.color.black));
                                marketProductsListContainer.addView(productTv);

                            } catch (JSONException e) {
                                Log.e("MarketProfileActivity", "Error parsing market product object: " + e.getMessage(), e);
                            }
                        }
                    } else {
                        TextView noMarketProducts = new TextView(MarketProfileActivity.this);
                        noMarketProducts.setText("אין מוצרים המוצעים ישירות מהשוק.");
                        noMarketProducts.setTextSize(15);
                        noMarketProducts.setPadding(0, 4, 0, 4);
                        noMarketProducts.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        marketProductsListContainer.addView(noMarketProducts);
                    }
                });

            } catch (IOException e) {
                Log.e("MarketProfileActivity", "Network error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: בעיית רשת. נסה שוב.", Toast.LENGTH_LONG).show();
                    // הסתר את ה-FAB במקרה של שגיאה
                    fabAddProduct.setVisibility(View.GONE);
                });
            } catch (JSONException e) {
                Log.e("MarketProfileActivity", "JSON parsing error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: פורמט נתונים שגוי.", Toast.LENGTH_LONG).show();
                    // הסתר את ה-FAB במקרה של שגיאה
                    fabAddProduct.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void addFarmerToDisplay(String farmerName, String farmerEmail, @Nullable JSONArray productsArray, boolean isFounder, @Nullable Map<String, Double> productsFromFounderCollection) {
        CardView farmerCard = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        farmerCard.setLayoutParams(cardParams);
        farmerCard.setCardElevation(4f);
        farmerCard.setRadius(8f);
        farmerCard.setContentPadding(16, 16, 16, 16);

        LinearLayout cardContentLayout = new LinearLayout(this);
        cardContentLayout.setOrientation(LinearLayout.VERTICAL);
        cardContentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView farmerNameTv = new TextView(this);
        String displayName = "• " + farmerName;
        if (isFounder) {
            displayName += " (מייסד)";
            farmerNameTv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            farmerNameTv.setTextSize(18);
            farmerNameTv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            farmerNameTv.setTextColor(getResources().getColor(android.R.color.black));
            farmerNameTv.setTextSize(16);
        }
        farmerNameTv.setText(displayName);
        farmerNameTv.setPadding(0, 0, 0, 4);
        cardContentLayout.addView(farmerNameTv);

        TextView productsTitle = new TextView(this);
        productsTitle.setText("מוצרים המוצעים על ידו:");
        productsTitle.setTextSize(14);
        productsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        productsTitle.setPadding(0, 8, 0, 4);
        cardContentLayout.addView(productsTitle);

        LinearLayout farmerProductsLayout = new LinearLayout(this);
        farmerProductsLayout.setOrientation(LinearLayout.VERTICAL);
        farmerProductsLayout.setPadding(16, 0, 0, 0);
        cardContentLayout.addView(farmerProductsLayout);

        if (productsArray != null && productsArray.length() > 0) {
            for (int i = 0; i < productsArray.length(); i++) {
                try {
                    JSONObject productObj = productsArray.getJSONObject(i);
                    String productName = productObj.optString("name", "מוצר ללא שם");
                    double productPrice = productObj.optDouble("marketPrice", 0.0);

                    TextView productTv = new TextView(this);
                    productTv.setText("  - " + productName + " (" + String.format("%.2f", productPrice) + " ₪)");
                    productTv.setTextSize(14);
                    productTv.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    farmerProductsLayout.addView(productTv);
                } catch (JSONException e) {
                    Log.e("MarketProfileActivity", "Error parsing product object for farmer: " + e.getMessage());
                }
            }
        } else {
            TextView noProductsTv = new TextView(this);
            noProductsTv.setText("  - אין מוצרים מוצעים על ידו בשוק זה.");
            noProductsTv.setTextSize(14);
            noProductsTv.setTextColor(getResources().getColor(android.R.color.darker_gray));
            farmerProductsLayout.addView(noProductsTv);
        }

        farmerCard.addView(cardContentLayout);
        farmersListContainer.addView(farmerCard);
    }

    private void showAddProductToMarketDialog(String farmerEmail, String marketId) {
        if (farmerEmail == null || marketId == null || marketId.isEmpty()) {
            Toast.makeText(this, "שגיאה: חסרים פרטים להוספת מוצר.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String response = Service.getFarmerItems(farmerEmail);
                JSONArray productsJsonArray = new JSONArray(response);

                List<Item> farmerProducts = new ArrayList<>();
                Map<String, Double> itemPricesMap = new HashMap<>();

                for (int i = 0; i < productsJsonArray.length(); i++) {
                    JSONObject productObj = productsJsonArray.getJSONObject(i);
                    String productName = productObj.optString("name");
                    String productDescription = productObj.optString("description");
                    double productPrice = productObj.optDouble("price", 0.0);

                    Item item = new Item(productName, productDescription);
                    farmerProducts.add(item);
                    itemPricesMap.put(productName, productPrice);
                }

                runOnUiThread(() -> {
                    if (farmerProducts.isEmpty()) {
                        Toast.makeText(MarketProfileActivity.this, "אין לך מוצרים זמינים להוספה. וודא שהוספת מוצרים לפרופיל האישי שלך.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    SelectProductForMarketDialogFragment dialog =
                            SelectProductForMarketDialogFragment.newInstance(farmerProducts, itemPricesMap);

                    dialog.setOnProductSelectedListener((selectedItem, marketPrice) -> {
                        if (selectedItem != null) {
                            addProductToMarket(farmerEmail, marketId, selectedItem.getName(), marketPrice);
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "SelectProductDialog");
                });

            } catch (IOException | JSONException e) {
                Log.e("MarketProfileActivity", "Error fetching farmer's offered products: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת המוצרים: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void addProductToMarket(String farmerEmail, String marketId, String itemName, double price) {
        new Thread(() -> {
            try {
                String response = Service.addProductToMarketWithWillBe(farmerEmail, marketId, itemName, price);
                Log.d("MarketProfileActivity", "Add product to market with WILL_BE response: " + response);

                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "המוצר נוסף בהצלחה לשוק!", Toast.LENGTH_SHORT).show();
                    loadMarketProfile();
                });

            } catch (IOException | JSONException e) {
                Log.e("MarketProfileActivity", "Error adding product to market with WILL_BE: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בהוספת מוצר לשוק: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}