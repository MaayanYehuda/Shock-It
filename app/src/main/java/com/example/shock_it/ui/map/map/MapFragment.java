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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.InvitationsActivity;
import com.example.shock_it.MarketProfileActivity; // ודא שאתה מייבא את זה
import com.example.shock_it.R;
import com.example.shock_it.databinding.ActivityFarmerInvitesBinding;
import com.example.shock_it.ui.map.MarketAdapter; // ודא שאתה מייבא את זה
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker; // ייבוא של Marker
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

// MapFragment צריך ליישם גם את MarketAdapter.OnMarketClickListener וגם את GoogleMap.OnMarkerClickListener
public class MapFragment extends Fragment implements
        MarketAdapter.OnMarketClickListener, // לחיצה על פריט ברשימה
        GoogleMap.OnMarkerClickListener { // לחיצה על אייקון במפה

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private MapViewModel mapViewModel;
    private GoogleMap mGoogleMap;
    private MarketAdapter marketAdapter;
    private RecyclerView recyclerView;
    private BottomSheetBehavior<View> bottomSheetBehavior; // הוספת המשתנה ל-BottomSheetBehavior
    private HashMap<Marker, Market> markerMarketMap = new HashMap<>(); // מפה לקישור Marker ל-Market

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        recyclerView = rootView.findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // יצירת ה-MarketAdapter והעברת 'this' כ-listener
        marketAdapter = new MarketAdapter(new ArrayList<>(), this); // **תיקון: מעבירים את ה-listener**
        recyclerView.setAdapter(marketAdapter);

        // הוספת קו הפרדה בין פריטים
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD")); // קו אפור-בהיר
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);

        // קישור התצפית ל־ViewModel
        mapViewModel.getMarkets().observe(getViewLifecycleOwner(), markets -> {
            marketAdapter.setMarketList(markets); // עדכן את רשימת השווקים באדפטר
            if (mGoogleMap != null) {
                mGoogleMap.clear(); // נקה סמנים קודמים
                markerMarketMap.clear(); // נקה גם את מפת הקישור

                for (Market market : markets) {
                    LatLng latLng = new LatLng(market.getLatitude(), market.getLongitude());
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(market.getLocation())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                    if (marker != null) {
                        markerMarketMap.put(marker, market); // קשר את המרקר לאובייקט ה-Market
                    }
                }
            }
        });

        // איתור ה-SupportMapFragment בתוך ה-Fragment עצמו
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mGoogleMap = googleMap;
                mGoogleMap.setOnMarkerClickListener(this); // **הגדרה חשובה: MapFragment הוא ה-listener של המרקרים**

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
                loadMarkets(); // טען את השווקים גם למפה וגם ל-ViewModel
            });
        }

        // הגדרת ה-BottomSheet
        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // כפתור הוספת שוק (אם רלוונטי ל-MapFragment)
        FloatingActionButton addMarketButton = rootView.findViewById(R.id.messages); // Assuming R.id.messages is your invites button
        if (addMarketButton != null) {
            addMarketButton.setOnClickListener(v -> {
                Log.d("Invitations:", "Navigating to Invitations Activity...");
                // Create an Intent to start InvitationsActivity
                Intent intent = new Intent(requireContext(), InvitationsActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e("MapFragment", "FloatingActionButton (for Invitations) not found!");
        }

        return rootView;
    }

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
                Toast.makeText(requireContext(), "הרשאת מיקום נדחתה. לא ניתן להציג את מיקומך.", Toast.LENGTH_LONG).show();
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

    // טוען את כל השווקים מהשרת
    private void loadMarkets() {
        new Thread(() -> {
            try {
                String response = Service.getMarkets(); // קריאה לשירות השרת
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
                    // עדכן את ה-ViewModel (אם קיים), מה שיפעיל את ה-Observer לעדכון המפה וה-RecyclerView
                    if(mapViewModel != null) {
                        mapViewModel.setMarkets(markets); // שלח את הרשימה ל-ViewModel
                    } else {
                        // גיבוי אם אין ViewModel, עדכן ישירות את האדפטר והמפה
                        marketAdapter.setMarketList(markets);
                        if (mGoogleMap != null) {
                            mGoogleMap.clear();
                            markerMarketMap.clear();
                            for (Market market : markets) {
                                LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
                                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title(market.getLocation())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.market)));
                                if (marker != null) {
                                    markerMarketMap.put(marker, market); // קשר את המרקר לאובייקט ה-Market
                                }
                            }
                        }
                    }

                    // אופציונלי: התקרב למיקום השוק הראשון אם יש
                    if (!markets.isEmpty() && mGoogleMap != null) {
                        Market firstMarket = markets.get(0);
                        LatLng firstMarketPos = new LatLng(firstMarket.getLatitude(), firstMarket.getLongitude());
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstMarketPos, 15));
                    }
                });
            } catch (Exception e) {
                Log.e("MapFragment", "Error loading markets: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "שגיאה בטעינת השווקים", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // יישום מתודת ה-OnMarketClickListener (לחיצה על פריט ברשימה)
    @Override
    public void onMarketClick(Market market) {
        // כאשר לוחצים על פריט ברשימה, העבר את המפה למיקום השוק
        Log.d("MapFragment", "List item clicked: " + market.getLocation() + ", " + market.getDate());

        if (mGoogleMap != null) {
            LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
            // אופציונלי: שנה את מצב ה-BottomSheet (לדוגמה, הרחב אותו או סגור חלקית)
            // bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        // חשוב: לא פותחים את MarketProfileActivity מכאן. זה יקרה בלחיצה על המרקר.
    }

    // יישום מתודת ה-OnMarkerClickListener (לחיצה על אייקון במפה)
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        // כאשר לוחצים על אייקון במפה, פתח את MarketProfileActivity
        Market market = markerMarketMap.get(marker); // קבל את אובייקט ה-Market מהמפה
        if (market != null) {
            Log.d("MapFragment", "Marker clicked: " + market.getLocation());

            Intent intent = new Intent(requireContext(), MarketProfileActivity.class);
            intent.putExtra("location", market.getLocation());
            if (market.getDate() != null) {
                intent.putExtra("date", market.getDate().toString()); // המרת LocalDate לסטרינג
            } else {
                intent.putExtra("date", "Unknown Date");
            }
            startActivity(intent);
        } else {
            Log.w("MapFragment", "Market object not found for clicked marker.");
        }
        return true; // החזר true כדי לציין שטיפלנו באירוע הלחיצה
    }
}