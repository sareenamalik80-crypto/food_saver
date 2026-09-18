package com.example.food_saver.donor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.food_saver.R;
import com.example.food_saver.databinding.ItemRequestBinding;
import com.example.food_saver.models.Request;
import com.example.food_saver.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(Request request);
        void onReject(Request request);
        void onOpenChat(Request request);
        void onMarkHandedOver(Request request);
    }

    private final List<Request> requests = new ArrayList<>();
    private final OnRequestActionListener listener;

    public RequestAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Request> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRequestBinding binding = ItemRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(requests.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final ItemRequestBinding binding;

        RequestViewHolder(ItemRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Request request, OnRequestActionListener listener) {
            // foodName/foodImageBase64 are populated by RequestRepository
            // after fetching the linked foodPosts document — the "requests"
            // collection itself doesn't store these.
            binding.tvFoodTitle.setText(request.getFoodName() != null ? request.getFoodName() : "Food item");
            binding.tvNgoName.setText("From: " + request.getNgoName());
            binding.tvStatus.setText(capitalize(request.getStatus()));

            byte[] imageBytes = ImageUtils.decodeBase64ToBytes(request.getFoodImageBase64());
            Glide.with(binding.getRoot().getContext())
                    .load(imageBytes)
                    .centerCrop()
                    .into(binding.ivFoodPhoto);

            boolean isPending = Request.STATUS_REQUESTED.equals(request.getStatus());
            boolean isDone = Request.STATUS_HANDED_OVER.equals(request.getStatus());
            boolean isApproved = Request.STATUS_APPROVED.equals(request.getStatus())
                    || Request.STATUS_COLLECTED.equals(request.getStatus())
                    || isDone;

            binding.actionsPending.setVisibility(isPending ? View.VISIBLE : View.GONE);
            binding.btnChat.setVisibility(isApproved ? View.VISIBLE : View.GONE);
            binding.deliveryInfoLayout.setVisibility(
                    isApproved && request.hasDeliveryDetails() ? View.VISIBLE : View.GONE);

            if (isApproved && request.hasDeliveryDetails()) {
                binding.tvRiderName.setText("Rider: " + request.getRiderName());
                binding.tvVehicleNumber.setText("Vehicle: " + request.getVehicleNumber());
                binding.tvRiderPhone.setText("Phone: " + request.getRiderPhone());
                binding.tvArrivalTime.setText("Arrival: " + request.getArrivalTime());
            }

            // Dual-confirmation handover: show the button only while approved
            // AND the donor hasn't confirmed yet; show the note once the
            // donor has confirmed (whether still waiting, or fully done).
            boolean canConfirm = isApproved && !isDone && !request.isDonorConfirmedHandover();
            boolean showNote = isApproved && (request.isDonorConfirmedHandover() || isDone);

            binding.btnMarkHandedOver.setVisibility(canConfirm ? View.VISIBLE : View.GONE);
            binding.tvHandoverNote.setVisibility(showNote ? View.VISIBLE : View.GONE);

            if (showNote) {
                if (isDone) {
                    binding.tvHandoverNote.setText("✅ Handover complete");
                    binding.tvHandoverNote.setTextColor(
                            binding.getRoot().getContext().getColor(R.color.success_green));
                } else {
                    binding.tvHandoverNote.setText("You confirmed — waiting for the NGO to confirm receipt");
                    binding.tvHandoverNote.setTextColor(
                            binding.getRoot().getContext().getColor(R.color.text_muted));
                }
            }

            binding.btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(request);
            });
            binding.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(request);
            });
            binding.btnChat.setOnClickListener(v -> {
                if (listener != null) listener.onOpenChat(request);
            });
            binding.btnMarkHandedOver.setOnClickListener(v -> {
                if (listener != null) listener.onMarkHandedOver(request);
            });
        }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return "";
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}