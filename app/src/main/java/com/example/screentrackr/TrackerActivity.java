package com.example.screentrackr;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TrackerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener); // Use `setOnItemSelectedListener` para o Material Components
        bottomNav.setSelectedItemId(R.id.nav_tracker); // Seleciona a aba Tracker por padrão
    }

    private BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.nav_tracker:
                            // Já estamos na aba Tracker
                            return true;
                        case R.id.nav_search:
                            startActivity(new Intent(TrackerActivity.this, SearchActivity.class));
                            return true;
                        case R.id.nav_profile:
                            startActivity(new Intent(TrackerActivity.this, ProfileActivity.class));
                            return true;
                    }
                    return false;
                }
            };
}
