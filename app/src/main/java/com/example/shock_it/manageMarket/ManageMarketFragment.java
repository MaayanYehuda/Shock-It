package com.example.shock_it.manageMarket;

import android.content.Context;
import android.content.Intent; // 🆕 Import Intent
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shock_it.R;
import com.example.shock_it.MarketProfileActivity; // 🆕 Import MarketProfileActivity

import java.util.ArrayList;
import java.util.List;

public class ManageMarketFragment extends Fragment {

    private ManageMarketViewModel viewModel;

    private TextView marketIdTextView;
    private EditText farmerEmailEditText;
    private Button inviteFarmerButton;

    private EditText searchFarmerEditText;
    private Button searchFarmerButton;
    private TextView searchResultsTextView;
    private ListView farmersResultsListView;
    private ArrayAdapter<String> farmersAdapter;
    private final long DEBOUNCE_DELAY = 500L;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private String marketId;
    private String inviterEmail;

    // 🆕 Declare new buttons
    private Button buttonBackToAddMarket;
    private Button buttonGoToMarketProfile;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_market, container, false);

        marketIdTextView = view.findViewById(R.id.textViewMarketId);
        farmerEmailEditText = view.findViewById(R.id.editTextFarmerEmail);
        inviteFarmerButton = view.findViewById(R.id.buttonInviteFarmer);

        searchFarmerEditText = view.findViewById(R.id.editTextSearchFarmer);
        searchFarmerButton = view.findViewById(R.id.buttonSearchFarmer);
        searchResultsTextView = view.findViewById(R.id.textViewSearchResults);
        farmersResultsListView = view.findViewById(R.id.listViewFarmersResults);

        // 🆕 Initialize new buttons
        buttonBackToAddMarket = view.findViewById(R.id.buttonBackToAddMarket);
        buttonGoToMarketProfile = view.findViewById(R.id.buttonGoToMarketProfile);

        viewModel = new ViewModelProvider(this).get(ManageMarketViewModel.class);

        if (getArguments() != null) {
            marketId = getArguments().getString("marketId");
            if (marketId != null) {
                marketIdTextView.setText("Market ID: " + marketId);
            } else {
                marketIdTextView.setText("שגיאה: Market ID לא התקבל.");
                Toast.makeText(requireContext(), "שגיאה: Market ID חסר. 🛑", Toast.LENGTH_LONG).show();
            }
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        inviterEmail = prefs.getString("user_email", null);

        if (inviterEmail == null || inviterEmail.isEmpty()) {
            Toast.makeText(requireContext(), "שגיאה: מייל המשתמש לא נמצא. לא ניתן להזמין חקלאים. ⚠️", Toast.LENGTH_LONG).show();
            inviteFarmerButton.setEnabled(false);
        }

        farmersAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, new ArrayList<>());
        farmersResultsListView.setAdapter(farmersAdapter);

        // --- צפייה ב-LiveData מה-ViewModel ועדכון ה-UI ---

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            inviteFarmerButton.setEnabled(!isLoading && inviterEmail != null && !inviterEmail.isEmpty());
            searchFarmerButton.setEnabled(!isLoading);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                if (message.contains("הוזמן בהצלחה")) {
                    farmerEmailEditText.setText("");
                }
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            farmersAdapter.clear();
            if (results != null && !results.isEmpty()) {
                farmersAdapter.addAll(results);
                farmersResultsListView.setVisibility(View.VISIBLE);
                searchResultsTextView.setVisibility(View.VISIBLE);
                searchResultsTextView.setText("תוצאות חיפוש:");
            } else {
                farmersResultsListView.setVisibility(View.GONE);
                searchResultsTextView.setVisibility(View.GONE);
            }
            farmersAdapter.notifyDataSetChanged();
        });

        viewModel.getSearchErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                searchResultsTextView.setVisibility(View.VISIBLE);
                searchResultsTextView.setText(errorMessage);
                farmersResultsListView.setVisibility(View.GONE);
            } else {
                searchResultsTextView.setVisibility(View.GONE);
            }
        });

        // --- הגדרת מאזינים ללחיצות כפתורים ---

        inviteFarmerButton.setOnClickListener(v -> {
            String farmerEmail = farmerEmailEditText.getText().toString().trim();

            if (marketId == null || marketId.isEmpty()) {
                Toast.makeText(requireContext(), "לא ניתן להזמין חקלאי ללא Market ID. ⛔", Toast.LENGTH_SHORT).show();
                return;
            }

            if (farmerEmail.isEmpty()) {
                Toast.makeText(requireContext(), "אנא הכנס מייל חקלאי. 📧", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inviterEmail == null || inviterEmail.isEmpty()) {
                Toast.makeText(requireContext(), "שגיאה: מייל המזמין לא זמין. לא ניתן להזמין. 🛑", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.inviteFarmerToMarket(marketId, farmerEmail, inviterEmail);
        });

        searchFarmerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                String currentQuery = s.toString().trim();

                if (currentQuery.isEmpty()) {
                    farmersAdapter.clear();
                    farmersAdapter.notifyDataSetChanged();
                    searchResultsTextView.setVisibility(View.GONE);
                    farmersResultsListView.setVisibility(View.GONE);
                    viewModel.clearSearchErrorMessage();
                    viewModel.clearSearchResults();
                    return;
                }

                searchRunnable = () -> {
                    viewModel.searchFarmers(currentQuery);
                };

                handler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
            }
        });

        searchFarmerButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "החיפוש מתבצע אוטומטית בעת הקלדה.", Toast.LENGTH_SHORT).show();
        });

        farmersResultsListView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedFarmerInfo = farmersAdapter.getItem(position);
            String selectedEmail = extractEmailFromFarmerInfo(selectedFarmerInfo);
            if (selectedEmail != null) {
                farmerEmailEditText.setText(selectedEmail);
                Toast.makeText(requireContext(), "מייל הועתק: " + selectedEmail + " 📋", Toast.LENGTH_SHORT).show();

                farmersResultsListView.setVisibility(View.GONE);
                searchResultsTextView.setVisibility(View.GONE);
                searchFarmerEditText.setText("");
            }
        });

        // 🆕 Set OnClickListeners for the new buttons
        buttonBackToAddMarket.setOnClickListener(v -> {
            // Option 1: Go back to the previous activity in the stack
            // This is usually appropriate if ManageMarketFragment is hosted by an activity
            // that was launched from AddMarketActivity.
            requireActivity().onBackPressed();
            // Or, if you want to explicitly start AddMarketActivity and clear others:
            // Intent intent = new Intent(requireContext(), AddMarketActivity.class);
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            // startActivity(intent);
            // requireActivity().finish(); // Finish the current activity
        });

        buttonGoToMarketProfile.setOnClickListener(v -> {
            if (marketId == null || marketId.isEmpty()) {
                Toast.makeText(requireContext(), "לא ניתן לעבור לפרופיל ללא Market ID. ⛔", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(requireContext(), MarketProfileActivity.class);
            intent.putExtra("marketId", marketId); // Pass the market ID

            // ⚠️ IMPORTANT: You need to get the actual location and date here.
            // For now, I'm using placeholder values.
            // You might need to fetch this data from your ViewModel or pass it as arguments
            // to this fragment when it's created.
            String marketLocation = "מיקום לדוגמה"; // Replace with actual location
            String marketDate = "תאריך לדוגמה"; // Replace with actual date

            intent.putExtra("location", marketLocation);
            intent.putExtra("date", marketDate);
            startActivity(intent);
        });

        return view;
    }

    private String extractEmailFromFarmerInfo(String farmerInfo) {
        if (farmerInfo != null && farmerInfo.contains("(") && farmerInfo.endsWith(")")) {
            int startIndex = farmerInfo.indexOf("(") + 1;
            int endIndex = farmerInfo.lastIndexOf(")");
            if (startIndex < endIndex) {
                return farmerInfo.substring(startIndex, endIndex);
            }
        }
        return null;
    }
}