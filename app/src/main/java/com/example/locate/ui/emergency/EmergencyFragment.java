package com.example.locate.ui.emergency;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.locate.Location;
import com.example.locate.R;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EmergencyFragment extends Fragment {

    public ArrayList<LatLng> positions;
    public ArrayList<Location> locations = new ArrayList<>();
    private EmergencyViewModel emergencyViewModel;
    private EmergencyRecyclerAdapter adapter = new EmergencyRecyclerAdapter();
    private TextView textView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        emergencyViewModel = ViewModelProviders.of(this).get(EmergencyViewModel.class);
        View root = inflater.inflate(R.layout.fragment_emergency, container, false);

        emergencyViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });

        if (getArguments() != null) {
            clearView();
            positions = getArguments().getParcelableArrayList("positions");
            for (int i = 0; i < positions.size(); i++) {
                Location location = new Location();
                location.setLatitude(positions.get(i).latitude);
                location.setLongitude(positions.get(i).longitude);
                Geocoder gc = new Geocoder(getContext());
                if (gc.isPresent()) {
                    ArrayList<Address> list = new ArrayList<>();

                    try {
                        list = (ArrayList<Address>) gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (list.size() > 0) {
                        if (list.get(0).getAddressLine(0) != null)
                            location.setAddress(String.valueOf(list.get(0).getLocality()));
                    } else {
                        location.setAddress("Unknown");
                    }
                }

                locations.add(location);
            }
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.emergency_recycler_view);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setHasFixedSize(true);
            adapter = new EmergencyRecyclerAdapter(getContext(), locations);
            recyclerView.setAdapter(adapter);
            System.out.println(locations);

            /*
            textView = root.findViewById(R.id.textView1);
            textView.setText(locations.get(0).address);

             */
        }


        return root;
    }

    public void clearView() {
        locations.clear();
        adapter.notifyDataSetChanged();
    }
}