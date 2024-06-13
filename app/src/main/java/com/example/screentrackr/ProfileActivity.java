package com.example.screentrackr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView userNameTextView;
    private TextView userBioTextView;
    private Button logoutButton;
    private Button editProfileButton;
    private OkHttpClient client;
    private SharedPreferences sharedPreferences;
    private int userId;
    private String authToken;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userNameTextView = findViewById(R.id.user_name);
        userBioTextView = findViewById(R.id.user_bio);
        logoutButton = findViewById(R.id.logout_button);
        editProfileButton = findViewById(R.id.edit_profile_button);
        client = new OkHttpClient();
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        // Recuperar informações do usuário
        userId = sharedPreferences.getInt("userId", -1);
        authToken = sharedPreferences.getString("authToken", null);

        if (userId == -1 || authToken == null) {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        logoutButton.setOnClickListener(v -> logoutUser());
        editProfileButton.setOnClickListener(v -> openEditProfileModal());
        fetchUserInfo();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_tracker:
                    startActivity(new Intent(ProfileActivity.this, TrackerActivity.class));
                    return true;
                case R.id.nav_search:
                    startActivity(new Intent(ProfileActivity.this, SearchActivity.class));
                    return true;
                case R.id.nav_profile:
                    return true;
            }
            return false;
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUserInfo();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void fetchUserInfo() {
        String url = "http://10.0.2.2:8080/screentrackr_war_exploded/GetUserProfileServlet?userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to fetch user info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject user = new JSONObject(responseData);
                        runOnUiThread(() -> updateUIWithUserInfo(user));
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to parse user info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    System.out.println(response);
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to fetch user info", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateUIWithUserInfo(JSONObject user) {
        try {
            String name = user.getString("name");
            String bio = user.optString("bio", null);  // Use null as default

            userNameTextView.setText(name);
            // Verifica se bio é nula ou "null" (string), então trata como ausente
            if (bio == null || "null".equals(bio)) {
                userBioTextView.setText("No bio available.");
            } else {
                userBioTextView.setText(bio);
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to update user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openEditProfileModal() {
        View modalView = getLayoutInflater().inflate(R.layout.profile_modal, null);
        EditText nameEditText = modalView.findViewById(R.id.edit_name);
        EditText bioEditText = modalView.findViewById(R.id.edit_bio);
        Button saveButton = modalView.findViewById(R.id.save_button);
        Button cancelButton = modalView.findViewById(R.id.cancel_button);

        nameEditText.setText(userNameTextView.getText().toString());
        bioEditText.setText(userBioTextView.getText().toString().equals("No bio available.") ? "" : userBioTextView.getText().toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(modalView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String updatedName = nameEditText.getText().toString().trim();
            String updatedBio = bioEditText.getText().toString().trim();
            updateUserInfo(dialog, updatedName, updatedBio);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateUserInfo(AlertDialog dialog, String updatedName, String updatedBio) {
        // Recupera o nome e a biografia atuais para uso caso os novos valores estejam vazios
        String currentName = userNameTextView.getText().toString();
        String currentBio = userBioTextView.getText().toString().equals("No bio available.") ? "" : userBioTextView.getText().toString();

        // Substitui os valores vazios pelos atuais para evitar a atualização para "null"
        updatedName = updatedName.isEmpty() ? currentName : updatedName;
        updatedBio = updatedBio.isEmpty() ? currentBio : updatedBio;

        String url = "http://10.0.2.2:8080/screentrackr_war_exploded/UpdateUserProfileServlet";

        RequestBody formBody = new FormBody.Builder()
                .add("userId", String.valueOf(userId))
                .add("name", updatedName)
                .add("bio", updatedBio)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to update user info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "User info updated successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchUserInfo(); // Re-fetch the user info to update UI
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to update user info", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("userId");
        editor.remove("authToken");
        editor.apply();

        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        finish();
    }
}
