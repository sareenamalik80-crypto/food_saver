package com.example.food_saver.admin.users;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.food_saver.R;
import com.example.food_saver.databinding.ItemUserManagementBinding;

import java.util.ArrayList;
import java.util.List;

public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.ViewHolder> {

    public interface Listener {
        void onToggleSuspend(AppUser user);
        void onDelete(AppUser user);
    }

    private final List<AppUser> items = new ArrayList<>();
    private final Listener listener;

    public UserManagementAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<AppUser> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).getUid().equals(newItems.get(newPos).getUid());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return String.valueOf(items.get(oldPos).getAccountStatus())
                        .equals(String.valueOf(newItems.get(newPos).getAccountStatus()));
            }
        });
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserManagementBinding binding = ItemUserManagementBinding.inflate(
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
        private final ItemUserManagementBinding binding;

        ViewHolder(ItemUserManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AppUser user, Listener listener) {
            String displayName = user.getName() != null ? user.getName() : "(no name)";
            if (user.isFlagged()) {
                binding.tvName.setText("\uD83D\uDEA9 " + displayName);
                binding.tvName.setTextColor(
                        binding.getRoot().getResources().getColor(R.color.error_red));
            } else {
                binding.tvName.setText(displayName);
                binding.tvName.setTextColor(
                        binding.getRoot().getResources().getColor(R.color.text_dark));
            }
            binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            binding.tvRole.setText("ngo".equalsIgnoreCase(user.getRole()) ? "NGO" : "Donor");
            binding.tvStatus.setText(user.isSuspended() ? "Suspended" : "Active");
            binding.tvStatus.setBackgroundResource(
                    user.isSuspended() ? R.drawable.bg_badge_error : R.drawable.bg_badge_success);
            binding.btnToggleSuspend.setText(user.isSuspended() ? "Reactivate" : "Suspend");
            binding.btnToggleSuspend.setOnClickListener(v -> listener.onToggleSuspend(user));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(user));
        }
    }
}
