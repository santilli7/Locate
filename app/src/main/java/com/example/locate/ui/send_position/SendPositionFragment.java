package com.example.locate.ui.send_position;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import com.example.locate.R;
import com.example.locate.ui.ControlBleDevices.BluetoothLeService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavArgument;
import androidx.navigation.Navigation;

public class SendPositionFragment extends Fragment implements OnMapReadyCallback {

    MapView mapview;
    Bundle posBundle;
    NavArgument lat;
    NavArgument lng;
    GoogleMap map;
    MaterialButton btnInvia;
    TextInputEditText latitude, longitude;
    Spinner priority;
    LatLng position;
    private BluetoothLeService mBluetooth;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_send_position, container, false);

        latitude = (TextInputEditText) v.findViewById(R.id.et_latitude);
        longitude = (TextInputEditText) v.findViewById(R.id.et_longitude);
        priority = (Spinner) v.findViewById(R.id.spinnerEmergency);
        lat = Navigation.findNavController(getActivity(), R.id.nav_host_fragment).getCurrentDestination().getArguments().get("position_latitude");
        lng = Navigation.findNavController(getActivity(), R.id.nav_host_fragment).getCurrentDestination().getArguments().get("position_longitude");

        String str_latitude = String.valueOf(lat.getDefaultValue());
        String str_longitude = String.valueOf(lng.getDefaultValue());
        position = new LatLng((double) lat.getDefaultValue(), (double) lng.getDefaultValue());
        latitude.setText(str_latitude);
        longitude.setText(str_longitude);
        btnInvia = (MaterialButton) v.findViewById(R.id.btnInvia);


        posBundle = new Bundle();
        posBundle.putParcelable("position", position);

        btnInvia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BluetoothLeService.getInstance() != null && BluetoothLeService.getDevice() != null) {
                    if (BluetoothLeService.getInstance().sendDataToBLE(position, (String) priority.getSelectedItem())) {
                        Navigation.findNavController(getView()).navigate(R.id.action_nav_send_position_to_nav_home, posBundle);
                    }
                } else {
                    System.out.println("Not present");
                }
                //
            }
        });

        /*
        mapview = (MapView) v.findViewById(R.id.mapview);
        mapview.onCreate(savedInstanceState);
        mapview.onResume();
        mapview.getMapAsync(this);
        */



        return v;
    }

    @Override
    public void onResume() {
        //mapview.onResume();
        super.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //mapview.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mapview.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
    }
}