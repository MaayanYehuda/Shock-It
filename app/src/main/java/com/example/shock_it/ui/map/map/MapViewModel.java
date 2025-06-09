package com.example.shock_it.ui.map.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import classes.Market;

public class MapViewModel extends ViewModel {

    private final MutableLiveData<List<Market>> markets = new MutableLiveData<>();

    public LiveData<List<Market>> getMarkets() {
        return markets;
    }

    public void setMarkets(List<Market> marketList) {
        markets.setValue(marketList);
    }
}
