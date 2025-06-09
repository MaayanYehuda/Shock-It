package com.example.shock_it.ui.map.map;

import android.Manifest;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.R;
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

public class MapFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private MapViewModel mapViewModel;
    private GoogleMap mGoogleMap;
    private MarketAdapter marketAdapter;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {



        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        recyclerView = rootView.findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        marketAdapter = new MarketAdapter(new ArrayList<>());
        recyclerView.setAdapter(marketAdapter);

// ✨ הוספת קו הפרדה בין פריטים
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD")); // קו אפור-בהיר
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);



        // ☑️ קישור התצפית ל־ViewModel
        mapViewModel.getMarkets().observe(getViewLifecycleOwner(), markets -> {
            marketAdapter.setMarketList(markets);
            if (mGoogleMap != null) {
                mGoogleMap.clear();
                for (Market market : markets) {
                    LatLng latLng = new LatLng(market.getLatitude(), market.getLongitude());
                    mGoogleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(market.getLocation())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mGoogleMap = googleMap;

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
                loadMarkets();
            });
        }

        // ☑️ BottomSheet
        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // ☑️ כפתור הוספת שוק
        FloatingActionButton addMarketButton = rootView.findViewById(R.id.addMarket);
        if (addMarketButton != null) {
            addMarketButton.setOnClickListener(v -> {
                Log.d("MapFragment", "Navigating to Add Market...");
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_farmer_home);
                navController.navigate(R.id.nav_add_market);
            });
        } else {
            Log.e("MapFragment", "FloatingActionButton not found!");
        }

        return rootView;
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
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
                                LatLng defaultLocation = new LatLng(32.0853, 34.7818);
                                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
                            }
                        });
            }
        }
    }

    // ☑️ טוען את כל השווקים ל־ViewModel
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

                requireActivity().runOnUiThread(() -> {
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
                    }));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
