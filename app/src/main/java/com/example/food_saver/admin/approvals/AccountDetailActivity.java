package com.example.food_saver.admin.approvals;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.food_saver.databinding.ActivityAccountDetailBinding;
import com.example.food_saver.utils.ImageUtils;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountDetailActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "extra_uid";

    private ActivityAccountDetailBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;
    private String currentStatus = "pending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        uid = getIntent().getStringExtra(EXTRA_UID);
        if (uid == null) {
            finish();
            return;
        }

        binding.btnApprove.setOnClickListener(v -> updateStatus("verified"));
        binding.btnReject.setOnClickListener(v -> updateStatus("rejected"));

        loadAccount();
    }

    private void loadAccount() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "This account no longer exists.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    PendingAccount account = doc.toObject(PendingAccount.class);
                    if (account == null) {
                        finish();
                        return;
                    }
                    account.setUid(doc.getId());
                    render(account);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void render(PendingAccount account) {
        boolean isNgo = "ngo".equalsIgnoreCase(account.getRole());

        binding.tvName.setText(account.getName() != null ? account.getName() : "(no name)");
        binding.tvRole.setText(isNgo ? "NGO" : "DONOR");
        binding.tvEmail.setText(account.getEmail() != null ? account.getEmail() : "\u2014");
        binding.tvPhone.setText(account.getPhone() != null ? account.getPhone() : "\u2014");
        binding.tvAddress.setText(account.getAddress() != null ? account.getAddress() : "\u2014");

        currentStatus = account.getStatus() != null ? account.getStatus() : "pending";
        binding.tvStatus.setText("Status: " + capitalize(currentStatus));
        switch (currentStatus) {
            case "verified":
                binding.tvStatus.setTextColor(getColor(com.example.food_saver.R.color.success_green));
                break;
            case "rejected":
                binding.tvStatus.setTextColor(getColor(com.example.food_saver.R.color.error_red));
                break;
            default:
                binding.tvStatus.setTextColor(getColor(com.example.food_saver.R.color.gray_secondary));
        }

        binding.tvDocumentLabel.setText(isNgo ? "NGO License Certificate" : "Food Authority Letter");

        String docBase64 = account.getOrgDocUrl();
        if (docBase64 != null && !docBase64.isEmpty()) {
            byte[] imageBytes = ImageUtils.decodeBase64ToBytes(docBase64);
            Glide.with(this).load(imageBytes).into(binding.ivDocument);
            binding.ivDocument.setVisibility(View.VISIBLE);
            binding.tvNoDocument.setVisibility(View.GONE);
        } else {
            binding.ivDocument.setVisibility(View.GONE);
            binding.tvNoDocument.setVisibility(View.VISIBLE);
        }

        // Once a decision has already been made, keep the buttons available
        // so admin can change their mind (matches the list screen's
        // Approve/Revoke behaviour) — just relabel for clarity.
        binding.btnApprove.setEnabled(!"verified".equals(currentStatus));
        binding.btnReject.setEnabled(!"rejected".equals(currentStatus));
    }

    private void updateStatus(String newStatus) {
        db.collection("users").document(uid)
                .update("status", newStatus)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "verified".equals(newStatus) ? "Account approved" : "Account rejected",
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Action failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
