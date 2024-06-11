package com.example.screentrackr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button loginButton;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa os botões
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);

        // Define o comportamento do botão de login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inicia a LoginActivity
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        // Define o comportamento do botão de registro
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inicia a SignupActivity
                Intent signupIntent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(signupIntent);
            }
        });
    }
}
