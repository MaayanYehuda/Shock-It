    package com.example.shock_it.ui.map;

    import android.Manifest;
    import android.content.pm.PackageManager;
    import android.content.res.Resources;
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
    import androidx.navigation.NavController;
    import androidx.navigation.Navigation;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.shock_it.R;
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
        private GoogleMap mGoogleMap;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            View rootView = inflater.inflate(R.layout.fragment_map, container, false);

            SupportMapFragment mapFragment = (SupportMapFragment)
                    getChildFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(googleMap -> {
                    mGoogleMap = googleMap;

                    // סטייל
                    try {
                        boolean success = googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
                        if (!success) {
                            Log.e("MapStyle", "Style parsing failed.");
                        }
                    } catch (Resources.NotFoundException e) {
                        Log.e("MapStyle", "Can't find style. Error: ", e);
                    }

                    loadMarkets();
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_farmer_home);
//                    navController.navigate(R.id.nav_add_market);

                    // מיקום
                    checkLocationPermission();
                });

            }

            // הגדרת BottomSheet
            View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
            BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setPeekHeight(120);
            bottomSheetBehavior.setHideable(false);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            // טעינת השווקים
            loadMarkets();

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

                    // קבל את המיקום האחרון והעבר את המצלמה לשם
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(requireActivity(), location -> {
                                if (location != null) {
                                    LatLng currentLocation = new LatLng(
                                            location.getLatitude(),
                                            location.getLongitude());

                                    // העבר את המצלמה למיקום המשתמש עם זום 15
                                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                            currentLocation, 15));
                                } else {
                                    // אם אין מיקום זמין, השתמש במיקום ברירת המחדל (תל אביב)
                                    LatLng defaultLocation = new LatLng(32.0853, 34.7818);
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

                    // הוספת הסמנים ב-UI Thread
                    // הפעלת הקוד ב-UI Thread להצגה
                    requireActivity().runOnUiThread(() -> {
                        if (mGoogleMap != null) { // השתמש ב-mGoogleMap שכבר שמור
                            for (Market market : markets) {
                                LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                                mGoogleMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title(market.getLocation())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                            }
                        }
                    });

                    RecyclerView recyclerView = requireView().findViewById(R.id.marketsView);
                    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    recyclerView.setAdapter(new MarketAdapter(markets));
                    recyclerView.setAdapter(new MarketAdapter(markets, market -> {
                        LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                    }));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }