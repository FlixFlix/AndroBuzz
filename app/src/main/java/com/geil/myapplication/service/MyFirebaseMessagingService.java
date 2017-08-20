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

    private NotificationUtils notificationUtils;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage == null)
            return;

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification().getBody());
        }

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

    private void handleNotification(String message) {
        if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
            // app is in foreground, broadcast the push message
            Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
            pushNotification.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);
            // play notification sound
            NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
            notificationUtils.playNotificationSound();

        } else {
            // If the app is in background, firebase itself handles the notification
        }
    }

//    private void doVibrate(String message) {
//        final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//        int count;
//        if (message.equalsIgnoreCase("message_1")) {
//            count = 1;
//        } else if (message.equalsIgnoreCase("message_2")) {
//            count = 2;
//        } else {
//            count = 3;
//        }
//        for (int i = 0; i < count; i ++) {
//            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    v.vibrate(250);
//                }
//            }, 500 * i);
//        }
//    }
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

            if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
                // app is in foreground, broadcast the push message
                Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
                pushNotification.putExtra("message", message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

                // play notification sound
                NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
                notificationUtils.playNotificationSound();
            } else {
                // app is in background, show the notification in notification tray
                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                resultIntent.putExtra("message", message);

                // check for image attachment
                if (TextUtils.isEmpty(imageUrl)) {
                    showNotificationMessage(getApplicationContext(), title, message, timestamp, resultIntent);
                } else {
                    // image is present, show notification with image
                    showNotificationMessageWithBigImage(getApplicationContext(), title, message, timestamp, resultIntent, imageUrl);
                }

                doVibrate(message);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Json Exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    /**
     * Showing notification with text only
     */
    private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent);
    }

    /**
     * Showing notification with text and image
     */
    private void showNotificationMessageWithBigImage(Context context, String title, String message, String timeStamp, Intent intent, String imageUrl) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent, imageUrl);
    }
}
