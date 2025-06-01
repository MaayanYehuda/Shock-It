package com.example.shock_it.fragments.loginAndRegister;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.shock_it.R;

public class RegisterFrag extends Fragment {


    public View onCreatView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @NonNull Bundle savedInstanceState){
        View view= inflater.inflate(R.layout.layout_register, container,false);
        return view;
    }


}
