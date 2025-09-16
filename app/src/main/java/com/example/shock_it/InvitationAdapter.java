package com.example.shock_it;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.HashMap;
import java.util.List;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder> {

    public List<HashMap<String, String>> invitationList;
    private OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAccept(HashMap<String, String> invitationData);
        void onDecline(HashMap<String, String> invitationData);
    }

    public InvitationAdapter(List<HashMap<String, String>> invitationList, OnInvitationActionListener listener) {
        this.invitationList = invitationList;
        this.listener = listener;
    }

    public void setInvitationList(List<HashMap<String, String>> newInvitations) {
        this.invitationList.clear();
        this.invitationList.addAll(newInvitations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.market_item_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        HashMap<String, String> invitation = invitationList.get(position);

        // Retrieve data using keys
        String marketId = invitation.get("marketId");
        String location = invitation.get("location");
        String date = invitation.get("date");

        String marketName = location;

        holder.marketNameTextView.setText(marketName);
        holder.marketLocationTextView.setText(location);
        holder.marketDateTextView.setText(date);

        holder.acceptButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(invitation); // Pass the HashMap directly
            }
        });

        holder.declineButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecline(invitation); // Pass the HashMap directly
            }
        });
    }

    @Override
    public int getItemCount() {
        return invitationList.size();
    }

    public static class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView marketNameTextView;
        TextView marketLocationTextView;
        TextView marketDateTextView;
        Button acceptButton;
        Button declineButton;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            marketNameTextView = itemView.findViewById(R.id.marketNameTextView);
            marketLocationTextView = itemView.findViewById(R.id.marketLocationTextView);
            marketDateTextView = itemView.findViewById(R.id.marketDateTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }
    }
}