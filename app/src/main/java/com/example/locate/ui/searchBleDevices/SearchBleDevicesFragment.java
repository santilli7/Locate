package com.example.locate.ui.searchBleDevices;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.example.locate.MainActivity;
import com.example.locate.R;
import com.google.android.gms.maps.model.LatLng;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class SearchBleDevicesFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private final static int REQUEST_ENABLE_BT = 1;
    private SearchBleDevicesViewModel searchBleDevicesViewModel;
    private BluetoothAdapter bluetoothAdapter;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private NavController navController;


    int MY_PERMISSIONS_REQUEST_LOCATION = 0;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private LatLng argument;
    private Bundle posBundle, deviceBundle;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        searchBleDevicesViewModel =
                ViewModelProviders.of(this).get(SearchBleDevicesViewModel.class);
        View root = inflater.inflate(R.layout.fragment_search_ble_devices, container, false);
        searchBleDevicesViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });

        argument = (LatLng) getArguments().get("position");
        posBundle = new Bundle();
        posBundle.putParcelable("position", argument);

        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        //navController = Navigation.findNavController(getContext());

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        return root;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_menu, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                return true;

            case R.id.menu_stop:
                scanLeDevice(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter((MainActivity) getActivity());
        setListAdapter(mLeDeviceListAdapter);
        getListView().setOnItemClickListener(this);
        scanLeDevice(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            getActivity().finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                    getActivity().invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        getActivity().invalidateOptionsMenu();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BluetoothDevice device = (BluetoothDevice) mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        System.out.println("NavController :" + Navigation.findNavController(view));
        posBundle.putString("name", device.getName());
        posBundle.putString("address", device.getAddress());
        Navigation.findNavController(getView()).navigate(R.id.action_nav_search_ble_devices_to_nav_control_ble_devices, posBundle);
        //final Intent intent = new Intent(this, DeviceControlActivity.class);
        //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            bluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        //startActivity(intent);
    }
}

