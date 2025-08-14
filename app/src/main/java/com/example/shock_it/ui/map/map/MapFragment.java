package com.example.shock_it.ui.map.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.NotificationsActivity;
import com.example.shock_it.MarketProfileActivity;
import com.example.shock_it.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;

import classes.Market;
import classes.Farmer;

public class MapFragment extends Fragment implements
        LocationAdapter.OnLocationClickListener,
        GoogleMap.OnMarkerClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private MapViewModel mapViewModel;
    private GoogleMap mGoogleMap;
    private LocationAdapter locationAdapter;
    private RecyclerView recyclerView;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private HashMap<Marker, Object> markerObjectMap = new HashMap<>();
    private Location currentUserLocation;

    private ImageButton openSearchButton;
    private LinearLayout searchContainer;
    private EditText searchEditText;
    private ImageButton searchButton;
    private ImageButton clearSearchButton;
    private ImageButton backButton;
    private TextView nearbyMarketsTitle; // הוספנו את ה-TextView כדי שנוכל לשנות את הטקסט שלו

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        // מציאת הרכיבים מה-XML
        recyclerView = rootView.findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        locationAdapter = new LocationAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(locationAdapter);

        // הגדרת קו מפריד ב-RecyclerView
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD"));
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);

        // צפייה בשינויים ברשימת המיקומים (שווקים/חקלאים)
        mapViewModel.getLocationsLiveData().observe(getViewLifecycleOwner(), locations -> {
            Log.d("MapFragment", "ViewModel locations updated. Updating UI components. Locations count: " + (locations != null ? locations.size() : 0));
            locationAdapter.setLocationList(locations);
            Log.d("MapFragment", "RecyclerView adapter updated and notified.");
            updateMapMarkers(locations);
            // אם הרשימה מעודכנת בעקבות חיפוש, נציג את כפתור החזרה.
            if (!searchEditText.getText().toString().isEmpty()) {
                backButton.setVisibility(View.VISIBLE);
                nearbyMarketsTitle.setText("תוצאות חיפוש"); // שינוי הכותרת
            } else {
                // אחרת, נסתיר אותו
                backButton.setVisibility(View.GONE);
                nearbyMarketsTitle.setText("רשימת שווקים קרובים"); // החזרת הכותרת למצב הראשוני
            }
        });

        mapViewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            // טיפול במצב טעינה
        });

        mapViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // הגדרת המפה
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mGoogleMap = googleMap;
                mGoogleMap.setOnMarkerClickListener(this);

                try {
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
                    if (!success) {
                        Log.e("MapStyle", "Style parsing failed.");
                    }
                } catch (Resources.NotFoundException e) {
                    Log.e("MapStyle", "Can't find style. Error: ", e);
                }
                checkLocationPermission();

                List<Object> currentLocations = mapViewModel.getLocationsLiveData().getValue();
                if (currentLocations != null && !currentLocations.isEmpty()) {
                    Log.d("MapFragment", "Map ready, updating with existing ViewModel data.");
                    updateMapMarkers(currentLocations);
                }
            });
        }

        // הגדרת ה-BottomSheet
        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setPeekHeight(180);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // הגדרת כפתור ההודעות
        FloatingActionButton messagesButton = rootView.findViewById(R.id.messages);
        if (messagesButton != null) {
            messagesButton.setOnClickListener(v -> {
                Log.d("MapFragment", "Navigating to Notifications Activity...");
                Intent intent = new Intent(requireContext(), NotificationsActivity.class);
                startActivity(intent);
            });
        }

        // איתור רכיבי החיפוש עם ה-IDs הנכונים מה-XML
        openSearchButton = rootView.findViewById(R.id.openSearchButton);
        searchContainer = rootView.findViewById(R.id.searchContainer);
        searchEditText = rootView.findViewById(R.id.searchEditText);
        searchButton = rootView.findViewById(R.id.searchButton);
        clearSearchButton = rootView.findViewById(R.id.clearSearchButton);
        backButton = rootView.findViewById(R.id.backButton); // מציאת כפתור החזרה החדש
        nearbyMarketsTitle = rootView.findViewById(R.id.nearbyMarketsTitle); // מציאת הכותרת

        // הגדרת ה-OnClickListener לכפתור הפותח את החיפוש
        openSearchButton.setOnClickListener(v -> {
            if (searchContainer.getVisibility() == View.GONE) {
                searchContainer.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
            } else {
                searchContainer.setVisibility(View.GONE);
            }
        });

        // לחיצה על כפתור החיפוש הפנימי
        searchButton.setOnClickListener(v -> performSearch());

        // לחיצה על "אישור" במקלדת
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // לחיצה על כפתור ניקוי החיפוש
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            // אין צורך לקרוא ל-performSearch כאן, ה-TextWatcher יטפל בזה
        });

        // לחיצה על כפתור החזרה
        backButton.setOnClickListener(v -> resetToInitialState());

        // TextWatcher לטיפול בשינויים בתיבת הטקסט
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // אם יש טקסט בתיבה, נציג את כפתור הניקוי
                if (s.length() > 0) {
                    clearSearchButton.setVisibility(View.VISIBLE);
                } else {
                    clearSearchButton.setVisibility(View.GONE);
                    // אם מנקים את הטקסט, נחזור אוטומטית למצב ההתחלתי
                    if (backButton.getVisibility() == View.VISIBLE) {
                        resetToInitialState();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return rootView;
    }

    private void performSearch() {
        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "אנא הזן מילת חיפוש.", Toast.LENGTH_SHORT).show();
            resetToInitialState();
        } else {
            if (currentUserLocation != null) {
                mapViewModel.searchLocations(query, currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
                // כפתור החזרה יהפוך לגלוי בעקבות עדכון הנתונים ב-LiveData
            } else {
                Toast.makeText(requireContext(), "לא ניתן לבצע חיפוש ללא מיקום משתמש.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // שיטה חדשה לחזרה למצב ההתחלתי
    private void resetToInitialState() {
        searchEditText.setText(""); // ניקוי תיבת החיפוש
        backButton.setVisibility(View.GONE); // הסתרת כפתור החזרה
        nearbyMarketsTitle.setText("רשימת שווקים קרובים"); // החזרת הכותרת למצב הראשוני
        mapViewModel.resetMarketsLoaded();
        if (currentUserLocation != null) {
            mapViewModel.loadMarkets(currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
        } else {
            Log.w("MapFragment", "Last known location is null. Loading markets without user location.");
            mapViewModel.loadMarkets(0.0, 0.0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // מוודאים שהאפליקציה מתחילה תמיד במצב הראשוני
        resetToInitialState();
        checkLocationPermission();
    }

    private void updateMapMarkers(List<Object> locations) {
        if (mGoogleMap != null && locations != null) {
            Log.d("MapFragment", "Updating map markers. Number of locations: " + locations.size());
            mGoogleMap.clear();
            markerObjectMap.clear();

            for (Object obj : locations) {
                if (obj instanceof Market) {
                    Market market = (Market) obj;
                    LatLng latLng = new LatLng(market.getLatitude(), market.getLongitude());

                    float[] results = new float[1];
                    double calculatedDistance = -1.0;
                    if (currentUserLocation != null) {
                        Location.distanceBetween(
                                currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                                market.getLatitude(), market.getLongitude(),
                                results
                        );
                        calculatedDistance = results[0];
                    }

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(latLng)
                            .title(market.getLocation() + " - " + market.getDate());

                    if (calculatedDistance != -1.0) {
                        markerOptions.snippet("מרחק: " + String.format(Locale.getDefault(), "%.2f ק\"מ", calculatedDistance / 1000.0));
                    } else {
                        markerOptions.snippet("מרחק: לא זמין");
                    }

                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.market));
                    Marker marker = mGoogleMap.addMarker(markerOptions);
                    if (marker != null) {
                        markerObjectMap.put(marker, market);
                    }
                }
                // ניתן להוסיף כאן לוגיקה גם עבור Farmer אם רוצים להציג אותם על המפה
            }
            if (!locations.isEmpty() && currentUserLocation != null) {
                // נעביר את המפה למיקום המשתמש רק אם יש מיקום
                LatLng userLatLng = new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10));
            } else if (!locations.isEmpty()){
                // אם אין מיקום משתמש אבל יש שווקים, נעבור לשוק הראשון
                Object firstLocation = locations.get(0);
                if (firstLocation instanceof Market) {
                    Market firstMarket = (Market) firstLocation;
                    LatLng firstMarketPos = new LatLng(firstMarket.getLatitude(), firstMarket.getLongitude());
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstMarketPos, 12));
                }
            }
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocationAndLoadMarkets();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationAndLoadMarkets();
            } else {
                Toast.makeText(requireContext(), "נדרשת הרשאת מיקום כדי להציג שווקים קרובים. מציג שווקים כלליים.", Toast.LENGTH_LONG).show();
                mapViewModel.loadMarkets(0.0, 0.0);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationAndLoadMarkets() {
        if (mGoogleMap != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGoogleMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentUserLocation = location;
                                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10));
                                mapViewModel.loadMarkets(location.getLatitude(), location.getLongitude());
                            } else {
                                Log.w("MapFragment", "Last known location is null. Loading markets without user location.");
                                Toast.makeText(requireContext(), "לא ניתן לקבל מיקום מדויק. מציג שווקים כלליים.", Toast.LENGTH_LONG).show();
                                mapViewModel.loadMarkets(0.0, 0.0);
                            }
                        }
                    });
        }
    }

    @Override
    public void onLocationClick(Object location) {
        Log.d("MapFragment", "List item clicked: " + location.toString());
        if (mGoogleMap != null) {
            LatLng pos = null;
            if (location instanceof Market) {
                Market market = (Market) location;
                pos = new LatLng(market.getLatitude(), market.getLongitude());
            } else if (location instanceof Farmer) {
                Farmer farmer = (Farmer) location;
                pos = new LatLng(farmer.getLatitude(), farmer.getLongitude());
            }

            if (pos != null) {
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    private Marker lastClickedMarker = null;
    private int normalMarketIcon = R.drawable.market;
    private int selectedMarketIcon = R.drawable.ic_selected_market;

    @SuppressLint("NewApi")
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object obj = markerObjectMap.get(marker);
        if (obj instanceof Market) {
            Market market = (Market) obj;
            Log.d("MapFragment", "Marker clicked: " + market.getLocation());

            if (lastClickedMarker != null && !lastClickedMarker.equals(marker)) {
                lastClickedMarker.setIcon(BitmapDescriptorFactory.fromResource(normalMarketIcon));
            }

            marker.setIcon(BitmapDescriptorFactory.fromResource(selectedMarketIcon));
            lastClickedMarker = marker;

            LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            Intent intent = new Intent(requireContext(), MarketProfileActivity.class);
            intent.putExtra("location", market.getLocation());
            if (market.getDate() != null) {
                intent.putExtra("date", market.getDate().toString());
            } else {
                intent.putExtra("date", "Unknown Date");
            }
            startActivity(intent);

            return true;
        } else {
            Log.w("MapFragment", "Market object not found for clicked marker.");
            return false;
        }
    }
}
