package com.example.locate.ui.emergency;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.locate.Location;
import com.example.locate.R;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

class EmergencyRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //private List<TimelineModel> models;
    private List<Location> locations;
    private Context context;

    EmergencyRecyclerAdapter(Context context, List<Location> locations) {
        this.locations = locations;
        this.context = context;
    }

    public EmergencyRecyclerAdapter() {

    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.emergency_item, parent, false);
        return new EmergencyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        LatLng positions = new LatLng(locations.get(position).latitude, locations.get(position).longitude);
        final Bundle bundle = new Bundle();
        bundle.putParcelable("position", positions);
        EmergencyViewHolder emergencyViewHolder = (EmergencyViewHolder) holder;
        emergencyViewHolder.myTextView.setText(locations.get(position).address);
        emergencyViewHolder.myTextView.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
        emergencyViewHolder.materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.action_nav_emergency_to_nav_map, bundle);

            }
        });
    }


    @Override
    public int getItemCount() {
        return (null != locations ? locations.size() : 0);
    }

}
