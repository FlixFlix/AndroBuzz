package com.geil.myapplication.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

public class BluetoothA2DPRequester implements BluetoothProfile.ServiceListener {

    private Callback callback;

    public interface Callback {
        void onA2DPProxyReceived(BluetoothA2dp bluetoothA2dp);
    }

    public BluetoothA2DPRequester(Callback callback) {
        this.callback = callback;
    }

    public void request(Context c, BluetoothAdapter adapter) {
        adapter.getProfileProxy(c, this, 2);
    }

    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        if (callback != null) {
            callback.onA2DPProxyReceived((BluetoothA2dp) proxy);
        }
    }

    @Override
    public void onServiceDisconnected(int profile) {

    }
}
