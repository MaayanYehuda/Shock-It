package services;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Service {
    // for the emulator: "http://10.0.2.2:3000"
    private final static String spec = "http://192.168.0.106:3000"; // current ip

    public static String get(String path) throws IOException {
        URL url = new URL(spec+ "/" +path);
        Log.d("Service", "Attempting GET request to: " + url.toString());
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
        conn.setRequestMethod("DELETE");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.connect();

        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        writer.write(data);
        writer.close();

        StringBuffer jsonString = new StringBuffer();
        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
            }
        }
        conn.disconnect();
        return jsonString.toString();
    }


    public static String login(String email, String password) throws IOException {
        String path = "users/login?email=" + URLEncoder.encode(email, "UTF-8") +
                "&password=" + URLEncoder.encode(password, "UTF-8");
        return get(path);
    }


    public static String register(
            String name,
            String email,
            String password,
            String phone,
            String address,
            Double latitude,
            Double longitude,
            Integer notificationRadius) throws IOException, JSONException {

        JSONObject jsonParam = new JSONObject();
        jsonParam.put("name", name);
        jsonParam.put("email", email);
        jsonParam.put("password", password);
        jsonParam.put("phone", phone);
        jsonParam.put("address", address);

        if (latitude != null) {
            jsonParam.put("latitude", latitude);
        }
        if (longitude != null) {
            jsonParam.put("longitude", longitude);
        }
        if (notificationRadius != null) {
            jsonParam.put("notificationRadius", notificationRadius);
        }

        return post("users/register", jsonParam.toString());
    }

    public static String getMarkets() throws IOException{
        return get("markets");
    }

    public static String addNewMarket(String date,String loc, String hours, double latitude, double longitude, String farmerEmail) {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("date", date);
            jsonParam.put("location", loc);
            jsonParam.put("hours", hours);
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

    public static String inviteFarmerToMarket(String marketId, String invitedEmail, String inviterEmail) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("marketId", marketId);
        jsonParam.put("invitedEmail", invitedEmail);
        jsonParam.put("inviterEmail", inviterEmail);
        String response = post("markets/inviteFarmer", jsonParam.toString());
        Log.d("InviteFarmer", "Server response: " + response);
        return response;
    }

    //   searchFarmers
    public static String searchFarmers(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String path = "markets/searchFarmers?query=" + encodedQuery;
        String response = get(path);
        Log.d("SearchFarmers", "Server response: " + response);
        return response;
    }

    // getInvitations
    public static String getInvitations(String email) throws IOException {
        String encodedEmail = URLEncoder.encode(email, "UTF-8");
        String path = "markets/invitations/" + encodedEmail;
        String response = get(path);
        Log.d("GetInvitations", "Server response: " + response);
        return response;
    }

    //  acceptInvitation
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
        Log.d("DeclineInvitation", "Server response: " + response);
        return response;
    }

    public static String marketsByEmail(String email) throws IOException, JSONException{
        String encodedEmail = URLEncoder.encode(email, "UTF-8");
        String path = "markets/farmer-markets/" + encodedEmail;
        String response = get(path);
        Log.d("GetMarkets", "Server response: " + response);
        return response;
    }
    public static String addProductToMarketWithWillBe(String farmerEmail, String marketId, String itemName, double price) throws IOException, JSONException {
        String path = String.format("markets/%s/add-product", marketId);
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("farmerEmail", farmerEmail);
        jsonBody.put("itemName", itemName);
        jsonBody.put("price", price);
        return post(path, jsonBody.toString());
    }

    public static String editProfile(String email, String name, String phone, String address, double longitude, double latitude, double notificationRadius) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("email", email);
        jsonParam.put("name", name);
        jsonParam.put("phone", phone);
        jsonParam.put("address", address);
        jsonParam.put("longitude", longitude);
        jsonParam.put("latitude", latitude);
        jsonParam.put("notificationRadius", notificationRadius);
        String response = put("users/update", jsonParam.toString());
        Log.d("Service", "editProfile server response: " + response);
        return response;
    }
    public static String editItem(String farmerEmail, String originalItemName, String newItemName, double newPrice, String newDescription) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("farmerEmail", farmerEmail);
        jsonParam.put("originalItemName", originalItemName);
        jsonParam.put("newItemName", newItemName);
        jsonParam.put("newPrice", newPrice);
        jsonParam.put("newDescription", newDescription);
        String response = put("items/update", jsonParam.toString());
        Log.d("Service", "editItem server response: " + response);
        return response;
    }

    public static String deleteItem(String farmerEmail, String itemName) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("farmerEmail", farmerEmail);
        jsonParam.put("itemName", itemName);
        Log.d("Service", "Sending delete request for farmer: " + farmerEmail + ", item: " + itemName + " with payload: " + jsonParam.toString());
        String response = delete("items/", jsonParam.toString());
        Log.d("Service", "deleteItem server response: " + response);
        return response;
    }

    public static String sendJoinRequestToMarket(String marketId, String email, JSONArray products) throws IOException, JSONException {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("email", email);
            jsonParam.put("products", products);
            String path = "markets/" + marketId + "/request";
            Log.d("SendRequest", "Sending: " + jsonParam.toString());
            String response = post(path, jsonParam.toString());
            Log.d("SendRequest", "Response: " + response);
            return response;
        } catch (JSONException | IOException e) {
            Log.e("SendRequest", "Error: " + e.getMessage());
            return null;
        }
    }

    public static String getMarketPendingRequests(String marketId) throws IOException {
        String path = "markets/" + marketId + "/requests";
        return get(path);
    }



    public static String approveMarketJoinRequest(String marketId, String farmerEmail) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("farmerEmail", farmerEmail);
        String path = "markets/" + marketId + "/requests/approve";
        return post(path, jsonParam.toString());
    }

    public static String declineMarketJoinRequest(String marketId, String farmerEmail) throws IOException, JSONException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("farmerEmail", farmerEmail);
        String path = "markets/" + marketId + "/requests/decline";
        return put(path, jsonParam.toString());
    }

    // ב-services/Service.java
    public static String getOrderedMarkets(double userLat, double userLon, String currentDate) throws IOException {
        String path = "markets/order";
        path += "?userLat=" + URLEncoder.encode(String.valueOf(userLat), "UTF-8");
        path += "&userLon=" + URLEncoder.encode(String.valueOf(userLon), "UTF-8");
        path += "&currentDate=" + URLEncoder.encode(currentDate, "UTF-8");
        return get(path);
    }

    public static String getRecomendedMarketsByYourRadius(String userEmail) throws IOException {
        String path = "users/findRecomendations?email=" + userEmail;
        return get(path);
    }

    public static String search(String query, double userLat, double userLon) throws IOException {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String path = "markets/search?query=" + encodedQuery + "&userLat=" + userLat + "&userLon=" + userLon;
            System.out.println("Final URL being sent: " +spec + path);
            return get(path);
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Error encoding search query", e);
        }
    }

    public static int getPendingInvitesCount(String userEmail) throws IOException, JSONException {
        String urlString = "users/invitations/count?email=" + userEmail;
        String jsonResponse = get(urlString);
        JSONObject jsonObject = new JSONObject(jsonResponse);
        return jsonObject.getInt("count");
    }
}
