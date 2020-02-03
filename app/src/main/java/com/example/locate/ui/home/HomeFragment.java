package com.example.locate.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.locate.MainActivity;
import com.example.locate.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class HomeFragment extends Fragment {
    MapView mapview;
    GoogleMap map;
    Bundle positionBundle;
    int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;
    NavController navController;
    TextView latTextView, lonTextView;
    protected double lat;
    protected double lng;
    Button button_view, button_send;
    public CameraUpdate cameraUpdate;
    MainActivity home;

    private HomeViewModel homeViewModel;
    private NavArgument latitude;
    private NavArgument longitude;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        button_send = (Button) root.findViewById(R.id.send_button);
        button_view = (Button) root.findViewById(R.id.view_button);
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        /*
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        getLastLocation();
        */
        //System.out.println("Home lat :"+getArguments().getDouble("position_longitude"));
        if (getArguments() == null) {
            latitude = navController.getCurrentDestination().getArguments().get("position_latitude");
            longitude = navController.getCurrentDestination().getArguments().get("position_longitude");
            lat = (double) latitude.getDefaultValue();
            lng = (double) longitude.getDefaultValue();
        } else {
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
        /*
        mapview.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {

                System.out.println("OnMapReady() ");
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lati,lng),8);
                googleMap.animateCamera(cameraUpdate);
                map = googleMap;
            }
        });

         */
        final TextView textView = root.findViewById(R.id.text_send);
        homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                //textView.setText(s);
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
    public void onDestroy() {
        super.onDestroy();
        mapview.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapview.onResume();
    }


}