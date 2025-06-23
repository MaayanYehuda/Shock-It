package com.example.shock_it;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.ui.map.MarketAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import classes.Market;
import services.Service;

public class MainActivity extends AppCompatActivity {
    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private RecyclerView recyclerView;
    private BottomSheetBehavior<View> bottomSheetBehavior;

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

// הוספת קו מפריד בין פריטים ברשימה
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(android.graphics.Color.parseColor("#DDDDDD")); // קו אפור-בהיר
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);


        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();


        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mGoogleMap = googleMap;

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
                loadMarkets();

            });
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
                            LatLng defaultLocation = new LatLng(32.0853, 34.7818); // תל אביב
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
                    for (Market market : markets) {
                        LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                        mGoogleMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(market.getLocation())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                    }

                    recyclerView.setAdapter(new MarketAdapter(markets, market -> {
                        LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                        Intent intent = new Intent(MainActivity.this, MarketProfileActivity.class);
                        intent.putExtra("location", market.getLocation());
                        intent.putExtra("date", market.getDate().toString()); // נניח שזה LocalDate
                        startActivity(intent);
                    }));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}






