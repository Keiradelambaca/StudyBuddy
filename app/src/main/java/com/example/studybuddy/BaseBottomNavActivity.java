package com.example.studybuddy;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studybuddy.focus.FocusActivity;
import com.example.studybuddy.home.HomeActivity;
import com.example.studybuddy.modules.ModulesActivity;
import com.example.studybuddy.profile.ProfileActivity;
import com.example.studybuddy.tasks.TasksActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseBottomNavActivity extends AppCompatActivity {

    protected void setupBottomNav(int selectedItemId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;

            Intent intent = null;

            if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
            else if (id == R.id.nav_tasks) intent = new Intent(this, TasksActivity.class);
            else if (id == R.id.nav_modules) intent = new Intent(this, ModulesActivity.class);
            else if (id == R.id.nav_focus) intent = new Intent(this, FocusActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }
}