package com.example.screentrackr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class AfterFirstLoginActivity extends AppCompatActivity {

    private EditText nameInput;
    private Button continueButton;
    private OkHttpClient client;
    private int userId;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_first_log);

        nameInput = findViewById(R.id.name_input);
        continueButton = findViewById(R.id.continue_button);
        client = new OkHttpClient();

        // Obter SharedPreferences
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "User ID is invalid. Please log in again.", Toast.LENGTH_SHORT).show();
            // Opcionalmente, redirecionar para a tela de login se userId for inválido
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInput.getText().toString().trim();
                if (!name.isEmpty()) {
                    updateUserName(name);
                } else {
                    Toast.makeText(AfterFirstLoginActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUserName(String name) {
        if (userId == -1) {
            Toast.makeText(this, "User ID is invalid. Cannot update name.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("name", name)
                .add("userId", String.valueOf(userId)) // Enviar userId para o servlet
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/screentrackr_war_exploded/UpdateNameServlet") // URL do seu servlet
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AfterFirstLoginActivity.this, "Failed to update name: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(AfterFirstLoginActivity.this, "Name updated successfully!", Toast.LENGTH_SHORT).show();
                        // Redirecionar para a página principal (tracker)
                        Intent intent = new Intent(AfterFirstLoginActivity.this, TrackerActivity.class);
                        startActivity(intent);
                        finish(); // Terminar esta atividade
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(AfterFirstLoginActivity.this, "Failed to update name", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
