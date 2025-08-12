package com.example.shock_it.ui.map.map; // ודא שה-package name נכון

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.location.Location; // ✅ ייבוא Location
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat; // ✅ ייבוא ActivityCompat
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.NotificationsActivity;
import com.example.shock_it.MarketProfileActivity;
import com.example.shock_it.R;
import com.example.shock_it.ui.map.MarketAdapter;
import com.example.shock_it.ui.map.map.MapViewModel; // ✅ ייבוא MapViewModel הנכון (ודא שזה ה-package הנכון אם הוא שונה)
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
import com.google.android.gms.tasks.OnSuccessListener; // ✅ ייבוא OnSuccessListener
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Locale; // ✅ ייבוא Locale

import classes.Market; // ✅ ייבוא Market (ודא שה-package הנכון)

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
    // מפה זו תשמור Market אובייקטים, כפי שהיה במקור
    private HashMap<Marker, Market> markerMarketMap = new HashMap<>();

    // שדה חדש לשמירת מיקום המשתמש הנוכחי לחישובי מרחק
    private Location currentUserLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        recyclerView = rootView.findViewById(R.id.marketsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // MarketAdapter כעת מקבל List<Market> ישירות
        marketAdapter = new MarketAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(marketAdapter);

        // הוספת קו הפרדה בין פריטים
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        ShapeDrawable dividerDrawable = new ShapeDrawable();
        dividerDrawable.setIntrinsicHeight(1);
        dividerDrawable.getPaint().setColor(Color.parseColor("#DDDDDD"));
        divider.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(divider);

        // 🟢 צפייה בנתוני השווקים מה-ViewModel. זה יעדכן גם את ה-RecyclerView וגם את המפה.
        // כעת צופה ב-List<Market>
        mapViewModel.getMarketsLiveData().observe(getViewLifecycleOwner(), markets -> {
            Log.d("MapFragment", "ViewModel markets updated. Updating UI components. Markets count: " + (markets != null ? markets.size() : 0));

            // עדכון ה-RecyclerView (אין צורך בהמרה)
            marketAdapter.setMarketList(markets);
            marketAdapter.notifyDataSetChanged();
            Log.d("MapFragment", "RecyclerView adapter updated and notified.");

            // עדכון סמני המפה (רק אם המפה מוכנה)
            updateMapMarkers(markets); // העבר את List<Market>
        });

        // 🟢 צפייה במצב טעינה מה-ViewModel
        mapViewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            // כאן תוכל להציג/להסתיר ProgressBar או הודעת טעינה ב-UI
            if (isLoading) {
                Log.d("MapFragment", "Loading markets...");
                // לדוגמה: showProgressBar();
            } else {
                Log.d("MapFragment", "Finished loading markets.");
                // לדוגמה: hideProgressBar();
            }
        });

        // 🟢 צפייה בהודעות שגיאה מה-ViewModel
        mapViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
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

                // בדוק הרשאות מיקום וטען שווקים
                checkLocationPermission();

                // 🌟 חשוב: אם הנתונים כבר נטענו ל-ViewModel לפני שהמפה הייתה מוכנה, עדכן אותה כעת.
                // זה מטפל במצב שבו ה-Fragment נבנה מחדש והנתונים כבר ב-ViewModel.
                // שימוש ב-getMarketsLiveData().getValue()
                List<Market> currentMarkets = mapViewModel.getMarketsLiveData().getValue();
                if (currentMarkets != null && !currentMarkets.isEmpty()) {
                    Log.d("MapFragment", "Malp ready, updating with existing ViewModel data.");
                    updateMapMarkers(currentMarkets);
                }
            });
        }

        // הגדרת ה-BottomSheet
        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setPeekHeight(180); // גובה ההצצה במצב מכווץ
        bottomSheetBehavior.setHideable(false); // אפשר לשנות את זה ל-true אם רוצים לאפשר הסתרה
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // כפתור הזמנות
        FloatingActionButton invitesButton = rootView.findViewById(R.id.messages);
        if (invitesButton != null) {
            invitesButton.setOnClickListener(v -> {
                Log.d("MapFragment", "Navigating to Invitations Activity...");
                Intent intent = new Intent(requireContext(), NotificationsActivity.class);
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
        // 🟢 רענן את דגל הטעינה ב-ViewModel.
        // זה יאפשר ל-loadMarkets() ב-ViewModel לטעון מחדש את השווקים
        // בכל פעם שה-Fragment חוזר ל-foreground. אם אתה רוצה טעינה *רק* בפתיחה הראשונה,
        // הסר את הקריאה ל-resetMarketsLoaded() מכאן.
        mapViewModel.resetMarketsLoaded();
        checkLocationPermission(); // טען שווקים מחדש (עם מיקום מעודכן)
    }

    /**
     * מעדכן את סמני המפה על בסיס רשימת אובייקטי Market החדשה.
     * חישוב המרחק מתבצע כאן עבור כל סמן.
     *
     * @param markets רשימת אובייקטי Market לעדכון.
     */
    private void updateMapMarkers(List<Market> markets) {
        if (mGoogleMap != null && markets != null) {
            Log.d("MapFragment", "Updating map markers. Number of markets: " + markets.size());
            mGoogleMap.clear(); // נקה סמנים קודמים
            markerMarketMap.clear(); // נקה את מפת הסמן-שוק

            for (Market market : markets) {
                LatLng latLng = new LatLng(market.getLatitude(), market.getLongitude());

                // ✅ חישוב המרחק כאן ב-MapFragment
                float[] results = new float[1];
                double calculatedDistance = -1.0; // ערך ברירת מחדל
                if (currentUserLocation != null) {
                    Location.distanceBetween(
                            currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                            market.getLatitude(), market.getLongitude(),
                            results
                    );
                    calculatedDistance = results[0]; // המרחק במטרים
                }

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(market.getLocation() + " - " + market.getDate()); // כותרת הסמן

                // ✅ השתמש במרחק המחושב ישירות ב-snippet
                if (calculatedDistance != -1.0) {
                    markerOptions.snippet("מרחק: " + String.format(Locale.getDefault(), "%.2f ק\"מ", calculatedDistance / 1000.0));
                } else {
                    markerOptions.snippet("מרחק: לא זמין");
                }

                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.market)); // אייקון מותאם אישית

                Marker marker = mGoogleMap.addMarker(markerOptions);
                if (marker != null) {
                    markerMarketMap.put(marker, market); // קשר את הסמן לאובייקט Market
                }
            }
            // אופציונלי: הזז מצלמה לשוק הראשון ברשימה (הקרוב ביותר/המוקדם ביותר)
            // שים לב: אם השווקים לא ממוינים לפי מרחק מהשרת, זה לא בהכרח השוק הקרוב ביותר.
            if (!markets.isEmpty()) {
                Market firstMarket = markets.get(0);
                LatLng firstMarketPos = new LatLng(firstMarket.getLatitude(), firstMarket.getLongitude());
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstMarketPos, 12));
            }
        } else {
            Log.d("MapFragment", "Cannot update map markers. mGoogleMap is null: " + (mGoogleMap == null) + ", markets list is null: " + (markets == null));
        }
    }

    /**
     * בודק הרשאות מיקום. אם ההרשאה קיימת, מפעיל את המיקום שלי. אם לא, מבקש אותה.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // אם אין הרשאה, בקש אותה
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // אם יש הרשאה, הפעל את המיקום שלי וטען את השווקים
            enableMyLocationAndLoadMarkets();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ההרשאה ניתנה
                enableMyLocationAndLoadMarkets();
            } else {
                // ההרשאה נדחתה. טען שווקים ללא מיקום משתמש (ימוינו רק לפי תאריך)
                Toast.makeText(requireContext(), "נדרשת הרשאת מיקום כדי להציג שווקים קרובים. מציג שווקים כלליים.", Toast.LENGTH_LONG).show();
                mapViewModel.loadMarkets(0.0, 0.0); // שלח 0,0 אם אין מיקום
            }
        }
    }

    /**
     * מפעיל את שכבת המיקום שלי במפה ומפעיל את טעינת השווקים.
     * נדרשת הרשאת ACCESS_FINE_LOCATION.
     */
    @SuppressLint("MissingPermission") // הוסף את זה כי setMyLocationEnabled דורש בדיקת הרשאה
    private void enableMyLocationAndLoadMarkets() {
        if (mGoogleMap != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGoogleMap.setMyLocationEnabled(true); // הצג את כפתור המיקום שלי

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentUserLocation = location; // ✅ שמור את מיקום המשתמש
                                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10)); // הזז מצלמה למיקום המשתמש
                                mapViewModel.loadMarkets(location.getLatitude(), location.getLongitude()); // טען שווקים עם מיקום המשתמש
                            } else {
                                Log.w("MapFragment", "Last known location is null. Loading markets without user location.");
                                Toast.makeText(requireContext(), "לא ניתן לקבל מיקום מדויק. מציג שווקים כלליים.", Toast.LENGTH_LONG).show();
                                mapViewModel.loadMarkets(0.0, 0.0); // טען שווקים ללא מיקום משתמש
                            }
                        }
                    });
        }
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
    // משתנה לשמירת הסמן האחרון שנלחץ, כדי שנוכל לשנות אותו בחזרה
    private Marker lastClickedMarker = null;

    // במקום לשנות גוון, נשתמש בשני קבצי אייקון - רגיל ומוגדל.
// ודא שהוספת את market_selected.png לתיקיית drawable.
    private int normalMarketIcon = R.drawable.market;
    private int selectedMarketIcon = R.drawable.ic_selected_market;

    @SuppressLint("NewApi")
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Market market = markerMarketMap.get(marker);
        if (market != null) {
            Log.d("MapFragment", "Marker clicked: " + market.getLocation());

            // 1. החזרת האייקון של הסמן הקודם למצבו הרגיל
            if (lastClickedMarker != null && !lastClickedMarker.equals(marker)) {
                lastClickedMarker.setIcon(BitmapDescriptorFactory.fromResource(normalMarketIcon));
            }

            // 2. שינוי האייקון של הסמן הנוכחי לאייקון המוגדל
            marker.setIcon(BitmapDescriptorFactory.fromResource(selectedMarketIcon));
            lastClickedMarker = marker; // עדכון הסמן האחרון שנלחץ

            // 3. אנימציית מצלמה חלקה למיקום הסמן
            LatLng pos = new LatLng(market.getLatitude(), market.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));

            // סגירת ה-BottomSheet כפי שהיה
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            // 4. מעבר לפעילות (Activity) הבאה
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
