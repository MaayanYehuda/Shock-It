package com.example.shock_it.ui.map;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.shock_it.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import services.Service;


public class AddMarketFragment extends Fragment {

    private EditText dateInput, locationInput;
    private Button addMarketButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_market, container, false);

        dateInput = view.findViewById(R.id.editTextDate);
        locationInput = view.findViewById(R.id.editTextLocation);
        addMarketButton = view.findViewById(R.id.buttonAddMarket);

        addMarketButton.setOnClickListener(v -> {
            String date = dateInput.getText().toString();
            String loc = locationInput.getText().toString();

            getCoordinatesFromLocation(requireContext(), loc, (latitude, longitude) -> {
                Service HttpService = null;
                String response = HttpService.addNewMarket(date, loc, latitude, longitude);
                Log.d("ADD_MARKET", response);
            });
        });

        return view;
    }

    // פונקציה שמבצעת Geocoding – מקבלת שם מקום ומחזירה קואורדינטות
    private void getCoordinatesFromLocation(Context context, String locationName, CoordinatesCallback callback) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = null;
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                callback.onCoordinatesReceived(latitude, longitude);
            } else {
                Toast.makeText(context, "לא נמצא מיקום", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ממשק כדי להעביר קואורדינטות חזרה מהפונקציה הא-סינכרונית
    interface CoordinatesCallback {
        void onCoordinatesReceived(double latitude, double longitude);
    }
}
