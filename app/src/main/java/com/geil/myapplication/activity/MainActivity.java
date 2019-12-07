package com.geil.myapplication.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androbuzz.android.R;
import com.geil.myapplication.app.Config;
import com.geil.myapplication.app.SharedPrefs;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;

import timber.log.Timber;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;
import static com.geil.myapplication.app.Config.Action.BLUETOOTH_CONNECTED;
import static com.geil.myapplication.app.Config.Action.BLUETOOTH_DISCONNECTED;
import static com.geil.myapplication.app.Config.Action.MESSAGE;
import static com.geil.myapplication.app.Config.Action.PUSH_NOTIFICATION;
import static com.geil.myapplication.app.Config.Action.REGISTRATION_COMPLETE;
import static com.geil.myapplication.app.Config.Database.BRAND;
import static com.geil.myapplication.app.Config.Database.CARRIER;
import static com.geil.myapplication.app.Config.Database.MODEL;
import static com.geil.myapplication.app.Config.Database.NAME;
import static com.geil.myapplication.app.Config.Database.NUMBER;
import static com.geil.myapplication.app.Config.Database.REG_DATE;
import static com.geil.myapplication.app.Config.Database.SERIAL;
import static com.geil.myapplication.app.Config.Extra.COMMAND;
import static com.geil.myapplication.app.Config.Extra.DEVICE_NAME;
import static com.geil.myapplication.app.Config.Extra.REG_TOKEN;
import static com.geil.myapplication.app.Config.PERMISSIONS_CODE;


public class MainActivity extends AppCompatActivity {

    private final int permissionCodeLocation = 1, permissionCodePhone = 2;
    private TextView txtMessage, txtStatus;

    FirebaseDatabase database;
    DatabaseReference theDatabase;
    Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestPermissions();

        // Set up text views
        txtMessage = findViewById(R.id.txt_main_message);
        txtStatus = findViewById(R.id.txt_status);
        txtStatus.setMovementMethod(new ScrollingMovementMethod());

        // Initialize database
        database = FirebaseDatabase.getInstance();
        theDatabase = database.getReference();

        // Register button
        registerButton = findViewById(R.id.btnRegister);
        if (SharedPrefs.getInstance().getToken() == null) {
            registerButton.setEnabled(false);
        } else {
            Status(String.format("Stored token exists: %s", SharedPrefs.getInstance().getToken()));
            registerButton.setText(R.string.alreadyregistered);
        }
        registerButton.setOnClickListener(view -> {
                    registerDevice();
                }
        );
    }

    private void requestPermissions() {
        if (!(ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ACCESS_COARSE_LOCATION, READ_PHONE_STATE},
                    PERMISSIONS_CODE);
        }
    }

    public void retrieveBluetoothHeadset() {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        String deviceName = "No device";
        String deviceMAC = "00:00:00:00:00:00";
        String connection = "⚠️ DISCONNECTED ⚠️";
        if (bt != null && bt.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    deviceName = device.getName();
                    deviceMAC = device.getAddress();
                    break;
                }
                SharedPrefs.getInstance().saveBluetoothHeadsetMac(deviceMAC);
                SharedPrefs.getInstance().saveBluetoothHeadsetName(deviceName);
            }
        }
        if (bt != null && bt.isEnabled() && bt.getProfileConnectionState(1) == 2) {
            connection = "connected:";
        }
        Status(String.format("Bluetooth %s %s@%s", connection, deviceName, deviceMAC));
    }

    public void Status(String text) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
        String formattedDate = df.format(c.getTime());
        String status = formattedDate + " " + text;
        txtStatus.append(status.substring(0, Math.min(100, status.length())) + "\n");
        while (txtStatus.canScrollVertically(1)) {
            txtStatus.scrollBy(0, 1);
        }
    }

    public void Message(String text) {
        txtMessage.setText(text);
    }

    public void registerDevice() {
        Status("Registering...");
        try {
            // Set up device object
            DeviceModel theDevice = new DeviceModel(SharedPrefs.getInstance().getToken(), this);

            SharedPrefs.getInstance().saveLastMessageTime("2000-01-01 12:00:00 -05:00");
            SharedPrefs.getInstance().saveDeviceKey(theDevice.getDeviceKey());

            // Update registration date
            theDevice.updateRegDate();

            DatabaseReference dbDeviceEntry = theDatabase.child(theDevice.getDeviceKey());
            // The reason we don't just use setValue is to preserve the [messages] node
            dbDeviceEntry.child(BRAND).setValue(theDevice.getBrand());
            dbDeviceEntry.child(MODEL).setValue(theDevice.getModel());
            dbDeviceEntry.child(NAME).setValue(theDevice.getName());

            String number = theDevice.getNumber(this);
            if (number.equals(R.string.noPermission))
                Status("Registering without a phone number");

            dbDeviceEntry.child(NUMBER).setValue(number);
            dbDeviceEntry.child(REG_DATE).setValue(theDevice.getRegDate());
            dbDeviceEntry.child(Config.Database.REG_TOKEN).setValue(theDevice.getRegToken());
            dbDeviceEntry.child(SERIAL).setValue(theDevice.getSerial());

            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String carrier = tm.getSimOperatorName();
            if (carrier.equals("")) {
                carrier = tm.getNetworkOperatorName();
            }
            theDevice.setCarrier(carrier);
            dbDeviceEntry.child(CARRIER).setValue(theDevice.getCarrier());

            Status(String.format("Token: %s", SharedPrefs.getInstance().getToken()));
            Status(String.format("regDate updated: %s", theDevice.getRegDate()));
            Button registerButton = findViewById(R.id.btnRegister);
            registerButton.setText("Device Registered");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void checkRegistration(final boolean forceUpdate) {
        final String token = SharedPrefs.getInstance().getToken();
        final String deviceKey = SharedPrefs.getInstance().getDeviceKey();

        if (!TextUtils.isEmpty(token)) {
            Status("Checking registration status...");
            final DatabaseReference dbDeviceEntry = theDatabase.child(deviceKey);
            dbDeviceEntry.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot data) {
                    if (!data.exists() || !token.equals(data.child("regToken").getValue())) {
                        Status("Not registered or token mismatch");
                        registerDevice();
                    } else {
                        Status(String.format("Registered on %s", data.child("regDate").getValue()));
                        if (forceUpdate) {
                            Status("Forced registration update:");
                            registerDevice();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            Status("No registration token yet. Waiting for token...");
        }
    }

    // Handling received intents
    private BroadcastReceiver serviceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action.equals(REGISTRATION_COMPLETE)) {
                Toast toast = Toast.makeText(getApplicationContext(), "Received Firebase registration token", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                Status(String.format("Firebase regToken created: %s", intent.getStringExtra(REG_TOKEN)));

                Button registerButton = findViewById(R.id.btnRegister);
                registerButton.setEnabled(true);
                checkRegistration(false);

            } else if (action.equals(PUSH_NOTIFICATION)) {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        String.format("Received Command: %s", intent.getStringExtra(COMMAND)),
                        Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (action.equals(BLUETOOTH_CONNECTED)) {
                Status(String.format("Bluetooth %s is connected", intent.getStringExtra(DEVICE_NAME)));
            } else if (action.equals(BLUETOOTH_DISCONNECTED)) {
                Status(String.format("⚠️ %s HAS BEEN DISCONNECTED ⚠️", intent.getStringExtra(DEVICE_NAME)));
            } else if (action.equals(MESSAGE)) {
                Status(intent.getStringExtra(NotificationCompat.CATEGORY_MESSAGE));
            }
        }
    };

    public void askForPermission(String[] permission, int permissionCode) {
        ActivityCompat.requestPermissions(this, permission, permissionCode);
    }

    public static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public void permissionDialog(
            final String[] permission,
            final int permissionCode,
            String message,
            Boolean hasPositiveButton,
            String positiveText,
            Boolean hasNegativeButton,
            String negativeText) {

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    askForPermission(permission, permissionCode);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
            }

        };
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(message);
        if (hasPositiveButton) alert.setPositiveButton(positiveText, dialogClickListener);
        if (hasNegativeButton) alert.setNegativeButton(negativeText, dialogClickListener);
        alert.show();
    }

    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, READ_PHONE_STATE)
                + ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Message("One or more permissions are not granted.\nApplication may not work properly.");
        } else {
            Message("Ready.");
            registerButton.setText("Update Device Registration");
            registerButton.setEnabled(false);
            registerButton.setOnClickListener(v -> {
                        checkRegistration(true);
                    }
            );
        }
    }

    public void checkPermissionRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_COARSE_LOCATION)) {
            Message("If you checked \"Do not ask again\" for the permissions, you can either reinstall the application, or enable the permissions from your phone settings:\n\nSettings > Apps > "
                    + getApplicationName(this));
            Status("");
        } else {
            permissionDialog(new String[]{ACCESS_COARSE_LOCATION}, permissionCodeLocation, "Please grant permissions for the app to work properly. The \"location\" permission is only used for getting signal quality information, not for tracking your location.", true, "Grant Permissions", true, "Cancel");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Message("Ready.");
        checkRegistration(false);
        // checkPermissions();
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceMessageReceiver, new IntentFilter(PUSH_NOTIFICATION));
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceMessageReceiver, new IntentFilter(REGISTRATION_COMPLETE));
        retrieveBluetoothHeadset();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Status(String.format("onRequestPermissionResult() firing: %d", requestCode));
        switch (requestCode) {
            case PERMISSIONS_CODE:
                if (grantResults.length > 0) {
                    boolean permLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean permPhone = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (permLocation && permPhone) {
                        Status("Permissions obtained");
                        if (SharedPrefs.getInstance().getToken() == null) {
                            Status("Still waiting for Firebase token");
                            registerButton.setEnabled(false);
                        } else {
                            checkRegistration(true);
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceMessageReceiver);
        super.onPause();
    }
}
