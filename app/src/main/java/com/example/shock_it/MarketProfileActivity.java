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
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import classes.Item;
import com.example.shock_it.MarketProfileMVP.MarketProfileContract;
import com.example.shock_it.MarketProfileMVP.MarketProfilePresenter;

import com.example.shock_it.utils.FarmerCardBuilder;

import com.example.shock_it.dialogs.PendingRequestsDialogFragment;


public class MarketProfileActivity extends AppCompatActivity implements MarketProfileContract.View {

    // רכיבי UI
    Button backToMainButton;
    Button navigateButton;
    Button manageMarketButton;
    Button viewRequestsButton;
    ImageView marketImage;
    TextView marketName, marketLocation, marketHours, marketDate;
    LinearLayout farmersListContainer;
    FloatingActionButton fabAddProduct;
    private View marketProfileContentScrollView;

    // נתוני שוק ומשתמש
    String location;
    String date;
    String marketId;
    String userEmail;

    // Presenter instance
    private MarketProfileContract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_profile);

        // אתחול רכיבי UI
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
        viewRequestsButton = findViewById(R.id.viewRequestsButton);
        fabAddProduct = findViewById(R.id.fab_add_product);

        // קבלת מייל המשתמש מה-SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);
        Log.d("MarketProfileActivity", "Logged-in user email: " + userEmail);

        // אתחול ה-Presenter
        presenter = new MarketProfilePresenter(userEmail, null); // marketId יועבר במתודות, לא צריך לאתחל כאן
        presenter.attachView(this);

        // טיפול ב-Intent וטעינת פרופיל השוק
        processIntentAndLoadMarket(getIntent());

        // הגדרות ראשוניות לכפתורים
        manageMarketButton.setVisibility(View.GONE);
        manageMarketButton.setEnabled(false);
        viewRequestsButton.setVisibility(View.GONE);
        viewRequestsButton.setEnabled(false);
        fabAddProduct.setVisibility(View.GONE);

        // Listeners
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

        viewRequestsButton.setOnClickListener(v -> {
            if (marketId == null || marketId.isEmpty()) {
                showToast("שגיאה: מזהה שוק אינו זמין.");
                return;
            }
            presenter.fetchPendingRequests(marketId);
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
                    presenter.loadMarketProfile(location, date, userEmail);
                } else if (findViewById(R.id.fragment_container_farmer_profile).getVisibility() == View.VISIBLE) {
                    fragmentManager.popBackStack("farmerProfile", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    marketProfileContentScrollView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_farmer_profile).setVisibility(View.GONE);
                    presenter.loadMarketProfile(location, date, userEmail);
                } else {
                    setEnabled(false);
                    MarketProfileActivity.super.onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
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

            Log.d("MarketProfileActivity", "Processing Intent. Location: " + location + ", Date: " + date); // ✅ הסר את ה-marketId מהלוג כאן

            clearFragmentContainers();
            if (marketLocation != null) {
                marketLocation.setText("📍 מיקום: " + location);
            }
            if (marketDate != null) {
                marketDate.setText("📅 תאריך: " + date);
            }

            if (location != null && date != null) {
                presenter.loadMarketProfile(location, date, userEmail);
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

    @Override
    public void showLoading() {
        runOnUiThread(() -> {
            Log.d("MarketProfileActivity", "Showing loading indicator...");
        });
    }

    @Override
    public void hideLoading() {
        runOnUiThread(() -> {
            Log.d("MarketProfileActivity", "Hiding loading indicator.");
        });
    }

    @Override
    public void displayMarketProfile(String name, String hours) {
        runOnUiThread(() -> {
            marketName.setText(name);
            marketHours.setText("🕒 שעות: " + hours);
        });
    }

    // ✅ שינוי: קבלת isUserFounder ישירות מה-Presenter
    @Override
    public void updateFabState(boolean isUserFounder, boolean isParticipating, boolean isInvited, boolean isRequestPending) {
        if(userEmail!=null){
        runOnUiThread(() -> {
            int activeColor = Color.parseColor("#42A5F5");
            int disabledColor = Color.parseColor("#BDBDBD");

            // ✅ ודא ש-marketId של ה-Activity מעודכן.
            // הוא אמור להגיע מה-Intent ב-processIntentAndLoadMarket.
            // אם הוא עדיין null/ריק, זה מצביע על בעיה באופן שבו ה-Activity נפתח.

            if (this.marketId == null || this.marketId.isEmpty()) {
                Log.e("MarketProfileActivity", "marketId is null or empty in updateFabState. Cannot determine button visibility.");
                fabAddProduct.setVisibility(View.GONE);
                viewRequestsButton.setVisibility(View.GONE);
                manageMarketButton.setVisibility(View.GONE); // ודא שגם זה מוסתר
                return;
            }

            // --- לוגיקה עבור כפתורי המייסד (manageMarketButton ו-viewRequestsButton) ---
            Log.d("ButtonVisibility", "updateFabState: isUserFounder = " + isUserFounder);
            if (isUserFounder) {
                // אם המשתמש הוא מייסד השוק
                manageMarketButton.setVisibility(View.VISIBLE);
                manageMarketButton.setEnabled(true);
                viewRequestsButton.setVisibility(View.VISIBLE);
                viewRequestsButton.setEnabled(true);
                Log.d("ButtonVisibility", "Founder buttons (Manage Market, View Requests) set VISIBLE and ENABLED.");
            } else {
                // אם המשתמש אינו מייסד השוק
                manageMarketButton.setVisibility(View.GONE);
                manageMarketButton.setEnabled(false);
                viewRequestsButton.setVisibility(View.GONE);
                viewRequestsButton.setEnabled(false);
                Log.d("ButtonVisibility", "Founder buttons (Manage Market, View Requests) set GONE and DISABLED.");
            }

            // --- לוגיקה עבור כפתור הוספת מוצר (fabAddProduct) ---
            Log.d("ButtonVisibility", "updateFabState: isParticipating = " + isParticipating +
                    ", isRequestPending = " + isRequestPending +
                    ", isInvited = " + isInvited);

            if (isParticipating) {
                // אם המשתמש משתתף בשוק (כולל המייסד, שכבר נחשב משתתף)
                fabAddProduct.setVisibility(View.VISIBLE);
                fabAddProduct.setEnabled(true);
                fabAddProduct.setBackgroundTintList(ColorStateList.valueOf(activeColor));
                fabAddProduct.setImageResource(R.drawable.ic_add); // אייקון להוספת מוצר
                fabAddProduct.setOnClickListener(v -> presenter.handleAddProductClick(userEmail, marketId, false));
                Log.d("ButtonVisibility", "FAB set to ADD PRODUCT (User is Participating).");
            } else if (isRequestPending) {
                // אם למשתמש יש בקשת הצטרפות ממתינה
                fabAddProduct.setVisibility(View.VISIBLE); // או GONE אם לא רוצים להציג כלל
                fabAddProduct.setEnabled(false); // לא ניתן ללחוץ שוב
                fabAddProduct.setBackgroundTintList(ColorStateList.valueOf(disabledColor)); // צבע אפור
                fabAddProduct.setImageResource(R.drawable.ic_send); // אייקון של שליחה
                fabAddProduct.setOnClickListener(null); // אין פעולה בלחיצה
                Log.d("ButtonVisibility", "FAB set to PENDING REQUEST (User has pending request).");
            } else if (isInvited) {
                // אם המשתמש הוזמן (אבל עוד לא אישר)
                fabAddProduct.setVisibility(View.VISIBLE);
                fabAddProduct.setEnabled(true);
                fabAddProduct.setBackgroundTintList(ColorStateList.valueOf(activeColor));
                fabAddProduct.setImageResource(R.drawable.ic_done); // אייקון של אישור
                // fabAddProduct.setOnClickListener(v -> presenter.handleInvitationAcceptance(userEmail, marketId)); // יש לממש
                Log.d("ButtonVisibility", "FAB set to ACCEPT INVITE (User is Invited).");
            } else {
                // אם המשתמש לא מייסד, לא משתתף, לא הוזמן ולא שלח בקשה
                fabAddProduct.setVisibility(View.VISIBLE); // או GONE אם לא רוצים להציג כלל
                fabAddProduct.setEnabled(true);
                fabAddProduct.setBackgroundTintList(ColorStateList.valueOf(activeColor));
                fabAddProduct.setImageResource(R.drawable.ic_send); // אייקון של שליחה
                fabAddProduct.setOnClickListener(v -> presenter.handleAddProductClick(userEmail, marketId, true)); // שלח בקשת הצטרפות
                Log.d("ButtonVisibility", "FAB set to SEND JOIN REQUEST (User is not involved).");
            }
        });
        }
    }

    @Override
    public void clearFarmersList() {
        runOnUiThread(() -> {
            farmersListContainer.removeAllViews();
        });
    }

    @Override
    public void addFarmerCard(String farmerName, String farmerEmail, @Nullable JSONArray productsArray, boolean isFounder) {
        runOnUiThread(() -> {
            FarmerCardBuilder.OnFarmerClickListener farmerClickListener = this::navigateToFarmerProfile;
            CardView farmerCard = FarmerCardBuilder.buildFarmerCard(
                    this,
                    farmerName,
                    farmerEmail,
                    productsArray,
                    isFounder,
                    farmerClickListener
            );
            farmersListContainer.addView(farmerCard);
        });
    }

    @Override
    public void showNoFarmersMessage() {
        runOnUiThread(() -> {
            TextView noFarmers = new TextView(this);
            noFarmers.setText("אין חקלאים משתתפים כרגע.");
            noFarmers.setTextSize(16);
            noFarmers.setPadding(0, 4, 0, 4);
            noFarmers.setTextColor(getResources().getColor(android.R.color.darker_gray));
            farmersListContainer.addView(noFarmers);
        });
    }

    @Override
    public void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showMarketNotFoundError() {
        runOnUiThread(() -> {
            Toast.makeText(this, "השוק לא נמצא.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showNetworkError() {
        runOnUiThread(() -> {
            Toast.makeText(this, "שגיאה בטעינת פרופיל השוק: בעיית רשת. נסה שוב.", Toast.LENGTH_LONG).show();
            fabAddProduct.setVisibility(View.GONE);
        });
    }

    @Override
    public void showJsonParsingError() {
        runOnUiThread(() -> {
            Toast.makeText(this, "שגיאה בטעינת פרופיל השוק: פורמט נתונים שגוי.", Toast.LENGTH_LONG).show();
            fabAddProduct.setVisibility(View.GONE);
        });
    }

    @Override
    public void showSelectProductDialog(List<Item> farmerProducts, Map<String, Double> itemPricesMap, boolean isJoinRequest) {
        runOnUiThread(() -> {
            SelectProductForMarketDialogFragment dialog =
                    SelectProductForMarketDialogFragment.newInstance(farmerProducts, itemPricesMap);

            // ✅ שינוי: ה-listener כעת מקבל List<JSONObject>
            dialog.setOnProductSelectedListener(selectedProducts -> {
                if (selectedProducts != null && !selectedProducts.isEmpty()) {
                    if (isJoinRequest) {
                        // ✅ עבור בקשת הצטרפות, שלח את כל הרשימה ל-Presenter
                        presenter.sendJoinRequest(userEmail, marketId, selectedProducts);
                    } else {
                        // ✅ עבור הוספת מוצר לשוק קיים, נצטרך לולאה אם רוצים להוסיף מספר מוצרים
                        // נניח ש-addProductToMarket ב-Presenter מטפלת במוצר בודד בכל קריאה
                        for (JSONObject productJson : selectedProducts) {
                            presenter.addProductToMarket(userEmail, marketId, productJson);
                        }
                    }
                } else {
                    Toast.makeText(this, "לא נבחרו מוצרים.", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show(getSupportFragmentManager(), "SelectProductDialog");
        });

    }

    @Override
    public void refreshMarketProfile() {
        runOnUiThread(() -> {
            presenter.loadMarketProfile(location, date, userEmail);
        });
    }

    @Override
    public void showPendingRequestsDialog(List<JSONObject> pendingRequests) {
        runOnUiThread(() -> {
            PendingRequestsDialogFragment dialog = PendingRequestsDialogFragment.newInstance(pendingRequests, marketId);
            dialog.setOnRequestActionListener(new PendingRequestsDialogFragment.OnRequestActionListener() {
                @Override
                public void onRequestApproved(String farmerEmail) {
                    presenter.approveJoinRequest(marketId, farmerEmail);
                }

                @Override
                public void onRequestDeclined(String farmerEmail) {
                    presenter.declineJoinRequest(marketId, farmerEmail);
                }
            });
            dialog.show(getSupportFragmentManager(), "PendingRequestsDialog");
        });
    }

    @Override
    public void setMarketId(String marketId) {
        runOnUiThread(() -> {
            this.marketId = marketId;
            Log.d("MarketIdDebug", "MarketProfileActivity - Market ID set by Presenter: " + this.marketId);
        });
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
}