package com.example.food_saver.ngo;
import com.example.food_saver.utils.ImageUtils;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // agar Glide use nahi kar rahi to is line + loadImage() ko simple ImageView.setImageURI se replace kar dein
import com.example.food_saver.R;
import com.example.food_saver.databinding.ItemFoodPostNgoBinding;
import com.example.food_saver.models.FoodPost;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FoodPostNgoAdapter extends RecyclerView.Adapter<FoodPostNgoAdapter.VH> {

    public interface OnPostClickListener {
        void onPostClick(FoodPost post);
    }

    private final List<FoodPost> posts = new ArrayList<>();
    private final String myNgoId;
    private final OnPostClickListener listener;

    // NGO's current position, used to show/sort by distance. Null until
    // location permission is granted and a fix is obtained — distance is
    // simply hidden until then.
    private Double myLat;
    private Double myLng;

    public FoodPostNgoAdapter(String myNgoId, OnPostClickListener listener) {
        this.myNgoId = myNgoId;
        this.listener = listener;
    }

    public void submitList(List<FoodPost> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        sortByDistanceIfKnown();
        notifyDataSetChanged();
    }

    /** Called once the NGO's GPS location is available — re-sorts nearest-first and shows distance on each card. */
    public void setMyLocation(double latitude, double longitude) {
        this.myLat = latitude;
        this.myLng = longitude;
        sortByDistanceIfKnown();
        notifyDataSetChanged();
    }

    private void sortByDistanceIfKnown() {
        if (myLat == null || myLng == null) return;
        posts.sort(Comparator.comparingDouble(this::distanceMetersTo));
    }

    private double distanceMetersTo(FoodPost post) {
        if (myLat == null || myLng == null) return Double.MAX_VALUE;
        if (post.getPickupLat() == 0.0 && post.getPickupLng() == 0.0) return Double.MAX_VALUE;
        float[] result = new float[1];
        Location.distanceBetween(myLat, myLng, post.getPickupLat(), post.getPickupLng(), result);
        return result[0];
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodPostNgoBinding binding = ItemFoodPostNgoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        FoodPost post = posts.get(position);
        holder.binding.tvFoodName.setText(post.getFoodName());
        holder.binding.tvQuantity.setText("Quantity: " + post.getQuantity());
        holder.binding.tvPostedBy.setText("Posted by: " + post.getDonorName());

        double distanceMeters = distanceMetersTo(post);
        if (myLat != null && myLng != null && distanceMeters != Double.MAX_VALUE) {
            String distanceText = distanceMeters < 1000
                    ? Math.round(distanceMeters) + " m away"
                    : String.format(Locale.getDefault(), "%.1f km away", distanceMeters / 1000);
            holder.binding.tvDistance.setText(distanceText);
            holder.binding.tvDistance.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvDistance.setVisibility(android.view.View.GONE);
        }

        byte[] imageBytes = ImageUtils.decodeBase64ToBytes(post.getImageUrl());
        Glide.with(holder.itemView.getContext())
                .load(imageBytes)
                .centerCrop()
                .into(holder.binding.ivFoodImage);

        // Status text is NGO-specific: same post ka status alag dikhta hai
        // depending on ke request kisne ki thi.
        String label;
        int color;
        switch (post.getStatus()) {
            case "available":
                label = "Available";
                color = R.color.indigo_purple;
                break;
            case "requested":
                if (myNgoId != null && myNgoId.equals(post.getClaimedByNgoId())) {
                    label = "Pending";
                    color = android.R.color.holo_orange_dark;
                } else {
                    label = "Claimed";
                    color = R.color.gray_secondary;
                }
                break;
            case "approved":
                label = "Approved";
                color = R.color.green_status;
                break;
            case "collected":
                label = "Food Collected";
                color = R.color.green_status;
                break;
            case "handedOver":
                label = "Handed Over";
                color = R.color.green_status;
                break;
            default:
                label = post.getStatus();
                color = R.color.gray_secondary;
        }
        holder.binding.tvStatusChip.setText(label);
        holder.binding.tvStatusChip.setTextColor(
                holder.itemView.getContext().getResources().getColor(color));

        holder.itemView.setOnClickListener(v -> listener.onPostClick(post));

    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemFoodPostNgoBinding binding;
        VH(ItemFoodPostNgoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
