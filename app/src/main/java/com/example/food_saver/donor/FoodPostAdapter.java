package com.example.food_saver.donor;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.food_saver.databinding.ItemFoodPostBinding;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FoodPostAdapter extends RecyclerView.Adapter<FoodPostAdapter.FoodPostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(FoodPost post);
    }

    private final List<FoodPost> posts = new ArrayList<>();
    private final OnPostClickListener listener;

    public FoodPostAdapter(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<FoodPost> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodPostBinding binding = ItemFoodPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FoodPostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodPostViewHolder holder, int position) {
        holder.bind(posts.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class FoodPostViewHolder extends RecyclerView.ViewHolder {
        private final ItemFoodPostBinding binding;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        FoodPostViewHolder(ItemFoodPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FoodPost post, OnPostClickListener listener) {
            binding.tvTitle.setText(post.getFoodName());
            binding.tvQuantity.setText(post.getQuantity());
            binding.tvExpiry.setText("Expires: " + dateFormat.format(new Date(post.getExpiresAt())));
            binding.tvStatus.setText(capitalize(post.getStatus()));
            binding.tvEditHint.setVisibility(post.isEditable() ? android.view.View.VISIBLE : android.view.View.GONE);

            // imageUrl field holds a Base64 string, not a Storage URL —
            // decode it and hand Glide the raw bytes.
            byte[] imageBytes = ImageUtils.decodeBase64ToBytes(post.getImageUrl());
            Glide.with(binding.getRoot().getContext())
                    .load(imageBytes)
                    .centerCrop()
                    .into(binding.ivFoodPhoto);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onPostClick(post);
            });
        }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return "";
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}