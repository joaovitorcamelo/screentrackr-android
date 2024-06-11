package com.example.screentrackr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TrackerActivity extends AppCompatActivity {

    private LinearLayout currentlyList;
    private LinearLayout watchlistList;
    private LinearLayout watchedList;
    private LinearLayout cancelledList;
    private View filmModal;
    private TextView filmTitle;
    private TextView filmPlot;
    private ImageView filmPoster;
    private Spinner relationType;
    private Button addSubmitFilm;
    private Button closeModal;
    private OkHttpClient client;
    private int userId;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        // Inicializar os componentes
        currentlyList = findViewById(R.id.currently_list);
        watchlistList = findViewById(R.id.watchlist_list);
        watchedList = findViewById(R.id.watched_list);
        cancelledList = findViewById(R.id.cancelled_list);
        filmModal = findViewById(R.id.film_modal);
        filmTitle = findViewById(R.id.film_title);
        filmPlot = findViewById(R.id.film_plot);
        filmPoster = findViewById(R.id.film_poster);
        relationType = findViewById(R.id.relation_type);
        addSubmitFilm = findViewById(R.id.add_submit_film);
        closeModal = findViewById(R.id.close_modal);
        client = new OkHttpClient();

        // Carregar informações do usuário do SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);
        authToken = sharedPreferences.getString("authToken", null);

        // Verificar se o usuário está autenticado
        if (userId == -1 || authToken == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Carregar filmes do usuário
        loadUserFilms();

        // Configurar o botão de fechamento do modal
        closeModal.setOnClickListener(v -> filmModal.setVisibility(View.GONE));

        // Configurar o botão de adicionar filme
        addSubmitFilm.setOnClickListener(v -> filmModal.setVisibility(View.GONE));

        // Configurar o menu de navegação inferior
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_tracker); // Marcar como selecionado

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_tracker:
                    // Já está na TrackerActivity, então não precisa fazer nada
                    return true;
                case R.id.nav_search:
                    startActivity(new Intent(TrackerActivity.this, SearchActivity.class));
                    return true;
                case R.id.nav_profile:
                    startActivity(new Intent(TrackerActivity.this, ProfileActivity.class));
                    return true;
            }
            return false;
        });
    }

    private void loadUserFilms() {
        String url = "http://10.0.2.2:8080/screentrackr_war_exploded/FilmRelationServlet";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to load films: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        runOnUiThread(() -> parseAndDisplayFilms(responseData));
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to parse film data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to load films", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void parseAndDisplayFilms(String response) {
        try {
            JSONArray filmRelations = new JSONArray(response);
            for (int i = 0; i < filmRelations.length(); i++) {
                JSONObject film = filmRelations.getJSONObject(i);
                String relationType = film.getString("relationType");
                String posterUrl = film.getString("posterImgUrl");
                addFilmToList(relationType, posterUrl);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to parse film data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addFilmToList(String relationType, String posterUrl) {
        ImageView filmImageView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(200, 300);
        layoutParams.setMargins(10, 0, 10, 0);
        filmImageView.setLayoutParams(layoutParams);

        Picasso.get().load(posterUrl).into(filmImageView);

        switch (relationType) {
            case "watching":
                currentlyList.addView(filmImageView);
                break;
            case "watchlist":
                watchlistList.addView(filmImageView);
                break;
            case "watched":
                watchedList.addView(filmImageView);
                break;
            case "cancelled":
                cancelledList.addView(filmImageView);
                break;
        }
    }
}
