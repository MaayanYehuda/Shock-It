package services;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray; // נשאר כי הוא נחוץ לניתוח JSON במקומות אחרים

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Service {
    // ⚠️ וודא שזה ה-IP הנכון של השרת שלך!
    // אם אתה מריץ על אמולטור: "http://10.0.2.2:3000"
    // אם אתה מריץ על מכשיר פיזי באותה רשת Wi-Fi: "http://192.168.1.10:3000" (או ה-IP הספציפי שלך)
    private final static String spec = "http://192.168.1.10:3000"; // השארתי את ה-IP ששלחת

    public static String get(String path) throws IOException {
        URL url = new URL(spec+ "/" +path);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        InputStream in = new BufferedInputStream(conn.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        String line;
        while((line=reader.readLine())!=null)
            result.append(line);
        reader.close();
        in.close();
        conn.disconnect();
        return result.toString();
    }



    public static String post(String path, String data) throws IOException {
        URL url = new URL(spec+ "/" +path);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(false);
        conn.connect();
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        writer.write(data);
        writer.close();
        StringBuffer jsonString = new StringBuffer();
        try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while((line = br.readLine()) != null){
                jsonString.append(line);
            }
            br.close();
        } catch (Exception ex) {
            Log.d("error",ex.getMessage()+" boo");
        }
        conn.disconnect();
        return jsonString.toString();
    }

    public static String put(String path, String data) throws IOException {
        URL url = new URL(spec + "/" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
            writer.write(data);
        }

        StringBuffer jsonString = new StringBuffer();
        int responseCode = conn.getResponseCode();
        InputStream responseStream = null;

        if (responseCode >= 200 && responseCode < 300) {
            responseStream = conn.getInputStream();
        } else {
            responseStream = conn.getErrorStream();
        }

        if (responseStream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(responseStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
            }
        } else {
            Log.w("Service", "No response stream available for HTTP " + responseCode + " for path: " + path);
        }

        conn.disconnect();
        return jsonString.toString();
    }

    public static String delete(String path, String data) throws IOException {
        URL url = new URL(spec + "/" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestMethod("DELETE"); // שינוי ל-DELETE
        conn.setDoOutput(true); // מאפשר שליחת גוף בקשה עם DELETE
        conn.setDoInput(true);
        conn.connect();

        // שליחת הנתונים בגוף הבקשה
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        writer.write(data);
        writer.close();

        // קריאת התגובה מהשרת
        StringBuffer jsonString = new StringBuffer();
        int responseCode = conn.getResponseCode(); // קבלת קוד התגובה
        if (responseCode >= 200 && responseCode < 300) { // הצלחה (2xx)
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
            }
        } else { // שגיאה (4xx, 5xx)
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
            }
            // אתה יכול גם לזרוק IOException כאן אם תרצה לטפל בשגיאות HTTP ספציפיות
            // throw new IOException("Server returned HTTP " + responseCode + ": " + jsonString.toString());
        }
        conn.disconnect();
        return jsonString.toString();
    }


    public static String login(String email, String password) throws IOException {
        String path = "users/login?email=" + URLEncoder.encode(email, "UTF-8") +
                "&password=" + URLEncoder.encode(password, "UTF-8");
        return get(path);
    }


    public static String register(String name, String email, String password, String phone, String address) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("name", name);
        jsonParam.put("email", email);
        jsonParam.put("password", password);
        jsonParam.put("phone", phone);
        jsonParam.put("address", address);

        return post("users/register", jsonParam.toString());
    }

    public static String getMarkets() throws IOException{
        return get("markets");
    }

    public static String addNewMarket(String date, String loc, double latitude, double longitude, String farmerEmail) {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("date", date);
            jsonParam.put("location", loc);
            jsonParam.put("latitude", latitude);
            jsonParam.put("longitude", longitude);
            jsonParam.put("farmerEmail", farmerEmail);

            Log.d("AddMarket", "Sending data: " + jsonParam.toString());
            Log.d("AddMarket", "FARMER EMAIL: " + farmerEmail);


            String response = post("markets/addMarket", jsonParam.toString());
            Log.d("RESPONSE", "Server response: " + response);
            return response;

        } catch (JSONException e) {
            Log.e("AddMarket", "Error creating JSON: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e("AddMarket", "Network error: " + e.getMessage());
            return null;
        }
    }


    public static String getUserProfile(String email) throws IOException {
        String path = "users/profile?email=" + URLEncoder.encode(email, "UTF-8");
        return get(path);
    }
    public static String getMarketProfile(String location, String date) throws IOException {
        String path = "markets/profile?location=" + URLEncoder.encode(location, "UTF-8") +
                "&date=" + URLEncoder.encode(date, "UTF-8");
        return get(path);
    }
    public static String getFarmerItems(String farmerEmail) throws IOException {
        return get("items?farmerEmail=" + farmerEmail);
    }
    public static String addNewItem(String name, String desc, double price, String farmerEmail) {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("name", name);
            jsonParam.put("description", desc);
            jsonParam.put("price", price);
            jsonParam.put("farmerEmail", farmerEmail);

            return post("items/add", jsonParam.toString());

        } catch (JSONException | IOException e) {
            Log.e("AddItem", "Error creating or sending JSON: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // 🆕 הוספה: inviteFarmerToMarket
    public static String inviteFarmerToMarket(String marketId, String invitedEmail, String inviterEmail) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("marketId", marketId);
        jsonParam.put("invitedEmail", invitedEmail);
        jsonParam.put("inviterEmail", inviterEmail);

        String response = post("markets/inviteFarmer", jsonParam.toString());
        Log.d("InviteFarmer", "Server response: " + response);
        return response;
    }

    // 🆕 הוספה: searchFarmers
    public static String searchFarmers(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String path = "markets/searchFarmers?query=" + encodedQuery;
        String response = get(path);
        Log.d("SearchFarmers", "Server response: " + response);
        return response;
    }

    // 🆕 הוספה: getInvitations
    public static String getInvitations(String email) throws IOException {
        String encodedEmail = URLEncoder.encode(email, "UTF-8");
        String path = "markets/invitations/" + encodedEmail;
        String response = get(path);
        Log.d("GetInvitations", "Server response: " + response);
        return response;
    }

    // 🆕 הוספה: acceptInvitation
    public static String acceptInvitation(String email, String marketId) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("email", email);
        jsonParam.put("marketId", marketId);
        String response = put("markets/acceptInvitation", jsonParam.toString());
        Log.d("AcceptInvitation", "Server response: " + response);

        return response;
    }
    public static String declineInvitation(String email, String marketId) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("email", email);
        jsonParam.put("marketId", marketId);
        String response = delete("markets/declineInvitation", jsonParam.toString());
        Log.d("DeclineInvitation", "Server response: " + response); // שינוי לוג
        return response;
    }

    public static String marketsByEmail(String email) throws IOException, JSONException{
        String encodedEmail = URLEncoder.encode(email, "UTF-8");
        String path = "markets/farmer-markets/" + encodedEmail;
        String response = get(path);
        Log.d("GetMarkets", "Server response: " + response);
        return response;
    }
}
