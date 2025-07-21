package com.example.shock_it;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Correct import for the InvitationAdapter
import com.example.shock_it.InvitationAdapter;

import org.json.JSONArray;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_invites); // Ensure this XML file exists and is named correctly

        invitationsRecyclerView = findViewById(R.id.invitationsRecyclerView);
        noInvitationsMessage = findViewById(R.id.noInvitationsMessage);

        invitationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Initialize InvitationAdapter, passing 'this' as listener
        invitationAdapter = new InvitationAdapter(new ArrayList<>(), this);
        invitationsRecyclerView.setAdapter(invitationAdapter);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "שגיאה: אימייל המשתמש לא נמצא. אנא התחבר מחדש.", Toast.LENGTH_LONG).show();
            finish(); // Close activity if no email
            return;
        }

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
                        invitationData.put("marketName", invJson.optString("name", invJson.optString("location"))); // Use "name" if available, else "location"

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
        String date = invitationData.get("date");

        Toast.makeText(this, "קבלת הזמנה לשוק: " + marketName, Toast.LENGTH_SHORT).show();
        // TODO: Call your service to update the invitation status on the backend
        removeInvitationFromList(invitationData);
    }

    @Override
    public void onDecline(HashMap<String, String> invitationData) {
        String marketName = invitationData.get("marketName");
        String marketId = invitationData.get("marketId");
        String date = invitationData.get("date");

        Toast.makeText(this, "דחית הזמנה לשוק: " + marketName, Toast.LENGTH_SHORT).show();
        // TODO: Call your service to update the invitation status on the backend
        removeInvitationFromList(invitationData);
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