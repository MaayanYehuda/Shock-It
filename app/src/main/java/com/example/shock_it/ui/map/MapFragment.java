package com.example.shock_it.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.shock_it.R;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MapFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // המפה
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                LatLng location = new LatLng(32.0853, 34.7818);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12));
            });
        }

        // הגדרת BottomSheet
        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120); // גובה התצוגה כשה- BottomSheet לא פתוח לגמרי
        bottomSheetBehavior.setHideable(false); // למנוע סגירה מלאה (אם רוצים)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // פתיחה במצב מוקטן

        return rootView;
    }

}
