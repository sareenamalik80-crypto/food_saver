package com.example.food_saver.donor;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.food_saver.R;
import com.example.food_saver.databinding.ItemMessageBinding;
import com.example.food_saver.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitList(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMessageBinding binding = ItemMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        boolean isMine = message.getSenderId() != null && message.getSenderId().equals(currentUserId);
        holder.bind(message, isMine);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageBinding binding;

        MessageViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message, boolean isMine) {
            binding.tvMessageText.setText(message.getText());

            // Align the whole row left or right depending on who sent it.
            binding.rootContainer.setGravity(isMine ? Gravity.END : Gravity.START);

            int color = isMine ? R.color.brown_primary : R.color.cream_light;
            int textColor = isMine ? R.color.white : R.color.text_dark;

            binding.messageCard.setCardBackgroundColor(
                    binding.getRoot().getContext().getColor(color));
            binding.tvMessageText.setTextColor(
                    binding.getRoot().getContext().getColor(textColor));
        }
    }
}
