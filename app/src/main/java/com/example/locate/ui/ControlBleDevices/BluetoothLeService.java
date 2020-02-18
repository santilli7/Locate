/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.locate.ui.ControlBleDevices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.locate.MainActivity;
import com.example.locate.R;
import com.example.locate.TinyDB;
import com.example.locate.ui.emergency.Emergency;
import com.example.locate.ui.home.BLEState;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.navigation.NavDeepLinkBuilder;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public final static String MainAction = "com.truiton.foregroundservice.action.main",
            StartForegroundAction = "com.truiton.foregroundservice.action.startforeground",
            StopForegroundAction = "com.truiton.foregroundservice.action.stopforeground";
    private static final long SCAN_PERIOD = 10000;

    private Emergency emergency;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private static BLEState state;
    public ArrayList<LatLng> stringPos = new ArrayList();
    private static BluetoothLeService instance;
    private Bundle posBundle;
    private LatLng strToLatLng;
    private ArrayList<ScanFilter> filters;

    private boolean mScanning;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    private static BluetoothDevice bleDevice;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private Handler mHandler;
    private StopServiceReceiver mStopReceiver;
    private boolean writestatus;
    private ArrayList<LatLng> positions;


    public final static UUID UUID_HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);
    public final static UUID UUID_HM_TX =
            UUID.fromString(SampleGattAttributes.HM_TX);
    private boolean mConnected;


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(EXTRA_DATA, characteristic);
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if (!initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");

            }
            // Automatically connects to the device upon successful start-up initialization.
            System.out.println("Connect");

            // connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            System.out.println("Disconnected");
            //mBluetoothLeService = null;
        }


    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                System.out.println("Intent connected");
                mConnected = true;

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                System.out.println("Intent disconnected");
                mConnected = false;
                    /*
                    if (positions.size() != 0) {
                        notifyEmergency(positions);
                    }
                    */

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(getSupportedGattServices());
            } else if (BluetoothLeService.EXTRA_DATA.equals(action)) {
                //System.out.println(intent.getParcelableExtra("positions") + " " + intent.getStringExtra("priority"));
                if (intent.getStringExtra("priority") != null) {
                    LatLng latLng = intent.getParcelableExtra("positions");
                    Emergency emergency = new Emergency();
                    emergency.setLatitude(latLng.latitude);
                    emergency.setLongitude(latLng.longitude);
                    emergency.setPriority(intent.getStringExtra("priority"));

                    System.out.println(emergency.getLatitude() + " " + emergency.getLongitude() + " " + emergency.getPriority());
                    notifyEmergency(emergency);
                }

                /*
                    if (intent.getParcelableExtra("positions") != null) {
                        positions.add((LatLng) intent.getParcelableExtra("positions"));
                        System.out.println("Positions" + positions.size());
                    }

                    */
            }
        }
    };

    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    bleDevice = result.getDevice();
                    super.onScanResult(callbackType, result);
                }
            };


    @Override
    public void onCreate() {
        super.onCreate();
        if (null == instance)
            instance = this;
        TinyDB tinyDB = new TinyDB(this);
        mHandler = new Handler();
        mStopReceiver = new StopServiceReceiver();
        state = BLEState.STOPPED;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null && intent.getAction().equals(StopForegroundAction)) {
            state = BLEState.STOPPED;
            if (null != mGattUpdateReceiver) unregisterReceiver(mGattUpdateReceiver);
            if (null != mStopReceiver) unregisterReceiver(mStopReceiver);
            System.out.println("Stop receiver" + state);
            if (bleDevice != null) {
                bleDevice = null;
            }
            Intent stopUIIntent = new Intent("stop_home");
            sendBroadcast(stopUIIntent);
            disconnect();
            stopService(intent);
            stopForeground(true);
            stopSelf();


        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(StartForegroundAction) && state.equals(BLEState.STOPPED)) {
            state = BLEState.RUNNING;
            initialize();
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            registerReceiver(mStopReceiver, new IntentFilter("stop_receiver"));
            filters = new ArrayList<>();
            System.out.println("On Start Command");

            startScan(true);

        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (state.equals(BLEState.RUNNING)) {
            stopForeground(true);
            stopSelf();
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        Log.i(TAG, "data" + characteristic.getValue());

        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            //intent.putExtra(EXTRA_DATA, String.format("%s", new String(data)));
            String s = new String(data);

            if (s.length() == 20) {
                strToLatLng = manageData(s);

            } else {
                System.out.println(s);
                intent.putExtra("priority", s);
            }

            intent.putExtra("positions", strToLatLng);
            //stringPos.add(strToLatLng);

        }
        System.out.println("Send intent");
        sendBroadcast(intent);
    }


    private LatLng manageData(String data) {
        System.out.println("data " + data.length());
        if (data.length() == 20) {
            double lat, lng;
            lat = Double.valueOf(data.substring(0, 10));
            lng = Double.valueOf(data.substring(10, 20));
            //emergency.setLatitude(lat);
            //emergency.setLongitude(lng);
            System.out.println(lat + " " + lng);
            return new LatLng(lat, lng);
        } else {
            String priority;
            emergency.setPriority(data);
            System.out.println("Priority " + emergency);

        }
        return new LatLng(0, 0);
    }


    private void startScan(boolean enable) {

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    System.out.println("Stop scan" + mBluetoothAdapter + bleDevice);
                    if (bleDevice != null) {
                        mConnected = connect(bleDevice.getAddress());
                        if (mConnected) {
                            System.out.println(bleDevice);
                            Notification notification = createForegroundService();
                            startForeground(101, notification);
                        }
                        System.out.println(mConnected);
                    }

                }
            }, SCAN_PERIOD);

            mScanning = true;
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SampleGattAttributes.HM_10_CONF)).build();
            filters.add(filter);
            ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
            builderScanSettings.setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
            mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, builderScanSettings.build(), mLeScanCallback);
            System.out.println("start scan");
        } else {
            mScanning = false;
            System.out.println("stop scan");
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }
    }
    
    public static BLEState getState() {
        return null != state ? state : BLEState.STOPPED;
    }

    public static void setState(BLEState blestate) {
        state = blestate;
    }

    public static BluetoothLeService getInstance() {
        return instance;
    }

    public static BluetoothDevice getDevice() {
        return bleDevice;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {

        System.out.println("Bind");
        instance = this;
        state = BLEState.RUNNING;
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        System.out.println("Unbind");
        state = BLEState.STOPPED;
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        System.out.println("connect");
        if (mBluetoothAdapter == null || address == null) {
            System.out.println("BluetoothAdapter not initialized or unspecified address." + mBluetoothAdapter);
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                System.out.println("Gatt problem");
                final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
                mBluetoothDeviceAddress = address;
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));


            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_TX);
            //characteristicTx1 = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }

    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean sendDataToBLE(LatLng position, String priority) {
        Log.d(TAG, "Sending result=" + String.valueOf(position.latitude));
        System.out.println(String.valueOf(position.latitude).getBytes() + "Latitude");
        String strPriority = setPriority(priority);
        String latstr = String.valueOf(position.latitude);
        String lngstr = String.valueOf(position.longitude);
        String positionstr = latstr + lngstr + strPriority;
        final byte[] tx = positionstr.getBytes();
        if (mConnected) {
            if (characteristicTX != null) {
                characteristicTX.setValue(positionstr);
                writestatus = writeCharacteristic(characteristicTX);
                setCharacteristicNotification(characteristicRX, true);
            }
            if (writestatus) {
                return true;
            }
            return false;

        }
        return false;
    }

    private String setPriority(String priority) {
        if (priority.equals("Primary")) {
            return "h";
        } else if (priority.equals("Urgent")) {
            return "u";
        } else if (priority.equals("Discrete")) {
            return "d";
        }
        return "";
    }

    /**
     * Write to a given char
     *
     * @param characteristic The characteristic to write to
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        boolean status = mBluetoothGatt.writeCharacteristic(characteristic);
        return status;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HM_RX_TX.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void notifyEmergency(Emergency emergency) {
        System.out.println("Notification");

        posBundle = new Bundle();
        posBundle.putParcelable("emergency", emergency);
        CharSequence channelName = "My Channel";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel notificationChannel = new NotificationChannel("default", channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{1000, 2000});

        NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        NavDeepLinkBuilder navDeepLinkBuilder = new NavDeepLinkBuilder(this);
        PendingIntent pendingIntent = navDeepLinkBuilder
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.nav_emergency)
                .setArguments(posBundle)
                .createPendingIntent();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText("New emergency");
        builder.setSmallIcon(R.drawable.place2);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        notificationManager.notify(0, notification);
        System.out.println("Notification" + notification);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createForegroundService() {
        CharSequence channelName = "My Channel";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
        notificationIntent.setAction(MainAction);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);

        Intent stopIntent = new Intent("stop_receiver");
        PendingIntent pStopIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.);
        NotificationChannel notificationChannel = new NotificationChannel("default", channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{1000, 2000});
        NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        return new NotificationCompat.Builder(this, "default")
                .setContentTitle("Locate")
                .setTicker("Locate")
                .setContentText("Start service")
                .setSmallIcon(R.drawable.place2)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Stop", pStopIntent)
                .build();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
        return intentFilter;
    }
}
