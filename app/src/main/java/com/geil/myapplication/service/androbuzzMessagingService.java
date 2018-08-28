package com.geil.myapplication.service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.androbuzz.android.R;
import com.geil.myapplication.activity.MessageModel;
import com.geil.myapplication.app.Config;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class androbuzzMessagingService extends FirebaseMessagingService {

    private static final String t = androbuzzMessagingService.class.getSimpleName();

    @Override
    public void onNewToken( String regToken ) {

        // Saving registration token to shared preferences
        SharedPreferences pref = getApplicationContext().getSharedPreferences( Config.SHARED_PREF, 0 );
        SharedPreferences.Editor editor = pref.edit();
        editor.putString( "regToken", regToken );
        editor.apply();

        // Notify Activity that registration has completed and token can be sent to server
        Intent registrationComplete = new Intent( Config.REGISTRATION_COMPLETE );
        registrationComplete.putExtra( "regToken", regToken );
        LocalBroadcastManager.getInstance( this ).sendBroadcast( registrationComplete );

    }

    @Override
    public void onMessageReceived( RemoteMessage remoteMessage ) {
        // Log.e( t, "From: " + remoteMessage.getFrom() );

        // Check if message contains a data payload.
        if ( remoteMessage.getData().size() > 0 ) {
            try {
                JSONObject json = new JSONObject( remoteMessage.getData().toString() );
                JSONObject data = json.getJSONObject( "data" );

                // Send to DB, vibrate, update signal etc.
                handleDataMessage( remoteMessage.getMessageId(), data );

                // Notify UI of incoming message
                Intent pushNotification = new Intent( Config.PUSH_NOTIFICATION );
                pushNotification.putExtra( "command", data.getString( "command" ) );
                LocalBroadcastManager.getInstance( this ).sendBroadcast( pushNotification );
            } catch (Exception e) {
                Log.e( t, "Exception: " + e.getMessage() );
            }
        }
    }

    private void doVibrate( String command ) {
        final Vibrator vibrator = (Vibrator) getSystemService( Context.VIBRATOR_SERVICE );
        int pattern = 0;
        final int zzzz = 400,
                ____ = 800,
                zz = 100,
                __ = 200,
                zzzzzzzzzzz = 4000;
        try {
            pattern = Integer.parseInt( command );
        } catch (NumberFormatException nfe) {
            System.out.println( "Could not parse " + nfe );
        }

        long[][] patterns = {
                {0, 1}, // dummy vibration pattern
                {0, zzzz},                                                      // A (1)
                {0, zzzz, ____, zzzz},                                            // B (2)
                {0, zzzz, ____, zzzz, ____, zzzz},                                  // C (3)
                {0, zzzz, ____, zzzz, ____, zzzz, ____, zzzz},                        // ⇄ (4)
                {0, zz, __, zz, __, zz, __, zz, __, zz, __, zz, __, zz, __, zz},              // ⏩ (5)
                {0, zzzzzzzzzzz},                                               // 🎧 (6)
                {0, zzzz, ____, zz, __, zz, __, zz, __, zz, ____, zzzz},                 // ↺ (7)
                {0, 0}
        };
        vibrator.vibrate( patterns[pattern], -1 );
    }

    public int getBatteryLevel() {
        Intent batteryIntent = registerReceiver( null, new IntentFilter( Intent.ACTION_BATTERY_CHANGED ) );
        int level = batteryIntent.getIntExtra( BatteryManager.EXTRA_LEVEL, -1 );
        int scale = batteryIntent.getIntExtra( BatteryManager.EXTRA_SCALE, -1 );

        // Error checking that probably isn't needed but I added just in case.
        if ( level == -1 || scale == -1 ) {
            return Math.round( 50.0f );
        }
        return Math.round( ((float) level / (float) scale) * 100f );
    }

    private void handleDataMessage( String msgId, JSONObject data ) {

        try {
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService( Context.POWER_SERVICE );
            PowerManager.WakeLock wakeLock = pm.newWakeLock( (PowerManager.PARTIAL_WAKE_LOCK), "t" );
            wakeLock.acquire( 300_000 );

            SharedPreferences pref = getApplicationContext().getSharedPreferences( Config.SHARED_PREF, 0 );
            SharedPreferences.Editor editor = pref.edit();

            String deviceKey = pref.getString( "deviceKey", null );

            String timestamp = data.getString( "timestamp" );
            String messageDbKey = data.getString( "messageDbKey" );
            String command = data.getString( "command" );

            Calendar time = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH );
            time.setTime( sdf.parse( timestamp ) );

            Calendar currentTime = Calendar.getInstance();
            time.add( Calendar.MILLISECOND, 15_000 );

            if ( time.before( currentTime ) ) {
                Log.e( t, "Expired message. Disregarded." );
            } else {

                String lastMessageTimeString = pref.getString( "lastMessageTime", "2000-01-01 12:00:00 -05:00" );
                Calendar lastMessageTime = Calendar.getInstance();
                lastMessageTime.setTime( sdf.parse( lastMessageTimeString ) );
                editor.putString( "lastMessageTime", sdf.format( currentTime.getTime() ) );
                editor.apply();


                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference theDatabase = database.getReference();

                // Creating a new database entry for the received message
                MessageModel dbEntry = new MessageModel();
                dbEntry.setbatteryLevel( String.valueOf( getBatteryLevel() ) );
                dbEntry.setCommand( command );
                dbEntry.setTimeStamp( timestamp );
                dbEntry.setId( msgId );
                // dbEntry.setmessageDbKey( messageDbKey );
                // listenToSignalStrength(messageModel, theDatabase);
                String[] signal = getCurrentSignal( this );
                dbEntry.setSignal( signal[0] );
                dbEntry


                        .setSignalInfo( signal[1] );

                Calendar lastMessageTimeOffset = lastMessageTime;
                lastMessageTimeOffset.add( Calendar.MILLISECOND, 3_000 );
                if ( currentTime.before( lastMessageTimeOffset ) ) {
                    // Consecutive messages received too close to each other
                    doVibrate( getString( R.string.vibrationPatternSkip ) );
                    dbEntry.setExtras( "skipped" );
                    Log.e( t, "xxx" );
                    Log.e( t, String.valueOf( dbEntry ) );
                } else {
                    doVibrate( command );
                }


                // Add created message entry to database
                theDatabase.child( deviceKey ).child( "messages" ).child( messageDbKey ).setValue( dbEntry );

            }
            wakeLock.release();
            System.gc();
        } catch (JSONException e) {
            Log.e( t, "Json Exception: " + e.getMessage() );
        } catch (Exception e) {
            Log.e( t, "Exception: " + e.getMessage() );
        }

    }

    private String[] getCurrentSignal( Context context ) {
        String signalOutput = "Unknown", fullSignalInfo = "Unknown";
        if ( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            try {
                final TelephonyManager tm = (TelephonyManager) this.getSystemService( Context.TELEPHONY_SERVICE );
                for (final CellInfo info : tm.getAllCellInfo()) {
                    if ( info instanceof CellInfoGsm ) {
                        final CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        fullSignalInfo = String.valueOf( gsm );
                        signalOutput = String.valueOf( gsm.getLevel() );
                    } else if ( info instanceof CellInfoCdma ) {
                        final CellSignalStrengthCdma cdma = ((CellInfoCdma) info).getCellSignalStrength();
                        fullSignalInfo = String.valueOf( cdma );
                        signalOutput = String.valueOf( cdma.getLevel() );
                    } else if ( info instanceof CellInfoLte ) {
                        final CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                        // fullSignalInfo = "{"
                        signalOutput = String.valueOf( lte.getLevel() );
                        Log.e( t, "DBM: " + lte.getDbm() );
                        Log.e( t, "ASU: " + lte.getAsuLevel() );
                        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                            Log.e( t, "RSRQ: " + lte.getRsrq() );
                        }
                    } else {
                        Log.e( t, "Unknown type of cell signal?" );
                        throw new Exception( "Unknown type of cell signal!" );
                    }
                }
            } catch (Exception e) {
                Log.e( t, "Unable to obtain cell signal information", e );
            }
        } else {
            signalOutput = "0";
            fullSignalInfo = "Permission ACCESS_COARSE_LOCATION not granted";
            // We can't ask for permission during this stage
        }
        return new String[]{signalOutput, fullSignalInfo};
    }
//    private void listenToSignalStrength(final MessageModel messageModel, final DatabaseReference databaseReference) {
//        Log.e(t, "Listening to signal strength changes...");
//        Log.e(t, "Currently it's " + signalOutput);
//        final TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Looper.prepare();
//                telephonyManager.listen(new PhoneStateListener() {
//                    @Override
//                    public void onSignalStrengthsChanged(final SignalStrength signalStrength) {
//                        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
//                        int signalSupport = signalStrength.getGsmSignalStrength();
//                        if (signalSupport == 99) {
//                            if (android.os.Build.VERSION.SDK_INT >= 23) {
//                                switch (signalStrength.getLevel()) {
//                                    case SIGNAL_STRENGTH_POOR:
//                                        signalOutput = "Weak";
//                                        break;
//                                    case SIGNAL_STRENGTH_MODERATE:
//                                        signalOutput = "Average";
//                                        break;
//                                    case SIGNAL_STRENGTH_GOOD:
//                                        signalOutput = "Good";
//                                        break;
//                                    case SIGNAL_STRENGTH_GREAT:
//                                        signalOutput = "Great";
//                                        break;
//                                }
//                            }
//                        } else if (signalSupport >= 30) {
//                            signalOutput = "Great";
//                        } else if (signalSupport >= 20) {
//                            signalOutput = "Good";
//                        } else if (signalSupport >= 3) {
//                            signalOutput = "Average";
//                        } else {
//                            signalOutput = "Weak";
//                        }
//                        messageModel.setSignalInfo(signalOutput);
//
//                        SharedPreferences pref = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
//                        String token = pref.getString("regToken", null);
//                        databaseReference.child("messages").child(token).child(messageModel.getmessageDbKey()).setValue(messageModel);
//                        Log.w(t, "token from signal function: " + token);
//                        Looper.myLooper().quit();
//
//                    }
//                }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
//
//                Looper.loop();
//            }
//        }).start();
//    }
}
