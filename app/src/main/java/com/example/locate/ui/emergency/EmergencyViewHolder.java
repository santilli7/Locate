package com.example.locate.ui.emergency;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.locate.R;
import com.google.android.material.card.MaterialCardView;

import androidx.recyclerview.widget.RecyclerView;

public class EmergencyViewHolder extends RecyclerView.ViewHolder {
    MaterialCardView cardView;
    ImageButton materialButton;
    TextView myTextView;
    ImageButton priorityButton;
    Button accept, decline;

    public EmergencyViewHolder(View itemView) {
        super(itemView);
        myTextView = (TextView) itemView.findViewById(R.id.textView2);
        cardView = (MaterialCardView) itemView.findViewById(R.id.emergency_card);
        materialButton = (ImageButton) itemView.findViewById(R.id.imageButton);
        priorityButton = (ImageButton) itemView.findViewById(R.id.priorityButton);
        accept = (Button) itemView.findViewById(R.id.accept_button);
        decline = (Button) itemView.findViewById(R.id.decline_button);
    }

    public TextView getMyTextView() {
        return myTextView;
    }

    public MaterialCardView getCardView() {
        return cardView;
    }

    public ImageButton getMaterialButton() {
        return materialButton;
    }

    public ImageButton getPriorityButton() {
        return priorityButton;
    }

    public Button getAccept() {
        return accept;
    }

    public Button getDecline() {
        return decline;
    }
}