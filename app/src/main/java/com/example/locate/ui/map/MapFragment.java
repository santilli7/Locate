package com.example.locate.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.locate.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class MapFragment extends Fragment {

    NavController navController;
    MapView fragment_map;
    GoogleMap map;
    LatLng position;
    private CameraUpdate cameraUpdate;
    private NavArgument latitude;
    private NavArgument longitude;
    private double lat;
    private double lng;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map, container, false);
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        if (getArguments() != null && getArguments().getParcelable("position") != null) {
            position = getArguments().getParcelable("position");
            lat = position.latitude;
            lng = position.longitude;
        } else {
            latitude = Navigation.findNavController(getActivity(), R.id.nav_host_fragment).getCurrentDestination().getArguments().get("position_latitude");
            longitude = Navigation.findNavController(getActivity(), R.id.nav_host_fragment).getCurrentDestination().getArguments().get("position_longitude");
            lat = (double) latitude.getDefaultValue();
            lng = (double) longitude.getDefaultValue();
        }
        fragment_map = (MapView) root.findViewById(R.id.fragment_map);
        fragment_map.onCreate(savedInstanceState);
        fragment_map.onResume();
        fragment_map.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 10);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(lat, lng));
                googleMap.addMarker(markerOptions);
                googleMap.animateCamera(cameraUpdate);
                map = googleMap;
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        fragment_map.onResume();
        super.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        fragment_map.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fragment_map.onDestroy();
    }

}