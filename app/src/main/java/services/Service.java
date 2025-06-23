package services;

// emulator ip: 10.0.2.2:3000

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class Service {
  //  private final static String spec = "http://10.0.2.2:3000";
    private final static String spec = "http://192.168.0.104:3000";
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

    public static String addNewMarket(String date, String loc, double latitude, double longitude) {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("date", date);
            jsonParam.put("location", loc);
            jsonParam.put("latitude", latitude);
            jsonParam.put("longitude", longitude);

            Log.d("AddMarket", "Sending data: " + jsonParam.toString());

            // שליחת הבקשה לנתיב markets/addMarket (עם הנתיב המלא)
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


}
