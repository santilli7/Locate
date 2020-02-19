package com.example.locate.ui.home;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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
    private HomeViewModel homeViewModel;

    private final BroadcastReceiver connectService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //System.out.println("Stop Receiver");
            btnStartStop.setText(getContext().getResources().getString(R.string.home_stop_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.discretePriority));

        }
    };
    private final BroadcastReceiver stopService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println("Stop Receiver");
            btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));

        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        button_send = (Button) root.findViewById(R.id.send_button);
        button_view = (Button) root.findViewById(R.id.view_button);
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        filters = new ArrayList<>();
        devices = new ArrayList<>();
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

                        System.out.println("Stopped & not bind");
                        Intent startIntent = new Intent(getContext(), BluetoothLeService.class);
                        startIntent.setAction(BluetoothLeService.StartForegroundAction);
                        getActivity().startService(startIntent);
                        btnStartStop.setText(getContext().getResources().getString(R.string.connecting));
                        btnStartStop.setBackgroundColor(Color.rgb(191, 54, 12));


                    } else if (BluetoothLeService.getState().equals(BLEState.RUNNING)) {
                        System.out.println("Service" + mServiceConnection);
                        System.out.println("Running & bind");
                        Intent stopIntent = new Intent(getContext(), BluetoothLeService.class);
                        stopIntent.setAction(BluetoothLeService.StopForegroundAction);
                        getActivity().startService(stopIntent);
                        mBluetoothLeService = null;

                        btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
                        btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                    }
                }
            }
        });

        if (BluetoothLeService.getState().equals(BLEState.STOPPED)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        } else if (BluetoothLeService.getState().equals(BLEState.RUNNING)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_stop_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.discretePriority));
        } else if (BluetoothLeService.getState().equals(BLEState.CONNECTING)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.connecting));
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
                btnStartStop.setText(s);

            }
        });
        homeViewModel.getColor().observe(this, new Observer<Color>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onChanged(Color color) {
                btnStartStop.setBackgroundColor(color.getComponentCount());
            }
        });
        return root;
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapview.onLowMemory();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(connectService);
        getActivity().unregisterReceiver(stopService);
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




    @Override
    public void onResume() {
        super.onResume();
        mapview.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        getActivity().registerReceiver(connectService, new IntentFilter("connected"));
        getActivity().registerReceiver(stopService, new IntentFilter("stop_home"));
        if (BluetoothLeService.getState().equals(BLEState.STOPPED)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_start_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        } else if (BluetoothLeService.getState().equals(BLEState.RUNNING)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.home_stop_monitoring));
            btnStartStop.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.discretePriority));
        } else if (BluetoothLeService.getState().equals(BLEState.CONNECTING)) {
            btnStartStop.setText(getContext().getResources().getString(R.string.connecting));
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