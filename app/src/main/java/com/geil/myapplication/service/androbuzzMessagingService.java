package com.geil.myapplication.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;

import com.androbuzz.android.R;
import com.geil.myapplication.activity.MessageModel;
import com.geil.myapplication.app.Config;
import com.geil.myapplication.app.SharedPrefs;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static com.geil.myapplication.app.Config.Action.MESSAGE;
import static com.geil.myapplication.app.Config.Action.PUSH_NOTIFICATION;
import static com.geil.myapplication.app.Config.Action.REGISTRATION_COMPLETE;
import static com.geil.myapplication.app.Config.Database.MESSAGES;
import static com.geil.myapplication.app.Config.Extra.COMMAND;
import static com.geil.myapplication.app.Config.Extra.REG_TOKEN;
import static com.geil.myapplication.app.Config.JSON.MESSAGE_DB_KEY;
import static com.geil.myapplication.app.Config.JSON.TIMESTAMP;
import static com.geil.myapplication.app.Config.MESSAGE_TIMEOUT;

public class androbuzzMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String regToken) {

        // Saving registration token to shared preferences
        SharedPrefs.getInstance().saveToken(regToken);

        // Notify Activity that registration has completed and token can be sent to server
        Intent registrationComplete = new Intent(REGISTRATION_COMPLETE);
        registrationComplete.putExtra(REG_TOKEN, regToken);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Log.e( t, "From: " + remoteMessage.getFrom() );

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            try {
                JSONObject json = new JSONObject(remoteMessage.getData().toString());
                JSONObject data = json.getJSONObject("data");

                // Send to DB, vibrate, update signal etc.
                handleDataMessage(remoteMessage.getMessageId(), data);

                // Notify UI of incoming message
                Intent pushNotification = new Intent(PUSH_NOTIFICATION);
                pushNotification.putExtra(COMMAND, data.getString(COMMAND));
                LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private void doVibrate(String command) {
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        int pattern = 0;
        final int zzzz = 400,
                ____ = 800,
                zz = 100,
                __ = 200,
                zzzzzzzzzzz = 4000;
        try {
            pattern = Integer.parseInt(command);
        } catch (NumberFormatException nfe) {
            System.out.println("Could not parse " + nfe);
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
        vibrator.vibrate(patterns[pattern], -1);
    }

    public int getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return Math.round(50.0f);
        }
        return Math.round(((float) level / (float) scale) * 100f);
    }

    private void handleDataMessage(String msgId, JSONObject data) {

        try {
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK), "t:wakeLog");
            wakeLock.acquire(300_000);

            String deviceKey = SharedPrefs.getInstance().getDeviceKey();

            String timestamp = data.getString(TIMESTAMP);
            String messageDbKey = data.getString(MESSAGE_DB_KEY);
            String command = data.getString(Config.JSON.COMMAND);

            Calendar time = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH);
            time.setTime(sdf.parse(timestamp));

            Calendar currentTime = Calendar.getInstance();
            time.add(Calendar.MILLISECOND, MESSAGE_TIMEOUT);

            if (time.before(currentTime)) {
                Timber.e("Expired message. Disregarded.");
            } else {
                String lastMessageTimeString = SharedPrefs.getInstance().getLastMessageTime();
                String lastMessageCommand = SharedPrefs.getInstance().getLastMessageCommand();
                Calendar lastMessageTime = Calendar.getInstance();
                lastMessageTime.setTime(sdf.parse(lastMessageTimeString));
                SharedPrefs.getInstance().saveLastMessageTime(sdf.format(currentTime.getTime()));
                SharedPrefs.getInstance().saveLastMessageCommand(command);

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference theDatabase = database.getReference();

                // Creating a new database entry for the received message
                MessageModel dbEntry = new MessageModel();
                dbEntry.setBatteryLevel(String.valueOf(getBatteryLevel()));
                dbEntry.setCommand(command);
                dbEntry.setTimeStamp(timestamp);
                dbEntry.setId(msgId);
                dbEntry.setTimeDelivered(dbEntry.getTimeDelivered());
                Map signal = getCurrentSignal(this);
                JSONObject signalJson = new JSONObject(signal);
                String signalString = String.valueOf(signalJson);
                dbEntry.setSignal(signalString);

                Calendar lastMessageTimeWithOffset = lastMessageTime;
                lastMessageTimeWithOffset.add(Calendar.MILLISECOND, 3000);
                if (currentTime.before(lastMessageTimeWithOffset) && command.matches("[1234]") && !lastMessageCommand.equals("0")) {
                    doVibrate(getString(R.string.vibrationPatternSkip));
                    dbEntry.setExtras("skipped");
                } else if (command.equals("9")) {
                    attemptBluetoothConnection();
                } else {
                    doVibrate(command);
                }
                theDatabase.child(deviceKey).child(MESSAGES).child(messageDbKey).setValue(dbEntry);
            }
            wakeLock.release();
            System.gc();
        } catch (JSONException e) {
            Timber.e("JSON exception: %s", e.getMessage());
        } catch (Exception e) {
            Timber.e("Exception: %s", e.getMessage());
        }
    }

    public void SvcSnackbar(String message) {
        Intent broadCastIntent = new Intent(MESSAGE);
        broadCastIntent.putExtra(NotificationCompat.CATEGORY_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadCastIntent);
    }

    public void attemptBluetoothConnection() throws NoSuchMethodException {
        SvcSnackbar("Attempting Bluetooth Connection");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String deviceName = SharedPrefs.getInstance().getBluetoothHeadsetName();
        bluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                androbuzzMessagingService.this.SvcSnackbar("BT Connected");
            }

            public void onServiceDisconnected(int i) {
                androbuzzMessagingService.this.SvcSnackbar("BT Disconnected");
            }
        }, 1);
        BluetoothDevice pairedDevice = null;
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        Timber.e(deviceName);
        if (devices != null) {
            Iterator it = devices.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                BluetoothDevice device = (BluetoothDevice) it.next();
                if (deviceName.equals(device.getName())) {
                    pairedDevice = device;
                    break;
                }
            }
        }
        if (pairedDevice.getBondState() == 12) {
            BluetoothHeadset.class.getDeclaredMethod("connect", new Class[]{BluetoothDevice.class});
        }
    }

    public String headsetName() {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        String deviceName = "Unknown device";
        String deviceMAC = "";
        if (!bt.isEnabled() || bt.getProfileConnectionState(1) != 2) {
            return "disconnected";
        }
        Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
        if (pairedDevices.size() <= 0) {
            return "error";
        }
        for (BluetoothDevice device : pairedDevices) {
            deviceName = device.getName();
            deviceMAC = device.getAddress();
            break;
        }
        SharedPrefs.getInstance().saveBluetoothHeadsetName(deviceName);
        SharedPrefs.getInstance().saveBluetoothHeadsetMac(deviceMAC);
        return deviceName;
    }

    public String getAudioRoute() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        String route = "bluetooth";
        if (!isBluetoothHeadsetConnected()) {
            return "disconnected";
        }
        if (audioManager.getMode() != 2) {
            return route;
        }
        if (audioManager.isMicrophoneMute() || !audioManager.isBluetoothScoOn()) {
            return "notgood";
        }
        return route;
    }

    public boolean isOnCall() {
        return ((AudioManager) getSystemService(AUDIO_SERVICE)).getMode() == 2;
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null || !bt.isEnabled() || bt.getProfileConnectionState(1) != 2) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("all")
    private Map getCurrentSignal(Context context) {
        Map signal = new HashMap();
        signal.put("bt", headsetName());
        signal.put("route", getAudioRoute());
        if (isOnCall()) {
            signal.put("oncall", "true");
        }
        if (ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == 0) {
            try {
                TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                for (CellInfo info : tm.getAllCellInfo()) {
                    if (info instanceof CellInfoGsm) {
                        CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        signal.put("type", "gsm");
                        signal.put("bars", String.valueOf(gsm.getLevel()));
                        signal.put("dbm", String.valueOf(gsm.getDbm()));
                        signal.put("asu", String.valueOf(gsm.getAsuLevel()));
                    } else if (info instanceof CellInfoCdma) {
                        CellSignalStrengthCdma cdma = ((CellInfoCdma) info).getCellSignalStrength();
                        String fullSignalInfo = String.valueOf(cdma);
                        String signalOutput = String.valueOf(cdma.getLevel());
                        signal.put("type", "cdma");
                        signal.put("bars", String.valueOf(cdma.getLevel()));
                        signal.put("dbm", String.valueOf(cdma.getDbm()));
                        signal.put("asu", String.valueOf(cdma.getAsuLevel()));
                    } else if (info instanceof CellInfoWcdma) {
                        CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) info).getCellSignalStrength();
                        String fullSignalInfo2 = String.valueOf(wcdma);
                        String signalOutput2 = String.valueOf(wcdma.getLevel());
                        signal.put("type", "wcdma");
                        signal.put("bars", String.valueOf(wcdma.getLevel()));
                        signal.put("dbm", String.valueOf(wcdma.getDbm()));
                        signal.put("asu", String.valueOf(wcdma.getAsuLevel()));
                    } else if (info instanceof CellInfoLte) {
                        CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                        signal.put("type", "lte");
                        signal.put("bars", String.valueOf(lte.getLevel()));
                        signal.put("dbm", String.valueOf(lte.getDbm()));
                        signal.put("asu", String.valueOf(lte.getAsuLevel()));
                        if (Build.VERSION.SDK_INT >= 26) {
                            signal.put("rsrq", Integer.valueOf(lte.getRsrq()));
                        }
                    } else {
                        signal.put("type", "?");
                    }
                }
            } catch (Exception e) {
                Timber.e("Unable to obtain cell signal information", e);
            }
        } else {
            String signalOutput3 = "0";
            String fullSignalInfo3 = "Permission ACCESS_COARSE_LOCATION not granted";
        }
        return signal;
    }
}
