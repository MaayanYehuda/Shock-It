package com.example.shock_it.fragments.loginAndRegister;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shock_it.R;

public class LoginFrag extends Fragment {
    private boolean isLoggingIn = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_login, container, false);
//        EditText userNameInput, passwordInput;
//        EditText userName = (EditText) view.findViewById(R.id.userName);

    }



}

