package com.example.shock_it;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shock_it.databinding.ActivityFarmerHomeBinding;

public class FarmerHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityFarmerHomeBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFarmerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        navigationView.setNavigationItemSelectedListener(this);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_map, R.id.nav_add_market , R.id.farmerProfile, R.id.nav_logout) // Keep nav_logout here
                .setOpenableLayout(drawer)
                .build();
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_farmer_home);
        navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
    }


    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            logoutUser();
            binding.drawerLayout.closeDrawers();
            return true;
        }


        boolean handled = true;

        if (id == R.id.nav_map) {
            navController.navigate(R.id.action_global_nav_map);

        } else if (id == R.id.farmerProfile) {
            navController.navigate(R.id.action_global_farmerProfile);

        } else if (id == R.id.nav_add_market) {
            navController.navigate(R.id.action_global_nav_add_market);

        } else {
            handled = false;
        }

        if (handled) {
            binding.drawerLayout.closeDrawers();
        }

        return handled || NavigationUI.onNavDestinationSelected(item, navController);
    }

    private void logoutUser() {
        Log.d("Hello", "Logout success (inside logoutUser)");
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_email");
        editor.apply();

        Toast.makeText(this, "התנתקת בהצלחה!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(FarmerHomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}