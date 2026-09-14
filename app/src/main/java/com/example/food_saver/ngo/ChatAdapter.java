package com.example.food_saver.ngo;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.food_saver.databinding.ItemChatReceivedBinding;
import com.example.food_saver.databinding.ItemChatSentBinding;
import com.example.food_saver.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String myUserId;

    public ChatAdapter(String myUserId) {
        this.myUserId = myUserId;
    }

    public void submitList(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return myUserId.equals(messages.get(position).getSenderId()) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentVH(ItemChatSentBinding.inflate(inflater, parent, false));
        } else {
            return new ReceivedVH(ItemChatReceivedBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof SentVH) {
            ((SentVH) holder).binding.tvMessage.setText(message.getText());
        } else if (holder instanceof ReceivedVH) {
            ((ReceivedVH) holder).binding.tvMessage.setText(message.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentVH extends RecyclerView.ViewHolder {
        final ItemChatSentBinding binding;
        SentVH(ItemChatSentBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        final ItemChatReceivedBinding binding;
        ReceivedVH(ItemChatReceivedBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}