package com.example.screentrackr;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class SearchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener); // Use `setOnItemSelectedListener` para o Material Components
        bottomNav.setSelectedItemId(R.id.nav_search); // Seleciona a aba Search por padrão
    }

    private BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.nav_tracker:
                            startActivity(new Intent(SearchActivity.this, TrackerActivity.class));
                            return true;
                        case R.id.nav_search:
                            // Já estamos na aba Search
                            return true;
                        case R.id.nav_profile:
                            startActivity(new Intent(SearchActivity.this, ProfileActivity.class));
                            return true;
                    }
                    return false;
                }
            };
}
