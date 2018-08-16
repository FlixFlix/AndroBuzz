package com.geil.myapplication.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androbuzz.android.R;
import com.geil.myapplication.app.Config;
import com.geil.myapplication.util.NotificationUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private TextView txtToken, txtMessage, txtRegStatus;
    private static final String permissionPhoneState = Manifest.permission.READ_PHONE_STATE;
    private static final String permissionCoarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int REQUEST_CODE = 1337;

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String token;
    String deviceKey;
    FirebaseDatabase database;
    DatabaseReference theDatabase;
    DeviceModel theDevice;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        // Set up shared preferences
        pref = getApplicationContext().getSharedPreferences( Config.SHARED_PREF, 0 );
        editor = pref.edit();

        token = pref.getString( "token", null );
        Log.e( TAG, String.valueOf(token) );

        // Set up device object
        theDevice = new DeviceModel(token, this );
        deviceKey = theDevice.getName() + " (" + theDevice.getSerial() + ") ";
        editor.putString( "lastMessageTime", "2000-01-01 12:00:00 -05:00" );
        editor.putString( "deviceKey", deviceKey );
        editor.apply();

        // Set up text views
        txtToken = findViewById( R.id.txt_reg_id );
        txtRegStatus = findViewById( R.id.txt_reg_status );
        txtMessage = findViewById( R.id.txt_main_message );

        // Initialize database
        database = FirebaseDatabase.getInstance();
        theDatabase = database.getReference();

        // Listen to registration status
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive( Context context, Intent intent ) {
                Log.e( TAG, "Notification received." );
                String message = intent.getStringExtra( "message" );
                // txtMessage.setText( "xxx" );

                // checking for type intent filter
                if ( intent.getAction().equals( Config.REGISTRATION_COMPLETE ) ) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic( Config.TOPIC_GLOBAL );
                    // Toast.makeText( getApplicationContext(), "Push notification: " + message, Toast.LENGTH_LONG ).show();
                    Toast.makeText( getApplicationContext(), "Received Firebase Registration Token!" + message, Toast.LENGTH_LONG ).show();

                    // checkRegistrationStatus();

                } else if ( intent.getAction().equals( Config.PUSH_NOTIFICATION ) ) {
                    // new push notification is received


                    //doVibrate(message);

                }
            }
        };

        // Register button
        final Button registerButton = findViewById( R.id.btnRegister );
        registerButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                inDatabase = false;
                RegisterDevice();
                // checkRegistrationStatus();
                Log.e( TAG, "Registration date: " + theDevice.getRegDate() );
            }
        } );

        // checkRegistrationStatus();
    }

    private void RegisterDevice() {
        Log.e( TAG, "Registering..." );
        if ( ActivityCompat.checkSelfPermission( this, permissionPhoneState ) == PackageManager.PERMISSION_DENIED ) {
            ActivityCompat.requestPermissions( this, new String[]{permissionPhoneState}, REQUEST_CODE );
        }
        if ( ActivityCompat.checkSelfPermission( this, permissionCoarseLocation ) == PackageManager.PERMISSION_DENIED ) {
            ActivityCompat.requestPermissions( this, new String[]{permissionCoarseLocation}, REQUEST_CODE );
        }

        try {
            // Refresh device
            theDevice = new DeviceModel( token, this );

            // Update registration date
            SimpleDateFormat currentTimeSdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
            currentTimeSdf.setTimeZone( TimeZone.getTimeZone( "America/Chicago" ) );
            theDevice.setRegDate( currentTimeSdf.format( new Date() ) );
            editor.putString( "token", token );
            editor.apply();

            DatabaseReference dbDeviceEntry = theDatabase.child( "clients" ).child( deviceKey );
            // The reason we don't just use setValue is to preserve the [messages] node
            dbDeviceEntry.child( "brand" ).setValue( theDevice.getBrand() );
            dbDeviceEntry.child( "model" ).setValue( theDevice.getModel() );
            dbDeviceEntry.child( "name" ).setValue( theDevice.getName() );
            dbDeviceEntry.child( "number" ).setValue( theDevice.getNumber() );
            dbDeviceEntry.child( "regDate" ).setValue( theDevice.getRegDate() );
            dbDeviceEntry.child( "regToken" ).setValue( theDevice.getRegToken() );
            dbDeviceEntry.child( "serial" ).setValue( theDevice.getSerial() );

        } catch (Exception e) {
            Log.e( "DBG", e.getMessage() );
        }
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        if ( requestCode == REQUEST_CODE ) {
            for (int grant : grantResults) {
                if ( grant != PackageManager.PERMISSION_GRANTED ) {
                    RegisterDevice();
                    return;
                }
            }
        }
    }

    // Fetches registration token from shared preferences
    // and displays on the screen
    private void displayFirebaseToken() {

        if ( !TextUtils.isEmpty( token ) ) {
            String tokenText;
            tokenText = "Registration token: " + token;
            txtToken.setText( tokenText );
        } else {
            txtToken.setText( "Registration token not yet received" );
        }
    }

    boolean inDatabase = false;

    public boolean checkRegistrationStatus() {
        final String token = pref.getString( "token", null );
        final Button registerButton = findViewById( R.id.btnRegister );

        // Log.e( TAG, "Firebase registration token: " + token );
        if ( !TextUtils.isEmpty( token ) ) {
            txtRegStatus.setText( "Checking registration status..." );
            displayFirebaseToken();
            final DatabaseReference dbDeviceEntry = theDatabase.child( "clients" ).child( deviceKey );
            dbDeviceEntry.addListenerForSingleValueEvent( new ValueEventListener() {
                @Override
                public void onDataChange( @NonNull DataSnapshot data ) {
                    if ( !data.exists() ) {
                        txtRegStatus.setText( R.string.notRegistered );
                        inDatabase = false;
                    } else {
                        if ( token.equals( data.child( "regToken" ).getValue() ) ) {
                            registerButton.setText( R.string.alreadyregistered );
                            txtRegStatus.setText( "Device registered on " + data.child( "regDate" ).getValue() );
                            inDatabase = true;
                        } else {
                            txtRegStatus.setText( "Token mismatch. Updating registration..." );
                            inDatabase = false;
                            DatabaseReference dbRegToken = dbDeviceEntry.child( "regToken" );
                            dbRegToken.addListenerForSingleValueEvent( new ValueEventListener() {
                                @Override
                                public void onDataChange( @NonNull DataSnapshot data ) {
                                    txtRegStatus.setText( "Device registered on " + data.child( "regDate" ).getValue() );
                                    inDatabase = true;
                                }

                                @Override
                                public void onCancelled( @NonNull DatabaseError databaseError ) {
                                }
                            } );
                        }
                    }
                }

                @Override
                public void onCancelled( @NonNull DatabaseError databaseError ) {

                }
            } );
        } else {
            txtRegStatus.setText( "No registration token yet. Waiting for token..." );
        }
        // if ( !inDatabase ) RegisterDevice();
        return inDatabase;
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkRegistrationStatus();
        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance( this ).registerReceiver( mRegistrationBroadcastReceiver,
                new IntentFilter( Config.REGISTRATION_COMPLETE ) );

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance( this ).registerReceiver( mRegistrationBroadcastReceiver,
                new IntentFilter( Config.PUSH_NOTIFICATION ) );

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications( getApplicationContext() );
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance( this ).unregisterReceiver( mRegistrationBroadcastReceiver );
        super.onPause();
    }
}
