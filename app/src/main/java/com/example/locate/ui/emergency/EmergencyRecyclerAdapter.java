package com.example.locate.ui.emergency;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.locate.R;
import com.example.locate.TinyDB;
import com.example.locate.ui.ControlBleDevices.BluetoothLeService;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

class EmergencyRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //private List<TimelineModel> models;
    private ArrayList<Object> listEmergency;
    private Context context;
    private String priority;
    private TinyDB tinyDB;
    private Emergency emergency;
    private boolean status;

    EmergencyRecyclerAdapter(Context context, ArrayList<Object> listEmergency) {
        this.context = context;
        this.listEmergency = listEmergency;
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        emergency = Emergency.class.cast(listEmergency.get(position));
        LatLng positions = new LatLng(emergency.getLatitude(), emergency.getLongitude());
        final Bundle bundle = new Bundle();
        bundle.putParcelable("position", positions);
        EmergencyViewHolder emergencyViewHolder = (EmergencyViewHolder) holder;
        emergencyViewHolder.myTextView.setText(emergency.getAddress());
        emergencyViewHolder.myTextView.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));

        if (emergency.getPriority().equals("h")) {
            emergencyViewHolder.priorityButton.setImageResource(R.drawable.round_gps_fixed_24);
            emergencyViewHolder.priorityButton.setColorFilter(context.getResources().getColor(R.color.highPriority));

        } else if (emergency.getPriority().equals("u")) {
            emergencyViewHolder.priorityButton.setImageResource(R.drawable.round_gps_fixed_24);
            emergencyViewHolder.priorityButton.setColorFilter(context.getResources().getColor(R.color.urgentPriority));

        } else if (emergency.getPriority().equals("d")) {
            emergencyViewHolder.priorityButton.setImageResource(R.drawable.round_gps_fixed_24);
            emergencyViewHolder.priorityButton.setColorFilter(context.getResources().getColor(R.color.discretePriority));

        }

        emergencyViewHolder.materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.action_nav_emergency_to_nav_map, bundle);

            }
        });

        emergencyViewHolder.accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BluetoothLeService.getInstance() != null && BluetoothLeService.getDevice() != null) {
                    if (BluetoothLeService.getInstance().sendAckToBLE()) {
                        Toast.makeText(context, context.getResources().getString(R.string.accept), Toast.LENGTH_SHORT).show();
                        listEmergency.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, listEmergency.size());
                        tinyDB = new TinyDB(context);
                        tinyDB.putListObject("list", listEmergency);
                    } else {
                        Toast.makeText(context, context.getResources().getString(R.string.not_send_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, context.getResources().getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
                }
            }
        });

        emergencyViewHolder.decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listEmergency.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, listEmergency.size());
                tinyDB = new TinyDB(context);
                tinyDB.putListObject("list", listEmergency);
            }
        });
    }


    @Override
    public int getItemCount() {
        return (null != listEmergency ? listEmergency.size() : 0);
    }

}
