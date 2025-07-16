package com.example.shock_it.manageMarket;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import services.Service;

public class ManageMarketViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    private final MutableLiveData<List<String>> _searchResults = new MutableLiveData<>();
    private final MutableLiveData<String> _searchErrorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return _isLoading; }
    public LiveData<String> getToastMessage() { return _toastMessage; }
    public LiveData<List<String>> getSearchResults() { return _searchResults; }
    public LiveData<String> getSearchErrorMessage() { return _searchErrorMessage; }

    public ManageMarketViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * מזמין חקלאי לשוק ספציפי.
     * הקריאה ל-Service מתבצעת ב-Thread נפרד.
     * @param marketId ה-ID של השוק אליו מזמינים.
     * @param invitedEmail המייל של החקלאי המוזמן.
     * @param inviterEmail המייל של החקלאי המזמין (מנהל השוק).
     */
    public void inviteFarmerToMarket(String marketId, String invitedEmail, String inviterEmail) {
        _isLoading.setValue(true);

        new Thread(() -> {
            try {
                String responseString = Service.inviteFarmerToMarket(marketId, invitedEmail, inviterEmail);
                JSONObject jsonResponse = new JSONObject(responseString);

                if (jsonResponse.has("message")) {
                    String message = jsonResponse.getString("message");
                    if (message.equals("Invitation sent successfully.")) {
                        _toastMessage.postValue("החקלאי הוזמן בהצלחה! 🎉");
                    } else {
                        _toastMessage.postValue("שגיאה בהזמנה: " + message + " 😟");
                    }
                } else {
                    _toastMessage.postValue("תגובה לא צפויה מהשרת בהזמנה. 🤔");
                }
            } catch (IOException e) {
                Log.e("ManageMarketViewModel", "Network error inviting farmer: " + e.getMessage());
                _toastMessage.postValue("שגיאת רשת בהזמנת חקלאי: " + e.getMessage() + " 😔");
            } catch (JSONException e) {
                Log.e("ManageMarketViewModel", "JSON error inviting farmer: " + e.getMessage());
                _toastMessage.postValue("שגיאה בעיבוד נתונים מהשרת: " + e.getMessage() + " 🐛");
            } catch (Exception e) {
                Log.e("ManageMarketViewModel", "General error inviting farmer: " + e.getMessage());
                _toastMessage.postValue("שגיאה כללית בהזמנת חקלאי: " + e.getMessage() + " 😵");
            } finally {
                _isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * מחפש חקלאים לפי שאילתת חיפוש.
     * הקריאה ל-Service מתבצעת ב-Thread נפרד.
     * @param query מחרוזת החיפוש (שם או מייל).
     */
    public void searchFarmers(String query) {
        _isLoading.setValue(true);
        // 🌟 שינוי: קוראים ל-setValue ישירות על המשתנה הפנימי
        _searchErrorMessage.setValue(null); // נקה הודעות שגיאה קודמות, קריאה ב-Main Thread

        if (query == null || query.trim().isEmpty()) {
            _searchErrorMessage.postValue("אנא הכנס שם או אימייל לחיפוש. 🔍");
            _isLoading.postValue(false);
            _searchResults.postValue(new ArrayList<>()); // נקה תוצאות קודמות
            return;
        }

        new Thread(() -> {
            try {
                String responseString = Service.searchFarmers(query);
                JSONObject jsonResponse = new JSONObject(responseString);

                if (jsonResponse.has("farmers")) {
                    JSONArray farmersArray = jsonResponse.getJSONArray("farmers");
                    List<String> results = new ArrayList<>();
                    for (int i = 0; i < farmersArray.length(); i++) {
                        JSONObject farmer = farmersArray.getJSONObject(i);
                        String name = farmer.optString("name", "שם לא ידוע");
                        String email = farmer.optString("email", "");
                        results.add(name + " (" + email + ")");
                    }
                    _searchResults.postValue(results);
                    if (results.isEmpty()) {
                        _searchErrorMessage.postValue("לא נמצאו חקלאים מתאימים. 🤷‍♀️");
                    } else {
                        _toastMessage.postValue("נמצאו " + results.size() + " חקלאים. 👍");
                    }
                } else if (jsonResponse.has("message") || jsonResponse.has("error")) {
                    _searchErrorMessage.postValue(jsonResponse.optString("message", jsonResponse.optString("error", "שגיאה בחיפוש חקלאים.")));
                } else {
                    _searchErrorMessage.postValue("תגובה לא צפויה מהשרת בחיפוש חקלאים. 😱");
                }
            } catch (IOException e) {
                Log.e("ManageMarketViewModel", "Network error searching farmers: " + e.getMessage());
                _searchErrorMessage.postValue("שגיאת רשת בחיפוש חקלאים: " + e.getMessage() + " 🌐");
            } catch (JSONException e) {
                Log.e("ManageMarketViewModel", "JSON error searching farmers: " + e.getMessage());
                _searchErrorMessage.postValue("שגיאה בעיבוד תוצאות חיפוש חקלאים: " + e.getMessage() + " 🐞");
            } catch (Exception e) {
                Log.e("ManageMarketViewModel", "General error searching farmers: " + e.getMessage());
                _searchErrorMessage.postValue("שגיאה כללית בחיפוש חקלאים: " + e.getMessage() + " 🚨");
            } finally {
                _isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * פונקציה לניקוי הודעת שגיאה של חיפוש חקלאים.
     * נקראת מה-Fragment כדי לבקש מה-ViewModel לנקות את המצב.
     */
    public void clearSearchErrorMessage() {
        // 🌟 חדש: נקרא מ-Fragment, מתבצע ב-Main Thread, לכן משתמשים ב-setValue
        _searchErrorMessage.setValue(null);
    }

    /**
     * פונקציה לניקוי תוצאות החיפוש.
     * נקראת מה-Fragment כדי לבקש מה-ViewModel לנקות את התוצאות.
     */
    public void clearSearchResults() {
        // 🌟 חדש: נקרא מ-Fragment, מתבצע ב-Main Thread, לכן משתמשים ב-setValue
        _searchResults.setValue(new ArrayList<>());
    }
}