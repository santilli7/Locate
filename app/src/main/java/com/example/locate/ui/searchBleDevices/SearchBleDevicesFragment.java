package com.example.locate.ui.searchBleDevices;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
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
import com.example.locate.ui.ControlBleDevices.BluetoothLeService;
import com.example.locate.ui.ControlBleDevices.SampleGattAttributes;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class SearchBleDevicesFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private final static int REQUEST_ENABLE_BT = 1;
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
    private ArrayList<ScanFilter> filters;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_search_ble_devices, container, false);


        argument = (LatLng) getArguments().get("position");
        posBundle = new Bundle();
        posBundle.putParcelable("position", argument);

        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().finish();
            }
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
            if (getActivity() != null) {
                getActivity().finish();
            }
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
            if (getActivity() != null) {
                getActivity().finish();
            }
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
            filters = new ArrayList<>();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
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
        } else {
            mScanning = false;
            bluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private ScanCallback mLeScanCallback =
            new ScanCallback() {


                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLeDeviceListAdapter.addDevice(result.getDevice());
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        });
                        super.onScanResult(callbackType, result);
                    }
                }
            };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BluetoothDevice device = (BluetoothDevice) mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        System.out.println("NavController :" + Navigation.findNavController(view));
        BluetoothLeService mble = (BluetoothLeService) getArguments().get("service");
        posBundle.putString("name", device.getName());
        posBundle.putString("address", device.getAddress());
        //final Intent intent = new Intent(this, DeviceControlActivity.class);
        //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
            mScanning = false;
        }
        Navigation.findNavController(getView()).navigate(R.id.action_nav_search_ble_devices_to_nav_control_ble_devices, posBundle);

        //startActivity(intent);
    }
}

