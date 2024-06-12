package com.example.screentrackr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

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

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private Button searchButton;
    private LinearLayout searchResultsContainer;
    private OkHttpClient client;
    private SharedPreferences sharedPreferences;
    private int userId;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);
        searchResultsContainer = findViewById(R.id.search_results_container);
        client = new OkHttpClient();
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE);

        // Recuperar informações do usuário
        userId = sharedPreferences.getInt("userId", -1);
        authToken = sharedPreferences.getString("authToken", null);

        // Verificar se o usuário está autenticado
        if (userId == -1 || authToken == null) {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SearchActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Logar os dados para verificação
        Log.d("UserPrefs", "User ID: " + userId);
        Log.d("UserPrefs", "Auth Token: " + authToken);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchMovies(query);
            } else {
                Toast.makeText(SearchActivity.this, "Please enter a movie title to search.", Toast.LENGTH_SHORT).show();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_search);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_tracker:
                    startActivity(new Intent(SearchActivity.this, TrackerActivity.class));
                    return true;
                case R.id.nav_search:
                    return true;
                case R.id.nav_profile:
                    startActivity(new Intent(SearchActivity.this, ProfileActivity.class));
                    return true;
            }
            return false;
        });
    }

    private void searchMovies(String query) {
        String url = "http://www.omdbapi.com/?t=" + query + "&apikey=4408d32b";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Failed to search movies: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject film = new JSONObject(responseData);
                        runOnUiThread(() -> displayResults(film));
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Failed to parse movie data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Failed to search movies", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void displayResults(JSONObject film) {
        searchResultsContainer.removeAllViews();

        try {
            String title = film.getString("Title");
            String year = film.getString("Year");
            String plot = film.getString("Plot");
            String posterUrl = film.getString("Poster");

            View filmView = getLayoutInflater().inflate(R.layout.film_item, searchResultsContainer, false);

            ImageView filmPoster = filmView.findViewById(R.id.film_poster);
            TextView filmTitle = filmView.findViewById(R.id.film_title);
            TextView filmYear = filmView.findViewById(R.id.film_year);
            TextView filmPlot = filmView.findViewById(R.id.film_plot);

            filmTitle.setText(title);
            filmYear.setText(year);
            filmPlot.setText(plot);
            Picasso.get().load(posterUrl).into(filmPoster);

            filmView.setOnClickListener(v -> showFilmModal(film));

            searchResultsContainer.addView(filmView);
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to display movie data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilmModal(JSONObject film) {
        try {
            String title = film.getString("Title");
            String year = film.getString("Year");
            String director = film.getString("Director");
            String rating = film.getString("imdbRating");
            String votes = film.getString("imdbVotes");
            String plot = film.getString("Plot");
            String posterUrl = film.getString("Poster");
            String filmId = film.getString("imdbID");

            View modalView = getLayoutInflater().inflate(R.layout.film_modal_search, null);

            ImageView modalPoster = modalView.findViewById(R.id.film_poster);
            TextView modalTitle = modalView.findViewById(R.id.film_title);
            TextView modalDetails = modalView.findViewById(R.id.film_details);
            TextView modalPlot = modalView.findViewById(R.id.film_plot);
            Spinner relationTypeSpinner = modalView.findViewById(R.id.relation_type_spinner);
            CheckBox favoriteCheckbox = modalView.findViewById(R.id.favorite_checkbox);
            Button addSubmitFilm = modalView.findViewById(R.id.add_submit_film);

            modalTitle.setText(title + " (" + year + ")");
            modalDetails.setText("Director: " + director + "\nRating: " + rating + " (" + votes + " votes)");
            modalPlot.setText(plot);
            Picasso.get().load(posterUrl).into(modalPoster);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.relation_types, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            relationTypeSpinner.setAdapter(adapter);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(modalView)
                    .setPositiveButton("Fechar", (dialog, which) -> dialog.dismiss())
                    .show();

            addSubmitFilm.setText("Adicionar Filme");
            addSubmitFilm.setOnClickListener(v -> {
                String selectedRelationType = relationTypeSpinner.getSelectedItem().toString();
                boolean isFavorite = favoriteCheckbox.isChecked();

                addOrUpdateFilmRelation(filmId, selectedRelationType, isFavorite, title, year, director, rating, votes, plot, posterUrl);
            });

        } catch (JSONException e) {
            Toast.makeText(this, "Failed to display modal data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addOrUpdateFilmRelation(String filmId, String relationType, boolean isFavorite, String title, String year, String director, String rating, String votes, String plot, String posterImgUrl) {
        if (userId == -1) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("filmId", filmId)
                .add("relationType", relationType)
                .add("favorite", String.valueOf(isFavorite))
                .add("title", title)
                .add("year", year)
                .add("director", director)
                .add("rating", rating)
                .add("votes", votes)
                .add("plot", plot)
                .add("posterImgUrl", posterImgUrl)
                .add("userId", String.valueOf(userId)) // Adicionando userId ao form
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/screentrackr_war_exploded/FilmRelationServlet")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Failed to update film relation: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Film relation updated successfully!", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Failed to update film relation", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
