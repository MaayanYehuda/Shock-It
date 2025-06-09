package com.example.shock_it.ui.map.addMarket;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import services.Service;

public class AddMarketViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> marketAddedSuccessfully = new MutableLiveData<>();

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

    // פונקציה להוספת שוק - לוגיקה של עבודה ברקע וקריאת API
    public void addMarket(String date, String location, double latitude, double longitude) {
        isLoading.setValue(true);

        // רץ ב־Thread נפרד
        new Thread(() -> {
            String response = Service.addNewMarket(date, location, latitude, longitude);

            // חזרה ל־UI Thread
            // לשם הפשטות כאן נניח שיש לך Context, אחרת יש להעביר את ההודעה עם LiveData
            if (response != null && !response.isEmpty()) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("message") && jsonResponse.getString("message").equals("Market already exists")) {
                        toastMessage.postValue("שוק כבר קיים במיקום ותאריך זה");
                        marketAddedSuccessfully.postValue(false);
                    } else {
                        toastMessage.postValue("שוק נוסף בהצלחה!");
                        marketAddedSuccessfully.postValue(true);
                    }
                } catch (JSONException e) {
                    toastMessage.postValue("שוק נוסף בהצלחה!");
                    marketAddedSuccessfully.postValue(true);
                }
            } else {
                toastMessage.postValue("שגיאה בהוספת השוק");
                marketAddedSuccessfully.postValue(false);
            }
            isLoading.postValue(false);
        }).start();
    }
}
