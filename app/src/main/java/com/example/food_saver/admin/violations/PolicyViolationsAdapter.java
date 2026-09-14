package com.example.food_saver.admin.violations;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.food_saver.databinding.ItemPolicyViolationBinding;

import java.util.ArrayList;
import java.util.List;

public class PolicyViolationsAdapter extends RecyclerView.Adapter<PolicyViolationsAdapter.ViewHolder> {

    public interface Listener {
        void onFlagDonor(PolicyViolation violation);
        void onDismiss(PolicyViolation violation);
    }

    private final List<PolicyViolation> items = new ArrayList<>();
    private final Listener listener;

    public PolicyViolationsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<PolicyViolation> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).getViolationId().equals(newItems.get(newPos).getViolationId());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).isReviewed() == newItems.get(newPos).isReviewed();
            }
        });
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPolicyViolationBinding binding = ItemPolicyViolationBinding.inflate(
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
        private final ItemPolicyViolationBinding binding;

        ViewHolder(ItemPolicyViolationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PolicyViolation violation, Listener listener) {
            binding.tvDonorName.setText(
                    violation.getDonorName() != null ? violation.getDonorName() : "(no name)");
            binding.tvFoodName.setText("Post: " + (violation.getFoodName() != null ? violation.getFoodName() : "\u2014"));
            binding.tvTimestamp.setText(
                    DateFormat.format("dd MMM, hh:mm a", violation.getTimestamp()));

            StringBuilder flags = new StringBuilder("\u26a0 ");
            List<String> tripped = new ArrayList<>();
            if (violation.isAiGenerated()) tripped.add("AI-generated");
            if (violation.isScreenshot()) tripped.add("Screenshot");
            if (!violation.isRealFood()) tripped.add("Not real food");
            flags.append(String.join(", ", tripped));
            binding.tvFlags.setText(flags.toString());

            binding.tvReason.setText(
                    violation.getAiReason() != null && !violation.getAiReason().isEmpty()
                            ? violation.getAiReason() : "No additional reason given.");

            binding.btnFlagDonor.setOnClickListener(v -> listener.onFlagDonor(violation));
            binding.btnDismiss.setOnClickListener(v -> listener.onDismiss(violation));
        }
    }
}
