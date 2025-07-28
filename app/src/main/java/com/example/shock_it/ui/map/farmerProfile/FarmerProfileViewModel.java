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
import java.util.ArrayList; // Used for new Farmer instance
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
                    Item newItem = new Item(itemName, itemDesc); // Item class should also store description if needed
                    currentFarmer.addProduct(newItem, price); // Add product to Farmer object
                }

                // 3. Fetch farmer's participating markets and add to the Farmer object
                String marketsResponse = Service.marketsByEmail(email);
                JSONArray marketsArray = new JSONArray(marketsResponse);
                for (int i = 0; i < marketsArray.length(); i++) {
                    JSONObject marketObj = marketsArray.getJSONObject(i);

                    String marketId = marketObj.optString("marketId", null);
                    String location = marketObj.optString("location", null);
                    String dateStr = marketObj.optString("date", null);

                    if (marketId == null || location == null || dateStr == null || "null".equalsIgnoreCase(dateStr)) {
                        Log.w("FarmerProfileVM", "Skipping market entry due to null or invalid data: " + marketObj.toString());
                        continue;
                    }

                    LocalDate date = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            date = LocalDate.parse(dateStr);
                        } catch (java.time.format.DateTimeParseException e) {
                            Log.e("FarmerProfileVM", "Error parsing date '" + dateStr + "': " + e.getMessage());
                            continue;
                        }
                    }

                    Market market = new Market(date, location, 0.0, 0.0);
                    FarmerMarket farmerMarket = new FarmerMarket(market);
                    farmerMarket.setParticipated(true);
                    currentFarmer.addFarmerMarket(farmerMarket);
                }

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
                loadFarmerProfile(email); // Reload to reflect changes
            } catch (Exception e) {
                Log.e("FarmerProfileVM", "Error adding product: " + e.getMessage(), e);
            }
        }).start();
    }

    // 🆕 NEW: Method to update farmer's profile details
    public void updateFarmerProfile(String email, String name, String phone, String address) {
        new Thread(() -> {
            try {
                String response = Service.editProfile(email, name, phone, address);
                // Optionally parse response for success/error message
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("message")) {
                    Log.d("FarmerProfileVM", "Profile update successful: " + jsonResponse.getString("message"));
                }
                loadFarmerProfile(email); // Reload profile to reflect changes
            } catch (IOException | JSONException e) {
                Log.e("FarmerProfileVM", "Error updating farmer profile: " + e.getMessage(), e);
                // Handle error (e.g., show a toast through a different LiveData)
            }
        }).start();
    }

    // 🆕 NEW: Method to edit an existing product
    public void editProduct(String farmerEmail, String originalItemName, String newItemName, String newDescription, double newPrice) {
        new Thread(() -> {
            try {
                String response = Service.editItem(farmerEmail, originalItemName, newItemName, newPrice, newDescription);
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("message")) {
                    Log.d("FarmerProfileVM", "Product edit successful: " + jsonResponse.getString("message"));
                }
                loadFarmerProfile(farmerEmail); // Reload profile to reflect changes
            } catch (IOException | JSONException e) {
                Log.e("FarmerProfileVM", "Error editing product: " + e.getMessage(), e);
            }
        }).start();
    }

    // 🆕 NEW: Method to delete a product
// Inside FarmerProfileViewModel.java

    // Method to delete a product
    public void deleteProduct(String farmerEmail, String itemName) {
        if (farmerEmail == null || farmerEmail.isEmpty() || itemName == null || itemName.isEmpty()) {
            Log.e("FarmerProfileVM", "Cannot delete product: farmerEmail or itemName is null/empty.");
            return;
        }

        new Thread(() -> {
            String response = null;
            try {
                Log.d("FarmerProfileVM", "Attempting to delete product: " + itemName + " for farmer: " + farmerEmail);
                response = Service.deleteItem(farmerEmail, itemName);
                Log.d("FarmerProfileVM", "Server response for deleteItem: " + response);

                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("message")) {
                    Log.d("FarmerProfileVM", "Product delete successful: " + jsonResponse.getString("message"));
                    // Reload profile only after successful deletion
                    loadFarmerProfile(farmerEmail);
                } else if (jsonResponse.has("error")) {
                    Log.e("FarmerProfileVM", "Product delete failed with server error: " + jsonResponse.getString("error"));
                    // Optionally show a toast or alert to the user here
                } else {
                    Log.w("FarmerProfileVM", "Product delete response unknown format: " + response);
                }
            } catch (IOException e) {
                // This catches network errors, timeout, server unreachable, etc.
                Log.e("FarmerProfileVM", "Network error deleting product: " + e.getMessage(), e);
            } catch (JSONException e) {
                // This catches errors if the server response is not valid JSON
                Log.e("FarmerProfileVM", "JSON parsing error deleting product: " + e.getMessage() + ", Response: " + response, e);
            } catch (Exception e) {
                // Catch any other unexpected exceptions
                Log.e("FarmerProfileVM", "Unexpected error deleting product: " + e.getMessage(), e);
            }
        }).start();
    }
}