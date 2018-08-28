package com.geil.myapplication.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androbuzz.android.R;
import com.geil.myapplication.app.Config;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final int permissionCodeLocation = 1, permissionCodePhone = 2;
    private TextView txtMessage, txtStatus;

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    FirebaseDatabase database;
    DatabaseReference theDatabase;
    Button registerButton;

    public String getToken() {
        return pref.getString( "regToken", null );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );


        ActivityCompat.requestPermissions( this, new String[] {ACCESS_COARSE_LOCATION, READ_PHONE_STATE}, permissionCodeLocation );




        // Set up shared preferences
        pref = getApplicationContext().getSharedPreferences( Config.SHARED_PREF, 0 );
        editor = pref.edit();

        // Set up text views
        txtMessage = findViewById( R.id.txt_main_message );
        txtStatus = findViewById( R.id.txt_status );
        txtStatus.setMovementMethod( new ScrollingMovementMethod() );

        // Initialize database
        database = FirebaseDatabase.getInstance();
        theDatabase = database.getReference();


        // Register button
        registerButton = findViewById( R.id.btnRegister );
        if ( !pref.contains( "regToken" ) ) {
            registerButton.setEnabled( false );
        } else {
            Status( "Stored token exists: " + pref.getString( "regToken", null ) );
            registerButton.setText( R.string.alreadyregistered );
        }
        registerButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                registerDevice();
                // checkRegistration();
            }
        } );

        // checkRegistration();
    }

    public void Status( String text ) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat( "HH:mm:ss" );
        df.setTimeZone( TimeZone.getTimeZone( "America/Chicago" ) );
        String formattedDate = df.format( c.getTime() );
        String status = formattedDate + " " + text;
        txtStatus.append( status.substring( 0, Math.min( 100, status.length() ) ) + "\n" );
        while (txtStatus.canScrollVertically( 1 )) {
            txtStatus.scrollBy( 0, 1 );
        }
    }

    public void Message( String text ) {
        txtMessage.setText( text );
        // Snackbar.make( txtMessage, text, Snackbar.LENGTH_LONG ).show();
    }

    public void registerDevice() {
        Status( "Registering..." );
        try {
            // Set up device object
            DeviceModel theDevice = new DeviceModel( getToken(), this );

            editor.putString( "lastMessageTime", "2000-01-01 12:00:00 -05:00" );
            editor.putString( "deviceKey", theDevice.getDeviceKey() );
            editor.apply();

            // Update registration date
            SimpleDateFormat currentTimeSdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
            currentTimeSdf.setTimeZone( TimeZone.getTimeZone( "America/Chicago" ) );
            theDevice.setRegDate( currentTimeSdf.format( new Date() ) );
            editor.apply();

            DatabaseReference dbDeviceEntry = theDatabase.child( theDevice.getDeviceKey() );
            // The reason we don't just use setValue is to preserve the [messages] node
            dbDeviceEntry.child( "brand" ).setValue( theDevice.getBrand() );
            dbDeviceEntry.child( "model" ).setValue( theDevice.getModel() );
            dbDeviceEntry.child( "name" ).setValue( theDevice.getName() );

            String number = theDevice.getNumber( this );
            if ( number.equals( R.string.noPermission ) )
                Status( "Registering without a phone number" );
            dbDeviceEntry.child( "number" ).setValue( number );

            dbDeviceEntry.child( "regDate" ).setValue( theDevice.getRegDate() );
            dbDeviceEntry.child( "regToken" ).setValue( theDevice.getRegToken() );
            dbDeviceEntry.child( "serial" ).setValue( theDevice.getSerial() );

            Status( "Token: " + pref.getString( "regToken", null ) );
            Status( "regDate updated: " + theDevice.getRegDate() );
            Button registerButton = findViewById( R.id.btnRegister );
            registerButton.setText( "Device Registered" );
        } catch (Exception e) {
            Log.e( "DBG", e.getMessage() );
        }

    }

    public void checkRegistration( final boolean forceUpdate ) {
        final String token = pref.getString( "regToken", null );
        final String deviceKey = pref.getString( "deviceKey", "" );

        if ( !TextUtils.isEmpty( token ) ) {
            Status( "Checking registration status..." );
            final DatabaseReference dbDeviceEntry = theDatabase.child( deviceKey );
            dbDeviceEntry.addListenerForSingleValueEvent( new ValueEventListener() {
                @Override
                public void onDataChange( @NonNull DataSnapshot data ) {
                    if ( !data.exists() || !token.equals( data.child( "regToken" ).getValue() ) ) {
                        Status( "Not registered or token mismatch" );
                        registerDevice();
                    } else {
                        Status( "Registered on " + data.child( "regDate" ).getValue() );
                        if ( forceUpdate ) {
                            Status( "Forced registration update:" );
                            registerDevice();
                        }
                    }
                }

                @Override
                public void onCancelled( @NonNull DatabaseError databaseError ) {

                }
            } );
        } else {
            Status( "No registration token yet. Waiting for token..." );
        }
    }

    // Handling received intents
    private BroadcastReceiver serviceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {

            if ( intent.getAction().equals( Config.REGISTRATION_COMPLETE ) ) {
                Toast toast = Toast.makeText( getApplicationContext(), "Received Firebase registration token", Toast.LENGTH_LONG );
                toast.setGravity( Gravity.CENTER, 0, 0 );
                toast.show();
                Status( "Firebase regToken created: " + intent.getStringExtra( "regToken" ) );

                Button registerButton = findViewById( R.id.btnRegister );
                registerButton.setEnabled( true );
                checkRegistration( false );

            } else if ( intent.getAction().equals( Config.PUSH_NOTIFICATION ) ) {
                Toast toast = Toast.makeText( getApplicationContext(), "Received Command: " + intent.getStringExtra( "command" ), Toast.LENGTH_SHORT );
                toast.setGravity( Gravity.CENTER, 0, 0 );
                toast.show();
            }
        }
    };

    public void askForPermission( String[] permission, int permissionCode ) {
        ActivityCompat.requestPermissions( this, permission, permissionCode );
    }

    public static String getApplicationName( Context context ) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString( stringId );
    }

    public void permissionDialog( final String[] permission, final int permissionCode, String message, Boolean hasPositiveButton, String positiveText, Boolean hasNegativeButton, String negativeText ) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialog, int which ) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        askForPermission( permission, permissionCode );
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        AlertDialog.Builder alert = new AlertDialog.Builder( this );
        alert.setMessage( message );
        if ( hasPositiveButton ) alert.setPositiveButton( positiveText, dialogClickListener );
        if ( hasNegativeButton ) alert.setNegativeButton( negativeText, dialogClickListener );
        alert.show();
    }

    public void checkPermissions() {
        if ( ContextCompat.checkSelfPermission( this, READ_PHONE_STATE )
                + ContextCompat.checkSelfPermission( this, ACCESS_COARSE_LOCATION )
                != PackageManager.PERMISSION_GRANTED ) {
            Message( "One or more permissions are not granted.\nApplication may not work properly." );
            // registerButton.setText( "Grant App Permissions" );
            // registerButton.setEnabled( true );
            // registerButton.setOnClickListener( new View.OnClickListener() {
            //     @Override
            //     public void onClick( View view ) {
            //         Status("clicked");
            //         checkPermissionRationale();
            //     }
            // } );
            checkPermissionRationale();
        } else {
            Message( "Ready." );
            registerButton.setText( "Update Device Registration" );
            registerButton.setEnabled( false );
            registerButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view ) {
                    checkRegistration( true );
                }
            } );
        }
    }

    public void checkPermissionRationale() {
        if ( ActivityCompat.shouldShowRequestPermissionRationale( this, ACCESS_COARSE_LOCATION ) ) {
            Message( "If you checked \"Do not ask again\" for the permissions, you can either reinstall the application, or enable the permissions from your phone settings:\n\nSettings > Apps > "
                    + getApplicationName( this ) );
            Status("");
        } else {
            permissionDialog( new String[]{ACCESS_COARSE_LOCATION}, permissionCodeLocation, "Please grant permissions for the app to work properly. The \"location\" permission is only used for getting signal quality information, not for tracking your location.", true, "Grant Permissions", true, "Cancel" );
        }
        // if ( ActivityCompat.shouldShowRequestPermissionRationale( this, READ_PHONE_STATE ) ) {
        //     Message( "If you checked \"Do not ask again\" for the permissions, you can either reinstall the application, or enable the permissions from your phone settings:\n\nSettings > Apps > "
        //             + getApplicationName( this ) );
        // } else {
        //     permissionDialog( new String[]{READ_PHONE_STATE}, permissionCodePhone, "Please grant permissions for the app to work properly.", true, "Grant Permissions", true, "Cancel" );
        // }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Message( "Ready." );
        checkRegistration( false );
        // checkPermissions();
        LocalBroadcastManager.getInstance( this ).registerReceiver( serviceMessageReceiver, new IntentFilter( Config.PUSH_NOTIFICATION ) );
        LocalBroadcastManager.getInstance( this ).registerReceiver( serviceMessageReceiver, new IntentFilter( Config.REGISTRATION_COMPLETE ) );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String permissions[], @NonNull int[] grantResults ) {
        Status( "onRequestPermissionResult() firing" );
        switch (requestCode) {
            case Config.PERMISSIONS_CODE:
                if ( grantResults.length > 0 ) {
                    boolean permLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean permPhone = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if ( permLocation && permPhone ) {
                        Status( "Permissions obtained" );
                        if ( !pref.contains( "regToken" ) ) {
                            Status( "Still waiting for Firebase token" );
                            registerButton.setEnabled( false );
                        } else {
                            checkRegistration( true );
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance( this ).unregisterReceiver( serviceMessageReceiver );
        super.onPause();
    }
}
