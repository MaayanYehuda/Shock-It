package com.example.shock_it;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.ui.map.MarketAdapter;
import com.example.shock_it.SearchResultAdapter;
import com.example.shock_it.ui.map.farmerProfile.farmerProfile;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import classes.Farmer;
import classes.Market;
import services.Service;

public class MainActivity extends AppCompatActivity implements
        SearchResultAdapter.OnSearchResultClickListener,
        GoogleMap.OnMarkerClickListener {

    private GoogleMap mGoogleMap;
    // המשתנים הללו ישמשו לטעינה ראשונית, אבל לא לחיפוש
    private double lat = 0.0;
    private double lot = 0.0;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private RecyclerView recyclerView;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private HashMap<Marker, Market> markerMarketMap = new HashMap<>();
    private EditText searchEditText;
    // עכשיו זהו כפתור מסוג Button כפי שהגדרנו ב-XML
    private ImageButton searchButton;
    private ImageButton clearSearchButton;
    private ImageButton backButton;

    // A list to store the initial markets for the "back" button functionality
    private List<Object> initialCombinedResults = new ArrayList<>();
    private List<Object> combinedResults = new ArrayList<>();
    private SearchResultAdapter searchResultAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString("user_email", null);
        if (userEmail != null) {
            Log.d("MainActivity", "User email found: " + userEmail + ". Redirecting to FarmerHomeActivity.");
            Intent intent = new Intent(this, FarmerHomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        Log.d("MainActivity", "User email is null. Setting content view.");
        setContentView(R.layout.activity_main);

        searchButton = findViewById(R.id.searchButton);
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        backButton = findViewById(R.id.backButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FloatingActionButton farmerButton = findViewById(R.id.farmerButton);
        farmerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntryActivity.class);
            Toast.makeText(this, "מעבר לאזור החקלאים", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchResultAdapter = new SearchResultAdapter(combinedResults, this);
        recyclerView.setAdapter(searchResultAdapter);

        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD"));
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);

        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(200); // Updated peek height to show the full search bar
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Click listener for the new search button
        searchButton.setOnClickListener(v -> performSearch());

        // Click listener for the new clear button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            // Optionally, restore the original market list here as well
            // restoreInitialMarkets();
        });

        // Click listener for the new back button
        backButton.setOnClickListener(v -> restoreInitialMarkets());

        // Update the search EditText listener for the "enter" key
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearSearchButton.setVisibility(View.VISIBLE);
            } else {
                if (searchEditText.getText().toString().isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mGoogleMap = googleMap;
                mGoogleMap.setOnMarkerClickListener(this);

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

            });
        }
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();

                // 1. בודקים אם יש פרגמנט במחסנית
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    // אם יש: מאפשרים התנהגות חזרה רגילה (הוצאת הפרגמנט)
                    fragmentManager.popBackStack();

                    // 2. מחזירים את הנראות של הרכיבים הראשיים
                    View mapContainer = findViewById(R.id.map_container);
                    View fragmentContainer = findViewById(R.id.fragment_container_farmer_profile);
                    FloatingActionButton fab = findViewById(R.id.farmerButton);
                    View bottomSheetContent = findViewById(R.id.bottom_sheet);

                    if (mapContainer != null) mapContainer.setVisibility(View.VISIBLE);
                    if (fab != null) fab.setVisibility(View.VISIBLE);

                    // מחזירים את ה-BottomSheet למצב COLLAPSED ומונעים הסתרה
                    if (bottomSheetContent != null && bottomSheetBehavior != null) {
                        bottomSheetContent.setVisibility(View.VISIBLE);
                        bottomSheetBehavior.setHideable(false); // חזרה למצב המקורי
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }

                    // מסתירים את קונטיינר הפרגמנט
                    if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);

                } else {
                    // אם אין: מאפשרים לאפליקציה לצאת
                    setEnabled(false);
                    onBackPressed();
                    setEnabled(true); // מחזירים מצב כדי שה-Callback יעבוד שוב בכניסה הבאה
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void restoreInitialMarkets() {
        Log.d("MainActivity", "Restoring initial markets list.");
        searchEditText.setText("");
        backButton.setVisibility(View.GONE);
        clearSearchButton.setVisibility(View.GONE);
        updateUIWithResults(initialCombinedResults);
    }

    private void performSearch() {
        String query = searchEditText.getText().toString().trim();
        Log.d("MainActivity", "Starting search for query: " + query);

        if (query.isEmpty()) {
            Toast.makeText(MainActivity.this, "הכנס מילת חיפוש", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "Search query is empty.");
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                double searchLat;
                double searchLot;

                if (location != null) {
                    searchLat = location.getLatitude();
                    searchLot = location.getLongitude();
                } else {
                    Log.w("MainActivity", "Last location not available, using default location.");
                    searchLat = 32.0853; // Tel Aviv Latitude
                    searchLot = 34.7818; // Tel Aviv Longitude
                    Toast.makeText(MainActivity.this, "לא ניתן היה לקבל את מיקומך. מוצגות תוצאות מבוססות מיקום ברירת מחדל.", Toast.LENGTH_LONG).show();
                }
                executeSearch(query, searchLat, searchLot);
            });
        } else {
            Toast.makeText(MainActivity.this, "אין הרשאת מיקום, מוצגות תוצאות מבוססות מיקום ברירת מחדל.", Toast.LENGTH_LONG).show();
            executeSearch(query, 32.0853, 34.7818); // Tel Aviv
        }
    }

    private void executeSearch(String query, double currentLat, double currentLot) {
        new Thread(() -> {
            try {
                Log.d("MainActivity", "Calling Service.search with query: " + query + ", lat: " + currentLat + ", lon: " + currentLot);
                String response = Service.search(query, currentLat, currentLot);
                Log.d("MainActivity", "Search response received: " + response);

                JSONObject jsonResponse = new JSONObject(response);
                JSONArray jsonResults = jsonResponse.getJSONArray("results");

                List<Object> searchResults = new ArrayList<>();

                for (int i = 0; i < jsonResults.length(); i++) {
                    JSONObject obj = jsonResults.getJSONObject(i);
                    String type = obj.getString("type");

                    if ("Market".equals(type)) {
                        String location = obj.getString("location");
                        String dateStr = obj.getString("date");
                        String hours= obj.getString("hours");

                        double marketLat = obj.optDouble("latitude", 0.0);
                        double marketLng = obj.optDouble("longitude", 0.0);

                        LocalDate date = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            date = LocalDate.parse(dateStr);
                        }
                        Market market = new Market(date, location, hours ,marketLat, marketLng);
                        searchResults.add(market);
                        Log.d("MainActivity", "Found Market: " + market.getLocation());
                    } else if ("Farmer".equals(type)) {
                        String name = obj.optString("name", "שם לא ידוע");
                        String email = obj.getString("email");
                        Farmer farmer = new Farmer(name, email);

                        searchResults.add(farmer);
                        Log.d("MainActivity", "Found Farmer: " + name);

                        // ** שינוי קריטי כאן: פענוח ושיטוח רשימת השווקים של החקלאי **
                        if (obj.has("participatingMarkets")) {
                            JSONArray participatingMarketsJson = obj.getJSONArray("participatingMarkets");
                            for (int j = 0; j < participatingMarketsJson.length(); j++) {
                                JSONObject marketObj = participatingMarketsJson.getJSONObject(j);
                                String marketLocation = marketObj.getString("location");
                                String marketDateStr = marketObj.getString("date");
                                String marketHours= marketObj.getString("hours");
                                double marketLat = marketObj.optDouble("latitude", 0.0);
                                double marketLng = marketObj.optDouble("longitude", 0.0);

                                LocalDate marketDate = null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    marketDate = LocalDate.parse(marketDateStr);
                                }
                                Market market = new Market(marketDate, marketLocation,marketHours, marketLat, marketLng);
                                searchResults.add(market);
                                Log.d("MainActivity", "Found Market for Farmer " + name + ": " + marketLocation);
                            }
                        }
                    }
                }

                runOnUiThread(() -> {
                    updateUIWithResults(searchResults);
                    backButton.setVisibility(View.VISIBLE); // Show back button after search
                    searchEditText.setText(query); // Keep the query in the EditText
                    clearSearchButton.setVisibility(View.VISIBLE); // Show the clear button
                    Log.d("MainActivity", "Search finished, showing results.");
                });

            } catch (Exception e) {
                Log.e("MainActivity", "Error performing search. Exception: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "שגיאה בחיפוש", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    // --- פונקציות קיימות (שינוי קל ב-enableMyLocation) ---
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
                            this.lat = location.getLatitude();
                            this.lot = location.getLongitude();
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        } else {
                            LatLng defaultLocation = new LatLng(32.0853, 34.7818); // Tel Aviv
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
                        }
                        // Now that we have the location, load the initial markets
                        loadMarkets();
                    });
        }
    }

    private void loadMarkets() {
        new Thread(() -> {
            try {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String response = Service.getOrderedMarkets(lat, lot, currentDate);
                JSONArray jsonArray = new JSONArray(response);
                List<Market> markets = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String location = obj.getString("location");
                    String dateStr = obj.getString("date");
                    String hoursStr=obj.getString("hours");
                    double lat = obj.getDouble("latitude");
                    double lng = obj.getDouble("longitude");

                    LocalDate date = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        date = LocalDate.parse(dateStr);
                    }
                    markets.add(new Market(date, location,hoursStr, lat, lng));
                }

                // Store the initial markets list
                initialCombinedResults.clear();
                initialCombinedResults.addAll(markets);

                // Update the UI with the initial markets
                runOnUiThread(() -> {
                    updateUIWithResults(initialCombinedResults);
                });

            } catch (Exception e) {
                Log.e("MainActivity", "Error loading markets: " + e.getMessage(), e);
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "שגיאה בטעינת השווקים", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // New helper method to update UI with any list of results
    private void updateUIWithResults(List<Object> results) {
        combinedResults.clear();
        combinedResults.addAll(results);

        mGoogleMap.clear();
        markerMarketMap.clear();

        for (Object result : results) {
            if (result instanceof Market) {
                Market market = (Market) result;
                LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(market.getLocation())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                if (marker != null) {
                    markerMarketMap.put(marker, market);
                }
            }
        }

        searchResultAdapter.notifyDataSetChanged();
        Log.d("MainActivity", "RecyclerView adapter updated. Total results: " + combinedResults.size());

        if (!combinedResults.isEmpty()) {
            Object firstResult = combinedResults.get(0);
            if (firstResult instanceof Market) {
                Market market = (Market) firstResult;
                LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                Log.d("MainActivity", "Animating map to first market result.");
            }
        } else {
            Toast.makeText(MainActivity.this, "לא נמצאו תוצאות לחיפוש", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "No search results found.");
        }
    }


    @Override
    public void onMarketClick(Market market) {
        Log.d("MainActivity", "List item clicked: " + market.getLocation() + ", " + market.getDate());

        if (mGoogleMap != null) {
            LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Market market = markerMarketMap.get(marker);
        if (market != null) {
            Log.d("MainActivity", "Marker clicked: " + market.getLocation());

            Intent intent = new Intent(MainActivity.this, MarketProfileActivity.class);
            intent.putExtra("location", market.getLocation());
            if (market.getDate() != null) {
                intent.putExtra("date", market.getDate().toString());
            } else {
                intent.putExtra("date", "Unknown Date");
            }
            startActivity(intent);
        } else {
            Log.w("MainActivity", "Market object not found for clicked marker.");
        }
        return true;
    }


    @Override
    public void onFarmerClick(Farmer farmer) {
        Log.d("MainActivity", "Farmer clicked. Navigating to farmer profile fragment locally. Email: " + farmer.getEmail());

        // 1. הסתרת המקלדת
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        // 2. הכנת ה-Fragment וה-Bundle
        farmerProfile farmerProfileFragment = new farmerProfile();
        Bundle args = new Bundle();
        args.putString("farmer_email_key", farmer.getEmail());
        farmerProfileFragment.setArguments(args);

        // 3. שינוי נראות הרכיבים
        Log.d("MainActivity", "Hiding main UI elements...");

        View mapContainer = findViewById(R.id.map_container);
        View fragmentContainer = findViewById(R.id.fragment_container_farmer_profile);
        FloatingActionButton fab = findViewById(R.id.farmerButton);
        View bottomSheetContent = findViewById(R.id.bottom_sheet);

        // הסתרת המפה
        if (mapContainer != null) mapContainer.setVisibility(View.GONE);

        // הסתרת הכפתור הצף
        if (fab != null) fab.setVisibility(View.GONE);

        // הסתרת ה-BottomSheet (שמכיל את ה-RecyclerView ואת סרגל החיפוש)
        if (bottomSheetContent != null && bottomSheetBehavior != null) {
            bottomSheetContent.setVisibility(View.GONE);
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        // 4. הצגת ה-Container וביצוע החלפת ה-Fragment
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
            Log.d("MainActivity", "Farmer profile container set to VISIBLE.");

            // ביצוע החלפת ה-Fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container_farmer_profile, farmerProfileFragment);
            fragmentTransaction.addToBackStack("farmerProfile");
            fragmentTransaction.commit();

            Toast.makeText(this, "טוען פרופיל של: " + farmer.getEmail(), Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "Fragment transaction committed.");

        } else {
            Log.e("MainActivity", "FATAL ERROR: fragment_container_farmer_profile not found! Check XML.");
            Toast.makeText(MainActivity.this, "שגיאה: לא נמצא קונטיינר להצגת הפרופיל. הוסף ל-XML.", Toast.LENGTH_LONG).show();
        }
    }
}
