package com.example.food_saver.admin.monitoring;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.food_saver.R;
import com.example.food_saver.databinding.ActivityDonationDetailBinding;
import com.example.food_saver.utils.ImageUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DonationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ID = "extra_food_id";

    private ActivityDonationDetailBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDonationDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        String foodId = getIntent().getStringExtra(EXTRA_FOOD_ID);
        if (foodId == null) {
            finish();
            return;
        }
        loadDonation(foodId);
    }

    private void loadDonation(String foodId) {
        db.collection("foodPosts").document(foodId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "This donation no longer exists.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    renderFood(doc);
                    loadRequest(foodId, doc.getString("claimedByNgoId"), doc.getString("claimedByNgoName"));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load donation: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void renderFood(DocumentSnapshot doc) {
        String status = doc.getString("status");
        Long expiresAt = doc.getLong("expiresAt");
        boolean open = "available".equals(status) || "requested".equals(status) || "approved".equals(status);
        if (open && expiresAt != null && expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            status = "expired";
        }
        binding.tvFoodName.setText(valueOr(doc.getString("foodName"), "(untitled donation)"));
        binding.tvStatus.setText(statusLabel(status));
        binding.tvStatus.setBackgroundResource("handedOver".equals(status) || "available".equals(status)
                ? R.drawable.bg_badge_success
                : "expired".equals(status) || "removed".equals(status)
                ? R.drawable.bg_badge_error : R.drawable.bg_badge_muted);

        byte[] imageBytes = ImageUtils.decodeBase64ToBytes(doc.getString("imageUrl"));
        if (imageBytes != null && imageBytes.length > 0) {
            Glide.with(this).load(imageBytes).centerCrop().into(binding.ivFood);
        }

        LinearLayout rows = binding.foodRows;
        rows.removeAllViews();
        addRow(rows, "Donor", doc.getString("donorName"));
        addRow(rows, "Quantity", doc.getString("quantity"));
        addRow(rows, "Description", doc.getString("description"));
        addRow(rows, "Pickup location", doc.getString("pickupLocation"));
        addRow(rows, "Pickup time", doc.getString("pickupWindow"));
        addRow(rows, "Posted on", formatDate(doc.getLong("postedAt")));
        addRow(rows, "Expires on", formatDate(doc.getLong("expiresAt")));
    }

    private void loadRequest(String foodId, String claimedNgoId, String claimedNgoName) {
        db.collection("requests").whereEqualTo("foodPostId", foodId).get()
                .addOnSuccessListener(snapshot -> {
                    DocumentSnapshot chosen = null;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if ("rejected".equals(status) || "cancelled".equals(status)) continue;
                        if (chosen == null || (claimedNgoId != null && claimedNgoId.equals(doc.getString("ngoId")))) {
                            chosen = doc;
                        }
                    }
                    renderDelivery(chosen, claimedNgoName);
                    showContent();
                })
                .addOnFailureListener(e -> {
                    renderDelivery(null, claimedNgoName);
                    showContent();
                    Toast.makeText(this, "Couldn't load delivery details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void renderDelivery(DocumentSnapshot request, String claimedNgoName) {
        LinearLayout rows = binding.deliveryRows;
        rows.removeAllViews();

        if (request == null) {
            if (TextUtils.isEmpty(claimedNgoName)) {
                binding.tvNoNgo.setVisibility(View.VISIBLE);
            } else {
                addRow(rows, "NGO", claimedNgoName);
            }
            return;
        }
        binding.tvNoNgo.setVisibility(View.GONE);

        addRow(rows, "NGO", valueOr(request.getString("ngoName"), claimedNgoName));
        addRow(rows, "Requested on", formatDate(request.getLong("requestedAt")));
        addRow(rows, "Approved on", formatDate(request.getLong("respondedAt")));

        String riderName = request.getString("riderName");
        if (TextUtils.isEmpty(riderName)) {
            addRow(rows, "Rider", "Rider details not added yet");
        } else {
            addRow(rows, "Rider name", riderName);
            addRow(rows, "Rider phone", request.getString("riderPhone"));
            addRow(rows, "Vehicle number", request.getString("vehicleNumber"));
            addRow(rows, "Arrival time", request.getString("arrivalTime"));
        }

        Boolean handedOver = request.getBoolean("donorConfirmedHandover");
        Boolean received = request.getBoolean("ngoConfirmedReceived");
        addRow(rows, "Donor handover", handedOver != null && handedOver ? "\u2705 Confirmed" : "\u23F3 Pending");
        addRow(rows, "NGO received", received != null && received ? "\u2705 Confirmed" : "\u23F3 Pending");
    }

    private void addRow(LinearLayout container, String label, String value) {
        if (TextUtils.isEmpty(value)) return;

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getColor(R.color.text_muted));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(12);
        container.addView(labelView, labelParams);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(getColor(R.color.text_dark));
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = dp(2);
        container.addView(valueView, valueParams);
    }

    private void showContent() {
        binding.progressBar.setVisibility(View.GONE);
        binding.content.setVisibility(View.VISIBLE);
    }

    private String formatDate(Long millis) {
        if (millis == null || millis <= 0) return null;
        return DateFormat.format("dd MMM yyyy, hh:mm a", millis).toString();
    }

    private String statusLabel(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "available": return "Available";
            case "requested": return "Requested";
            case "approved": return "Approved";
            case "collected": return "Handed Over";
            case "handedOver": return "Completed";
            case "expired": return "Expired";
            default: return status.isEmpty() ? status : Character.toUpperCase(status.charAt(0)) + status.substring(1);
        }
    }

    private String valueOr(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
