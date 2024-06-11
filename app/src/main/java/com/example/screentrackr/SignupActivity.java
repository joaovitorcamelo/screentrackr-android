package com.example.screentrackr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private TextView errorMessageTextView;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        errorMessageTextView = findViewById(R.id.error_message);
        client = new OkHttpClient();

        Button registerButton = findViewById(R.id.register_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        TextView signinLinkTextView = findViewById(R.id.signin_link);
        signinLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirecionar para a atividade de login
                Intent loginIntent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .add("confirmPassword", confirmPassword)
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/screentrackr_war_exploded/RegisterServlet") // Certifique-se de ajustar a URL correta do servlet
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showError("Failed to register: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(SignupActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        // Redirecionar para a página de login
                        Intent loginIntent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(loginIntent);
                    });
                } else {
                    runOnUiThread(() -> showError("Failed to register: " + response.message()));
                }
            }
        });
    }

    private void showError(String message) {
        errorMessageTextView.setText(message);
        errorMessageTextView.setVisibility(View.VISIBLE);
    }
}
