package com.example.shock_it.ui.map.addMarket;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import services.Service;

public class AddMarketViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> marketAddedSuccessfully = new MutableLiveData<>();

    private String newMarketId = null;

    // getters
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Boolean> getMarketAddedSuccessfully() {
        return marketAddedSuccessfully;
    }
    public String getNewMarketId() {
        return newMarketId;
    }


    // פונקציה להוספת שוק - לוגיקה של עבודה ברקע וקריאת API
    public void addMarket(String date, String location, double latitude, double longitude, String farmerEmail) {
        isLoading.setValue(true);

        new Thread(() -> {
            String response = null;
            response = Service.addNewMarket(date, location, latitude, longitude, farmerEmail);
            if (response != null && !response.isEmpty()) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("message") && jsonResponse.getString("message").equals("Market already exists")) {
                        toastMessage.postValue("שוק כבר קיים במיקום ותאריך זה");
                        marketAddedSuccessfully.postValue(false);
                        this.newMarketId = null; // 🆕 הוסף את השורה הזו
                    } else {

                        if (jsonResponse.has("marketId")) {
                            this.newMarketId = jsonResponse.getString("marketId");
                            toastMessage.postValue("שוק נוסף בהצלחה!");
                        } else {
                            this.newMarketId = null;
                            toastMessage.postValue("שוק נוסף בהצלחה, אך ID לא התקבל!");
                        }
                        marketAddedSuccessfully.postValue(true);
                    }
                } catch (JSONException e) {
                    toastMessage.postValue("שגיאה בפיענוח תגובת השרת: " + e.getMessage());
                    marketAddedSuccessfully.postValue(false); // 🆕 עדכון למקרה שגיאה
                    this.newMarketId = null; // 🆕 הוסף את השורה הזו
                }
            } else {
                toastMessage.postValue("שגיאה בהוספת השוק: תגובה ריקה או Null");
                marketAddedSuccessfully.postValue(false);
                this.newMarketId = null; // 🆕 הוסף את השורה הזו
            }
// ... קוד קיים אחרי ...
            isLoading.postValue(false);
        }).start();
    }
    interface AddMarketCallback {
        void onSuccess(String marketId, String message);
        void onFailure(String errorMessage);
    }

}
