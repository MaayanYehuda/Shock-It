package com.example.shock_it;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color; // Import Color class
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.ui.map.MarketAdapter; // Ensure this import is correct
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker; // Import Marker class
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap; // For linking markers to Market objects
import java.util.List;

import classes.Market; // Ensure this import is correct
import services.Service;

// MainActivity must implement both MarketAdapter.OnMarketClickListener and GoogleMap.OnMarkerClickListener
public class MainActivity extends AppCompatActivity implements
        MarketAdapter.OnMarketClickListener, // For clicks on RecyclerView items
        GoogleMap.OnMarkerClickListener { // For clicks on map markers

    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private RecyclerView recyclerView;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private HashMap<Marker, Market> markerMarketMap = new HashMap<>(); // Map to link Marker to Market object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FloatingActionButton farmerButton = findViewById(R.id.farmerButton);
        farmerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntryActivity.class);
            Toast.makeText(this, "מעבר לאזור החקלאים", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Add item decoration (dividers)
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD")); // Changed to android.graphics.Color
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);

        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_container); // Assuming a container ID in XML
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map_container, mapFragment) // Use your actual container ID
                    .commit();
            getSupportFragmentManager().executePendingTransactions(); // Ensure fragment is added immediately
        }


        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mGoogleMap = googleMap;
                mGoogleMap.setOnMarkerClickListener(this); // **IMPORTANT: MainActivity is the marker click listener**

                try {
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
                    if (!success) {
                        Log.e("MapStyle", "Style parsing failed.");
                    }
                } catch (Resources.NotFoundException e) {
                    Log.e("MapStyle", "Can't find style. Error: ", e);
                }
                checkLocationPermission();
                loadMarkets(); // Load markets after map is ready
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "הרשאת מיקום נדחתה. לא ניתן להציג את מיקומך.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGoogleMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        } else {
                            LatLng defaultLocation = new LatLng(32.0853, 34.7818); // Tel Aviv
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
                        }
                    });
        }
    }

    private void loadMarkets() {
        new Thread(() -> {
            try {
                String response = Service.getMarkets();
                JSONArray jsonArray = new JSONArray(response);
                List<Market> markets = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String location = obj.getString("location");
                    String dateStr = obj.getString("date");
                    double lat = obj.getDouble("latitude");
                    double lng = obj.getDouble("longitude");

                    LocalDate date = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        date = LocalDate.parse(dateStr);
                    }

                    markets.add(new Market(date, location, lat, lng));
                }

                runOnUiThread(() -> {
                    mGoogleMap.clear(); // Clear existing markers
                    markerMarketMap.clear(); // Clear the map linking markers to Market objects

                    for (Market market : markets) {
                        LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(market.getLocation())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                        if (marker != null) {
                            markerMarketMap.put(marker, market); // Store the Market object with its Marker
                        }
                    }

                    // Initialize MarketAdapter and pass 'this' as the OnMarketClickListener
                    // This adapter will handle clicks on RecyclerView items
                    recyclerView.setAdapter(new MarketAdapter(markets, this)); // Pass 'this'
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error loading markets: " + e.getMessage(), e);
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "שגיאה בטעינת השווקים", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // --- Implement the OnMarketClickListener interface method (for RecyclerView list items) ---
    @Override
    public void onMarketClick(Market market) {
        // When a list item is clicked, animate the map camera to the market's location
        Log.d("MainActivity", "List item clicked: " + market.getLocation() + ", " + market.getDate());

        if (mGoogleMap != null) {
            LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
            // You might also want to expand the BottomSheet here, or set its state
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // Or STATE_EXPANDED
        }
    }

    // --- Implement the GoogleMap.OnMarkerClickListener interface method (for map markers) ---
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        // When a map marker is clicked, open MarketProfileActivity
        Market market = markerMarketMap.get(marker); // Retrieve the Market object linked to this marker
        if (market != null) {
            Log.d("MainActivity", "Marker clicked: " + market.getLocation());

            Intent intent = new Intent(MainActivity.this, MarketProfileActivity.class);
            intent.putExtra("location", market.getLocation());
            if (market.getDate() != null) {
                intent.putExtra("date", market.getDate().toString()); // Convert LocalDate to String for Intent
            } else {
                intent.putExtra("date", "Unknown Date"); // Default if date is null
            }
            startActivity(intent);
        } else {
            Log.w("MainActivity", "Market object not found for clicked marker.");
        }
        return true; // Return true to consume the event and prevent default behavior (like info window)
    }
}