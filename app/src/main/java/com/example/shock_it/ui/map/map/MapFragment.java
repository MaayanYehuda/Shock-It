package com.example.shock_it.ui.map.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback; // 🌟 הוספת אימפורט ל-OnBackPressedCallback
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
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
    private TextView nearbyMarketsTitle;

    // 🌟 דגל למניעת לולאה אינסופית בעת איפוס שדה החיפוש
    private boolean isResettingSearch = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌟 טיפול בכפתור החזור של אנדרואיד
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // בדיקה אם המשתמש נמצא במצב חיפוש
                if (backButton != null && backButton.getVisibility() == View.VISIBLE) {
                    resetToInitialState(); // איפוס למצב רגיל
                } else {
                    // אם לא במצב חיפוש, חזור אחורה כרגיל ב-Navigation Stack
                    NavHostFragment.findNavController(MapFragment.this).navigateUp();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        // UI Initialization (RecyclerView, Adapters, LiveData Observers)
        recyclerView = rootView.findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        locationAdapter = new LocationAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(locationAdapter);

        // Divider
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD"));

        divider.setDrawable(dividerDrawable);

        recyclerView.addItemDecoration(divider);

        // LiveData Observers
        mapViewModel.getLocationsLiveData().observe(getViewLifecycleOwner(), locations -> {
            Log.d("MapFragment", "ViewModel locations updated. Count: " + (locations != null ? locations.size() : 0));
            locationAdapter.setLocationList(locations);
            updateMapMarkers(locations);
            if (searchEditText != null && !searchEditText.getText().toString().isEmpty()) {
                backButton.setVisibility(View.VISIBLE);
                nearbyMarketsTitle.setText("תוצאות חיפוש");
            } else {
                backButton.setVisibility(View.GONE);
                nearbyMarketsTitle.setText("רשימת שווקים קרובים");
            }
        });

        mapViewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state
        });

        mapViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // ----------------------------------------------------
        // ניהול SupportMapFragment
        // ----------------------------------------------------
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_container);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }

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
        // ----------------------------------------------------

        // BottomSheet
        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setPeekHeight(180);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Messages Button
        FloatingActionButton messagesButton = rootView.findViewById(R.id.messages);
        if (messagesButton != null) {
            messagesButton.setOnClickListener(v -> {
                Log.d("MapFragment", "Navigating to Notifications Activity...");
                Intent intent = new Intent(requireContext(), NotificationsActivity.class);
                startActivity(intent);
            });
        }

        // Search Components
        openSearchButton = rootView.findViewById(R.id.openSearchButton);
        searchContainer = rootView.findViewById(R.id.searchContainer);
        searchEditText = rootView.findViewById(R.id.searchEditText);
        searchButton = rootView.findViewById(R.id.searchButton);
        clearSearchButton = rootView.findViewById(R.id.clearSearchButton);
        backButton = rootView.findViewById(R.id.backButton);
        nearbyMarketsTitle = rootView.findViewById(R.id.nearbyMarketsTitle);

        openSearchButton.setOnClickListener(v -> {
            if (searchContainer.getVisibility() == View.GONE) {
                searchContainer.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
                // פתיחת המקלדת
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                searchContainer.setVisibility(View.GONE);
                // סגירת המקלדת
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && requireActivity().getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
                }
            }
        });

        searchButton.setOnClickListener(v -> performSearch());

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
        });

        backButton.setOnClickListener(v -> resetToInitialState());

        // 🌟 המאזין TextWatcher המתוקן
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // אם אנחנו בעיצומו של איפוס, התעלם מהשינוי כדי למנוע לולאה אינסופית
                if (isResettingSearch) {
                    return;
                }

                if (s.length() > 0) {
                    clearSearchButton.setVisibility(View.VISIBLE);
                } else {
                    clearSearchButton.setVisibility(View.GONE);
                    // ❌ הלוגיקה ל-resetToInitialState() הוסרה מכאן - היא מטופלת עכשיו רק על ידי backButton או כפתור החזור של אנדרואיד
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return rootView;
    }

    private void performSearch() {
        // הסתרת המקלדת
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
        }

        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "אנא הזן מילת חיפוש.", Toast.LENGTH_SHORT).show();
            // 🌟 קריאה ל-resetToInitialState() בחיפוש ריק מבטיחה חזרה למצב הראשוני
            resetToInitialState();
        } else {
            if (currentUserLocation != null) {
                mapViewModel.searchLocations(query, currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
            } else {
                Toast.makeText(requireContext(), "לא ניתן לבצע חיפוש ללא מיקום משתמש.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resetToInitialState() {
        // 🌟 הגדרת הדגל ל-TRUE לפני הקריאה ל-setText כדי למנוע לולאה אינסופית
        isResettingSearch = true;

        searchEditText.setText("");

        // 🌟 איפוס הדגל ל-FALSE מיד לאחר מכן
        isResettingSearch = false;

        // הסתרת המקלדת
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
        }

        backButton.setVisibility(View.GONE);
        nearbyMarketsTitle.setText("רשימת שווקים קרובים");
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
            }
            if (!locations.isEmpty() && currentUserLocation != null) {
                LatLng userLatLng = new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10));
            } else if (!locations.isEmpty()){
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

        if (location instanceof Market) {
            Market market = (Market) location;

            if (mGoogleMap != null) {
                LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

        } else if (location instanceof Farmer) {
            Farmer farmer = (Farmer) location;
            navigateToFarmerProfile(farmer);
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

    private void navigateToFarmerProfile(Farmer farmer) {
        Log.d("MapFragment", "Navigating to farmer profile fragment. Email: " + farmer.getEmail());

        // הסתרת המקלדת
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
        }

        // הכנת הנתונים (Bundle)
        Bundle args = new Bundle();
        args.putString("farmer_email_key", farmer.getEmail());

        // ביצוע הניווט באמצעות Navigation Component
        try {
            NavHostFragment.findNavController(this).navigate(
                    R.id.action_global_farmerProfile,
                    args
            );
            Log.d("MapFragment", "Successfully navigated using action ID.");
            Toast.makeText(requireContext(), "טוען פרופיל של: " + farmer.getEmail(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("MapFragment", "Navigation failed: " + e.getMessage());
            Toast.makeText(requireContext(), "שגיאת ניווט! ודא שהאקשן מוגדר בגרף הניווט.", Toast.LENGTH_LONG).show();
        }
    }

}