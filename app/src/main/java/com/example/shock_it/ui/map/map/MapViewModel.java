package com.example.shock_it.ui.map.map;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import classes.Farmer;
import classes.Market;
import services.Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private MutableLiveData<List<Object>> locationsLiveData;
    private MutableLiveData<Boolean> isLoadingLiveData;
    private MutableLiveData<String> errorMessageLiveData;

    // 🌟 New LiveData to hold the pending invites count
    private MutableLiveData<Integer> pendingInvitesCountLiveData;

    private boolean isLocationsLoaded = false;

    public MapViewModel(Application application) {
        super(application);
        locationsLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
        // 🌟 Initialize the new LiveData
        pendingInvitesCountLiveData = new MutableLiveData<>();
        pendingInvitesCountLiveData.setValue(0); // Set initial value to 0
    }

    public LiveData<List<Object>> getLocationsLiveData() {
        return locationsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    // 🌟 Public getter for the new LiveData
    public LiveData<Integer> getPendingInvitesCountLiveData() {
        return pendingInvitesCountLiveData;
    }

    /**
     * Loads the count of pending invitations for a specific user.
     * This method runs on a background thread to avoid blocking the UI.
     * @param userEmail The email of the user whose invitations to count.
     */
    public void loadPendingInvitesCount(String userEmail) {
        new Thread(() -> {
            try {
                int count = Service.getPendingInvitesCount(userEmail);
                Log.d("MapViewModel", "Pending invites count fetched: " + count);
                // 🌟 Update the LiveData with the new count
                pendingInvitesCountLiveData.postValue(count);
            } catch (IOException | JSONException e) {
                Log.e("MapViewModel", "Error fetching pending invites count: " + e.getMessage(), e);
                // 🌟 Handle error: You might want to log this but not show an error to the user
                // as it's a non-critical feature. For now, we'll set the count to 0.
                pendingInvitesCountLiveData.postValue(0);
            }
        }).start();
    }

    public void loadMarkets(double userLat, double userLon) {
        if (isLocationsLoaded) {
            Log.d("MapViewModel", "Locations already loaded, skipping redundant fetch.");
            return;
        }

        isLoadingLiveData.postValue(true);
        errorMessageLiveData.postValue(null);

        new Thread(() -> {
            try {
                String response = Service.getOrderedMarkets(userLat, userLon, LocalDate.now().toString());
                List<Object> fetchedLocations = parseMarketsFromJson(response);
                locationsLiveData.postValue(fetchedLocations);
                isLocationsLoaded = true;
            } catch (IOException | JSONException e) {
                Log.e("MapViewModel", "Error loading markets: " + e.getMessage(), e);
                errorMessageLiveData.postValue("שגיאה בטעינת שווקים: " + e.getMessage());
            } finally {
                isLoadingLiveData.postValue(false);
            }
        }).start();
    }

    public void searchLocations(String query, double userLat, double userLon) {
        if (query == null || query.isEmpty()) {
            loadMarkets(userLat, userLon);
            return;
        }

        isLoadingLiveData.postValue(true);
        errorMessageLiveData.postValue(null);

        new Thread(() -> {
            try {
                String response = Service.search(query, userLat, userLon);
                Log.d("MapViewModel", "Search response received: " + response);
                List<Object> searchResults = parseSearchResponse(response);
                locationsLiveData.postValue(searchResults);
            } catch (IOException | JSONException e) {
                Log.e("MapViewModel", "Error searching locations: " + e.getMessage(), e);
                errorMessageLiveData.postValue("שגיאה בחיפוש: " + e.getMessage());
            } finally {
                isLoadingLiveData.postValue(false);
            }
        }).start();
    }

    // ... all other existing methods (parseSearchResponse, parseMarketsFromJson, resetMarketsLoaded) ...

    @SuppressLint("NewApi")
    private List<Object> parseSearchResponse(String jsonResponse) throws JSONException {
        // (No changes needed here)
        List<Object> locations = new ArrayList<>();
        JSONObject responseObject = new JSONObject(jsonResponse);
        JSONArray jsonArray = responseObject.getJSONArray("results");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String type = obj.optString("type");
            if ("Market".equals(type)) {
                String locationName = obj.optString("location");
                String dateStr = obj.optString("date");
                double latitude = obj.optDouble("latitude");
                double longitude = obj.optDouble("longitude");
                locations.add(new Market(LocalDate.parse(dateStr), locationName, latitude, longitude));
            } else if ("Farmer".equals(type)) {
                String name = obj.optString("name");
                String email = obj.optString("email");
                locations.add(new Farmer(name, email));
                if (obj.has("participatingMarkets")) {
                    JSONArray participatingMarketsJson = obj.getJSONArray("participatingMarkets");
                    for (int j = 0; j < participatingMarketsJson.length(); j++) {
                        JSONObject marketObj = participatingMarketsJson.getJSONObject(j);
                        String marketLocation = marketObj.getString("location");
                        String marketDateStr = marketObj.getString("date");
                        double marketLat = marketObj.optDouble("latitude", 0.0);
                        double marketLng = marketObj.optDouble("longitude", 0.0);

                        LocalDate marketDate = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            marketDate = LocalDate.parse(marketDateStr);
                        }
                        Market market = new Market(marketDate, marketLocation, marketLat, marketLng);
                        locations.add(market);
                    }
                }
            }
        }
        return locations;
    }

    @SuppressLint("NewApi")
    private List<Object> parseMarketsFromJson(String jsonResponse) throws JSONException {
        // (No changes needed here)
        List<Object> markets = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonResponse);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject marketObj = jsonArray.getJSONObject(i);
            String locationName = marketObj.optString("location");
            String dateStr = marketObj.optString("date");
            double latitude = marketObj.optDouble("latitude");
            double longitude = marketObj.optDouble("longitude");
            markets.add(new Market(LocalDate.parse(dateStr), locationName, latitude, longitude));
        }
        return markets;
    }

    public void resetMarketsLoaded() {
        // (No changes needed here)
        isLocationsLoaded = false;
        Log.d("MapViewModel", "Locations loaded flag reset.");
    }
}