package services;

// emulator ip: 10.0.2.2:3000

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
public class Service {
  //  private final static String spec = "http://192.168.1.10:3000";
    private final static String spec = "http://10.0.2.2:3000";
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
}
