package com.example.shock_it.ui.map.farmerProfile;

import android.app.Application;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import classes.Farmer;
import classes.FarmerMarket;
import classes.Item;
import classes.Market;
import services.Service;

public class FarmerProfileViewModel extends AndroidViewModel {

    private final MutableLiveData<Farmer> _farmer = new MutableLiveData<>();
    public LiveData<Farmer> getFarmer() {
        return _farmer;
    }

    public FarmerProfileViewModel(@NonNull Application application) {
        super(application);
    }

    public void loadFarmerProfile(String email) {
        if (email == null || email.isEmpty()) {
            Log.e("FarmerProfileVM", "Email is null or empty, cannot load profile.");
            _farmer.postValue(null);
            return;
        }

        new Thread(() -> {
            try {
                //Fetch basic farmer profile data
                String profileResponse = Service.getUserProfile(email);
                Log.d("FarmerProfileVM", "Server response for profile: " + profileResponse);
                JSONObject profileJson = new JSONObject(profileResponse);

                String name = profileJson.optString("name", "לא נמצא");
                String farmerEmail = profileJson.optString("email", email);
                String phone = profileJson.optString("phone", "לא צוין");
                String address = profileJson.optString("address", "לא צוינה");
                double notificationRadius = profileJson.optDouble("notificationRadius", 0.0);
                double longitude = profileJson.optDouble("longitude", 0.0);
                double latitude = profileJson.optDouble("latitude", 0.0);
                String password = profileJson.optString("password", null);

                Farmer currentFarmer = new Farmer(name, farmerEmail, password, phone, address, latitude, longitude, (int) (notificationRadius));

                Log.d("FarmerProfileVM", "Loaded profile data -> Name: " + name + ", Phone: " + currentFarmer.getPhone() + ", Address: " + address + ", Radius: " + notificationRadius);

                // Fetch farmer's products and add to the Farmer object
                String productsResponse = Service.getFarmerItems(email);
                JSONArray itemsArray = new JSONArray(productsResponse);
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemJson = itemsArray.getJSONObject(i);
                    String itemName = itemJson.optString("name", "מוצר ללא שם");
                    String itemDesc = itemJson.optString("description", "");
                    double price = itemJson.optDouble("price", 0.0);
                    Item newItem = new Item(itemName, itemDesc);
                    currentFarmer.addProduct(newItem, price);
                }

                //Fetch farmer's participating markets and add to the Farmer object
                String marketsResponse = Service.marketsByEmail(email);
                JSONArray marketsArray = new JSONArray(marketsResponse);
                for (int i = 0; i < marketsArray.length(); i++) {
                    JSONObject marketObj = marketsArray.getJSONObject(i);

                    String marketId = marketObj.optString("marketId", null);
                    String location = marketObj.optString("location", null);
                    String dateStr = marketObj.optString("date", null);
                    String hoursStr= marketObj.optString("hours", null);

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

                    Market market = new Market(date, location,hoursStr , 0.0, 0.0);
                    FarmerMarket farmerMarket = new FarmerMarket(market);
                    farmerMarket.setParticipated(true);
                    currentFarmer.addFarmerMarket(farmerMarket);
                }

                _farmer.postValue(currentFarmer);

            } catch (IOException | JSONException e) {
                Log.e("FarmerProfileVM", "Error loading farmer profile: " + e.getMessage(), e);
                _farmer.postValue(null);
            }
        }).start();
    }

    public void addProduct(String email, Item item, double price) {
        new Thread(() -> {
            try {
                Service.addNewItem(email, item.getName(), price, item.getDescription());
                loadFarmerProfile(email);
            } catch (Exception e) {
                Log.e("FarmerProfileVM", "Error adding product: " + e.getMessage(), e);
            }
        }).start();
    }

    public void updateFarmerProfile(String email, String name, String phone, String address, String notificationRadiusStr) {
        new Thread(() -> {
            try {
                Log.d("FarmerProfileVM", "Updating profile with -> Name: " + name + ", Phone: " + phone + ", Address: " + address + ", Radius: " + notificationRadiusStr);
                double longitude = 0;
                double latitude = 0;
                double notificationRadius = Double.parseDouble(notificationRadiusStr);

                Geocoder geocoder = new Geocoder(getApplication());
                List<Address> addresses = geocoder.getFromLocationName(address, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address location = addresses.get(0);
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                    Log.d("FarmerProfileVM", "Geocoding successful. Longitude: " + longitude + ", Latitude: " + latitude);
                } else {
                    Log.e("FarmerProfileVM", "Geocoding failed for address: " + address);
                    return;
                }

                String response = Service.editProfile(email, name, phone, address, longitude, latitude, notificationRadius);
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("message")) {
                    Log.d("FarmerProfileVM", "Profile update successful: " + jsonResponse.getString("message"));
                }
                loadFarmerProfile(email);
            } catch (IOException | JSONException e) {
                Log.e("FarmerProfileVM", "Error updating farmer profile: " + e.getMessage(), e);
            } catch (NumberFormatException e) {
                Log.e("FarmerProfileVM", "Error parsing notification radius: " + notificationRadiusStr, e);
            }
        }).start();
    }

    public void editProduct(String farmerEmail, String originalItemName, String newItemName, String newDescription, double newPrice) {
        new Thread(() -> {
            try {
                String response = Service.editItem(farmerEmail, originalItemName, newItemName, newPrice, newDescription);
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("message")) {
                    Log.d("FarmerProfileVM", "Product edit successful: " + jsonResponse.getString("message"));
                }
                loadFarmerProfile(farmerEmail);
            } catch (IOException | JSONException e) {
                Log.e("FarmerProfileVM", "Error editing product: " + e.getMessage(), e);
            }
        }).start();
    }

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
                    loadFarmerProfile(farmerEmail);
                } else if (jsonResponse.has("error")) {
                    Log.e("FarmerProfileVM", "Product delete failed with server error: " + jsonResponse.getString("error"));
                } else {
                    Log.w("FarmerProfileVM", "Product delete response unknown format: " + response);
                }
            } catch (IOException e) {
                Log.e("FarmerProfileVM", "Network error deleting product: " + e.getMessage(), e);
            } catch (JSONException e) {
                Log.e("FarmerProfileVM", "JSON parsing error deleting product: " + e.getMessage() + ", Response: " + response, e);
            } catch (Exception e) {
                Log.e("FarmerProfileVM", "Unexpected error deleting product: " + e.getMessage(), e);
            }
        }).start();
    }
}
