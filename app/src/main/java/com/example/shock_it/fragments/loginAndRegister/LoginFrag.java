package com.example.shock_it.fragments.loginAndRegister;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.shock_it.R;

public class LoginFrag extends Fragment {
    public View onCreatView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @NonNull Bundle savedInstanceState){
        View view= inflater.inflate(R.layout.layout_login, container,false);
        return view;
    }
}
