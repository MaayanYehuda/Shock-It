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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.farmer_home, menu); // This is for the Toolbar's overflow menu
//        return false;
//    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    // This method handles clicks on items in the Navigation Drawer
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // 1. טיפול בהתנתקות (Logout)
        if (id == R.id.nav_logout) {
            logoutUser();
            binding.drawerLayout.closeDrawers();
            return true; // נטפל באירוע
        }

        // 2. טיפול ביעדים המרכזיים באמצעות Actions גלובליים
        boolean handled = true;

        if (id == R.id.nav_map) {
            // ניווט ל-Map (ניקוי המחסנית)
            navController.navigate(R.id.action_global_nav_map);

        } else if (id == R.id.farmerProfile) {
            // ניווט ל-Farmer Profile (Launch Single Top)
            navController.navigate(R.id.action_global_farmerProfile);

        } else if (id == R.id.nav_add_market) {
            // ניווט ל-Add Market (Launch Single Top)
            navController.navigate(R.id.action_global_nav_add_market);

        } else {
            // אם המזהה לא מוכר, נחזיר false (לדוגמה, אם יש פריט לא מנוהל)
            handled = false;
        }

        // אם הניווט טופל, נסגור את ה-Drawer
        if (handled) {
            binding.drawerLayout.closeDrawers();
        }

        // אם לא טופל ע"י הלוגיקה המפורשת, נשתמש ב-NavigationUI (למקרה של פריט לא מוגדר)
        return handled || NavigationUI.onNavDestinationSelected(item, navController);
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