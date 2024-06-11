package com.example.screentrackr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private Button searchButton;
    private LinearLayout searchResultsContainer;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);
        searchResultsContainer = findViewById(R.id.search_results_container);
        client = new OkHttpClient();

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchMovies(query);
                } else {
                    Toast.makeText(SearchActivity.this, "Please enter a movie title to search.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Configurar o menu de navegação inferior
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_search); // Marcar como selecionado

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_tracker:
                        startActivity(new Intent(SearchActivity.this, TrackerActivity.class));
                        return true;
                    case R.id.nav_search:
                        // Já está na SearchActivity, então não precisa fazer nada
                        return true;
                    case R.id.nav_profile:
                        startActivity(new Intent(SearchActivity.this, ProfileActivity.class));
                        return true;
                }
                return false;
            }
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
        // Implementação para mostrar detalhes do filme no modal
        // Você pode usar um diálogo personalizado ou qualquer outra abordagem para exibir detalhes do filme
        try {
            String title = film.getString("Title");
            String year = film.getString("Year");
            String director = film.getString("Director");
            String rating = film.getString("imdbRating");
            String votes = film.getString("imdbVotes");
            String plot = film.getString("Plot");
            String posterUrl = film.getString("Poster");

            View modalView = getLayoutInflater().inflate(R.layout.film_modal, null);

            ImageView modalPoster = modalView.findViewById(R.id.film_poster);
            TextView modalTitle = modalView.findViewById(R.id.film_title);
            TextView modalDetails = modalView.findViewById(R.id.film_details);
            TextView modalPlot = modalView.findViewById(R.id.film_plot);

            modalTitle.setText(title + " (" + year + ")");
            modalDetails.setText("Director: " + director + "\nRating: " + rating + " (" + votes + " votes)");
            modalPlot.setText(plot);
            Picasso.get().load(posterUrl).into(modalPoster);

            new android.app.AlertDialog.Builder(this)
                    .setView(modalView)
                    .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                    .show();

        } catch (JSONException e) {
            Toast.makeText(this, "Failed to display modal data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
