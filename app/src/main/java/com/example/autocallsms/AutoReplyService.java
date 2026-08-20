package com.example.autocallsms;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class AutoReplyService extends Service {

    private static final String TAG = "AutoReplyService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String phoneNumber = intent.getStringExtra("phone_number");
        SharedPreferences preferences = getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE);

        boolean enabled = preferences.getBoolean("enabled", false);
        String currentMessageInput = preferences.getString("message", "Welcome!");

        String messageToSend = currentMessageInput;

        // Fire SMS if enabled and data is valid
        if (enabled && phoneNumber != null && !phoneNumber.isEmpty()
                && messageToSend != null && !messageToSend.isEmpty()) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                java.util.ArrayList<String> parts = smsManager.divideMessage(messageToSend);
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
                Toast.makeText(this, "Auto-reply sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS", e);
            }
        }

        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}