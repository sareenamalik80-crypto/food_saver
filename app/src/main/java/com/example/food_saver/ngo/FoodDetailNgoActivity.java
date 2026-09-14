package com.example.food_saver.ngo;
import com.example.food_saver.utils.ImageUtils;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.food_saver.R;
import com.example.food_saver.databinding.ActivityFoodDetailNgoBinding;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.models.FoodRequest;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class FoodDetailNgoActivity extends AppCompatActivity {

    public static final String EXTRA_DONOR_NAME = "extra_donor_name";

    private ActivityFoodDetailNgoBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private ListenerRegistration postListener;
    private ListenerRegistration requestListener;

    private String foodId;
    private String myNgoId;
    private String myNgoName = "My NGO"; // overwritten once the saved NGO profile loads
    private FoodPost currentPost;
    private FoodRequest currentRequest;
    private String lastRatedDonorId; // avoids re-fetching rating stats on every post update

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFoodDetailNgoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        foodId = getIntent().getStringExtra(NgoDashboardActivity.EXTRA_FOOD_ID);
        myNgoId = FirebaseAuth.getInstance().getUid();

        if (myNgoId == null) {
            // FirebaseAuth session isn't ready yet (or NGO isn't actually
            // logged in) — proceeding would create a request/claim with a
            // null ngoId that this NGO could never recognize as "mine"
            // afterwards. Bail out instead of silently writing broken data.
            Toast.makeText(this, "You're not logged in — please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        repository.getNgoProfile(myNgoId, new FoodRepository.ProfileCallback() {
            @Override
            public void onProfile(String name, String phone, String address) {
                if (name != null && !name.trim().isEmpty()) {
                    myNgoName = name;
                }
            }

            @Override
            public void onError(String message) {
                // Keep the "My NGO" fallback — non-critical for this screen.
            }
        });

        postListener = repository.listenPost(foodId, new FoodRepository.SinglePostCallback() {
            @Override
            public void onPost(FoodPost post) {
                currentPost = post;
                renderPost(post);
                updateHandoverUi();
                loadDonorRatingIfNeeded(post.getDonorId());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(FoodDetailNgoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Dual-confirmation handover: this feeds the "Mark as Received"
        // button + note, tracked separately from the post since the
        // confirmation flags live on the linked "requests" document, not
        // on the FoodPost itself (mirrors the Donor module's design).
        requestListener = repository.listenRequestForPost(foodId, myNgoId, new FoodRepository.RequestCallback() {
            @Override
            public void onRequest(FoodRequest request) {
                currentRequest = request;
                updateHandoverUi();
            }

            @Override
            public void onError(Exception e) {
                // Non-critical for the main screen — the primary post
                // status/actions above still work without this.
            }
        });

        binding.btnMarkReceived.setOnClickListener(v -> onMarkReceived());
        binding.btnCancelRequest.setOnClickListener(v -> onCancelRequest());
    }

    /** Shows the donor's average rating + count next to their name — fetched once per donor, not on every post update. */
    private void loadDonorRatingIfNeeded(String donorId) {
        if (donorId == null || donorId.equals(lastRatedDonorId)) return;
        lastRatedDonorId = donorId;

        repository.getDonorRatingStats(donorId, new FoodRepository.RatingStatsCallback() {
            @Override
            public void onStats(float average, int count) {
                if (count == 0) {
                    binding.tvDonorRating.setText("No ratings yet");
                } else {
                    binding.tvDonorRating.setText(String.format(Locale.getDefault(),
                            "\u2b50 %.1f (%d rating%s)", average, count, count == 1 ? "" : "s"));
                }
            }

            @Override
            public void onError(String message) {
                binding.tvDonorRating.setText("");
            }
        });
    }

    private void renderPost(FoodPost post) {
        binding.tvFoodName.setText(post.getFoodName());
        binding.tvQuantity.setText("Quantity: " + post.getQuantity());
        binding.tvPostedBy.setText("Posted by: " + post.getDonorName());
        binding.tvDescription.setText(post.getDescription());
        binding.tvPostedTime.setText("Posted: " + DateFormat.format("dd MMM, hh:mm a", post.getPostedAt()));

        long msLeft = post.getExpiresAt() - System.currentTimeMillis();
        if (msLeft > 0) {
            long hrs = msLeft / (1000 * 60 * 60);
            long mins = (msLeft / (1000 * 60)) % 60;
            binding.tvExpiresIn.setText("Expires in: " + hrs + " hrs " + mins + " min");
        } else {
            binding.tvExpiresIn.setText("Expired");
        }

        byte[] imageBytes = ImageUtils.decodeBase64ToBytes(post.getImageUrl());
        Glide.with(this).load(imageBytes).centerCrop().into(binding.ivFoodImage);

        boolean isMine = myNgoId != null && myNgoId.equals(post.getClaimedByNgoId());

        binding.btnOpenChat.setVisibility(View.GONE);
        binding.btnCancelRequest.setVisibility(View.GONE);

        switch (post.getStatus()) {
            case "available":
                binding.tvStatusChip.setText("Available");
                setActionButton("Request This Food", true, this::onRequestFood);
                break;

            case "requested":
                if (isMine) {
                    binding.tvStatusChip.setText("Pending Approval");
                    setActionButton("Waiting for Donor's Approval", false, null);
                    binding.btnCancelRequest.setVisibility(View.VISIBLE);
                } else {
                    binding.tvStatusChip.setText("Claimed");
                    setActionButton("Already Claimed by Another NGO", false, null);
                }
                break;

            case "approved":
                binding.tvStatusChip.setText("Approved");
                if (isMine) {
                    binding.btnOpenChat.setVisibility(View.VISIBLE);
                    binding.btnOpenChat.setOnClickListener(v -> openChat(post));
                    setActionButton("Fill Delivery Details", true, () -> openDeliveryForm(post));
                } else {
                    setActionButton("Claimed by Another NGO", false, null);
                }
                break;

            case "collected":
                binding.tvStatusChip.setText("Food Collected");
                if (isMine) {
                    binding.btnOpenChat.setVisibility(View.VISIBLE);
                    binding.btnOpenChat.setOnClickListener(v -> openChat(post));
                    setActionButton("Awaiting Handover Confirmation", false, null);
                } else {
                    setActionButton("Collected by Another NGO", false, null);
                }
                break;

            case "handedOver":
                binding.tvStatusChip.setText("Handed Over");
                if (isMine) {
                    setActionButton("Rate Donor", true, () -> openRateDonor(post));
                } else {
                    setActionButton("Completed", false, null);
                }
                break;
        }
    }

    /**
     * Dual-confirmation handover UI: shows "Mark as Received" while this
     * NGO hasn't confirmed yet, or a status note once it has (either
     * waiting on the donor, or fully complete). Runs independently of
     * renderPost() because it depends on both the FoodPost's status AND
     * the linked request's confirmation flags.
     */
    private void updateHandoverUi() {
        if (currentPost == null) return;

        boolean isMine = myNgoId != null && myNgoId.equals(currentPost.getClaimedByNgoId());
        String status = currentPost.getStatus();
        boolean isDone = "handedOver".equals(status);
        boolean isActive = isMine && ("approved".equals(status) || "collected".equals(status) || isDone);

        if (!isActive || currentRequest == null) {
            binding.btnMarkReceived.setVisibility(View.GONE);
            binding.tvHandoverNote.setVisibility(View.GONE);
            return;
        }

        boolean ngoConfirmed = currentRequest.isNgoConfirmedReceived();
        boolean canConfirm = !isDone && !ngoConfirmed;
        boolean showNote = ngoConfirmed || isDone;

        binding.btnMarkReceived.setVisibility(canConfirm ? View.VISIBLE : View.GONE);
        binding.tvHandoverNote.setVisibility(showNote ? View.VISIBLE : View.GONE);

        if (showNote) {
            if (isDone) {
                binding.tvHandoverNote.setText("\u2705 Handover complete");
                binding.tvHandoverNote.setTextColor(getColor(R.color.success_green));
            } else {
                binding.tvHandoverNote.setText("You confirmed \u2014 waiting for the donor to confirm handover");
                binding.tvHandoverNote.setTextColor(getColor(R.color.gray_secondary));
            }
        }
    }

    private void onMarkReceived() {
        if (currentRequest == null) return;
        repository.markReceivedByNgo(currentRequest.getRequestId(), foodId, new FoodRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(FoodDetailNgoActivity.this, "Marked as received", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(FoodDetailNgoActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCancelRequest() {
        if (currentRequest == null) {
            Toast.makeText(this, "Please wait a moment and try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        repository.cancelRequest(currentRequest.getRequestId(), foodId, new FoodRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(FoodDetailNgoActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(FoodDetailNgoActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setActionButton(String text, boolean enabled, Runnable onClick) {
        binding.btnPrimaryAction.setText(text);
        binding.btnPrimaryAction.setEnabled(enabled);
        binding.btnPrimaryAction.setAlpha(enabled ? 1f : 0.6f);
        binding.btnPrimaryAction.setOnClickListener(enabled && onClick != null ? v -> onClick.run() : null);
    }

    private void onRequestFood() {
        if (myNgoId == null) {
            Toast.makeText(this, "You're not logged in — please log in again.", Toast.LENGTH_LONG).show();
            return;
        }
        repository.requestFood(foodId, myNgoId, myNgoName, currentPost.getDonorId(),
                new FoodRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(FoodDetailNgoActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(FoodDetailNgoActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // NOTE: "Mark as Collected" button aap donor se food lete waqt kahin aur
    // trigger karengi (jaise ek confirmation dialog se) — yahan directly
    // repository.markCollected(foodId, callback) call kar sakti hain.

    private void openDeliveryForm(FoodPost post) {
        Intent intent = new Intent(this, DeliveryDetailsActivity.class);
        intent.putExtra(NgoDashboardActivity.EXTRA_FOOD_ID, post.getFoodId());
        startActivity(intent);
    }

    private void openChat(FoodPost post) {
        if (currentRequest == null) {
            Toast.makeText(this, "Please wait a moment and try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_REQUEST_ID, currentRequest.getRequestId());
        intent.putExtra(EXTRA_DONOR_NAME, post.getDonorName());
        startActivity(intent);
    }

    private void openRateDonor(FoodPost post) {
        Intent intent = new Intent(this, RateDonorActivity.class);
        intent.putExtra(NgoDashboardActivity.EXTRA_FOOD_ID, post.getFoodId());
        intent.putExtra(EXTRA_DONOR_NAME, post.getDonorName());
        intent.putExtra(RateDonorActivity.EXTRA_DONOR_ID, post.getDonorId());   // 👈 yeh nayi line add karein
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postListener != null) postListener.remove();
        if (requestListener != null) requestListener.remove();
    }
}
