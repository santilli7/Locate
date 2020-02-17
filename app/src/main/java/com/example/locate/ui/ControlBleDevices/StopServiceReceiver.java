package com.example.locate.ui.ControlBleDevices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent stopIntent = new Intent(context, BluetoothLeService.class);
        stopIntent.setAction(BluetoothLeService.StopForegroundAction);
        context.startService(stopIntent);
    }
}
