package com.example.food_saver.admin.monitoring;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.food_saver.R;
import com.example.food_saver.databinding.ItemDonationMonitoringBinding;

import java.util.ArrayList;
import java.util.List;

public class DonationMonitoringAdapter extends RecyclerView.Adapter<DonationMonitoringAdapter.ViewHolder> {

    public interface Listener {
        void onRemove(DonationItem item);
    }

    private final List<DonationItem> items = new ArrayList<>();
    private final Listener listener;

    public DonationMonitoringAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<DonationItem> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).getId().equals(newItems.get(newPos).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return String.valueOf(items.get(oldPos).getStatus())
                        .equals(String.valueOf(newItems.get(newPos).getStatus()));
            }
        });
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDonationMonitoringBinding binding = ItemDonationMonitoringBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDonationMonitoringBinding binding;

        ViewHolder(ItemDonationMonitoringBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DonationItem item, Listener listener) {
            binding.tvTitle.setText(item.getFoodName() != null ? item.getFoodName() : "(untitled donation)");
            binding.tvDonor.setText("By " + (item.getDonorName() != null ? item.getDonorName() : "Unknown donor"));
            binding.tvQuantity.setText(item.getQuantity() != null ? item.getQuantity() : "");

            String status = item.getStatus() != null ? item.getStatus() : "unknown";
            binding.tvStatus.setText(status);
            switch (status.toLowerCase()) {
                case "available":
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
                    break;
                case "expired":
                case "removed":
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_error);
                    break;
                default:
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_muted);
            }

            binding.btnRemove.setOnClickListener(v -> listener.onRemove(item));
        }
    }
}
