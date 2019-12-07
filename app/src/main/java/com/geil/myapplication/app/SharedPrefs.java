package com.geil.myapplication.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {

    private static final String SHARED_PREF = "ah_firebase";
    private static final String REG_TOKEN = "regToken";
    private static final String LAST_MESSAGE_TIME = "lastMessageTime";
    private static final String LAST_MESSAGE_COMMAND = "lastMessageCommand";
    private static final String DEVICE_KEY = "deviceKey";
    private static final String PHONE_NUMBER = "number";
    private static final String BLUETOOTH_HEADSET_MAC = "bluetooth_headset_MAC";
    private static final String BLUETOOTH_HEADSET_NAME = "bluetooth_headset_name";

    private static final SharedPrefs instance = new SharedPrefs();
    private SharedPreferences prefs;

    public static SharedPrefs getInstance(){
        return instance;
    }

    public void init(Context context){
        prefs = context.getSharedPreferences(SHARED_PREF, 0);
    }

    public String getToken(){
        return prefs.getString(REG_TOKEN, null);
    }

    public void saveToken(String token){
        prefs.edit().putString(REG_TOKEN, token).apply();
    }

    public void saveLastMessageTime(String time){
        prefs.edit().putString(LAST_MESSAGE_TIME, time).apply();
    }

    public String getLastMessageTime(){
        return prefs.getString(LAST_MESSAGE_TIME, "2000-01-01 12:00:00 -05:00");
    }

    public String getLastMessageCommand() {
        return prefs.getString(LAST_MESSAGE_COMMAND, "0");
    }

    public void saveLastMessageCommand(String command){
        prefs.edit().putString(LAST_MESSAGE_COMMAND, command).apply();
    }

    public void saveDeviceKey(String deviceKey){
        prefs.edit().putString(DEVICE_KEY, deviceKey).apply();
    }

    public String getDeviceKey(){
        return prefs.getString(DEVICE_KEY, "");
    }

    public String getPhoneNumber(){
        return prefs.getString(PHONE_NUMBER, "No number permission");
    }

    public void savePhoneNumber(String number){
        prefs.edit().putString(PHONE_NUMBER, number).apply();
    }

    public String getBluetoothHeadsetMac(){
        return prefs.getString(BLUETOOTH_HEADSET_MAC, null);
    }

    public String getBluetoothHeadsetName(){
        return prefs.getString(BLUETOOTH_HEADSET_NAME, "Bluetooth");
    }

    public void saveBluetoothHeadsetMac(String mac){
        prefs.edit().putString(BLUETOOTH_HEADSET_MAC, mac).apply();
    }

    public void saveBluetoothHeadsetName(String name){
        prefs.edit().putString(BLUETOOTH_HEADSET_NAME, name).apply();
    }
}
