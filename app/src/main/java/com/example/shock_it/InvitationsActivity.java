package com.example.shock_it;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton; // Import ImageButton
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.InvitationAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import services.Service;

public class InvitationsActivity extends AppCompatActivity implements InvitationAdapter.OnInvitationActionListener {

    private RecyclerView invitationsRecyclerView;
    private TextView noInvitationsMessage;
    private InvitationAdapter invitationAdapter;
    private String userEmail;
    private ImageButton backButton; // Declare ImageButton

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_invites);

        invitationsRecyclerView = findViewById(R.id.invitationsRecyclerView);
        noInvitationsMessage = findViewById(R.id.noInvitationsMessage);
        backButton = findViewById(R.id.backButton); // Initialize the back button

        invitationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        invitationAdapter = new InvitationAdapter(new ArrayList<>(), this);
        invitationsRecyclerView.setAdapter(invitationAdapter);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "שגיאה: אימייל המשתמש לא נמצא. אנא התחבר מחדש.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set OnClickListener for the back button
        backButton.setOnClickListener(v -> onBackPressed());

        loadInvitations(userEmail);
    }

    private void loadInvitations(String email) {
        new Thread(() -> {
            try {
                String response = Service.getInvitations(email);
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray invitationsArray = jsonResponse.optJSONArray("invitations");

                List<HashMap<String, String>> invitations = new ArrayList<>();
                if (invitationsArray != null) {
                    for (int i = 0; i < invitationsArray.length(); i++) {
                        JSONObject invJson = invitationsArray.getJSONObject(i);

                        HashMap<String, String> invitationData = new HashMap<>();
                        invitationData.put("marketId", invJson.optString("marketId"));
                        invitationData.put("date", invJson.optString("date"));
                        invitationData.put("location", invJson.optString("location"));
                        invitationData.put("marketName", invJson.optString("name", invJson.optString("location")));

                        invitations.add(invitationData);
                    }
                }

                runOnUiThread(() -> {
                    if (invitations.isEmpty()) {
                        noInvitationsMessage.setVisibility(View.VISIBLE);
                        invitationsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noInvitationsMessage.setVisibility(View.GONE);
                        invitationsRecyclerView.setVisibility(View.VISIBLE);
                        invitationAdapter.setInvitationList(invitations);
                    }
                });

            } catch (IOException e) {
                Log.e("InvitationsActivity", "Network error loading invitations: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "שגיאה בטעינת הזמנות: בעיית רשת", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("InvitationsActivity", "Error parsing invitations or general error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "שגיאה בטעינת הזמנות: נתונים שגויים או שגיאה כללית", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onAccept(HashMap<String, String> invitationData) {
        String marketName = invitationData.get("marketName");
        String marketId = invitationData.get("marketId");

        Toast.makeText(this, "מאשר הזמנה לשוק: " + marketName, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                Log.d("InvitationsActivity", "Attempting to accept: Email=" + userEmail + ", MarketId=" + marketId);
                String response = Service.acceptInvitation(userEmail, marketId);
                JSONObject jsonResponse = new JSONObject(response);
                Log.d("InvitationsActivity", "Raw server response (accept): " + (response != null ? response : "null"));

                boolean success = jsonResponse.optBoolean("success", false);
                String message = jsonResponse.optString("message", "פעולה הושלמה.");

                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(InvitationsActivity.this, "הזמנה אושרה בהצלחה! " + message, Toast.LENGTH_SHORT).show();
                        removeInvitationFromList(invitationData);
                    } else {
                        Toast.makeText(InvitationsActivity.this, "שגיאה באישור ההזמנה: " + message, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IOException e) {
                Log.e("InvitationsActivity", "Network error accepting invitation: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "שגיאה ברשת בעת אישור ההזמנה.", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("InvitationsActivity", "Error accepting invitation: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "שגיאה כללית באישור ההזמנה.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onDecline(HashMap<String, String> invitationData) {
        String marketName = invitationData.get("marketName");
        String marketId = invitationData.get("marketId");

        Toast.makeText(this, "דוחה הזמנה לשוק: " + marketName, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String response = Service.declineInvitation(userEmail, marketId);
                JSONObject jsonResponse = new JSONObject(response);

                boolean success = jsonResponse.optBoolean("success", false);
                String message = jsonResponse.optString("message", "פעולה הושלמה.");

                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(InvitationsActivity.this, "הזמנה נדחתה בהצלחה! " + message, Toast.LENGTH_SHORT).show();
                        removeInvitationFromList(invitationData);
                    } else {
                        Toast.makeText(InvitationsActivity.this, "שגיאה בדחיית ההזמנה: " + message, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IOException e) {
                Log.e("InvitationsActivity", "Network error declining invitation: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "שגיאה ברשת בעת דחיית ההזמנה.", Toast.LENGTH_LONG).show());
            } catch (JSONException e) {
                Log.e("InvitationsActivity", "JSON parsing error declining invitation: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "שגיאה בניתוח נתונים מהשרת.", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("InvitationsActivity", "Unexpected error declining invitation: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "שגיאה כללית בדחיית ההזמנה.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void removeInvitationFromList(HashMap<String, String> invitationData) {
        List<HashMap<String, String>> currentList = new ArrayList<>(invitationAdapter.invitationList);
        currentList.remove(invitationData);
        invitationAdapter.setInvitationList(currentList);

        if (currentList.isEmpty()) {
            noInvitationsMessage.setVisibility(View.VISIBLE);
            invitationsRecyclerView.setVisibility(View.GONE);
        }
    }
}