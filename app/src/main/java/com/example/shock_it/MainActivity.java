package com.example.shock_it;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import services.Service;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isLoggingIn = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get a handle to the fragment and register the callback
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    public void goToLogin(View view) {
        if (isLoggingIn) return;
        isLoggingIn = true;
        new Thread(() -> {
            try {
                String res = Service.get("users");
                Log.d("OK", res);

                // אם תרצה להחזיר תוצאה למסך או להראות משהו למשתמש
                runOnUiThread(() -> {
                    // כאן אפשר להציג Toast, Alert או לעדכן UI
                    Log.d("UI", "Response from server: " + res);
                    // למשל: Toast.makeText(this, "התחברת בהצלחה", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                Log.e("server error", e.getMessage());

                runOnUiThread(() -> {
                    // אפשר גם להציג הודעת שגיאה
                    // למשל: Toast.makeText(this, "שגיאה בחיבור לשרת", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

        // אפשר לשלוח את המשתמש למסך הבא בינתיים
        Intent intent = new Intent(this, EntryActivity.class);
        startActivity(intent);
    }


    public void goToMarketMap(View view) {
        Intent intent = new Intent(this, Market.class);
        startActivity(intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Enable the my-location layer
            mMap.setMyLocationEnabled(true);

            // Get the last known location and move camera there
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng currentLocation = new LatLng(
                                        location.getLatitude(),
                                        location.getLongitude());

                                mMap.addMarker(new MarkerOptions()
                                        .position(currentLocation)
                                        .title("My Location"));

                                // Move camera to user's location with zoom level 15
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        currentLocation, 15));
                            }
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        }
    }
}