package com.example.autocallsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class PhoneStateReceiver extends BroadcastReceiver {

    private static boolean wasRinging = false;
    private static boolean wasOffhook = false;
    private static String incomingNumber = "";
    private static String cachedOutgoingNumber = ""; // Safe local memory tracker

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        // STEP 1: Catch dialed number from keypad immediately when call triggers
        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            cachedOutgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            return; // Halt here; wait for system PHONE_STATE to change
        }

        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) return;

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) return;

        // STEP 2: Handle Ringing (Incoming Call Only)
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            wasRinging = true;
            wasOffhook = false;
            incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Toast.makeText(context, "Incoming Call", Toast.LENGTH_SHORT).show();
        }

        // STEP 3: Handle Call Active (Answered or Dialing Outbound)
        else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            wasOffhook = true;
            Toast.makeText(context, "Call Active", Toast.LENGTH_SHORT).show();
        }

        // STEP 4: Handle Call Ended (IDLE State)
        else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            if (wasRinging) {
                // ---- INCOMING call just ended ----
                wasRinging = false;
                wasOffhook = false;

                Toast.makeText(context, "Incoming Call Ended", Toast.LENGTH_SHORT).show();

                if (incomingNumber != null && !incomingNumber.isEmpty()) {
                    sendToService(context, incomingNumber);
                    incomingNumber = ""; // Clear
                }

            } else if (wasOffhook) {
                // ---- OUTGOING call just ended ----
                wasOffhook = false;

                Toast.makeText(context, "Outgoing Call Ended", Toast.LENGTH_SHORT).show();

                // STEP 5: Fire target message using our saved key string instantly
                if (cachedOutgoingNumber != null && !cachedOutgoingNumber.isEmpty()) {
                    sendToService(context, cachedOutgoingNumber);
                    cachedOutgoingNumber = ""; // Clear out variable for next execution cycle
                } else {
                    Toast.makeText(context, "No dialed number captured", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void sendToService(Context context, String phoneNumber) {
        Intent serviceIntent = new Intent(context, AutoReplyService.class);
        serviceIntent.putExtra("phone_number", phoneNumber);
        context.startService(serviceIntent);
    }
}