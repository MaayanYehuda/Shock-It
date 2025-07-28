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
import com.example.shock_it.ui.map.farmerProfile.farmerProfile;
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
    // LinearLayout marketProductsListContainer; // הצהרה על הקונטיינר הישן - הוסר!
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

        // --- NEW: Initialize views first, BEFORE handling intent data ---
        // This ensures marketLocation and marketDate TextViews exist when handleIntent tries to update them.
        marketProfileContentScrollView = findViewById(R.id.market_profile_content_scroll_view);
        marketImage = findViewById(R.id.marketImage);
        marketName = findViewById(R.id.marketName);
        marketLocation = findViewById(R.id.marketLocation);
        marketHours = findViewById(R.id.marketHours);
        marketDate = findViewById(R.id.marketDate);
        farmersListContainer = findViewById(R.id.farmersList);
        backToMainButton = findViewById(R.id.backToMainButton);
        navigateButton = findViewById(R.id.navigateButton);
        manageMarketButton = findViewById(R.id.manageMarketButton);
        fabAddProduct = findViewById(R.id.fab_add_product);

        // --- IMPORTANT: Now, process the intent ONLY ONCE ---
        processIntentAndLoadMarket(getIntent());

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);
        Log.d("MarketProfileActivity", "Logged-in user email: " + userEmail);

        // No need to set marketLocation/Date text here anymore, handleIntent will do it.
        // No need for a redundant loadMarketProfile() call here.

        manageMarketButton.setVisibility(View.GONE);
        manageMarketButton.setEnabled(false);
        fabAddProduct.setVisibility(View.GONE); // Initial visibility, handleIntent will update it.

        backToMainButton.setOnClickListener(v -> {
            Intent backIntent;
            if (userEmail == null || userEmail.isEmpty()) {
                backIntent = new Intent(MarketProfileActivity.this, MainActivity.class);
            } else {
                backIntent = new Intent(MarketProfileActivity.this, FarmerHomeActivity.class);
            }
            // Add these flags to ensure the target activity is brought to front
            // and existing activities in the stack are handled correctly.
            backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(backIntent);
            finish();
        });

        navigateButton.setOnClickListener(v -> {
            openWazeNavigation("32.0853,34.7818");
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
            fragmentTransaction.addToBackStack("manageMarket"); // Give it a specific tag for better control
            fragmentTransaction.commit();
        });

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

                // 1. Check if ManageMarketFragment is visible
                if (findViewById(R.id.fragment_container_manage_market).getVisibility() == View.VISIBLE) {
                    fragmentManager.popBackStack("manageMarket", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    marketProfileContentScrollView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_manage_market).setVisibility(View.GONE);
                    fabAddProduct.setVisibility(View.VISIBLE);
                    loadMarketProfile(); // Re-load to refresh if manageMarket might change data
                }
                // 2. Check if FarmerProfileFragment is visible
                else if (findViewById(R.id.fragment_container_farmer_profile).getVisibility() == View.VISIBLE) {
                    // אם הפראגמנט גלוי, פשוט הסתר אותו והצג שוב את תוכן השוק הנוכחי
                    fragmentManager.popBackStack("farmerProfile", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    marketProfileContentScrollView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_farmer_profile).setVisibility(View.GONE);
                    loadMarketProfile(); // Re-load the current market profile to refresh the UI
                }
                else {
                    setEnabled(false); // Disable this callback
                    MarketProfileActivity.super.onBackPressed(); // Let the system handle back press
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // IMPORTANT: Update the activity's current intent
        processIntentAndLoadMarket(intent); // Process the new intent
    }

    // --- NEW METHOD: processIntentAndLoadMarket to centralize Intent processing and loading ---
    private void processIntentAndLoadMarket(Intent intent) {
        if (intent != null) {
            location = intent.getStringExtra("location");
            date = intent.getStringExtra("date");
            Log.d("MarketProfileActivity", "Processing Intent. Location: " + location + ", Date: " + date);


            clearFragmentContainers();
            // Update UI fields if they are initialized
            if (marketLocation != null) { // Check for null to avoid NullPointerException on first onCreate call
                marketLocation.setText("📍 מיקום: " + location);
            }
            if (marketDate != null) {
                marketDate.setText("📅 תאריך: " + date);
            }

            // This is the ONLY place loadMarketProfile() should be called based on new intent data
            if (location != null && date != null) {
                loadMarketProfile();
            } else {
                Log.e("MarketProfileActivity", "Location or Date is null in Intent. Cannot load market profile.");
                Toast.makeText(this, "שגיאה בנתוני השוק. לא ניתן לטעון.", Toast.LENGTH_LONG).show();
                // Optionally navigate back or show an error state
            }
        } else {
            Log.e("MarketProfileActivity", "processIntentAndLoadMarket received a null intent.");
            Toast.makeText(this, "שגיאה פנימית. נתונים חסרים.", Toast.LENGTH_LONG).show();
            // Optionally navigate back or show an error state
        }
    }

    private void clearFragmentContainers() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // הסר את farmerProfile fragment אם הוא קיים
        if (findViewById(R.id.fragment_container_farmer_profile).getVisibility() == View.VISIBLE) {
            farmerProfile farmerFrag = (farmerProfile) fragmentManager.findFragmentById(R.id.fragment_container_farmer_profile);
            if (farmerFrag != null) {
                fragmentTransaction.remove(farmerFrag);
            }
            findViewById(R.id.fragment_container_farmer_profile).setVisibility(View.GONE);
        }

        if (findViewById(R.id.fragment_container_manage_market).getVisibility() == View.VISIBLE) {
            ManageMarketFragment manageFrag = (ManageMarketFragment) fragmentManager.findFragmentById(R.id.fragment_container_manage_market);
            if (manageFrag != null) {
                fragmentTransaction.remove(manageFrag);
            }
            findViewById(R.id.fragment_container_manage_market).setVisibility(View.GONE);
        }

        // הפוך את marketProfileContentScrollView לגלוי שוב
        marketProfileContentScrollView.setVisibility(View.VISIBLE);

        // בצע את השינויים
        fragmentTransaction.commitAllowingStateLoss(); // השתמש ב-commitAllowingStateLoss אם אתה קורא לזה לאחר onSaveInstanceState
        fragmentManager.executePendingTransactions(); // ודא שהשינויים בוצעו מיד
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

                JSONArray participatingFarmersArray = json.optJSONArray("participatingFarmers");
                JSONArray marketProductsArray = json.optJSONArray("marketProducts");

                // --- לוגיקה חדשה: איסוף מוצרים לפי חקלאי ---
                // מפה שתחזיק רשימת מוצרים לכל חקלאי לפי אימייל
                Map<String, List<JSONObject>> farmerProductsMap = new HashMap<>();
                if (marketProductsArray != null) {
                    for (int i = 0; i < marketProductsArray.length(); i++) {
                        JSONObject productObj = marketProductsArray.getJSONObject(i);
                        String offeringFarmerEmail = productObj.optString("offeringFarmerEmail");
                        if (offeringFarmerEmail != null && !offeringFarmerEmail.isEmpty()) {
                            if (!farmerProductsMap.containsKey(offeringFarmerEmail)) {
                                farmerProductsMap.put(offeringFarmerEmail, new ArrayList<>());
                            }
                            farmerProductsMap.get(offeringFarmerEmail).add(productObj);
                        }
                    }
                }
                // --- סוף לוגיקת איסוף מוצרים ---


                boolean isUserParticipating = false;

                // 1. בדוק אם המשתמש הוא המייסד של השוק
                if (userEmail != null && founderEmail != null && userEmail.equals(founderEmail)) {
                    isUserParticipating = true;
                    Log.d("FAB_VISIBILITY", "User is founder. FAB should be visible.");
                }

                // 2. בדוק אם המשתמש הוא חקלאי משתתף דרך קשר 'INVITE' (או אם המוצרים שלו כבר בשוק)
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
                // בנוסף, אם למשתמש יש כבר מוצרים בשוק (כלומר, הוא הציע אותם והם ב-marketProductsArray), הוא נחשב משתתף
                if (!isUserParticipating && userEmail != null && farmerProductsMap.containsKey(userEmail)) {
                    isUserParticipating = true;
                    Log.d("FAB_VISIBILITY", "User has products in marketProductsArray. FAB should be visible.");
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
                        // הוסף את מוצרי המייסד מתוך המפה
                        JSONArray founderProductsJsonArray = null;
                        if (farmerProductsMap.containsKey(founderEmail)) {
                            founderProductsJsonArray = new JSONArray(farmerProductsMap.get(founderEmail));
                        }
                        addFarmerToDisplay(founderName, founderEmail, founderProductsJsonArray, true);
                        atLeastOneFarmerDisplayed = true;
                    }

                    // הצג חקלאים משתתפים אחרים (שאינם המייסד)
                    if (participatingFarmersArray != null && participatingFarmersArray.length() > 0) {
                        for (int i = 0; i < participatingFarmersArray.length(); i++) {
                            try {
                                JSONObject farmerObj = participatingFarmersArray.getJSONObject(i);
                                String farmerName = farmerObj.optString("name");
                                String farmerEmailInMarket = farmerObj.optString("email");

                                // וודא שהמייסד לא מוצג שוב אם הוא גם ברשימת ה-INVITE
                                if (founderEmail != null && founderEmail.equals(farmerEmailInMarket)) {
                                    continue; // אם ה-founder כבר טופל בנפרד, דלג עליו כאן
                                }

                                // קח את רשימת המוצרים הספציפית לחקלאי הזה מהמפה
                                JSONArray farmerSpecificProductsArray = null;
                                if (farmerProductsMap.containsKey(farmerEmailInMarket)) {
                                    farmerSpecificProductsArray = new JSONArray(farmerProductsMap.get(farmerEmailInMarket));
                                }

                                addFarmerToDisplay(farmerName, farmerEmailInMarket, farmerSpecificProductsArray, false);
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

                });

            } catch (IOException e) {
                Log.e("MarketProfileActivity", "Network error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: בעיית רשת. נסה שוב.", Toast.LENGTH_LONG).show();
                    fabAddProduct.setVisibility(View.GONE);
                });
            } catch (JSONException e) {
                Log.e("MarketProfileActivity", "JSON parsing error loading market profile: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בטעינת פרופיל השוק: פורמט נתונים שגוי.", Toast.LENGTH_LONG).show();
                    fabAddProduct.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    // הוסר הפרמטר האחרון 'productsFromFounderCollection' מכיוון שהוא לא נחוץ יותר
    private void addFarmerToDisplay(String farmerName, String farmerEmail, @Nullable JSONArray productsArray, boolean isFounder) {
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

        // --- הוספה חדשה כאן: הפיכת ה-TextView ללחיץ ---
        farmerNameTv.setClickable(true); // הופך את ה-TextView ללחיץ
        farmerNameTv.setFocusable(true); // הופך את ה-TextView לבר-מיקוד
        // אפשר גם להוסיף רקע מוגדר ללחיצה אם תרצה אפקט ויזואלי (ripple effect)
        // farmerNameTv.setBackgroundResource(android.R.drawable.selectable_item_background);

        final String finalFarmerEmail = farmerEmail; // וודא שהמשתנה יעיל לשימוש בתוך ה-OnClickListener
        farmerNameTv.setOnClickListener(v -> {
            if (finalFarmerEmail != null && !finalFarmerEmail.isEmpty()) {
                Log.d("MarketProfileActivity", "Clicked on farmer: " + finalFarmerEmail);
                // קריאה לפונקציה החדשה שתטען את פרופיל החקלאי
                navigateToFarmerProfile(finalFarmerEmail);
            } else {
                Toast.makeText(this, "שגיאה: מייל החקלאי לא זמין.", Toast.LENGTH_SHORT).show();
            }
        });
        // --- סוף הוספה חדשה ---

        cardContentLayout.addView(farmerNameTv);

        LinearLayout farmerProductsLayout = new LinearLayout(this);
        farmerProductsLayout.setOrientation(LinearLayout.VERTICAL);
        farmerProductsLayout.setPadding(16, 0, 0, 0); // הזחה קלה
        cardContentLayout.addView(farmerProductsLayout);

        // ... (שאר הקוד של הצגת המוצרים נשאר ללא שינוי) ...

        if (productsArray != null && productsArray.length() > 0) {
            TextView productsTitle = new TextView(this);
            productsTitle.setText("מוצרים המוצעים על ידו:");
            productsTitle.setTextSize(14);
            productsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            productsTitle.setPadding(0, 8, 0, 4);
            farmerProductsLayout.addView(productsTitle);

            for (int i = 0; i < productsArray.length(); i++) {
                try {
                    JSONObject productObj = productsArray.getJSONObject(i);
                    String productName = productObj.optString("name", "מוצר ללא שם");
                    double productPrice = productObj.optDouble("price", 0.0);

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
    // In your MarketProfileActivity.java

    private void navigateToFarmerProfile(String farmerEmail) {
        farmerProfile farmerProfileFragment = new farmerProfile();
        Bundle args = new Bundle();
        args.putString("farmer_email_key", farmerEmail);
        farmerProfileFragment.setArguments(args);

        marketProfileContentScrollView.setVisibility(View.GONE);
        findViewById(R.id.fragment_container_farmer_profile).setVisibility(View.VISIBLE);

        // --- HIDE THE FAB HERE ---
        fabAddProduct.setVisibility(View.GONE); // <-- ADD THIS LINE

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container_farmer_profile, farmerProfileFragment);
        fragmentTransaction.addToBackStack("farmerProfile");
        fragmentTransaction.commit();

        Toast.makeText(this, "טוען פרופיל של: " + farmerEmail, Toast.LENGTH_SHORT).show();
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