package com.example.shock_it;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem; // Import this!
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import androidx.annotation.NonNull; // Import this!
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shock_it.databinding.ActivityFarmerHomeBinding;

// Make sure FarmerHomeActivity implements NavigationView.OnNavigationItemSelectedListener
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

        // Set the listener for navigation item clicks
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.farmer_home, menu); // This is for the Toolbar's overflow menu
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    // This method handles clicks on items in the Navigation Drawer
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId(); // Get the ID of the clicked item

        // --- Start of Changes Here ---
        // Log to see if this method is called and what item is clicked
        Log.d("DrawerDebug", "Clicked item ID: " + id);
        try {
            Log.d("DrawerDebug", "Clicked item name: " + getResources().getResourceEntryName(id));
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e("DrawerDebug", "Resource name not found for ID: " + id);
        }
        Log.d("DrawerDebug", "Expected Logout ID (R.id.nav_logout): " + R.id.nav_logout);


        if (id == R.id.nav_logout) { // <--- **CRITICAL: You MUST check the ID here!**
            Log.d("DrawerDebug", "Logout item detected! Calling logoutUser().");
            logoutUser();
            binding.drawerLayout.closeDrawers();
            return true; // Consume the event, don't let NavigationUI handle it
        } else {
            // If it's not the logout item, let the Navigation Component handle it.
            // This will navigate to the fragments defined in your mobile_navigation.xml
            Log.d("DrawerDebug", "Clicked item is NOT nav_logout. Letting Navigation Component handle it.");
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                binding.drawerLayout.closeDrawers(); // Close the drawer if Navigation Component successfully handled navigation
            }
            Log.d("DrawerDebug", "NavigationUI handled: " + handled);
            return handled; // Return whether NavigationUI handled it
        }
        // --- End of Changes Here ---
    }

    private void logoutUser() {
        Log.d("Hello", "Logout success (inside logoutUser)");
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_email"); // Clear the email
        // If you store other user-related data (like a token), remove it here too
        // editor.remove("user_token");
        editor.apply(); // Apply changes asynchronously

        Toast.makeText(this, "התנתקת בהצלחה!", Toast.LENGTH_SHORT).show();

        // Navigate to the main login screen (MainActivity)
        Intent intent = new Intent(FarmerHomeActivity.this, MainActivity.class);
        // These flags are crucial to clear the back stack so the user can't go back to FarmerHomeActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish FarmerHomeActivity so it's removed from the back stack
    }
}