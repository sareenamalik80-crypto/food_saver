package com.example.food_saver.admin.approvals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.food_saver.databinding.ItemPendingAccountBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApprovalsAdapter extends RecyclerView.Adapter<ApprovalsAdapter.ViewHolder> {

    public interface Listener {
        void onApprove(PendingAccount account);
        void onReject(PendingAccount account);
        void onRevokeToPending(PendingAccount account);
        void onItemClick(PendingAccount account);
    }

    private final List<PendingAccount> items = new ArrayList<>();
    private final Listener listener;

    public ApprovalsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<PendingAccount> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).getUid().equals(newItems.get(newPos).getUid());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                PendingAccount oldItem = items.get(oldPos);
                PendingAccount newItem = newItems.get(newPos);
                return Objects.equals(oldItem.getStatus(), newItem.getStatus())
                        && Objects.equals(oldItem.getName(), newItem.getName());
            }
        });
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingAccountBinding binding = ItemPendingAccountBinding.inflate(
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
        private final ItemPendingAccountBinding binding;

        ViewHolder(ItemPendingAccountBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PendingAccount account, Listener listener) {
            binding.tvName.setText(account.getName() != null ? account.getName() : "(no name)");
            binding.tvEmail.setText(account.getEmail() != null ? account.getEmail() : "");
            binding.tvRole.setText("ngo".equalsIgnoreCase(account.getRole()) ? "NGO" : "Donor");

            binding.getRoot().setOnClickListener(v -> listener.onItemClick(account));

            String status = account.getStatus() != null ? account.getStatus() : "pending";
            switch (status) {
                case "verified":
                    binding.btnReject.setVisibility(View.GONE);
                    binding.btnApprove.setVisibility(View.VISIBLE);
                    binding.btnApprove.setText("Revoke");
                    binding.btnApprove.setOnClickListener(v -> listener.onRevokeToPending(account));
                    break;
                case "rejected":
                    binding.btnReject.setVisibility(View.GONE);
                    binding.btnApprove.setVisibility(View.VISIBLE);
                    binding.btnApprove.setText("Approve");
                    binding.btnApprove.setOnClickListener(v -> listener.onApprove(account));
                    break;
                default: // pending
                    binding.btnReject.setVisibility(View.VISIBLE);
                    binding.btnReject.setText("Reject");
                    binding.btnReject.setOnClickListener(v -> listener.onReject(account));
                    binding.btnApprove.setVisibility(View.VISIBLE);
                    binding.btnApprove.setText("Approve");
                    binding.btnApprove.setOnClickListener(v -> listener.onApprove(account));
                    break;
            }
        }
    }
}