package com.example.locate.ui.emergency;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.locate.Location;
import com.example.locate.R;
import com.example.locate.TinyDB;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EmergencyFragment extends Fragment {

    public ArrayList<LatLng> positions;
    public ArrayList<Location> locations = new ArrayList<>();
    private EmergencyRecyclerAdapter adapter = new EmergencyRecyclerAdapter();
    private TextView textView;
    private Emergency emergency;
    private ArrayList<Object> listEmergency = new ArrayList<>();
    private TinyDB tinyDB;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_emergency, container, false);
        tinyDB = new TinyDB(getContext());
        listEmergency = tinyDB.getListObject("list", Emergency.class);
        if (listEmergency == null || listEmergency.size() == 0) {

        } else {
            clearView();
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.emergency_recycler_view);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setHasFixedSize(true);
            adapter = new EmergencyRecyclerAdapter(getContext(), listEmergency);
            recyclerView.setAdapter(adapter);
            System.out.println(locations);
        }

        return root;
    }

    public void clearView() {
        locations.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }
}