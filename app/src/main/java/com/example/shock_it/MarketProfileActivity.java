package com.example.shock_it;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.content.res.ColorStateList;

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

        processIntentAndLoadMarket(getIntent());

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);
        Log.d("MarketProfileActivity", "Logged-in user email: " + userEmail);

        manageMarketButton.setVisibility(View.GONE);
        manageMarketButton.setEnabled(false);
        fabAddProduct.setVisibility(View.GONE);

        backToMainButton.setOnClickListener(v -> {
            Intent backIntent;

            if (userEmail == null || userEmail.isEmpty()) {
                backIntent = new Intent(MarketProfileActivity.this, MainActivity.class);
            } else {
                backIntent = new Intent(MarketProfileActivity.this, FarmerHomeActivity.class);
            }

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
            fragmentTransaction.addToBackStack("manageMarket");
            fragmentTransaction.commit();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();

                if (findViewById(R.id.fragment_container_manage_market).getVisibility() == View.VISIBLE) {
                    fragmentManager.popBackStack("manageMarket", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    marketProfileContentScrollView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_manage_market).setVisibility(View.GONE);
                    fabAddProduct.setVisibility(View.VISIBLE);
                    loadMarketProfile();
                } else if (findViewById(R.id.fragment_container_farmer_profile).getVisibility() == View.VISIBLE) {
                    fragmentManager.popBackStack("farmerProfile", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    marketProfileContentScrollView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_farmer_profile).setVisibility(View.GONE);
                    loadMarketProfile();
                } else {
                    setEnabled(false);
                    MarketProfileActivity.super.onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntentAndLoadMarket(intent);
    }

    private void processIntentAndLoadMarket(Intent intent) {
        if (intent != null) {
            location = intent.getStringExtra("location");
            date = intent.getStringExtra("date");
            Log.d("MarketProfileActivity", "Processing Intent. Location: " + location + ", Date: " + date);

            clearFragmentContainers();
            if (marketLocation != null) {
                marketLocation.setText("📍 מיקום: " + location);
            }
            if (marketDate != null) {
                marketDate.setText("� תאריך: " + date);
            }

            if (location != null && date != null) {
                loadMarketProfile();
            } else {
                Log.e("MarketProfileActivity", "Location or Date is null in Intent. Cannot load market profile.");
                Toast.makeText(this, "שגיאה בנתוני השוק. לא ניתן לטעון.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e("MarketProfileActivity", "processIntentAndLoadMarket received a null intent.");
            Toast.makeText(this, "שגיאה פנימית. נתונים חסרים.", Toast.LENGTH_LONG).show();
        }
    }

    private void clearFragmentContainers() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

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

        marketProfileContentScrollView.setVisibility(View.VISIBLE);

        fragmentTransaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
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
                JSONArray invitedFarmersArray = json.optJSONArray("invitedFarmers");
                JSONArray pendingRequestsArray = json.optJSONArray("pendingRequests");
                JSONArray marketProductsArray = json.optJSONArray("marketProducts");

                // --- שינויים עיקריים כאן לצורך דיבוג ---
                Log.d("MarketProfileActivity_DEBUG", "-------------------- Server Arrays Content --------------------");
                Log.d("MarketProfileActivity_DEBUG", "Participating Farmers: " + (participatingFarmersArray != null ? participatingFarmersArray.toString() : "null"));
                Log.d("MarketProfileActivity_DEBUG", "Invited Farmers: " + (invitedFarmersArray != null ? invitedFarmersArray.toString() : "null"));
                Log.d("MarketProfileActivity_DEBUG", "Pending Requests: " + (pendingRequestsArray != null ? pendingRequestsArray.toString() : "null"));
                Log.d("MarketProfileActivity_DEBUG", "-------------------------------------------------------------");

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

                boolean isUserFounder = (userEmail != null && founderEmail != null && userEmail.equals(founderEmail));
                boolean isUserParticipating = isUserFounder;
                boolean isUserInvited = false;
                boolean isUserRequestPending = false;


                // בדיקה האם המשתמש כבר משתתף
                if (!isUserParticipating && participatingFarmersArray != null) {
                    for (int i = 0; i < participatingFarmersArray.length(); i++) {
                        JSONObject farmerObj = participatingFarmersArray.getJSONObject(i);
                        if (userEmail != null && userEmail.equals(farmerObj.optString("email"))) {
                            isUserParticipating = true;
                            break;
                        }
                    }
                }

                // בדיקה האם המשתמש מוזמן
                if (!isUserParticipating && invitedFarmersArray != null) {
                    for (int i = 0; i < invitedFarmersArray.length(); i++) {
                        JSONObject invitedObj = invitedFarmersArray.getJSONObject(i);
                        if (userEmail != null && userEmail.equals(invitedObj.optString("email"))) {
                            isUserInvited = true;
                            break;
                        }
                    }
                }

                // בדיקה האם למשתמש יש בקשה ממתינה
                if (!isUserParticipating && !isUserInvited && pendingRequestsArray != null) {
                    for (int i = 0; i < pendingRequestsArray.length(); i++) {
                        try {
                            JSONObject requestObj = pendingRequestsArray.getJSONObject(i);
                            String requestedFarmerEmail = requestObj.optString("email");
                            if (userEmail != null && userEmail.equals(requestedFarmerEmail)) {
                                isUserRequestPending = true;
                                break;
                            }
                        } catch (JSONException e) {
                            Log.e("MarketProfileActivity", "Error parsing pending request object: " + e.getMessage(), e);
                        }
                    }
                }

                final boolean finalIsUserParticipating = isUserParticipating;
                final boolean finalIsUserFounder = isUserFounder;
                final boolean finalIsUserInvited = isUserInvited;
                final boolean finalIsUserRequestPending = isUserRequestPending;

                // --- לוג נוסף לצורך דיבוג ---
                Log.d("MarketProfileActivity_DEBUG", "isUserParticipating: " + finalIsUserParticipating);
                Log.d("MarketProfileActivity_DEBUG", "isUserInvited: " + finalIsUserInvited);
                Log.d("MarketProfileActivity_DEBUG", "isUserRequestPending: " + finalIsUserRequestPending);
                // -----------------------------

                runOnUiThread(() -> {
                    marketName.setText(name);
                    marketHours.setText("🕒 שעות: " + hours);

                    // לוגיקה לכפתור 'ניהול השוק' - מוצג רק למייסד
                    if (finalIsUserFounder) {
                        manageMarketButton.setVisibility(View.VISIBLE);
                        manageMarketButton.setEnabled(true);
                    } else {
                        manageMarketButton.setVisibility(View.GONE);
                        manageMarketButton.setEnabled(false);
                    }

                    // קריאה למתודה החדשה שתטפל רק במצב הכפתור הצף
                    updateFloatingActionButtonState(finalIsUserParticipating, finalIsUserInvited, finalIsUserRequestPending);

                    // שאר הקוד של טעינת החקלאים בפרופיל נשאר ללא שינוי
                    farmersListContainer.removeAllViews();
                    boolean atLeastOneFarmerDisplayed = false;

                    if (founderName != null && !founderName.isEmpty()) {
                        JSONArray founderProductsJsonArray = null;
                        if (farmerProductsMap.containsKey(founderEmail)) {
                            founderProductsJsonArray = new JSONArray(farmerProductsMap.get(founderEmail));
                        }
                        addFarmerToDisplay(founderName, founderEmail, founderProductsJsonArray, true);
                        atLeastOneFarmerDisplayed = true;
                    }

                    if (participatingFarmersArray != null && participatingFarmersArray.length() > 0) {
                        for (int i = 0; i < participatingFarmersArray.length(); i++) {
                            try {
                                JSONObject farmerObj = participatingFarmersArray.getJSONObject(i);
                                String farmerName = farmerObj.optString("name");
                                String farmerEmailInMarket = farmerObj.optString("email");

                                if (founderEmail != null && founderEmail.equals(farmerEmailInMarket)) {
                                    continue;
                                }

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

    private void updateFloatingActionButtonState(boolean isParticipating, boolean isInvited, boolean isRequestPending) {
        int activeColor = Color.parseColor("#42A5F5");
        int disabledColor = Color.parseColor("#BDBDBD");

        if (userEmail == null || marketId == null || marketId.isEmpty()) {
            fabAddProduct.setVisibility(View.GONE);
            return;
        }

        fabAddProduct.setVisibility(View.VISIBLE);
        fabAddProduct.setEnabled(true);
        // השתמש ב-ColorStateList כדי לצבוע את הכפתור בצבע הרגיל
        fabAddProduct.setBackgroundTintList(ColorStateList.valueOf(activeColor));

        if (isParticipating) {
            Log.d("FAB_LOGIC", "User is participating. Setting fab to ADD PRODUCT.");
            fabAddProduct.setImageResource(R.drawable.ic_add);
            fabAddProduct.setOnClickListener(v -> showAddProductToMarketDialog(userEmail, marketId, false));
        } else if (isRequestPending) {
            Log.d("FAB_LOGIC", "User has a pending join request. Disabling button.");
            fabAddProduct.setImageResource(R.drawable.ic_send);
            fabAddProduct.setEnabled(false); // הופך את הכפתור ללא פעיל
            // ⭐ תיקון: השתמש ב-ColorStateList עם Color.parseColor
            fabAddProduct.setBackgroundTintList(ColorStateList.valueOf(disabledColor));
            fabAddProduct.setOnClickListener(null); // חשוב: הסרת ה-OnClickListener
        } else if (isInvited) {
            Log.d("FAB_LOGIC", "User has an invite. Setting fab to ACCEPT INVITE.");
            fabAddProduct.setImageResource(R.drawable.ic_done);
            // כאן תוכל להוסיף OnClickListener שיטפל באישור ההזמנה, כפי שציינת
            // fabAddProduct.setOnClickListener(v -> handleInvitationAcceptance(userEmail, marketId));
        } else {
            Log.d("FAB_LOGIC", "User is not participating, invited, or has a request. Setting fab to JOIN REQUEST.");
            fabAddProduct.setImageResource(R.drawable.ic_send);
            fabAddProduct.setOnClickListener(v -> showAddProductToMarketDialog(userEmail, marketId, true));
        }
    }


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

        farmerNameTv.setClickable(true);
        farmerNameTv.setFocusable(true);

        final String finalFarmerEmail = farmerEmail;
        farmerNameTv.setOnClickListener(v -> {
            if (finalFarmerEmail != null && !finalFarmerEmail.isEmpty()) {
                Log.d("MarketProfileActivity", "Clicked on farmer: " + finalFarmerEmail);
                navigateToFarmerProfile(finalFarmerEmail);
            } else {
                Toast.makeText(this, "שגיאה: מייל החקלאי לא זמין.", Toast.LENGTH_SHORT).show();
            }
        });

        cardContentLayout.addView(farmerNameTv);

        LinearLayout farmerProductsLayout = new LinearLayout(this);
        farmerProductsLayout.setOrientation(LinearLayout.VERTICAL);
        farmerProductsLayout.setPadding(16, 0, 0, 0);
        cardContentLayout.addView(farmerProductsLayout);

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

    private void navigateToFarmerProfile(String farmerEmail) {
        farmerProfile farmerProfileFragment = new farmerProfile();
        Bundle args = new Bundle();
        args.putString("farmer_email_key", farmerEmail);
        farmerProfileFragment.setArguments(args);

        marketProfileContentScrollView.setVisibility(View.GONE);
        findViewById(R.id.fragment_container_farmer_profile).setVisibility(View.VISIBLE);

        fabAddProduct.setVisibility(View.GONE);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container_farmer_profile, farmerProfileFragment);
        fragmentTransaction.addToBackStack("farmerProfile");
        fragmentTransaction.commit();

        Toast.makeText(this, "טוען פרופיל של: " + farmerEmail, Toast.LENGTH_SHORT).show();
    }

    private void showAddProductToMarketDialog(String farmerEmail, String marketId, boolean isJoinRequest) {
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
                        Toast.makeText(MarketProfileActivity.this, "אין לך מוצרים זמינים להוסיף. וודא שהוספת מוצרים לפרופיל האישי שלך.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    SelectProductForMarketDialogFragment dialog =
                            SelectProductForMarketDialogFragment.newInstance(farmerProducts, itemPricesMap);

                    dialog.setOnProductSelectedListener((selectedItem, marketPrice) -> {
                        if (selectedItem != null) {
                            if (isJoinRequest) {
                                sendJoinRequestWithSelectedProduct(farmerEmail, marketId, selectedItem.getName(), marketPrice);
                            } else {
                                addProductToMarket(farmerEmail, marketId, selectedItem.getName(), marketPrice);
                            }
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

    private void sendJoinRequestWithSelectedProduct(String farmerEmail, String marketId, String itemName, double price) {
        new Thread(() -> {
            try {
                JSONObject productObject = new JSONObject();
                productObject.put("name", itemName);
                productObject.put("price", price);

                JSONArray productsArray = new JSONArray();
                productsArray.put(productObject);

                String response = Service.sendJoinRequestToMarket(marketId, farmerEmail, productsArray);
                Log.d("MarketProfileActivity", "Join request response: " + response);

                runOnUiThread(() -> {
                    if (response != null) {
                        Toast.makeText(MarketProfileActivity.this, "הבקשה נשלחה בהצלחה! ⭐", Toast.LENGTH_LONG).show();
                        loadMarketProfile();
                    } else {
                        Toast.makeText(MarketProfileActivity.this, "שגיאה בשליחת הבקשה. נסה שוב מאוחר יותר.", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (JSONException | IOException e) {
                Log.e("MarketProfileActivity", "Error sending join request: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MarketProfileActivity.this, "שגיאה בהכנת הבקשה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
