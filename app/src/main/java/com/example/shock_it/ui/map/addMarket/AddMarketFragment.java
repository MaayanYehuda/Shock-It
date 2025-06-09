package com.example.shock_it.ui.map.addMarket;

import android.app.DatePickerDialog;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.shock_it.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import services.Service;


public class AddMarketFragment extends Fragment {
    private AddMarketViewModel viewModel;


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

        viewModel = new ViewModelProvider(this).get(AddMarketViewModel.class);

        setupDateInput();

        // מאזין ל-LiveData מה-ViewModel
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            addMarketButton.setEnabled(!isLoading);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getMarketAddedSuccessfully().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                clearInputs();
            }
        });

        addMarketButton.setOnClickListener(v -> {
            String date = dateInput.getText().toString().trim();
            String loc = locationInput.getText().toString().trim();

            if (date.isEmpty()) {
                Toast.makeText(requireContext(), "אנא הכנס תאריך", Toast.LENGTH_SHORT).show();
                return;
            }

            if (loc.isEmpty()) {
                Toast.makeText(requireContext(), "אנא הכנס מיקום", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidDateFormat(date)) {
                Toast.makeText(requireContext(), "פורמט תאריך לא תקין. השתמש בפורמט: DD/MM/YYYY", Toast.LENGTH_LONG).show();
                return;
            }

            // השגת קואורדינטות לפני שליחת הנתונים ל-ViewModel
            getCoordinatesFromLocation(requireContext(), loc, new CoordinatesCallback() {
                @Override
                public void onCoordinatesReceived(double latitude, double longitude) {
                    String formattedDate = convertToISODate(date);
                    // קריאה ל-ViewModel להוספת השוק
                    viewModel.addMarket(formattedDate, loc, latitude, longitude);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), "שגיאה: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });

        return view;
    }
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("AddMarketFragment", "onViewCreated: fragment UI should be visible now");
    }

    private void setupDateInput() {
        // הוספת hint לשדה התאריך
        dateInput.setHint("DD/MM/YYYY");

        // אופציה: הוספת DatePickerDialog
        dateInput.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    dateInput.setText(formattedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private boolean isValidDateFormat(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false); // קפדני על הפורמט
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private String convertToISODate(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            return date;
        }
    }

    private void clearInputs() {
        dateInput.setText("");
        locationInput.setText("");
    }

    private void getCoordinatesFromLocation(Context context, String locationName, CoordinatesCallback callback) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    double latitude = address.getLatitude();
                    double longitude = address.getLongitude();

                    requireActivity().runOnUiThread(() -> {
                        callback.onCoordinatesReceived(latitude, longitude);
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        callback.onError("לא נמצא מיקום");
                    });
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    callback.onError("שגיאה בחיפוש המיקום: " + e.getMessage());
                });
            }
        }).start();
    }

    // עדכון ה-interface
    interface CoordinatesCallback {
        void onCoordinatesReceived(double latitude, double longitude);
        void onError(String error);
    }
}
