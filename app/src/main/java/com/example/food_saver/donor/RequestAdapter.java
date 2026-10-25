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
import com.example.food_saver.utils.TimeUtils;

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
            binding.tvStatus.setText(statusLabel(request.getStatus()));

            byte[] imageBytes = ImageUtils.decodeBase64ToBytes(request.getFoodImageBase64());
            Glide.with(binding.getRoot().getContext())
                    .load(imageBytes)
                    .centerCrop()
                    .into(binding.ivFoodPhoto);

            boolean isExpired = "expired".equals(request.getStatus())
                    || (request.isFoodExpired()
                    && (Request.STATUS_REQUESTED.equals(request.getStatus())
                    || Request.STATUS_APPROVED.equals(request.getStatus())));
            if (isExpired) {
                binding.tvStatus.setText("Expired");
            }
            boolean isPending = !isExpired && Request.STATUS_REQUESTED.equals(request.getStatus());
            boolean isDone = Request.STATUS_HANDED_OVER.equals(request.getStatus());
            boolean isApproved = !isExpired && (Request.STATUS_APPROVED.equals(request.getStatus())
                    || Request.STATUS_COLLECTED.equals(request.getStatus())
                    || isDone);

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

            boolean donorConfirmed = request.isDonorConfirmedHandover();
            boolean hasRider = request.hasDeliveryDetails();
            boolean arrivalReached = TimeUtils.hasArrivalTimePassed(request.getArrivalAt());
            boolean canConfirm = isApproved && !isDone && !donorConfirmed;
            boolean readyToConfirm = canConfirm && hasRider && arrivalReached;

            binding.btnMarkHandedOver.setVisibility(canConfirm ? View.VISIBLE : View.GONE);
            binding.btnMarkHandedOver.setAlpha(readyToConfirm ? 1f : 0.5f);

            String note = null;
            int noteColor = R.color.text_muted;
            if (isExpired) {
                note = "This donation expired before it was picked up";
                noteColor = R.color.error_red;
            } else if (isDone) {
                note = "✅ Handover complete";
                noteColor = R.color.success_green;
            } else if (isApproved && donorConfirmed) {
                note = "You handed over the food — waiting for the NGO to mark it as received";
            } else if (canConfirm && !hasRider) {
                note = "Waiting for the NGO to add the rider details";
            } else if (canConfirm && !arrivalReached) {
                note = "You can press Handover after the rider's arrival time (" + request.getArrivalTime() + ")";
            }
            binding.tvHandoverNote.setVisibility(note != null ? View.VISIBLE : View.GONE);
            if (note != null) {
                binding.tvHandoverNote.setText(note);
                binding.tvHandoverNote.setTextColor(binding.getRoot().getContext().getColor(noteColor));
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

        private String statusLabel(String status) {
            if (status == null || status.isEmpty()) return "";
            switch (status) {
                case Request.STATUS_REQUESTED: return "Requested";
                case Request.STATUS_APPROVED: return "Approved";
                case Request.STATUS_COLLECTED: return "Handed Over";
                case Request.STATUS_HANDED_OVER: return "Completed";
                case Request.STATUS_REJECTED: return "Rejected";
                case "expired": return "Expired";
                default: return Character.toUpperCase(status.charAt(0)) + status.substring(1);
            }
        }
    }
}