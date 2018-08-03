package com.geil.myapplication.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.geil.myapplication.activity.MessageModel;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import com.geil.myapplication.activity.MainActivity;
import com.geil.myapplication.app.Config;
import com.geil.myapplication.util.NotificationUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.telephony.CellSignalStrength.SIGNAL_STRENGTH_GOOD;
import static android.telephony.CellSignalStrength.SIGNAL_STRENGTH_GREAT;
import static android.telephony.CellSignalStrength.SIGNAL_STRENGTH_MODERATE;
import static android.telephony.CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
import static android.telephony.CellSignalStrength.SIGNAL_STRENGTH_POOR;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage == null)
            return;

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.e(TAG, "Data Payload: " + remoteMessage.getData().toString());

            try {
                JSONObject json = new JSONObject(remoteMessage.getData().toString());
                handleDataMessage(remoteMessage.getMessageId(), json);
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }

        }
    }

    private void doVibrate(String message) {
        final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        int pattern = 0,
                zzzz = 400,
                ____ = 800,
                zz = 99,
                __ = 99,
                zzzzzzzzzzz = 4000;
        try {
            pattern = Integer.parseInt(message);
        } catch (NumberFormatException nfe) {
            System.out.println("Could not parse " + nfe);
        }
        if(pattern == 0){
            //null message
            return;
        }

        long[][] patterns = {
                {0, 0},
                {0, zzzz},                                                      // A (1)
                {0, zzzz, ____, zzzz},                                            // B (2)
                {0, zzzz, ____, zzzz, ____, zzzz},                                  // C (3)
                {0, zzzz, ____, zzzz, ____, zzzz, ____, zzzz},                        // ⇄ (4)
                {0, zz, __, zz, __, zz, __, zz, __, zz, __, zz, __, zz, __, zz},              // ⏩ (5)
                {0, zzzzzzzzzzz},                                               // 🎧 (6)
                {0, zzzz, ____, zz, __, zz, __, zz, __, zz, ____, zzzz},                 // ↺ (7)
                {0, 0}
        };
        v.vibrate(patterns[pattern], -1);
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    private void handleDataMessage(String msgId, JSONObject json) {
        Log.e(TAG, "push json: " + json.toString());

        try {
            SharedPreferences pref = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
            String regId = pref.getString("regId", null);

            JSONObject data = json.getJSONObject("data");

//            String title = data.getString("title");
            String message = data.getString("message");
            boolean isBackground = data.getBoolean("is_background");
            String batteryLevel = String.valueOf(getBatteryLevel());
            String timestamp = data.getString("timestamp");
            String uniqueId = data.getString("uniqueId");
//            JSONObject payload = data.getJSONObject("payload");

            //Log.e(TAG, "title: " + title);
            Log.e(TAG, "message: " + message);
            Log.e(TAG, "isBackground: " + isBackground);
//            Log.e(TAG, "payload: " + payload.toString());
            Log.e(TAG, "batteryLevel: " + batteryLevel);
            Log.e(TAG, "timestamp: " + timestamp);
            Log.e(TAG, "msgId: " + msgId);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH);
            calendar.setTime(sdf.parse(timestamp));

            Calendar currentTime = Calendar.getInstance();
            calendar.add(Calendar.MILLISECOND, 15000);
            if (calendar.before(currentTime)) {
                Log.e("DBG", currentTime.toString());
                Log.e("DBG", calendar.toString());
                return; //if push message exceeds 15 seconds then we disregard it.
            } else {
                PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK), "TAG");
                wakeLock.acquire(300000);
//                float batteryPct = getBatteryLevel();
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference();

                MessageModel msg = new MessageModel();
                msg.setMessage(message);
                msg.setTimeStamp(timestamp);
                msg.setbatteryLevel(batteryLevel);
                msg.setId(msgId);
                msg.setUniqueId(uniqueId);
                msg.setBackground(isBackground);
                if(isMessageZero(message)){
                    listenToSignalStrength(msg, myRef);
                    return;
                }
//                myRef.child("messages").setValue(uniqueId);
                myRef.child("messages").child(regId).child(uniqueId).setValue(msg);
                Log.e(TAG, "sent");
                doVibrate(message);
//                wakeLock.release();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Json Exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    private boolean isMessageZero(String message){
        try{
            int integer = Integer.parseInt(message);
            return integer == 0;
        }catch (Exception e){ }
        return false;
    }

    private void listenToSignalStrength(final MessageModel mm, final DatabaseReference myRef){
        final TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                telephonyManager.listen(new PhoneStateListener() {
                    @Override
                    public void onSignalStrengthsChanged(final SignalStrength signalStrength) {
                        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
                        int signalSupport = signalStrength.getGsmSignalStrength();
                        String signalOutput = "";
                        if(signalSupport == 99){
                            signalOutput = "Unknown";
                            if(android.os.Build.VERSION.SDK_INT >= 23){
                                switch (signalStrength.getLevel()){
                                    case SIGNAL_STRENGTH_POOR:
                                        signalOutput = "Weak";
                                        break;
                                    case SIGNAL_STRENGTH_MODERATE:
                                        signalOutput = "Average";
                                        break;
                                    case SIGNAL_STRENGTH_GOOD:
                                        signalOutput = "Good";
                                        break;
                                    case SIGNAL_STRENGTH_GREAT:
                                        signalOutput = "Great";
                                        break;
                                }
                            }
                        }else if(signalSupport >= 30){
                            signalOutput = "Great";
                        }else if(signalSupport >= 20){
                            signalOutput = "Good";
                        }else if(signalSupport >= 3){
                            signalOutput = "Average";
                        }else {
                            signalOutput = "Weak";
                        }
                        mm.setSignalStrength(signalOutput);

                        SharedPreferences pref = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
                        String regId = pref.getString("regId", null);
                        myRef.child("messages").child(regId).child(mm.getUniqueId()).setValue(mm);

                        Looper.myLooper().quit();

                    }
                }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

                Looper.loop();
            }
        }).start();
    }
}
