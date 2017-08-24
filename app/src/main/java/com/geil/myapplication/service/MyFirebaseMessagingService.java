package com.geil.myapplication.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
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

/**
 * Created by Ahmed Al-Bayati on 08/08/16.
 */
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
                zz = 150,
                __ = 150,
                zzzzzzzzzzz = 4000;
        try {
            pattern = Integer.parseInt(message);
        } catch (NumberFormatException nfe) {
            System.out.println("Could not parse " + nfe);
        }
        long[][] patterns = {
                {0,0},
                {0,zzzz},
                {0,zzzz,____,zzzz},
                {0,zzzz,____,zzzz,____,zzzz},
                {0,zzzz,____,zzzz,____,zzzz,____,zzzz},
                {0,zz,__,zz,__,zz,__,zz,__,zz,__,zz,__,zz,__,zz,__},
                {0,zzzzzzzzzzz},
                {0,0}
        };
        v.vibrate(patterns[pattern], -1);
    }

    private void handleDataMessage(String msgId, JSONObject json) {
        Log.e(TAG, "push json: " + json.toString());

        try {
            JSONObject data = json.getJSONObject("data");

            String title = data.getString("title");
            String message = data.getString("message");
            boolean isBackground = data.getBoolean("is_background");
            String imageUrl = data.getString("image");
            String timestamp = data.getString("timestamp");
            String uniqueId = data.getString("uniqueId");
//            JSONObject payload = data.getJSONObject("payload");

            Log.e(TAG, "title: " + title);
            Log.e(TAG, "message: " + message);
            Log.e(TAG, "isBackground: " + isBackground);
//            Log.e(TAG, "payload: " + payload.toString());
            Log.e(TAG, "imageUrl: " + imageUrl);
            Log.e(TAG, "timestamp: " + timestamp);
            Log.e(TAG, "msgId: " + msgId);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH);
            calendar.setTime(sdf.parse(timestamp));

            Calendar currentTime = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 5);
            if(calendar.before(currentTime)){
                Log.e("DBG", currentTime.toString());
                Log.e("DBG", calendar.toString());
                return; //if push message exceeds 5 seconds then we disregard it.
            }else{
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference();

                MessageModel msg = new MessageModel();
                msg.setTitle(title);
                msg.setMessage(message);
                msg.setTimeStamp(timestamp);
                msg.setImageUrl(imageUrl);
                msg.setId(msgId);
                msg.setUniqueId(uniqueId);
                msg.setBackground(isBackground);
//                myRef.child("messages").setValue(uniqueId);
                myRef.child("messages").child(uniqueId).setValue(msg);
            }
            doVibrate(message);
        } catch (JSONException e) {
            Log.e(TAG, "Json Exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }
}
