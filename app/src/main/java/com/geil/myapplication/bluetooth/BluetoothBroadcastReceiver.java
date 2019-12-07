package com.geil.myapplication.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import timber.log.Timber;

import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;
import static android.bluetooth.BluetoothAdapter.EXTRA_STATE;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    private Callback callback;

    public interface Callback {
        void onBluetoothConnected();

        void onBluetoothError();
    }

    private BluetoothBroadcastReceiver(Callback callback) {
        this.callback = callback;
    }

    public static void register(Callback callback, Context c) {
        c.registerReceiver(new BluetoothBroadcastReceiver(callback), getFilter());
    }

    private static IntentFilter getFilter() {
        return new IntentFilter(ACTION_STATE_CHANGED);
    }

    public void onReceive(Context context, Intent intent) {
        if (!ACTION_STATE_CHANGED.equals(intent.getAction())) {
            Timber.v("Received irrelevant broadcast. Disregarding.");
        } else {
            int state = intent.getIntExtra(EXTRA_STATE, Integer.MIN_VALUE);
            if (state == Integer.MIN_VALUE) {
                safeUnregisterReceiver(context, this);
                fireOnBluetoothError();
            } else if (state == 2) {
                safeUnregisterReceiver(context, this);
                fireOnBluetoothConnected();
            }
        }
    }

    private static void safeUnregisterReceiver(Context c, BroadcastReceiver receiver) {
        try {
            c.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Timber.w("Tried to unregister BluetoothBroadcastReceiver that was not registered.");
        }
    }

    private void fireOnBluetoothConnected() {
        if (this.callback != null) {
            this.callback.onBluetoothConnected();
        }
    }

    private void fireOnBluetoothError() {
        if (this.callback != null) {
            this.callback.onBluetoothError();
        }
    }
}
