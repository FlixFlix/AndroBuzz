package com.geil.myapplication.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;
import java.net.HttpURLConnection;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import com.androbuzz.android.R;
import com.geil.myapplication.app.Config;
import com.geil.myapplication.util.NotificationUtils;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private TextView txtRegId, txtMessage;

    private static final String DB_URL = "https://androbuzz-8d0b1.firebaseio.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        txtRegId = (TextView) findViewById(R.id.txt_reg_id);
        txtMessage = (TextView) findViewById(R.id.txt_push_message);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(Config.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic(Config.TOPIC_GLOBAL);

                    displayFirebaseRegId();

                } else if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received

                    String message = intent.getStringExtra("message");

                    Toast.makeText(getApplicationContext(), "Push notification: " + message, Toast.LENGTH_LONG).show();
//                    doVibrate(message);

                    txtMessage.setText(message);
                }
            }
        };

        displayFirebaseRegId();
    }

//    private void doVibrate(String message) {
//        final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//
//        int pattern = 0,
//                zzzz = 400,
//                ____ = 800,
//                zz = 150,
//                __ = 100,
//                zzzzzzzzzzz = 4000;
//        try {
//            pattern = Integer.parseInt(message);
//        } catch (NumberFormatException nfe) {
//            System.out.println("Could not parse " + nfe);
//        }
//        long[][] patterns = {
//                {0, 0},
//                {0, zzzz},
//                {0, zzzz, ____, zzzz},
//                {0, zzzz, ____, zzzz, ____, zzzz},
//                {0, zzzz, ____, zzzz, ____, zzzz, ____, zzzz},
//                {0, zz, __, zz, __, zz, __, zz, __, zz, __, zz, __, zz, __, zz, __},
//                {0, zzzzzzzzzzz},
//                {0, 0}
//        };
//        v.vibrate(patterns[pattern], -1);
//    }



    // Fetches reg id from shared preferences
    // and displays on the screen
    private void displayFirebaseRegId() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
        String regId = pref.getString("regId", null);

        Log.e(TAG, "Firebase reg id: " + regId);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();

        try{
            myRef.child("clientId").setValue(regId);

        }catch (Exception e){
            Log.e("DBG", e.getMessage());
        }

        if (!TextUtils.isEmpty(regId))
            txtRegId.setText("Firebase Reg Id: " + regId);

        else {


            txtRegId.setText("Firebase Reg Id is not received yet!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }
}
