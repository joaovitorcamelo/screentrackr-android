package com.example.screentrackr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView signupLink;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        signupLink = findViewById(R.id.signup_link);

        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (!email.isEmpty() && !password.isEmpty()) {
                    loginUser(email, password);
                } else {
                    Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirecionar para a atividade de registro
                Intent signupIntent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(signupIntent);
            }
        });
    }

    private void loginUser(String email, String password) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/screentrackr_war_exploded/LoginServlet") // Certifique-se de ajustar a URL correta do servlet
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("LoginActivity", "Response Data: " + responseData); // Log da resposta completa

                // Verificar se a resposta é um JSON válido
                if (responseData.startsWith("<")) {
                    // Provavelmente é HTML, não JSON
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Unexpected response format: HTML received", Toast.LENGTH_SHORT).show());
                } else {
                    handleLoginResponse(responseData);
                }
            }
        });
    }

    private void handleLoginResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");

            if ("success".equals(status)) {
                boolean firstLogin = jsonResponse.getBoolean("firstLogin");
                int userId = jsonResponse.getInt("userId");
                String authToken = jsonResponse.getString("authToken");
                String email = emailInput.getText().toString().trim();

                // Salvar userId, authToken e email em SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("userId", userId);
                editor.putString("authToken", authToken);
                editor.putString("userEmail", email);
                editor.apply();

                if (firstLogin) {
                    // Redirecionar para a página after_first_log com o userId
                    Intent intent = new Intent(LoginActivity.this, AfterFirstLoginActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                } else {
                    // Redirecionar para a página principal (tracker)
                    Intent intent = new Intent(LoginActivity.this, TrackerActivity.class);
                    startActivity(intent);
                }
                finish(); // Fechar a atividade atual
            } else {
                String message = jsonResponse.getString("message");
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to parse response: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            Log.e("LoginActivity", "Response Parsing Error: ", e); // Log do erro de parsing
        }
    }
}
