package com.example.shock_it.ui.map.addMarket;
// בתוך AddMarketFragment.java

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import com.example.shock_it.R;
import com.example.shock_it.MarketProfileActivity;

import android.content.Intent;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddMarketFragment extends Fragment {
    private AddMarketViewModel viewModel;

    private EditText dateInput, locationInput, startTimeInput, endTimeInput;
    private Button addMarketButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_market, container, false);

        viewModel = new ViewModelProvider(this).get(AddMarketViewModel.class);
        viewModel.resetMarketAddedSuccessfully();

        dateInput = view.findViewById(R.id.editTextDate);
        locationInput = view.findViewById(R.id.editTextLocation);
        startTimeInput = view.findViewById(R.id.editTextStartTime);
        endTimeInput = view.findViewById(R.id.editTextEndTime);
        addMarketButton = view.findViewById(R.id.buttonAddMarket);

        setupDateInput();
        setupTimeInputs();

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
                String marketId = viewModel.getNewMarketId();
                String originalLocation = locationInput.getText().toString().trim();
                String originalDate = dateInput.getText().toString().trim();
                String formattedDateForIntent = convertToISODate(originalDate);

                if (marketId != null) {
                    Log.d("AddMarketFragment", "Market added successfully, navigating to MarketProfileActivity with ID: " + marketId);
                    navigateToMarketProfile(marketId, originalLocation, formattedDateForIntent);
                    clearInputs();
                } else {
                    Log.w("AddMarketFragment", "Market added successfully, but no ID received for navigation.");
                    Toast.makeText(requireContext(), "השוק נוסף בהצלחה, אך לא ניתן לנווט לפרופיל השוק. ID חסר.", Toast.LENGTH_LONG).show();
                }
                viewModel.resetMarketAddedSuccessfully();
            }
        });

        addMarketButton.setOnClickListener(v -> {
            String date = dateInput.getText().toString().trim();
            String loc = locationInput.getText().toString().trim();
            String startTime = startTimeInput.getText().toString().trim();
            String endTime = endTimeInput.getText().toString().trim();

            if (date.isEmpty() || loc.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(requireContext(), "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🌟 שינוי: בדיקת הפורמט מתבצעת כעת על dd/MM/yyyy
            if (!isValidDateFormat(date)) {
                Toast.makeText(requireContext(), "פורמט תאריך לא תקין. השתמש בפורמט: DD/MM/YYYY", Toast.LENGTH_LONG).show();
                return;
            }

            // 🌟 שינוי: העברת תאריך בפורמט dd/MM/yyyy ל-isValidMarketTimes
            if (!isValidMarketTimes(date, startTime, endTime)) {
                return;
            }

            showConfirmationDialog(date, loc, startTime, endTime);
        });

        return view;
    }

    private void showConfirmationDialog(String date, String location, String startTime, String endTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_market_confirmation, null);
        builder.setView(dialogView);

        TextView marketDetailsTextView = dialogView.findViewById(R.id.marketDetailsTextView);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        String hours = startTime + " - " + endTime;
        String details = "תאריך: " + date + "\n" +
                "שעות: " + hours + "\n" +
                "מיקום: " + location;
        marketDetailsTextView.setText(details);

        final AlertDialog dialog = builder.create();

        confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            // Start a new thread for geocoding to prevent blocking the UI
            getCoordinatesFromLocation(requireContext(), location, new CoordinatesCallback() {
                @Override
                public void onCoordinatesReceived(double latitude, double longitude) {
                    String farmerEmail = getFarmerEmail();
                    if (farmerEmail != null) {
                        // 🌟 שינוי: המרת פורמט התאריך לפני הקריאה ל-viewModel.addMarket
                        String formattedDateForDb = convertToISODate(date);
                        viewModel.addMarket(formattedDateForDb, hours, location, latitude, longitude, farmerEmail);
                    } else {
                        Toast.makeText(requireContext(), "שגיאה: אימייל החקלאי לא נמצא.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), "שגיאה בחיפוש מיקום: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private String getFarmerEmail() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return sharedPref.getString("user_email", null);
    }

    private void setupTimeInputs() {
        startTimeInput.setOnClickListener(v -> showTimePicker(startTimeInput));
        endTimeInput.setOnClickListener(v -> showTimePicker(endTimeInput));
    }

    private void showTimePicker(EditText timeInput) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    timeInput.setText(formattedTime);
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private boolean isValidMarketTimes(String date, String startTime, String endTime) {
        try {
            // 🌟 שינוי: שימוש בפורמט dd/MM/yyyy עבור הקלט של המשתמש
            SimpleDateFormat fullDateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            fullDateTimeFormat.setLenient(false);

            // 1. בדיקה שהשוק נמשך עד 10 שעות לכל היותר
            Date startDateTime = fullDateTimeFormat.parse(date + " " + startTime);
            Date endDateTime = fullDateTimeFormat.parse(date + " " + endTime);

            if (endDateTime.before(startDateTime)) {
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(endDateTime);
                endCal.add(Calendar.DAY_OF_YEAR, 1);
                endDateTime = endCal.getTime();
            }

            long durationMillis = endDateTime.getTime() - startDateTime.getTime();
            long durationHours = durationMillis / (1000 * 60 * 60);

            if (durationHours > 10) {
                Toast.makeText(requireContext(), "משך השוק לא יכול לעלות על 10 שעות.", Toast.LENGTH_LONG).show();
                return false;
            }

            // 2. בדיקה שהשעה שברחנו היא לא לפני או בטווח של שעתיים אחרי הזמן הנוכחי
            Calendar now = Calendar.getInstance();
            Calendar marketStart = Calendar.getInstance();
            marketStart.setTime(startDateTime);

            long diffMillis = marketStart.getTimeInMillis() - now.getTimeInMillis();
            long diffHours = diffMillis / (1000 * 60 * 60);

            if (diffHours < 2) {
                Toast.makeText(requireContext(), "יש לבחור שעת פתיחה שהיא לפחות שעתיים מהזמן הנוכחי.", Toast.LENGTH_LONG).show();
                return false;
            }

            return true;

        } catch (ParseException e) {
            Toast.makeText(requireContext(), "שגיאה בפורמט התאריך או השעה.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("AddMarketFragment", "onViewCreated: fragment UI should be visible now");
    }

    private void setupDateInput() {
        // 🌟 שינוי: עדכון ההינט למשתמש לפורמט הרצוי
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
                    // 🌟 שינוי: יצירת מחרוזת תאריך בפורמט dd/MM/yyyy
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
            // 🌟 שינוי: בדיקת תקינות הקלט על פורמט dd/MM/yyyy
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private String convertToISODate(String date) {
        try {
            // 🌟 שינוי: המרה מפורמט dd/MM/yyyy לפורמט yyyy-MM-dd
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            return date;
        }
    }

    private void navigateToMarketProfile(String marketId, String location, String formattedDateForIntent) {
        Intent intent = new Intent(requireActivity(), MarketProfileActivity.class);
        intent.putExtra("marketId", marketId);
        intent.putExtra("location", location);
        intent.putExtra("date", formattedDateForIntent);
        startActivity(intent);
        requireActivity().getSupportFragmentManager().popBackStack();
        Toast.makeText(requireContext(), "נווט לפרופיל שוק " + marketId, Toast.LENGTH_SHORT).show();
    }

    private void clearInputs() {
        dateInput.setText("");
        locationInput.setText("");
        startTimeInput.setText("");
        endTimeInput.setText("");
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

    interface CoordinatesCallback {
        void onCoordinatesReceived(double latitude, double longitude);
        void onError(String error);
    }
}