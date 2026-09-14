package com.example.food_saver.admin.violations;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityPolicyViolationsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PolicyViolationsActivity extends AppCompatActivity {

    private ActivityPolicyViolationsBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private PolicyViolationsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPolicyViolationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefresh.setOnClickListener(v -> loadViolations());

        adapter = new PolicyViolationsAdapter(new PolicyViolationsAdapter.Listener() {
            @Override
            public void onFlagDonor(PolicyViolation violation) {
                flagDonor(violation);
            }

            @Override
            public void onDismiss(PolicyViolation violation) {
                markReviewed(violation, "Dismissed");
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        loadViolations();
    }

    private void loadViolations() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);

        // Deliberately no .orderBy() here — combining it with .whereEqualTo()
        // on a different field would need a Firestore composite index.
        // Sorting the (small) result list client-side avoids that entirely.
        db.collection("policyViolations")
                .whereEqualTo("reviewed", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    List<PolicyViolation> violations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        PolicyViolation violation = doc.toObject(PolicyViolation.class);
                        violation.setViolationId(doc.getId());
                        violations.add(violation);
                    }
                    violations.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    adapter.submitList(violations);
                    boolean isEmpty = violations.isEmpty();
                    binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    String message = e.getMessage() != null ? e.getMessage() : "Failed to load flagged content";
                    binding.tvEmptyState.setText(message);
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                });
    }

    /** Marks the donor's account as flagged (visible in User Management) and closes out this violation. */
    private void flagDonor(PolicyViolation violation) {
        if (violation.getDonorId() == null) {
            markReviewed(violation, "Flagged (no donor id on record)");
            return;
        }

        db.collection("users").document(violation.getDonorId())
                .update(
                        "flagged", true,
                        "flagCount", FieldValue.increment(1)
                )
                .addOnSuccessListener(unused -> markReviewed(violation,
                        "Flagged " + safeName(violation)))
                .addOnFailureListener(e -> Snackbar.make(binding.getRoot(),
                        "Could not flag donor: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    private void markReviewed(PolicyViolation violation, String successMessage) {
        db.collection("policyViolations").document(violation.getViolationId())
                .update("reviewed", true)
                .addOnSuccessListener(unused -> {
                    Snackbar.make(binding.getRoot(), successMessage, Snackbar.LENGTH_SHORT).show();
                    loadViolations();
                })
                .addOnFailureListener(e -> Snackbar.make(binding.getRoot(),
                        "Action failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    private String safeName(PolicyViolation violation) {
        return violation.getDonorName() != null ? violation.getDonorName() : "donor";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
