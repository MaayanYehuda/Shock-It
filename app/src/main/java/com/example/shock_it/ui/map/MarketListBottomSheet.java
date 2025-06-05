package com.example.shock_it.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.shock_it.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MarketListBottomSheet extends BottomSheetDialogFragment {

    public MarketListBottomSheet() {
        // נדרש קונסטרקטור ריק
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false);
    }
}

