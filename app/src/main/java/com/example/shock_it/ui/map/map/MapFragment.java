package com.example.shock_it.ui.map.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.InvitationsActivity;
import com.example.shock_it.MarketProfileActivity;
import com.example.shock_it.R;
import com.example.shock_it.ui.map.MarketAdapter;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import classes.Market;
import services.Service;

public class MapFragment extends Fragment implements
        MarketAdapter.OnMarketClickListener,
        GoogleMap.OnMarkerClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private MapViewModel mapViewModel;
    private GoogleMap mGoogleMap;
    private MarketAdapter marketAdapter;
    private RecyclerView recyclerView;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private HashMap<Marker, Market> markerMarketMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        recyclerView = rootView.findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        marketAdapter = new MarketAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(marketAdapter);

        // הוספת קו הפרדה בין פריטים
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD"));
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);

        // 🟢 Observe the markets LiveData. This will update both the RecyclerView and the Map.
        mapViewModel.getMarkets().observe(getViewLifecycleOwner(), markets -> {
            Log.d("MapFragment", "ViewModel markets updated. Updating UI components.");

            // --- CHANGE HERE: Post the RecyclerView update to ensure layout is ready ---
            // This ensures the RecyclerView has finished its initial layout pass
            // before trying to populate it with data, which might be critical
            // for the first display within a BottomSheet.
            recyclerView.post(() -> {
                marketAdapter.setMarketList(markets); // Update the RecyclerView
                marketAdapter.notifyDataSetChanged(); // Explicitly notify for full redraw
                Log.d("MapFragment", "RecyclerView adapter updated and notified.");
            });
            // ----------------------------------------------------------------------

            // This helper method will handle map updates, checking if mGoogleMap is ready
            updateMapMarkers(markets); // Call the helper here!
        });

        // איתור ה-SupportMapFragment בתוך ה-Fragment עצמו
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

                // 🌟 IMPORTANT: When the map is finally ready, update it with the current data from the ViewModel.
                // This handles the case where data was fetched *before* the map was initialized.
                List<Market> currentMarkets = mapViewModel.getMarkets().getValue();
                if (currentMarkets != null && !currentMarkets.isEmpty()) {
                    Log.d("MapFragment", "Map ready, updating with existing ViewModel data.");
                    updateMapMarkers(currentMarkets); // Call the helper here too!
                }
            });
        }

        // הגדרת ה-BottomSheet
        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // כפתור הזמנות
        FloatingActionButton invitesButton = rootView.findViewById(R.id.messages);
        if (invitesButton != null) {
            invitesButton.setOnClickListener(v -> {
                Log.d("MapFragment", "Navigating to Invitations Activity...");
                Intent intent = new Intent(requireContext(), InvitationsActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e("MapFragment", "FloatingActionButton (for Invitations) not found!");
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 🟢 טען את השווקים כאן. זה יבטיח שהנתונים נטענים מחדש (אם צריך) בכל פעם שהפרגמנט מוצג למשתמש
        loadMarkets();
    }

    // --- New helper method to update map markers (ADD THIS METHOD) ---
    private void updateMapMarkers(List<Market> markets) {
        if (mGoogleMap != null && markets != null) {
            Log.d("MapFragment", "Updating map markers. Number of markets: " + markets.size());
            mGoogleMap.clear(); // Clear previous markers
            markerMarketMap.clear(); // Clear the marker-market map

            for (Market market : markets) {
                LatLng latLng = new LatLng(market.getLatitude(), market.getLongitude());
                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(market.getLocation())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                if (marker != null) {
                    markerMarketMap.put(marker, market); // Link the marker to the Market object
                }
            }
            // Optional: Animate camera to the first market if available
            if (!markets.isEmpty()) {
                Market firstMarket = markets.get(0);
                LatLng firstMarketPos = new LatLng(firstMarket.getLatitude(), firstMarket.getLongitude());
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstMarketPos, 12));
            }
        } else {
            Log.d("MapFragment", "Cannot update map markers. mGoogleMap is null: " + (mGoogleMap == null) + ", markets list is null: " + (markets == null));
        }
    }
    // -----------------------------------------------

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied. Cannot display your location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (mGoogleMap != null) {
                mGoogleMap.setMyLocationEnabled(true);

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), location -> {
                            if (location != null) {
                                LatLng currentLocation = new LatLng(
                                        location.getLatitude(),
                                        location.getLongitude());
                                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            } else {
                                LatLng defaultLocation = new LatLng(32.0853, 34.7818); // תל אביב
                                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
                            }
                        });
            }
        }
    }

    private void loadMarkets() {
        new Thread(() -> {
            try {
                String response = Service.getMarkets();
                Log.d("MapFragment", "Service.getMarkets() response: " + response); // ADDED LOG
                if (response == null || response.isEmpty() || response.equals("[]")) { // ADDED CHECK
                    Log.w("MapFragment", "Service.getMarkets() returned empty or null response.");
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "No markets found or service error.", Toast.LENGTH_SHORT).show();
                        mapViewModel.setMarkets(new ArrayList<>()); // Clear previous data if any
                    });
                    return; // Exit if no data
                }

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

                Log.d("MapFragment", "Parsed markets list size: " + markets.size()); // ADDED LOG

                requireActivity().runOnUiThread(() -> {
                    // Always update the ViewModel. Its Observer will handle UI updates.
                    mapViewModel.setMarkets(markets);
                    Log.d("MapFragment", "Markets set to ViewModel. Observer should update UI.");
                });
            } catch (Exception e) {
                Log.e("MapFragment", "Error loading markets: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "שגיאה בטעינת השווקים", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onMarketClick(Market market) {
        Log.d("MapFragment", "List item clicked: " + market.getLocation() + ", " + market.getDate());
        if (mGoogleMap != null) {
            LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // סגור את הרשימה לאחר לחיצה
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Market market = markerMarketMap.get(marker);
        if (market != null) {
            Log.d("MapFragment", "Marker clicked: " + market.getLocation());
            Intent intent = new Intent(requireContext(), MarketProfileActivity.class);
            intent.putExtra("location", market.getLocation());
            if (market.getDate() != null) {
                intent.putExtra("date", market.getDate().toString());
            } else {
                intent.putExtra("date", "Unknown Date");
            }
            startActivity(intent);
        } else {
            Log.w("MapFragment", "Market object not found for clicked marker.");
        }
        return true;
    }
}