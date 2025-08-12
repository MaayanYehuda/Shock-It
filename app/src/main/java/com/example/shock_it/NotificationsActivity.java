package com.example.shock_it;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.dialogs.SelectProductForMarketDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import classes.Item;
import services.Service;

public class NotificationsActivity extends AppCompatActivity implements RecomendedMarketAdapter.OnItemActionListener, SelectProductForMarketDialogFragment.OnProductsSelectedListener {

    private RecyclerView marketsRecyclerView;
    private TextView noItemsMessage;
    private TextView mainTitleTextView;
    private MaterialButton invitationsButton, recommendationsButton;
    private RecomendedMarketAdapter marketAdapter;
    private String userEmail;

    private static final int VIEW_TYPE_INVITATIONS = 0;
    private static final int VIEW_TYPE_RECOMMENDATIONS = 1;
    private int currentViewType = VIEW_TYPE_INVITATIONS;

    private String currentMarketId;
    private HashMap<String, String> currentMarketData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        marketsRecyclerView = findViewById(R.id.marketsRecyclerView);
        noItemsMessage = findViewById(R.id.noItemsMessage);
        mainTitleTextView = findViewById(R.id.mainTitleTextView);
        invitationsButton = findViewById(R.id.invitationsButton);
        recommendationsButton = findViewById(R.id.recommendationsButton);
        ImageButton backButton = findViewById(R.id.backButton);

        marketsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        marketAdapter = new RecomendedMarketAdapter(new ArrayList<>(), this, currentViewType);
        marketsRecyclerView.setAdapter(marketAdapter);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "שגיאה: אימייל המשתמש לא נמצא. אנא התחבר מחדש.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> onBackPressed());
        invitationsButton.setOnClickListener(v -> switchView(VIEW_TYPE_INVITATIONS));
        recommendationsButton.setOnClickListener(v -> switchView(VIEW_TYPE_RECOMMENDATIONS));

        switchView(VIEW_TYPE_INVITATIONS);
    }

    private void switchView(int viewType) {
        currentViewType = viewType;
        marketAdapter.setViewType(currentViewType);
        marketAdapter.setMarketList(new ArrayList<>());

        if (viewType == VIEW_TYPE_INVITATIONS) {
            mainTitleTextView.setText("הזמנות");
            invitationsButton.setEnabled(false);
            recommendationsButton.setEnabled(true);
            loadInvitations(userEmail);
        } else {
            mainTitleTextView.setText("שווקים מומלצים");
            invitationsButton.setEnabled(true);
            recommendationsButton.setEnabled(false);
            loadRecommendedMarkets(userEmail);
        }
    }

    private void loadInvitations(String email) {
        new Thread(() -> {
            try {
                String response = Service.getInvitations(email);
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray invitationsArray = jsonResponse.optJSONArray("invitations");
                List<HashMap<String, String>> markets = parseMarkets(invitationsArray);

                runOnUiThread(() -> {
                    if (markets.isEmpty()) {
                        noItemsMessage.setText("אין לך הזמנות חדשות כרגע.");
                        noItemsMessage.setVisibility(View.VISIBLE);
                        marketsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noItemsMessage.setVisibility(View.GONE);
                        marketsRecyclerView.setVisibility(View.VISIBLE);
                        marketAdapter.setMarketList(markets);
                    }
                });
            } catch (IOException e) {
                Log.e("MarketsActivity", "Network error loading invitations: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה בטעינת הזמנות: בעיית רשת", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("MarketsActivity", "Error parsing invitations or general error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה בטעינת הזמנות: נתונים שגויים או שגיאה כללית", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void loadRecommendedMarkets(String email) {
        new Thread(() -> {
            try {
                String response = Service.getRecomendedMarketsByYourRadius(email);
                JSONArray recommendationsArray = new JSONArray(response);
                List<HashMap<String, String>> markets = parseMarkets(recommendationsArray);

                runOnUiThread(() -> {
                    if (markets.isEmpty()) {
                        noItemsMessage.setText("לא נמצאו שווקים מומלצים בקרבתך.");
                        noItemsMessage.setVisibility(View.VISIBLE);
                        marketsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noItemsMessage.setVisibility(View.GONE);
                        marketsRecyclerView.setVisibility(View.VISIBLE);
                        marketAdapter.setMarketList(markets);
                    }
                });
            } catch (IOException e) {
                Log.e("MarketsActivity", "Network error loading recommendations: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה בטעינת המלצות: בעיית רשת", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("MarketsActivity", "Error parsing recommendations or general error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה בטעינת המלצות: נתונים שגויים", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // 🆕 תיקון קריטי: הפונקציה מנסה כעת למצוא את מזהה השוק גם תחת המפתח "marketId"
    private List<HashMap<String, String>> parseMarkets(JSONArray marketsArray) throws JSONException {
        List<HashMap<String, String>> markets = new ArrayList<>();
        if (marketsArray != null) {
            for (int i = 0; i < marketsArray.length(); i++) {
                JSONObject marketJson = marketsArray.getJSONObject(i);
                HashMap<String, String> marketData = new HashMap<>();

                // 🆕 שינוי כאן: קודם מנסים לקבל את מזהה השוק עם המפתח "marketId",
                // ואם הוא לא קיים, משתמשים במפתח "id" כברירת מחדל
                String marketId = marketJson.optString("marketId", marketJson.optString("id", ""));
                marketData.put("marketId", marketId);

                marketData.put("date", marketJson.optString("date"));
                marketData.put("location", marketJson.optString("location"));
                marketData.put("marketName", marketJson.optString("name", marketJson.optString("location")));
                markets.add(marketData);
            }
        }
        return markets;
    }

    @Override
    public void onAccept(HashMap<String, String> marketData) {
        String marketId = marketData.get("marketId");
        // � הוספנו בדיקה כדי למנוע שליחת בקשה עם מזהה ריק
        if (marketId == null || marketId.isEmpty()) {
            Toast.makeText(NotificationsActivity.this, "שגיאה: מזהה שוק ריק, לא ניתן לאשר.", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                String response = Service.acceptInvitation(userEmail, marketId);
                JSONObject jsonResponse = new JSONObject(response);
                boolean success = jsonResponse.optBoolean("success", false);
                String message = jsonResponse.optString("message", "פעולה הושלמה.");
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(NotificationsActivity.this, "הזמנה אושרה בהצלחה!", Toast.LENGTH_SHORT).show();
                        removeMarketFromList(marketData);
                    } else {
                        Toast.makeText(NotificationsActivity.this, "שגיאה באישור ההזמנה: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("MarketsActivity", "Error accepting invitation: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה כללית באישור ההזמנה.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onDecline(HashMap<String, String> marketData) {
        String marketId = marketData.get("marketId");
        if (marketId == null || marketId.isEmpty()) {
            Toast.makeText(NotificationsActivity.this, "שגיאה: מזהה שוק ריק, לא ניתן לדחות.", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                String response = Service.declineInvitation(userEmail, marketId);
                JSONObject jsonResponse = new JSONObject(response);
                boolean success = jsonResponse.optBoolean("success", false);
                String message = jsonResponse.optString("message", "פעולה הושלמה.");
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(NotificationsActivity.this, "הזמנה נדחתה בהצלחה!", Toast.LENGTH_SHORT).show();
                        removeMarketFromList(marketData);
                    } else {
                        Toast.makeText(NotificationsActivity.this, "שגיאה בדחיית ההזמנה: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("MarketsActivity", "Error declining invitation: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה כללית בדחיית ההזמנה.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onRequest(HashMap<String, String> marketData) {
        currentMarketId = marketData.get("marketId");
        currentMarketData = marketData;

        if (currentMarketId == null || currentMarketId.isEmpty()) {
            Toast.makeText(NotificationsActivity.this, "שגיאה: מזהה שוק ריק, לא ניתן לשלוח בקשה.", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                String productsResponse = Service.getFarmerItems(userEmail);
                JSONArray productsArray = new JSONArray(productsResponse);

                List<Item> farmerProducts = new ArrayList<>();
                for (int i = 0; i < productsArray.length(); i++) {
                    JSONObject productJson = productsArray.getJSONObject(i);
                    farmerProducts.add(new Item(productJson.optString("name"), productJson.optString("marketPrice")));
                }

                Map<String, Double> itemPrices = new HashMap<>();

                runOnUiThread(() -> {
                    SelectProductForMarketDialogFragment dialogFragment = SelectProductForMarketDialogFragment.newInstance(farmerProducts, itemPrices);
                    dialogFragment.setOnProductSelectedListener(this);
                    dialogFragment.show(getSupportFragmentManager(), "SelectProductForMarketDialog");
                });

            } catch (IOException e) {
                Log.e("MarketsActivity", "Network error loading farmer products: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה בטעינת המוצרים: בעיית רשת", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("MarketsActivity", "Error parsing farmer products: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה בטעינת המוצרים: נתונים שגויים", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onProductsSelected(List<JSONObject> selectedProducts) {
        Toast.makeText(this, "שולח בקשת הצטרפות לשוק: " + currentMarketData.get("marketName"), Toast.LENGTH_SHORT).show();

        JSONArray productsJsonArray = new JSONArray(selectedProducts);

        new Thread(() -> {
            try {
                String response = Service.sendJoinRequestToMarket(currentMarketId, userEmail, productsJsonArray);
                JSONObject jsonResponse = new JSONObject(response);

                boolean success = jsonResponse.optBoolean("success", false);
                String message = jsonResponse.optString("message", "פעולה הושלמה.");
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(NotificationsActivity.this, "בקשה נשלחה בהצלחה!", Toast.LENGTH_SHORT).show();
                        removeMarketFromList(currentMarketData);
                    } else {
                        Toast.makeText(NotificationsActivity.this, "שגיאה בשליחת הבקשה: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("MarketsActivity", "Error requesting invitation with products: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(NotificationsActivity.this, "שגיאה כללית בשליחת הבקשה.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onViewProfile(HashMap<String, String> marketData) {
        String loc = marketData.get("location");
        String date= marketData.get("date");
        if (loc != null && !loc.isEmpty()) {
            Toast.makeText(this, "צופה בפרופיל של " + marketData.get("marketName"), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MarketProfileActivity.class);
            intent.putExtra("location",loc);
            intent.putExtra("date",date);
            startActivity(intent);
        } else {
            Toast.makeText(this, "שגיאה: לא ניתן למצוא מזהה שוק.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeMarketFromList(HashMap<String, String> marketData) {
        List<HashMap<String, String>> currentList = new ArrayList<>(marketAdapter.marketList);
        currentList.remove(marketData);
        marketAdapter.setMarketList(currentList);

        if (currentList.isEmpty()) {
            noItemsMessage.setVisibility(View.VISIBLE);
            marketsRecyclerView.setVisibility(View.GONE);
        }
    }
}