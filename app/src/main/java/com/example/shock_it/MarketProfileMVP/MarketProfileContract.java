package com.example.shock_it.MarketProfileMVP;


import classes.Item;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface MarketProfileContract {

    interface View {
        void showLoading();
        void hideLoading();
        void displayMarketProfile(String name, String hours);
        // ✅ שינוי: מעביר את isUserFounder ישירות ל-View
        void updateFabState(boolean isUserFounder, boolean isParticipating, boolean isInvited, boolean isRequestPending);
        void clearFarmersList();
        void addFarmerCard(String farmerName, String farmerEmail, JSONArray productsArray, boolean isFounder);
        void showNoFarmersMessage();
        void showToast(String message);
        void showMarketNotFoundError();
        void showNetworkError();
        void showJsonParsingError();
        void showSelectProductDialog(List<Item> farmerProducts, Map<String, Double> itemPricesMap, boolean isJoinRequest);
        void refreshMarketProfile();
        void showPendingRequestsDialog(List<JSONObject> pendingRequests);

        void setMarketId(String marketId); // ✅ חדש: מתודה להעברת marketId מה-Presenter ל-View

    }

    interface Presenter {
        void attachView(MarketProfileContract.View view);
        void detachView();
        void loadMarketProfile(String location, String date, String userEmail);
        void handleAddProductClick(String userEmail, String marketId, boolean isJoinRequest);
        void sendJoinRequest(String farmerEmail, String marketId, String itemName, double price);
        void addProductToMarket(String farmerEmail, String marketId, String itemName, double price);
        void fetchPendingRequests(String marketId);
        void approveJoinRequest(String marketId, String farmerEmail); // ✅ נוסף
        void declineJoinRequest(String marketId, String farmerEmail); // ✅ נוסף
    }
}