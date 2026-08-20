package com.example.autocallsms;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etMessage;
    private Switch swAutoReply;

    private TextView tvPhoneState;
    private TextView tvSms;
    private TextView tvCallLog;
    private TextView tvStatus;
    private TextView tvWelcome;
    private TextView tvAvatar;
    private TextView btnLogout;

    private SharedPreferences preferences;
    private SharedPreferences authPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("AutoReplyPrefs", MODE_PRIVATE);
        authPrefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);

        etMessage = findViewById(R.id.etMessage);
        swAutoReply = findViewById(R.id.swAutoReply);

        tvPhoneState = findViewById(R.id.tvPhoneState);
        tvSms = findViewById(R.id.tvSms);
        tvCallLog = findViewById(R.id.tvCallLog);
        tvStatus = findViewById(R.id.tvStatus);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvAvatar = findViewById(R.id.tvAvatar);
        btnLogout = findViewById(R.id.btnLogout);

        String username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = authPrefs.getString("username", "");
        }

        String displayName = username.isEmpty() ? "" :
                username.substring(0, 1).toUpperCase() + username.substring(1);

        tvWelcome.setText(username.isEmpty() ? "Welcome!" : "Welcome! " + displayName);
        tvAvatar.setText(username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase());

        etMessage.setText(preferences.getString("message", "Welcome!"));
        swAutoReply.setChecked(preferences.getBoolean("enabled", false));

        updateUI();

        btnLogout.setOnClickListener(v -> logout());

        swAutoReply.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit()
                    .putBoolean("enabled", isChecked)
                    .putString("message", etMessage.getText().toString())
                    .apply();

            if (isChecked) {
                startService(new Intent(this, AutoReplyService.class));
                Toast.makeText(this, "Auto Reply Enabled", Toast.LENGTH_SHORT).show();
            } else {
                stopService(new Intent(this, AutoReplyService.class));
                Toast.makeText(this, "Auto Reply Disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        swAutoReply.setChecked(false);
        stopService(new Intent(this, AutoReplyService.class));
        preferences.edit().putBoolean("enabled", false).apply();
        authPrefs.edit().clear().apply();

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateUI() {
        boolean allGranted = checkPermissions();

        if (allGranted) {
            swAutoReply.setEnabled(true);
            tvStatus.setText("Ready");
            tvStatus.setTextColor(Color.GREEN);
        } else {
            swAutoReply.setChecked(false);
            swAutoReply.setEnabled(false);
            tvStatus.setText("Grant all permissions first");
            tvStatus.setTextColor(Color.RED);
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        boolean phone = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        boolean sms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean callLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;

        // FIXED: Verifies that your phone allows tracking outbound dial data
        boolean outgoing = ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_GRANTED;

        updatePermissionStatus(tvPhoneState, phone && outgoing);
        updatePermissionStatus(tvSms, sms);
        updatePermissionStatus(tvCallLog, callLog);

        return phone && sms && callLog && outgoing;
    }

    private void updatePermissionStatus(TextView tv, boolean granted) {
        if (granted) {
            tv.setText("Granted");
            tv.setTextColor(Color.GREEN);
        } else {
            tv.setText("Not Granted");
            tv.setTextColor(Color.RED);
        }
    }

    private void requestPermissions() {
        // FIXED: Explicitly triggers a system alert window prompting for dial tracking permissions
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.PROCESS_OUTGOING_CALLS
                },
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updateUI();
        }
    }
}
