package com.example.locate.ui.home;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.locate.MainActivity;
import com.example.locate.R;
import com.example.locate.ui.ControlBleDevices.BluetoothLeService;
import com.example.locate.ui.ControlBleDevices.SampleGattAttributes;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.Navigation;

import static android.content.Context.BIND_AUTO_CREATE;
import static androidx.constraintlayout.widget.Constraints.TAG;

public class HomeFragment extends Fragment {
    MapView mapview;
    GoogleMap map;
    Bundle positionBundle;
    private final static int REQUEST_ENABLE_BT = 1;
    int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;
    NavController navController;
    TextView latTextView, lonTextView;
    protected double lat;
    protected double lng;
    MaterialButton btnStartStop;
    private BluetoothLeService mBluetoothLeService;

    Button button_view, button_send;
    public CameraUpdate cameraUpdate;
    MainActivity home;
    private static final long SCAN_PERIOD = 10000;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    private HomeViewModel homeViewModel;
    private NavArgument latitude;
    private NavArgument longitude;
    private BluetoothAdapter bluetoothAdapter;
    private boolean mBind = false;
    ArrayList<ScanFilter> filters;
    ArrayList<BluetoothDevice> devices;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    private ArrayList<LatLng> positions = new ArrayList<>();
    private Bundle posBundle;
    private Handler mHandler;
    private boolean mScanning;
    private boolean writestatus;
    private boolean mConnected;





    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                getActivity().finish();
            }
            mBind = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            System.out.println("Service Disconnected");
            mBind = false;
            mBluetoothLeService = null;
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
                if (positions.size() != 0) {
                    notifyEmergency(positions);
                }

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.EXTRA_DATA.equals(action)) {
                //System.out.println("Positions"+positions.size());
                if (intent.getParcelableExtra("positions") != null) {
                    positions.add((LatLng) intent.getParcelableExtra("positions"));
                    System.out.println("Positions" + positions.size());
                }
            }
        }
    };



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        button_send = (Button) root.findViewById(R.id.send_button);
        button_view = (Button) root.findViewById(R.id.view_button);
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        filters = new ArrayList<>();
        devices = new ArrayList<>();
        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        btnStartStop = (MaterialButton) root.findViewById(R.id.btn_start_stop_monitoring);

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // fire an intent to display a dialog asking the user to grant permission to enable it.
                if (!bluetoothAdapter.isEnabled()) {
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                } else {
                    if (BluetoothLeService.getState().equals(BLEState.STOPPED)) {
                        //if(!mBind) {
                        System.out.println("Stopped & not bind");
                        Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
                        getActivity().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                        btnStartStop.setText(getContext().getResources().getString(R.string.home_stop_monitoring));
                        btnStartStop.setBackgroundColor(Color.rgb(191, 54, 12));
                        //getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                        startScan(true);

                        //System.out.println("Devices" + positions.size());



                    } else if (BluetoothLeService.getState().equals(BLEState.RUNNING)) {
                        System.out.println("Service" + mServiceConnection);
                        //if(mBind) {
                        System.out.println("Running & bind");
                        getActivity().unbindService(mServiceConnection);
                        mBluetoothLeService = null;
                        mBind = false;
                        btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
                        btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                        startScan(false);
                        //getActivity().unregisterReceiver(mGattUpdateReceiver);
                        devices.clear();
                            /*}else if(!mBind){
                                System.out.println("Running & not bind");
                                //Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
                                //getActivity().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                                //getActivity().unbindService(mServiceConnection);
                                //mBluetoothLeService = null;
                                //mBind = false;
                                BluetoothLeService.setState(BLEState.STOPPED);
                                btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
                                btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                            }*/
                    }
                }
            }
        });

        if (BluetoothLeService.getState().equals(BLEState.STOPPED)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        } else if (BluetoothLeService.getState().equals(BLEState.RUNNING)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_stop_monitoring));
            btnStartStop.setBackgroundColor(Color.rgb(191, 54, 12));
        }

        if (getArguments() == null) {
            System.out.println("NavArgument ");
            latitude = navController.getCurrentDestination().getArguments().get("position_latitude");
            longitude = navController.getCurrentDestination().getArguments().get("position_longitude");
            lat = (double) latitude.getDefaultValue();
            lng = (double) longitude.getDefaultValue();
        } else {
            System.out.println("Argument ");
            lat = getArguments().getDouble("position_latitude");
            lng = getArguments().getDouble("position_longitude");
        }


        mapview = (MapView) root.findViewById(R.id.mapview);
        mapview.onCreate(savedInstanceState);
        mapview.onResume();
        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                positionBundle = new Bundle();
                positionBundle.putDouble("position_latitude", lat);
                positionBundle.putDouble("position_longitude", lng);
                navController.navigate(R.id.home_to_send_position, positionBundle);
            }
        });

        button_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                positionBundle = new Bundle();
                positionBundle.putDouble("position_latitude", lat);
                positionBundle.putDouble("position_longitude", lng);
                navController.navigate(R.id.home_to_map, positionBundle);
            }
        });
        mapview.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 8);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(lat, lng));
                googleMap.addMarker(markerOptions);
                googleMap.animateCamera(cameraUpdate);
                map = googleMap;
            }
        });

        final TextView textView = root.findViewById(R.id.text_send);
        homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                //textView.setText(s);
            }
        });
        return root;
    }


    private void startScan(boolean enable) {

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    System.out.println("Stop scan");
                    if (getActivity() != null) {

                        getActivity().invalidateOptionsMenu();
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SampleGattAttributes.HM_10_CONF)).build();
            filters.add(filter);
            ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
            bluetoothAdapter.getBluetoothLeScanner().startScan(filters, builderScanSettings.build(), mLeScanCallback);
            System.out.println("start scan");
        } else {
            mScanning = false;
            System.out.println("stop scan");
            bluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }


    }

    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    if (getActivity() != null) {
                        System.out.println("porcooooooooooo");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!devices.contains(result.getDevice())) {
                                    devices.add(result.getDevice());
                                    mBluetoothLeService.connect(result.getDevice().getAddress());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (characteristicTX != null) {
                                                characteristicTX.setValue("r");
                                                writestatus = mBluetoothLeService.writeCharacteristic(characteristicTX);
                                                mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                                            }
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (mConnected) {
                                                        mBluetoothLeService.disconnect();
                                                    }
                                                }
                                            }, 10000);
                                        }

                                    }, 10000);

                                }
                            }
                        });
                        super.onScanResult(callbackType, result);
                    }
                }
            };

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapview.onLowMemory();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        if (BluetoothLeService.getState().equals(BLEState.RUNNING) && mBind) {
            getActivity().unbindService(mServiceConnection);
            btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        }
        System.out.println("UnregisterReceiver");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapview != null) {
            mapview.onDestroy();
        }
        /*
         */
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void notifyEmergency(ArrayList<LatLng> positions) {
        System.out.println("Notification");

        posBundle = new Bundle();
        posBundle.putParcelableArrayList("positions", positions);
        CharSequence channelName = "My Channel";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel notificationChannel = new NotificationChannel("default", channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{1000, 2000});

        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        NavDeepLinkBuilder navDeepLinkBuilder = new NavDeepLinkBuilder(getContext());
        PendingIntent pendingIntent = navDeepLinkBuilder
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.nav_emergency)
                .setArguments(posBundle)
                .createPendingIntent();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "default");
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText("Ci sono " + positions.size() + " emergenze");
        builder.setSmallIcon(R.drawable.place2);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        notificationManager.notify(0, notification);
        System.out.println("Notification" + notification);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapview.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        System.out.println("RegisterReceiver");
        if (BluetoothLeService.getState().equals(BLEState.STOPPED)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));

        } else {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_stop_monitoring));
            btnStartStop.setBackgroundColor(Color.rgb(191, 54, 12));
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }
        //Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
        //getActivity().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        System.out.println("Result" + mServiceConnection);
        super.onActivityResult(requestCode, resultCode, data);
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