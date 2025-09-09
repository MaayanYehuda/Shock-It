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
import java.time.format.DateTimeFormatter; // Added
import java.time.format.DateTimeParseException; // Added
import java.util.ArrayList;
import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private MutableLiveData<List<Object>> locationsLiveData;
    private MutableLiveData<Boolean> isLoadingLiveData;
    private MutableLiveData<String> errorMessageLiveData;

    private MutableLiveData<Integer> pendingInvitesCountLiveData;

    private boolean isLocationsLoaded = false;

    public MapViewModel(Application application) {
        super(application);
        locationsLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
        pendingInvitesCountLiveData = new MutableLiveData<>();
        pendingInvitesCountLiveData.setValue(0);
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

    public LiveData<Integer> getPendingInvitesCountLiveData() {
        return pendingInvitesCountLiveData;
    }

    public void loadPendingInvitesCount(String userEmail) {
        new Thread(() -> {
            try {
                int count = Service.getPendingInvitesCount(userEmail);
                Log.d("MapViewModel", "Pending invites count fetched: " + count);
                pendingInvitesCountLiveData.postValue(count);
            } catch (IOException | JSONException e) {
                Log.e("MapViewModel", "Error fetching pending invites count: " + e.getMessage(), e);
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
                String response = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Note: LocalDate.now().toString() returns YYYY-MM-DD
                    response = Service.getOrderedMarkets(userLat, userLon, LocalDate.now().toString());
                }
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

    @SuppressLint("NewApi")
    private List<Object> parseSearchResponse(String jsonResponse) throws JSONException {
        List<Object> locations = new ArrayList<>();
        JSONObject responseObject = new JSONObject(jsonResponse);
        JSONArray jsonArray = responseObject.getJSONArray("results");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String type = obj.optString("type");
            if ("Market".equals(type)) {
                String locationName = obj.optString("location");
                String dateStr = obj.optString("date");
                String hours = obj.getString("hours");
                double latitude = obj.optDouble("latitude");
                double longitude = obj.optDouble("longitude");
                locations.add(new Market(parseDateSafely(dateStr), locationName, hours, latitude, longitude));
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
                        String marketHours = obj.getString("hours");
                        double marketLat = marketObj.optDouble("latitude", 0.0);
                        double marketLng = marketObj.optDouble("longitude", 0.0);

                        locations.add(new Market(parseDateSafely(marketDateStr), marketLocation, marketHours, marketLat, marketLng));
                    }
                }
            }
        }
        return locations;
    }

    @SuppressLint("NewApi")
    private List<Object> parseMarketsFromJson(String jsonResponse) throws JSONException {
        List<Object> markets = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonResponse);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject marketObj = jsonArray.getJSONObject(i);
            String locationName = marketObj.optString("location");
            String dateStr = marketObj.optString("date");
            String hours = marketObj.optString("hours");
            double latitude = marketObj.optDouble("latitude");
            double longitude = marketObj.optDouble("longitude");
            markets.add(new Market(parseDateSafely(dateStr), locationName, hours, latitude, longitude));
        }
        return markets;
    }

    @SuppressLint("NewApi")
    private LocalDate parseDateSafely(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Try YYYY-MM-DD first (ISO 8601 standard)
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            // If that fails, try DD/MM/YYYY
            try {
                DateTimeFormatter formatterDMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateStr, formatterDMY);
            } catch (DateTimeParseException e2) {
                Log.e("MapViewModel", "Failed to parse date string in any known format: " + dateStr, e2);
                return null;
            }
        }
    }

    public void resetMarketsLoaded() {
        isLocationsLoaded = false;
        Log.d("MapViewModel", "Locations loaded flag reset.");
    }
}