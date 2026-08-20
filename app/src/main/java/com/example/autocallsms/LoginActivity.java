package com.example.autocallsms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    TextView tvGoToRegister;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // Hardcoded default accounts (always available, no registration needed)
    String[][] users = {
            {"admin", "7109"},
            {"demo", "test"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip login if a session is already active
        SharedPreferences authPrefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        if (authPrefs.getBoolean("is_logged_in", false)) {
            String savedUsername = authPrefs.getString("username", "");
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("username", savedUsername);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> {

            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both User ID and Password", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Check hardcoded default accounts first
            boolean isDefaultUser = false;
            for (String[] user : users) {
                if (username.equals(user[0]) && password.equals(user[1])) {
                    isDefaultUser = true;
                    break;
                }
            }

            if (isDefaultUser) {
                completeLogin(authPrefs, username);
                return;
            }

            // 2. Not a default account, check Room database on a background thread
            dbExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                AppDatabase.User storedUser = db.userDao().getUserByUsername(username);

                boolean valid = storedUser != null
                        && storedUser.password.equals(RegisterActivity.hashPassword(password));

                runOnUiThread(() -> {
                    if (valid) {
                        completeLogin(authPrefs, username);
                    } else {
                        Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        if (tvGoToRegister != null) {
            tvGoToRegister.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }
    }

    private void completeLogin(SharedPreferences authPrefs, String username) {
        authPrefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("username", username)
                .apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }
}