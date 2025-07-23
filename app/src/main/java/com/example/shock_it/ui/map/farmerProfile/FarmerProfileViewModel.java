package com.example.shock_it.ui.map.farmerProfile;

import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import classes.Farmer;
import classes.FarmerMarket;
import classes.Item;
import classes.Market;
import services.Service;

public class FarmerProfileViewModel extends ViewModel {

    private final MutableLiveData<Farmer> _farmer = new MutableLiveData<>();
    public LiveData<Farmer> getFarmer() {
        return _farmer;
    }
    public void loadFarmerProfile(String email) {
        if (email == null || email.isEmpty()) {
            Log.e("FarmerProfileVM", "Email is null or empty, cannot load profile.");
            _farmer.postValue(null); // Indicate no data or error
            return;
        }

        new Thread(() -> {
            try {
                // 1. Fetch basic farmer profile data (name, email, phone, address)
                String profileResponse = Service.getUserProfile(email);
                JSONObject profileJson = new JSONObject(profileResponse);

                String name = profileJson.optString("name", "לא נמצא");
                String farmerEmail = profileJson.optString("email", email);
                String phone = profileJson.optString("phone", "לא צוין");
                String address = profileJson.optString("address", "לא צוינה");

                // Create Farmer object
                Farmer currentFarmer = new Farmer(name, farmerEmail, null, phone, address);

                // 2. Fetch farmer's products and add to the Farmer object
                String productsResponse = Service.getFarmerItems(email);
                JSONArray itemsArray = new JSONArray(productsResponse);
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemJson = itemsArray.getJSONObject(i);
                    String itemName = itemJson.optString("name", "מוצר ללא שם");
                    String itemDesc = itemJson.optString("description", "");
                    double price = itemJson.optDouble("price", 0.0);
                    Item newItem = new Item(itemName, itemDesc);
                    currentFarmer.addProduct(newItem, price); // Add product to Farmer object
                }

                // 3. Fetch farmer's participating markets and add to the Farmer object
                String marketsResponse = Service.marketsByEmail(email);
                JSONArray marketsArray = new JSONArray(marketsResponse);
                for (int i = 0; i < marketsArray.length(); i++) {
                    JSONObject marketObj = marketsArray.getJSONObject(i);

                    // 🎯 IMPORTANT: Check for nulls before using optString
                    String marketId = marketObj.optString("marketId", null); // Use null as default to detect actual nulls
                    String location = marketObj.optString("location", null);
                    String dateStr = marketObj.optString("date", null); // Will be "null" if server sends null

                    // If any of these core fields are null, skip this market entry
                    if (marketId == null || location == null || dateStr == null || "null".equalsIgnoreCase(dateStr)) {
                        Log.w("FarmerProfileVM", "Skipping market entry due to null or invalid data: " + marketObj.toString());
                        continue; // Skip to the next market in the loop
                    }

                    LocalDate date = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            date = LocalDate.parse(dateStr);
                        } catch (java.time.format.DateTimeParseException e) {
                            Log.e("FarmerProfileVM", "Error parsing date '" + dateStr + "': " + e.getMessage());
                            continue; // Skip this market if date parsing fails
                        }
                    }

                    // Only create Market and FarmerMarket if all essential data is valid
                    Market market = new Market(date, location, 0.0, 0.0);
                    FarmerMarket farmerMarket = new FarmerMarket(market);
                    farmerMarket.setParticipated(true);
                    currentFarmer.addFarmerMarket(farmerMarket);
                }

                // Update the LiveData with the fully populated Farmer object
                _farmer.postValue(currentFarmer);

            } catch (IOException | JSONException e) {
                Log.e("FarmerProfileVM", "Error loading farmer profile: " + e.getMessage(), e);
                _farmer.postValue(null); // Indicate an error state
            }
        }).start();
    }

    // Method to add a product, then reload the entire profile for consistency
    public void addProduct(String email, Item item, double price)  {
        new Thread(() -> {
            try {
                Service.addNewItem(email, item.getName(), price, item.getDescription());
                loadFarmerProfile(email);
            } catch (Exception e) {
                Log.e("FarmerProfileVM", "Error adding product: " + e.getMessage(), e);
            }
        }).start();
    }
}