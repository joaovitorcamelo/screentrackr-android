package com.example.screentrackr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
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

public class TrackerActivity extends AppCompatActivity {

    private LinearLayout currentlyList;
    private LinearLayout watchlistList;
    private LinearLayout watchedList;
    private LinearLayout cancelledList;
    private View filmModal;
    private TextView filmTitle;
    private TextView filmDetails;
    private TextView filmPlot;
    private ImageView filmPoster;
    private Spinner relationType;
    private Button addSubmitFilm;
    private Button deleteFilmRelation;
    private Button closeModal;
    private CheckBox favoriteCheckbox;
    private OkHttpClient client;
    private int userId;
    private String authToken;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        // Inicializar os componentes
        currentlyList = findViewById(R.id.currently_list);
        watchlistList = findViewById(R.id.watchlist_list);
        watchedList = findViewById(R.id.watched_list);
        cancelledList = findViewById(R.id.cancelled_list);

        // Inflar o modal do layout film_modal_tracker.xml
        filmModal = LayoutInflater.from(this).inflate(R.layout.film_modal_tracker, null);

        // Inicializar componentes do modal
        filmTitle = filmModal.findViewById(R.id.film_title);
        filmDetails = filmModal.findViewById(R.id.film_details);
        filmPlot = filmModal.findViewById(R.id.film_plot);
        filmPoster = filmModal.findViewById(R.id.film_poster);
        relationType = filmModal.findViewById(R.id.relation_type_spinner);
        addSubmitFilm = filmModal.findViewById(R.id.add_submit_film);
        deleteFilmRelation = filmModal.findViewById(R.id.delete_film_relation);
        closeModal = filmModal.findViewById(R.id.close_modal);
        favoriteCheckbox = filmModal.findViewById(R.id.favorite_checkbox);
        client = new OkHttpClient();
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE);

        // Carregar informações do usuário do SharedPreferences
        userId = sharedPreferences.getInt("userId", -1);
        authToken = sharedPreferences.getString("authToken", null);

        // Verificar se o usuário está autenticado
        if (userId == -1 || authToken == null) {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(TrackerActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Carregar filmes do usuário
        loadUserFilms();

        // Configurar o botão de fechamento do modal
        closeModal.setOnClickListener(v -> filmModal.setVisibility(View.GONE));

        // Configurar o botão de adicionar filme
        addSubmitFilm.setOnClickListener(v -> updateFilmRelation());

        // Configurar o botão de deletar filme
        deleteFilmRelation.setOnClickListener(v -> {
            String filmId = (String) filmModal.getTag(); // Usar a tag para armazenar o filmId
            if (filmId != null) {
                deleteFilmRelation(filmId);
            }
        });

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

        // Configurar o SwipeRefreshLayout para atualizar a lista ao arrastar para baixo
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserFilms();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadUserFilms() {
        // Verificar se o userId está disponível
        if (userId == -1) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(TrackerActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Construir a URL com o parâmetro userId
        String url = "http://10.0.2.2:8080/screentrackr_war_exploded/LoadUserFilmsServlet?userId=" + userId;

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
            currentlyList.removeAllViews();
            watchlistList.removeAllViews();
            watchedList.removeAllViews();
            cancelledList.removeAllViews();
            for (int i = 0; i < filmRelations.length(); i++) {
                JSONObject film = filmRelations.getJSONObject(i);
                String relationType = film.getString("relationType");
                String posterUrl = film.getString("posterImgUrl");
                addFilmToList(film, relationType, posterUrl);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to parse film data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addFilmToList(JSONObject film, String relationType, String posterUrl) {
        try {
            String filmId = film.getString("filmId");

            ImageView filmImageView = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(200, 300);
            layoutParams.setMargins(10, 0, 10, 0);
            filmImageView.setLayoutParams(layoutParams);

            Picasso.get().load(posterUrl).into(filmImageView);

            // Adiciona o filmId como tag para uso futuro
            filmImageView.setTag(filmId);

            filmImageView.setOnClickListener(v -> showFilmModal(filmId));

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
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to parse film data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilmModal(String filmId) {
        String url = "http://10.0.2.2:8080/screentrackr_war_exploded/FilmDetailsServlet?filmId=" + filmId + "&userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to load film details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject film = new JSONObject(responseData);
                        runOnUiThread(() -> {
                            try {
                                displayFilmDetails(film);
                            } catch (JSONException e) {
                                Toast.makeText(TrackerActivity.this, "Failed to show film details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to parse film data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to load film details", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void displayFilmDetails(JSONObject film) throws JSONException {
        // Obter todos os componentes do modal a partir da referência do layout film_modal_tracker
        filmTitle = filmModal.findViewById(R.id.film_title);
        filmDetails = filmModal.findViewById(R.id.film_details);
        filmPlot = filmModal.findViewById(R.id.film_plot);
        filmPoster = filmModal.findViewById(R.id.film_poster);
        relationType = filmModal.findViewById(R.id.relation_type_spinner);
        favoriteCheckbox = filmModal.findViewById(R.id.favorite_checkbox);

        // Obter os dados do JSON do filme
        String title = film.getString("title");
        String year = film.getString("year");
        String director = film.getString("director");
        String rating = film.getString("rating");
        String votes = film.getString("votes");
        String plot = film.getString("plot");
        String posterUrl = film.getString("posterImgUrl");
        String currentRelationType = film.getString("relationType");
        boolean isFavorite = film.getBoolean("isFavorite");

        // Atualizar o conteúdo do modal
        filmTitle.setText(title + " (" + year + ")");
        filmDetails.setText("Director: " + director + "\nRating: " + rating + " (" + votes + " votes)");
        filmPlot.setText(plot);
        Picasso.get().load(posterUrl).into(filmPoster);

        // Configurar o Spinner de relação
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.relation_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationType.setAdapter(adapter);
        relationType.setSelection(getRelationTypeIndex(currentRelationType));

        // Configurar o checkbox de favorito
        favoriteCheckbox.setChecked(isFavorite);

        // Exibir o modal
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(filmModal);
        AlertDialog dialog = builder.create();

        // Configurar o botão de fechamento do modal dentro do dialog
        closeModal.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Configurar o botão de deletar filme
        deleteFilmRelation.setOnClickListener(v -> {
            dialog.dismiss();
            String filmId = (String) filmModal.getTag(); // Usar a tag para armazenar o filmId
            if (filmId != null) {
                deleteFilmRelation(filmId);
            }
        });
    }


    private void updateFilmRelation() {
        String filmId = (String) filmModal.getTag();
        if (filmId == null) {
            Toast.makeText(this, "No film selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        String relationType = this.relationType.getSelectedItem().toString();
        boolean isFavorite = favoriteCheckbox.isChecked();

        RequestBody formBody = new FormBody.Builder()
                .add("filmId", filmId)
                .add("relationType", relationType)
                .add("favorite", String.valueOf(isFavorite))
                .add("userId", String.valueOf(userId))
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/screentrackr_war_exploded/FilmRelationServlet")
                .put(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to update film relation: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(TrackerActivity.this, "Film relation updated successfully!", Toast.LENGTH_SHORT).show();
                        filmModal.setVisibility(View.GONE);
                        loadUserFilms(); // Reload to reflect changes
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to update film relation", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void deleteFilmRelation(String filmId) {
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/screentrackr_war_exploded/DeleteFilmRelationServlet?filmId=" + filmId + "&userId=" + userId)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to delete film relation: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(TrackerActivity.this, "Film relation deleted successfully!", Toast.LENGTH_SHORT).show();
                        filmModal.setVisibility(View.GONE);
                        loadUserFilms(); // Reload to reflect changes
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(TrackerActivity.this, "Failed to delete film relation", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private int getRelationTypeIndex(String relationType) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.relation_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.relationType.setAdapter(adapter);

        switch (relationType) {
            case "watching":
                return 0;
            case "watchlist":
                return 1;
            case "watched":
                return 2;
            case "cancelled":
                return 3;
            default:
                return 0;
        }
    }
}
