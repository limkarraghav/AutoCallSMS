package com.example.autocallsms;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    EditText etRegUsername, etRegPassword, etRegConfirmPassword, etKeyValue;
    Button btnGenerate, btnRegister;
    TextView tvKeyLabel, tvGoToLogin;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private static final int MIN_PASSWORD_LENGTH = 4;

    // Fixed dictionary of 50 key -> 5-digit value pairs (predefined, never changes)
    private static final Map<Integer, String> keyMap = new HashMap<>();
    static {
        keyMap.put(1, "51742");  keyMap.put(2, "86415");  keyMap.put(3, "29183");
        keyMap.put(4, "73056");  keyMap.put(5, "14897");  keyMap.put(6, "68234");
        keyMap.put(7, "90521");  keyMap.put(8, "37648");  keyMap.put(9, "45912");
        keyMap.put(10, "81367"); keyMap.put(11, "24580"); keyMap.put(12, "69135");
        keyMap.put(13, "12864"); keyMap.put(14, "57429"); keyMap.put(15, "93716");
        keyMap.put(16, "46285"); keyMap.put(17, "31974"); keyMap.put(18, "75603");
        keyMap.put(19, "18492"); keyMap.put(20, "52831"); keyMap.put(21, "89047");
        keyMap.put(22, "34765"); keyMap.put(23, "61528"); keyMap.put(24, "97214");
        keyMap.put(25, "43697"); keyMap.put(26, "75182"); keyMap.put(27, "20349");
        keyMap.put(28, "68451"); keyMap.put(29, "15973"); keyMap.put(30, "82740");
        keyMap.put(31, "39518"); keyMap.put(32, "64107"); keyMap.put(33, "28695");
        keyMap.put(34, "90463"); keyMap.put(35, "17350"); keyMap.put(36, "54829");
        keyMap.put(37, "79261"); keyMap.put(38, "43186"); keyMap.put(39, "60594");
        keyMap.put(40, "25817"); keyMap.put(41, "83942"); keyMap.put(42, "16478");
        keyMap.put(43, "58730"); keyMap.put(44, "92653"); keyMap.put(45, "31085");
        keyMap.put(46, "74819"); keyMap.put(47, "49526"); keyMap.put(48, "87134");
        keyMap.put(49, "13259"); keyMap.put(50, "66482");
    }

    private Integer selectedKey = null;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegUsername = findViewById(R.id.etRegUsername);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        etKeyValue = findViewById(R.id.etKeyValue);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnRegister = findViewById(R.id.btnRegister);
        tvKeyLabel = findViewById(R.id.tvKeyLabel);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordsMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etRegPassword.addTextChangedListener(passwordWatcher);
        etRegConfirmPassword.addTextChangedListener(passwordWatcher);

        btnGenerate.setOnClickListener(v -> {
            selectedKey = random.nextInt(50) + 1;
            tvKeyLabel.setText("Key : " + selectedKey);
            etKeyValue.setText("");
        });

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void checkPasswordsMatch() {
        String password = etRegPassword.getText().toString();
        String confirmPassword = etRegConfirmPassword.getText().toString();

        boolean matches = !password.isEmpty()
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.equals(confirmPassword);

        btnGenerate.setEnabled(matches);

        if (!matches) {
            selectedKey = null;
            tvKeyLabel.setText("Key : -");
            etKeyValue.setText("");
        }
    }

    private void attemptRegister() {
        String username = etRegUsername.getText().toString().trim();
        String password = etRegPassword.getText().toString();
        String confirmPassword = etRegConfirmPassword.getText().toString();
        String enteredKeyValue = etKeyValue.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.contains(" ")) {
            Toast.makeText(this, "Username cannot contain spaces", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Password not matching", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedKey == null) {
            Toast.makeText(this, "Please tap Generate to get a key first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(enteredKeyValue)) {
            Toast.makeText(this, "Please enter the key value", Toast.LENGTH_SHORT).show();
            return;
        }

        String correctValue = keyMap.get(selectedKey);
        if (correctValue == null || !correctValue.equals(enteredKeyValue)) {
            Toast.makeText(this, "Key not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // All checks passed, verify username availability and save on background thread
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            AppDatabase.User existing = db.userDao().getUserByUsername(username);

            if (existing != null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "That username is already taken", Toast.LENGTH_SHORT).show());
                return;
            }

            String hashedPassword = hashPassword(password);
            db.userDao().insert(new AppDatabase.User(username, hashedPassword));

            runOnUiThread(() -> {
                Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        });
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}