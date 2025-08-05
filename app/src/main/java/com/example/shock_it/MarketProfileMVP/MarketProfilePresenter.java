package com.example.shock_it.MarketProfileMVP;

import android.util.Log;
import classes.Item;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import services.Service;

public class MarketProfilePresenter implements MarketProfileContract.Presenter {

    private MarketProfileContract.View view;
    private String userEmail;
    // private String marketId; // ✅ אין צורך שיהיה כאן משתנה קלאס marketId, הוא מועבר במתודות
    private String founderEmail; // ✅ משתנה קלאס לשמירת מייל המייסד

    public MarketProfilePresenter(String userEmail, String marketId) {
        this.userEmail = userEmail;
        // this.marketId = marketId; // ✅ אין צורך לאתחל כאן
    }

    // ✅ מתודה לקבלת מייל המייסד (לצורך בדיקה ב-Activity)
    public String getFounderEmail() {
        return founderEmail;
    }

    @Override
    public void attachView(MarketProfileContract.View view) {
        this.view = view;
    }

    @Override
    public void detachView() {
        this.view = null;
    }

    @Override
    public void loadMarketProfile(String location, String date, String userEmail) {
        if (view != null) {
            view.showLoading();
        }
        new Thread(() -> {
            Log.d("MarketProfilePresenter", "Attempting to load market profile for: " + location + ", " + date);
            try {
                String response = Service.getMarketProfile(location, date);
                Log.d("MarketProfilePresenter", "Server Response for Market Profile: " + response);

                JSONObject json = new JSONObject(response);

                String name = json.optString("location", location);
                String hours = json.optString("hours", "09:00 - 14:00");
                String founderNameFromResponse = json.optString("founderName", null); // השם של המייסד מהשרת
                this.founderEmail = json.optString("founderEmail", null); // ✅ שמירת מייל המייסד במשתנה הקלאס
                String currentMarketId = json.optString("id", null); // ✅ קבלת marketId מהתגובה
                if (view != null) {
                    view.setMarketId(currentMarketId);
                    Log.d("MarketIdDebug", "Presenter: Sent Market ID to View: " + currentMarketId);
                }





                JSONArray participatingFarmersArray = json.optJSONArray("participatingFarmers");
                JSONArray invitedFarmersArray = json.optJSONArray("invitedFarmers");
                JSONArray pendingRequestsArray = json.optJSONArray("pendingRequests");
                JSONArray marketProductsArray = json.optJSONArray("marketProducts");

                Map<String, List<JSONObject>> farmerProductsMap = new HashMap<>();
                if (marketProductsArray != null) {
                    for (int i = 0; i < marketProductsArray.length(); i++) {
                        JSONObject productObj = marketProductsArray.getJSONObject(i);
                        String offeringFarmerEmail = productObj.optString("offeringFarmerEmail");
                        if (offeringFarmerEmail != null && !offeringFarmerEmail.isEmpty()) {
                            if (!farmerProductsMap.containsKey(offeringFarmerEmail)) {
                                farmerProductsMap.put(offeringFarmerEmail, new ArrayList<>());
                            }
                            farmerProductsMap.get(offeringFarmerEmail).add(productObj);
                        }
                    }
                }

                boolean isUserFounder = (userEmail != null && this.founderEmail != null && userEmail.equals(this.founderEmail));
                Log.d("FounderDebug", "Calculated isUserFounder: " + isUserFounder);

                boolean isUserParticipating = isUserFounder;
                boolean isUserInvited = false;
                boolean isUserRequestPending = false;

                if (!isUserParticipating && participatingFarmersArray != null) {
                    for (int i = 0; i < participatingFarmersArray.length(); i++) {
                        JSONObject farmerObj = participatingFarmersArray.getJSONObject(i);
                        if (userEmail != null && userEmail.equals(farmerObj.optString("email"))) {
                            isUserParticipating = true;
                            break;
                        }
                    }
                }

                if (!isUserParticipating && invitedFarmersArray != null) {
                    for (int i = 0; i < invitedFarmersArray.length(); i++) {
                        JSONObject invitedObj = invitedFarmersArray.getJSONObject(i);
                        if (userEmail != null && userEmail.equals(invitedObj.optString("email"))) {
                            isUserInvited = true;
                            break;
                        }
                    }
                }

                if (!isUserParticipating && !isUserInvited && pendingRequestsArray != null) {
                    for (int i = 0; i < pendingRequestsArray.length(); i++) {
                        try {
                            JSONObject requestObj = pendingRequestsArray.getJSONObject(i);
                            String requestedFarmerEmail = requestObj.optString("email");
                            if (userEmail != null && userEmail.equals(requestedFarmerEmail)) {
                                isUserRequestPending = true;
                                break;
                            }
                        } catch (JSONException e) {
                            Log.e("MarketProfilePresenter", "Error parsing pending request object: " + e.getMessage(), e);
                        }
                    }
                }

                if (view != null) {
                    view.displayMarketProfile(name, hours);
                    // ✅ העברת isUserFounder ישירות
                    view.updateFabState(isUserFounder, isUserParticipating, isUserInvited, isUserRequestPending);
                    view.clearFarmersList();

                    boolean atLeastOneFarmerDisplayed = false;
                    if (founderNameFromResponse != null && !founderNameFromResponse.isEmpty()) {
                        JSONArray founderProductsJsonArray = null;
                        if (farmerProductsMap.containsKey(this.founderEmail)) {
                            founderProductsJsonArray = new JSONArray(farmerProductsMap.get(this.founderEmail));
                        }
                        // ✅ תיקון: שימוש ב-founderNameFromResponse
                        view.addFarmerCard(founderNameFromResponse, this.founderEmail, founderProductsJsonArray, true);
                        atLeastOneFarmerDisplayed = true;
                    }

                    if (participatingFarmersArray != null && participatingFarmersArray.length() > 0) {
                        for (int i = 0; i < participatingFarmersArray.length(); i++) {
                            try {
                                JSONObject farmerObj = participatingFarmersArray.getJSONObject(i);
                                String farmerNameInMarket = farmerObj.optString("name");
                                String farmerEmailInMarket = farmerObj.optString("email");

                                if (this.founderEmail != null && this.founderEmail.equals(farmerEmailInMarket)) {
                                    continue;
                                }

                                JSONArray farmerSpecificProductsArray = null;
                                if (farmerProductsMap.containsKey(farmerEmailInMarket)) {
                                    farmerSpecificProductsArray = new JSONArray(farmerProductsMap.get(farmerEmailInMarket));
                                }

                                view.addFarmerCard(farmerNameInMarket, farmerEmailInMarket, farmerSpecificProductsArray, false);
                                atLeastOneFarmerDisplayed = true;

                            } catch (JSONException e) {
                                Log.e("MarketProfilePresenter", "Error parsing farmer object in array: " + e.getMessage(), e);
                            }
                        }
                    }

                    if (!atLeastOneFarmerDisplayed) {
                        view.showNoFarmersMessage();
                    }
                    view.hideLoading();
                }

            } catch (IOException e) {
                Log.e("MarketProfilePresenter", "Network error loading market profile: " + e.getMessage(), e);
                if (view != null) {
                    view.showNetworkError();
                    view.hideLoading();
                }
            } catch (JSONException e) {
                Log.e("MarketProfilePresenter", "JSON parsing error loading market profile: " + e.getMessage(), e);
                if (view != null) {
                    view.showJsonParsingError();
                    view.hideLoading();
                }
            }
        }).start();
    }

    @Override
    public void handleAddProductClick(String userEmail, String marketId, boolean isJoinRequest) {
        if (view != null) {
            new Thread(() -> {
                try {
                    String response = Service.getFarmerItems(userEmail);
                    JSONArray productsJsonArray = new JSONArray(response);

                    List<Item> farmerProducts = new ArrayList<>();
                    Map<String, Double> itemPricesMap = new HashMap<>();

                    for (int i = 0; i < productsJsonArray.length(); i++) {
                        JSONObject productObj = productsJsonArray.getJSONObject(i);
                        String productName = productObj.optString("name");
                        String productDescription = productObj.optString("description");

                        String priceStr = productObj.optString("price", "0.0");
                        double productPrice = 0.0;
                        try {
                            productPrice = Double.parseDouble(priceStr);
                        } catch (NumberFormatException e) {
                            Log.e("MarketProfilePresenter", "Error parsing product price string: " + priceStr, e);
                        }

                        Item item = new Item(productName, productDescription);
                        farmerProducts.add(item);
                        itemPricesMap.put(productName, productPrice);
                    }

                    if (view != null) {
                        view.showSelectProductDialog(farmerProducts, itemPricesMap, isJoinRequest);
                    }
                } catch (IOException | JSONException e) {
                    Log.e("MarketProfilePresenter", "Error fetching farmer's offered products: " + e.getMessage(), e);
                    if (view != null) {
                        view.showToast("שגיאה בטעינת המוצרים: " + e.getMessage());
                    }
                }
            }).start();
        }
    }

    @Override
    public void sendJoinRequest(String farmerEmail, String marketId, String itemName, double price) {
        if (view != null) {
            new Thread(() -> {
                try {
                    JSONObject productObject = new JSONObject();
                    productObject.put("name", itemName);
                    productObject.put("price", price);

                    JSONArray productsArray = new JSONArray();
                    productsArray.put(productObject);

                    String response = Service.sendJoinRequestToMarket(marketId, farmerEmail, productsArray);
                    Log.d("MarketProfilePresenter", "Join request response: " + response);

                    if (view != null) {
                        if (response != null) {
                            view.showToast("הבקשה נשלחה בהצלחה! ⭐");
                            view.refreshMarketProfile();
                        } else {
                            view.showToast("שגיאה בשליחת הבקשה. נסה שוב מאוחר יותר.");
                        }
                    }
                } catch (JSONException | IOException e) {
                    Log.e("MarketProfilePresenter", "Error sending join request: " + e.getMessage(), e);
                    if (view != null) {
                        view.showToast("שגיאה בהכנת הבקשה: " + e.getMessage());
                    }
                }
            }).start();
        }
    }

    @Override
    public void addProductToMarket(String farmerEmail, String marketId, String itemName, double price) {
        if (view != null) {
            new Thread(() -> {
                try {
                    String response = Service.addProductToMarketWithWillBe(farmerEmail, marketId, itemName, price);
                    Log.d("MarketProfilePresenter", "Add product to market with WILL_BE response: " + response);

                    if (view != null) {
                        view.showToast("המוצר נוסף בהצלחה לשוק!");
                        view.refreshMarketProfile();
                    }
                } catch (IOException | JSONException e) {
                    Log.e("MarketProfilePresenter", "Error adding product to market with WILL_BE: " + e.getMessage(), e);
                    if (view != null) {
                        view.showToast("שגיאה בהוספת מוצר לשוק: " + e.getMessage());
                    }
                }
            }).start();
        }
    }

    @Override
    public void fetchPendingRequests(String marketId) {
        if (view != null) {
            view.showLoading();
        }
        new Thread(() -> {
            try {
                String response = Service.getMarketPendingRequests(marketId);
                Log.d("MarketProfilePresenter", "Pending requests response: " + response);

                JSONArray jsonArray = new JSONArray(response);
                List<JSONObject> pendingRequests = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    pendingRequests.add(jsonArray.getJSONObject(i));
                }

                if (view != null) {
                    view.hideLoading();
                    if (pendingRequests.isEmpty()) {
                        view.showToast("אין בקשות הצטרפות ממתינות לשוק זה.");
                    } else {
                        view.showPendingRequestsDialog(pendingRequests);
                    }
                }
            } catch (IOException e) {
                Log.e("MarketProfilePresenter", "Network error fetching pending requests: " + e.getMessage(), e);
                if (view != null) {
                    view.showNetworkError();
                    view.hideLoading();
                }
            } catch (JSONException e) {
                Log.e("MarketProfilePresenter", "JSON parsing error fetching pending requests: " + e.getMessage(), e);
                if (view != null) {
                    view.showJsonParsingError();
                    view.hideLoading();
                }
            }
        }).start();
    }

    @Override
    public void approveJoinRequest(String marketId, String farmerEmail) {
        if (view != null) {
            view.showLoading();
        }
        new Thread(() -> {
            try {
                String response = Service.approveMarketJoinRequest(marketId, farmerEmail);
                Log.d("MarketProfilePresenter", "Approve request response: " + response);


                if (view != null) {
                    view.hideLoading();
                    if (response != null && response.contains("success")) {
                        view.showToast("בקשה אושרה בהצלחה!");
                        view.refreshMarketProfile();
                    } else {
                        view.showToast("שגיאה באישור הבקשה.");
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e("MarketProfilePresenter", "Error approving join request: " + e.getMessage(), e);
                if (view != null) {
                    view.showToast("שגיאה באישור הבקשה: " + e.getMessage());
                    view.hideLoading();
                }
            }
        }).start();
    }

    @Override
    public void declineJoinRequest(String marketId, String farmerEmail) {
        if (view != null) {
            view.showLoading();
        }
        new Thread(() -> {
            try {
                String response = Service.declineMarketJoinRequest(marketId, farmerEmail);
                Log.d("MarketProfilePresenter", "Decline request response: " + response);

                if (view != null) {
                    view.hideLoading();
                    if (response != null && response.contains("success")) {
                        view.showToast("בקשה נדחתה בהצלחה.");
                        view.refreshMarketProfile();
                    } else {
                        view.showToast("שגיאה בדחיית הבקשה.");
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e("MarketProfilePresenter", "Error declining join request: " + e.getMessage(), e);
                if (view != null) {
                    view.showToast("שגיאה בדחיית הבקשה: " + e.getMessage());
                    view.hideLoading();
                }
            }
        }).start();
    }
}