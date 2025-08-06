package com.example.shock_it.ui.map.map; // ודא שה-package name נכון

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import classes.Market; // ודא ייבוא Market הנכון
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate; // ייבוא LocalDate, כפי שנדרש על ידי Market
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import services.Service; // ודא ש-Service מיובא נכון

public class MapViewModel extends AndroidViewModel {

    // LiveData יחזיק כעת רשימה של אובייקטי Market ישירות
    private MutableLiveData<List<Market>> marketsLiveData;
    private MutableLiveData<Boolean> isLoadingLiveData;
    private MutableLiveData<String> errorMessageLiveData;

    private boolean isMarketsLoaded = false;

    public MapViewModel(Application application) {
        super(application);
        marketsLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
    }

    // מתודות לקבלת ה-LiveData מחוץ ל-ViewModel (עבור ה-Fragment)
    public LiveData<List<Market>> getMarketsLiveData() {
        return marketsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    /**
     * טוען את רשימת השווקים מהשרת, ממוינים לפי תאריך וקרבה למיקום המשתמש.
     * הקריאה מתבצעת רק אם הנתונים לא נטענו עדיין (isMarketsLoaded הוא false).
     *
     * @param userLat קו רוחב של מיקום המשתמש.
     * @param userLon קו אורך של מיקום המשתמש.
     */
    public void loadMarkets(double userLat, double userLon) {
        // אם השווקים כבר נטענו, אל תבצע קריאה נוספת לשרת.
        if (isMarketsLoaded) {
            Log.d("MapViewModel", "Markets already loaded, skipping redundant fetch.");
            return;
        }

        // עדכן את מצב הטעינה ל-true (הצג אינדיקטור טעינה ב-UI)
        isLoadingLiveData.postValue(true);
        // נקה הודעות שגיאה קודמות
        errorMessageLiveData.postValue(null);

        // בצע את קריאת הרשת ב-Thread נפרד כדי לא לחסום את ה-UI Thread
        new Thread(() -> {
            try {
                // קבלת התאריך הנוכחי בפורמט YYYY-MM-DD
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                Log.d("MapViewModel", "Fetching markets for location: " + userLat + ", " + userLon + " and date: " + currentDate);

                // קריאה לשירות ה-Service כדי לקבל את נתוני השווקים מהשרת
                // (השרת עדיין ישלח distance, אך ה-ViewModel יתעלם ממנו)
                String response = Service.getOrderedMarkets(userLat, userLon, currentDate);
                Log.d("MapViewModel", "Server response for markets: " + response);

                // פרסור תגובת ה-JSON לרשימת אובייקטי Market
                List<Market> fetchedMarkets = parseMarketsFromJson(response);
                // עדכן את ה-LiveData של השווקים. זה יפעיל את ה-Observer ב-Fragment.
                marketsLiveData.postValue(fetchedMarkets);
                // סמן שהנתונים נטענו בהצלחה
                isMarketsLoaded = true;

            } catch (IOException e) {
                // טיפול בשגיאות רשת
                Log.e("MapViewModel", "Network error loading markets: " + e.getMessage(), e);
                errorMessageLiveData.postValue("שגיאת רשת בטעינת שווקים: " + e.getMessage());
            } catch (JSONException e) {
                // טיפול בשגיאות פענוח JSON
                Log.e("MapViewModel", "JSON parsing error loading markets: " + e.getMessage(), e);
                errorMessageLiveData.postValue("שגיאת פענוח נתונים בטעינת שווקים: " + e.getMessage());
            } finally {
                // תמיד הסתר את אינדיקטור הטעינה בסיום, בין אם הצליח או נכשל
                isLoadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * מפרסר את תגובת ה-JSON מהשרת לרשימת אובייקטי Market.
     * שימו לב: מתודה זו תתעלם משדות כמו 'id', 'hours' ו-'distance'
     * בתגובת השרת, מכיוון שהם אינם קיימים במחלקת Market הנוכחית.
     *
     * @param jsonResponse תגובת ה-JSON מהשרת כמחרוזת.
     * @return רשימת אובייקטי Market.
     * @throws JSONException אם יש שגיאה בפענוח ה-JSON.
     */
    private List<Market> parseMarketsFromJson(String jsonResponse) throws JSONException {
        List<Market> markets = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonResponse);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject marketObj = jsonArray.getJSONObject(i);
            // String id = marketObj.optString("id"); // מתעלם
            String locationName = marketObj.optString("location");
            String dateStr = marketObj.optString("date");
            // String hours = marketObj.optString("hours"); // מתעלם
            double latitude = marketObj.optDouble("latitude");
            double longitude = marketObj.optDouble("longitude");
            // double distance = marketObj.optDouble("distance", -1.0); // מתעלם

            // המרת מחרוזת התאריך ל-LocalDate, כפי שמחלקת Market מצפה
            LocalDate date = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                date = LocalDate.parse(dateStr);
            }

            // יצירת אובייקט Market באמצעות הקונסטרוקטור המקורי שלו
            markets.add(new Market(date, locationName, latitude, longitude));
        }
        return markets;
    }

    /**
     * מאפס את דגל הטעינה כדי לאפשר טעינה מחדש של השווקים.
     * שימושי כאשר רוצים לרענן את נתוני המפה (לדוגמה, לאחר שינוי מיקום המשתמש
     * או כאשר המשתמש מבקש רענון ידני).
     */
    public void resetMarketsLoaded() {
        isMarketsLoaded = false;
        Log.d("MapViewModel", "Markets loaded flag reset.");
    }
}
